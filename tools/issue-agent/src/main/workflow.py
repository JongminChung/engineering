from dataclasses import dataclass

from .models import NoteContext, Status, WorkflowState
from .providers.base import CommitAction, ProviderAdapter
from .skills import AGENT_ORDER, load_agent_skills
from .templates import (
    PlanContext,
    default_tasks,
    render_plan_markdown,
    render_process_markdown,
    render_progress_markdown,
)


def _branch_name(issue_id: str, mode: str) -> str:
    return f"agent/issue-{issue_id}"


def _find_task(state: WorkflowState, agent: str):
    for task in state.task_queue:
        if task.agent == agent:
            return task
    msg = f"task for agent '{agent}' not found"
    raise ValueError(msg)


@dataclass(slots=True)
class WorkflowEngine:
    adapter: ProviderAdapter

    def _upsert_stage_comment(self, scope: str, target_id: str, stage_key: str,
                              summary: str) -> None:
        body = "\n".join(
            [
                f"[{stage_key}] {summary}",
                "What to do differently? Reply with @agent differently: ...",
                "If no change, reply @agent proceed",
            ],
        )
        self.adapter.upsert_bot_comment(scope=scope, target_id=target_id, stage_key=stage_key,
                                        body=body)

    def _plan_and_progress_files(self, state: WorkflowState) -> list[CommitAction]:
        plan = render_plan_markdown(
            PlanContext(issue_id=state.issue_id, mode=state.mode, branch=state.branch))
        progress = render_progress_markdown(state)
        process = render_process_markdown(state)
        issue_dir = f"issues/{state.issue_id}"
        return [
            CommitAction(path=f"{issue_dir}/PLAN.md", content=plan),
            CommitAction(path=f"{issue_dir}/PROGRESS.md", content=progress),
            CommitAction(path=f"{issue_dir}/process.md", content=process),
        ]

    def handle_note(self, note: NoteContext, states: dict[str, WorkflowState]) -> str:
        parsed = self.adapter.parse_command(note.body)
        if parsed is None:
            return "ignored: command not found"

        if note.scope == "issue":
            if parsed.command not in ("fix", "feat"):
                return "ignored: only @agent fix|feat is allowed on issues"
            if not note.issue_id:
                return "error: issue_id is required"
            return self._handle_issue_plan(note, parsed.command, states)

        if note.scope in ("mr", "pr"):
            if parsed.command not in ("proceed", "differently", "stop"):
                return "ignored: only @agent proceed|differently|stop is allowed on MR/PR"
            if not note.mr_id:
                return "error: mr_id is required"
            return self._handle_mr_command(note, parsed.command, parsed.instruction, states)

        return "ignored: unsupported scope"

    def _handle_issue_plan(self, note: NoteContext, mode: str,
                           states: dict[str, WorkflowState]) -> str:
        issue_id = str(note.issue_id)
        branch = _branch_name(issue_id, mode)
        state = WorkflowState(
            mode=mode,
            issue_id=issue_id,
            branch=branch,
            mr_id="",
            current_stage="plan",
            task_queue=default_tasks(),
        )
        files = self._plan_and_progress_files(state)

        self.adapter.create_branch_and_commit_plan(
            issue_id=issue_id,
            branch=branch,
            commit_message=f"chore(plan): bootstrap plan for #{issue_id}",
            files=files,
        )

        mr = self.adapter.create_or_update_draft_mr(
            issue_id=issue_id,
            source_branch=branch,
            mode=mode,
            title=f"Draft: [agent][{mode}] issue #{issue_id}",
            description=f"Closes #{issue_id}",
        )

        state.mr_id = mr.mr_id
        states[mr.mr_id] = state
        self._upsert_stage_comment("mr", mr.mr_id, "plan", "Plan created")
        return f"ok: issue #{issue_id} planned with draft mr {mr.mr_id}"

    def _handle_mr_command(
        self,
        note: NoteContext,
        command: str,
        instruction: str | None,
        states: dict[str, WorkflowState],
    ) -> str:
        mr_id = str(note.mr_id)
        state = states.get(mr_id)
        if state is None:
            return f"error: state not found for mr {mr_id}"

        if command == "stop":
            state.current_stage = "stopped"
            self._upsert_stage_comment("mr", mr_id, "stopped", "Execution stopped by user")
            return "ok: stopped"

        if command == "differently":
            text = instruction or "(empty)"
            state.decision_log.append(text)
            state.current_stage = "plan"
            for task in state.task_queue:
                task.status = Status.PENDING
                task.result = ""
            files = self._plan_and_progress_files(state)
            self.adapter.commit_to_branch(
                branch=state.branch,
                commit_message="chore(plan): incorporate differently feedback",
                files=files,
            )
            self._upsert_stage_comment("mr", mr_id, "plan",
                                       "Plan updated from differently feedback")
            return "ok: differently applied"

        for agent in AGENT_ORDER:
            skillset = load_agent_skills(self.adapter.repo_root(), agent)
            task = _find_task(state, agent)
            task.status = Status.RUNNING
            state.current_stage = task.agent  # type: ignore[assignment]
            self._upsert_stage_comment("mr", mr_id, task.agent,
                                       f"{agent} started ({len(skillset.files)} skills)")

            task.status = Status.DONE
            task.result = f"completed with {len(skillset.files)} skill files"
            files = self._plan_and_progress_files(state)
            self.adapter.commit_to_branch(
                branch=state.branch,
                commit_message=f"chore(agent): {agent} stage update",
                files=files,
            )
            self._upsert_stage_comment("mr", mr_id, task.agent, f"{agent} completed")

        state.current_stage = "completed"
        files = self._plan_and_progress_files(state)
        self.adapter.commit_to_branch(
            branch=state.branch,
            commit_message="chore(agent): workflow completed",
            files=files,
        )
        self._upsert_stage_comment("mr", mr_id, "completed", "Workflow completed")
        return "ok: workflow completed"
