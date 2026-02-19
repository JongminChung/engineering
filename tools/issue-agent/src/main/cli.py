from __future__ import annotations

import argparse
import json
import os
from pathlib import Path

from dotenv import load_dotenv

from .gitlab_api import discover_origin2_remote, discover_repo_root, parse_gitlab_project_path
from .models import NoteContext
from .providers.gitlab import InMemoryGitLabAdapter
from .providers.gitlab_api import GitLabApiAdapter
from .providers.local_fs import LocalFsAdapter
from .models import Status, TaskItem, WorkflowState
from .workflow import WorkflowEngine


def _parser() -> argparse.ArgumentParser:
  parser = argparse.ArgumentParser(prog="issue-agent")
  sub = parser.add_subparsers(dest="command", required=True)

  run = sub.add_parser("run")
  run.add_argument("--provider", choices=["gitlab"], default="gitlab")
  run.add_argument("--scope", choices=["issue", "mr", "pr"], required=True)
  run.add_argument("--project-id", required=True)
  run.add_argument("--actor", required=True)
  run.add_argument("--note-id", required=True)
  run.add_argument("--issue-id")
  run.add_argument("--mr-id")
  run.add_argument("--body", required=True)

  bootstrap = sub.add_parser("bootstrap-origin2")
  bootstrap.add_argument("--title", required=True)
  bootstrap.add_argument("--description", required=True)
  bootstrap.add_argument("--mode", choices=["fix", "feat"], required=True)
  bootstrap.add_argument("--actor", default="issue-agent")
  bootstrap.add_argument("--api-base", default="https://gitlab.com")
  bootstrap.add_argument("--no-dotenv", action="store_true")
  bootstrap.add_argument("--enable-issues-if-disabled", action="store_true")
  bootstrap.add_argument("--issue-id")

  local = sub.add_parser("local-workflow")
  local.add_argument("--issue-id", required=True)
  local.add_argument("--action", choices=["plan", "proceed", "differently", "stop"], required=True)
  local.add_argument("--mode", choices=["fix", "feat"], default="fix")
  local.add_argument("--instruction")
  local.add_argument("--actor", default="local-user")
  local.add_argument("--project-id", default="local")
  local.add_argument("--note-id", default="local-note")

  return parser


def _state_file(root: Path) -> Path:
  return root / ".issue-agent" / "local-states.json"


def _dump_states(states: dict[str, WorkflowState], path: Path) -> None:
  path.parent.mkdir(parents=True, exist_ok=True)
  payload = {}
  for mr_id, state in states.items():
    payload[mr_id] = {
      "mode": state.mode,
      "issue_id": state.issue_id,
      "branch": state.branch,
      "mr_id": state.mr_id,
      "current_stage": state.current_stage,
      "task_queue": [
        {
          "id": task.id,
          "agent": task.agent,
          "title": task.title,
          "status": task.status.value,
          "result": task.result,
        }
        for task in state.task_queue
      ],
      "decision_log": state.decision_log,
      "bot_comment_keys": state.bot_comment_keys,
    }
  path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def _load_states(path: Path) -> dict[str, WorkflowState]:
  if not path.exists():
    return {}
  raw = json.loads(path.read_text(encoding="utf-8"))
  states: dict[str, WorkflowState] = {}
  for mr_id, item in raw.items():
    task_queue = [
      TaskItem(
        id=task["id"],
        agent=task["agent"],
        title=task["title"],
        status=Status(task["status"]),
        result=task.get("result", ""),
      )
      for task in item.get("task_queue", [])
    ]
    states[mr_id] = WorkflowState(
      mode=item["mode"],
      issue_id=item["issue_id"],
      branch=item["branch"],
      mr_id=item["mr_id"],
      current_stage=item.get("current_stage", "plan"),
      task_queue=task_queue,
      decision_log=item.get("decision_log", []),
      bot_comment_keys=item.get("bot_comment_keys", {}),
    )
  return states


def _run_local(args: argparse.Namespace) -> int:
  repo_root = discover_repo_root(Path.cwd())
  adapter = InMemoryGitLabAdapter(root=repo_root)
  engine = WorkflowEngine(adapter=adapter)
  states = {}

  note = NoteContext(
    provider="gitlab",
    scope=args.scope,
    project_id=args.project_id,
    actor=args.actor,
    note_id=args.note_id,
    issue_id=args.issue_id,
    mr_id=args.mr_id,
    body=args.body,
  )

  result = engine.handle_note(note, states)
  output = {
    "result": result,
    "states": {
      key: {
        "mode": state.mode,
        "issue_id": state.issue_id,
        "branch": state.branch,
        "mr_id": state.mr_id,
        "current_stage": state.current_stage,
      }
      for key, state in states.items()
    },
    "comments": [
      {"scope": scope, "target_id": target_id, "stage": stage, "body": body}
      for (scope, target_id, stage), body in adapter.comments.items()
    ],
  }

  print(json.dumps(output, ensure_ascii=False, indent=2))
  return 0


def _bootstrap_origin2(args: argparse.Namespace) -> int:
  repo_root = discover_repo_root(Path.cwd())
  if not args.no_dotenv and os.getenv("ISSUE_AGENT_DISABLE_DOTENV") != "1":
    load_dotenv(repo_root / ".env")

  token = os.getenv("GITLAB_TOKEN") or os.getenv("GITLAB_PRIVATE_TOKEN")
  if not token:
    raise SystemExit("GITLAB_TOKEN (or GITLAB_PRIVATE_TOKEN) is required")

  remote = discover_origin2_remote(repo_root)
  project = parse_gitlab_project_path(remote)
  adapter = GitLabApiAdapter(
    root=repo_root,
    base_url=args.api_base,
    token=token,
    project=project,
  )
  engine = WorkflowEngine(adapter=adapter)
  states = {}

  issue_id = args.issue_id
  if not issue_id:
    if not adapter.issues_enabled():
      if args.enable_issues_if_disabled:
        adapter.enable_issues()
      else:
        raise SystemExit(
          "GitLab issues are disabled for this project. "
          "Enable project issues or run with --enable-issues-if-disabled, "
          "or pass --issue-id to reuse an existing issue.",
        )
    issue_id = adapter.create_issue(args.title, args.description)
  note = NoteContext(
    provider="gitlab",
    scope="issue",
    project_id=project.project_path,
    actor=args.actor,
    note_id=f"bootstrap-{issue_id}",
    issue_id=issue_id,
    body=f"@agent {args.mode}",
  )

  result = engine.handle_note(note, states)
  state = next((item for item in states.values() if item.issue_id == issue_id), None)
  output = {
    "result": result,
    "remote": remote,
    "project": project.project_path,
    "issue_id": issue_id,
    "mr_id": state.mr_id if state else None,
    "branch": state.branch if state else None,
  }
  print(json.dumps(output, ensure_ascii=False, indent=2))
  return 0


def _run_local_workflow(args: argparse.Namespace) -> int:
  repo_root = discover_repo_root(Path.cwd())
  adapter = LocalFsAdapter(root=repo_root)
  engine = WorkflowEngine(adapter=adapter)
  states_path = _state_file(repo_root)
  states = _load_states(states_path)

  issue_id = str(args.issue_id)
  action = args.action
  scope = "issue" if action == "plan" else "mr"
  body = {
    "plan": f"@agent {args.mode}",
    "proceed": "@agent proceed",
    "stop": "@agent stop",
    "differently": f"@agent differently: {args.instruction or ''}".strip(),
  }[action]

  note = NoteContext(
    provider="gitlab",
    scope=scope,
    project_id=args.project_id,
    actor=args.actor,
    note_id=args.note_id,
    issue_id=issue_id if scope == "issue" else None,
    mr_id=issue_id if scope == "mr" else None,
    body=body,
  )
  result = engine.handle_note(note, states)
  _dump_states(states, states_path)

  state = states.get(issue_id)
  output = {
    "result": result,
    "issue_id": issue_id,
    "mr_id": issue_id,
    "state_file": str(states_path),
    "state": {
      "current_stage": state.current_stage if state else None,
      "branch": state.branch if state else None,
      "task_queue": [
        {"id": task.id, "agent": task.agent, "status": task.status.value}
        for task in (state.task_queue if state else [])
      ],
    },
  }
  print(json.dumps(output, ensure_ascii=False, indent=2))
  return 0


def main() -> int:
  args = _parser().parse_args()
  if args.command == "run":
    return _run_local(args)
  if args.command == "bootstrap-origin2":
    return _bootstrap_origin2(args)
  if args.command == "local-workflow":
    return _run_local_workflow(args)
  raise SystemExit(f"unknown command: {args.command}")


if __name__ == "__main__":
  raise SystemExit(main())
