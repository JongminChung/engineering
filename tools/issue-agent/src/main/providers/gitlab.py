from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path

from .base import (
    BranchCommitResult,
    CommitAction,
    MergeRequestResult,
)
from ..models import ParsedCommand
from ..parser import parse_command


@dataclass(slots=True)
class InMemoryGitLabAdapter:
    root: Path
    issue_contexts: dict[str, str] = field(default_factory=dict)
    branches: dict[str, list[dict[str, str]]] = field(default_factory=dict)
    mrs: dict[str, dict[str, str]] = field(default_factory=dict)
    comments: dict[tuple[str, str, str], str] = field(default_factory=dict)
    notes: list[dict[str, str]] = field(default_factory=list)

    def parse_command(self, note_body: str) -> ParsedCommand | None:
        return parse_command(note_body)

    def create_branch_and_commit_plan(
        self,
        *,
        issue_id: str,
        branch: str,
        commit_message: str,
        files: list[CommitAction],
    ) -> BranchCommitResult:
        sha = f"plan-{issue_id}-{len(self.branches) + 1}"
        self.branches.setdefault(branch, []).append(
            {
                "sha": sha,
                "message": commit_message,
                "files": ",".join(file.path for file in files),
            },
        )
        return BranchCommitResult(branch=branch, commit_sha=sha)

    def commit_to_branch(
        self,
        *,
        branch: str,
        commit_message: str,
        files: list[CommitAction],
    ) -> str:
        sha = f"commit-{branch}-{len(self.branches.get(branch, [])) + 1}"
        self.branches.setdefault(branch, []).append(
            {
                "sha": sha,
                "message": commit_message,
                "files": ",".join(file.path for file in files),
            },
        )
        return sha

    def create_or_update_draft_mr(
        self,
        *,
        issue_id: str,
        source_branch: str,
        mode: str,
        title: str,
        description: str,
    ) -> MergeRequestResult:
        mr_id = issue_id
        self.mrs[mr_id] = {
            "source_branch": source_branch,
            "mode": mode,
            "title": title,
            "description": description,
            "draft": "true",
        }
        return MergeRequestResult(mr_id=mr_id)

    def upsert_bot_comment(
        self,
        *,
        scope: str,
        target_id: str,
        stage_key: str,
        body: str,
    ) -> str:
        key = (scope, target_id, stage_key)
        self.comments[key] = body
        return f"{scope}:{target_id}:{stage_key}"

    def append_issue_or_mr_note(
        self,
        *,
        scope: str,
        target_id: str,
        body: str,
    ) -> str:
        self.notes.append({"scope": scope, "target_id": target_id, "body": body})
        return f"{scope}:{target_id}:{len(self.notes)}"

    def read_issue_context(self, issue_id: str) -> str:
        return self.issue_contexts.get(issue_id, "")

    def repo_root(self) -> Path:
        return self.root
