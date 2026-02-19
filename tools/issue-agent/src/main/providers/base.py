from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Protocol

from ..models import ParsedCommand


@dataclass(slots=True)
class CommitAction:
    path: str
    content: str


@dataclass(slots=True)
class BranchCommitResult:
    branch: str
    commit_sha: str


@dataclass(slots=True)
class MergeRequestResult:
    mr_id: str


class ProviderAdapter(Protocol):
    def parse_command(self, note_body: str) -> ParsedCommand | None: ...

    def create_branch_and_commit_plan(
        self,
        *,
        issue_id: str,
        branch: str,
        commit_message: str,
        files: list[CommitAction],
    ) -> BranchCommitResult: ...

    def commit_to_branch(
        self,
        *,
        branch: str,
        commit_message: str,
        files: list[CommitAction],
    ) -> str: ...

    def create_or_update_draft_mr(
        self,
        *,
        issue_id: str,
        source_branch: str,
        mode: str,
        title: str,
        description: str,
    ) -> MergeRequestResult: ...

    def upsert_bot_comment(
        self,
        *,
        scope: str,
        target_id: str,
        stage_key: str,
        body: str,
    ) -> str: ...

    def append_issue_or_mr_note(
        self,
        *,
        scope: str,
        target_id: str,
        body: str,
    ) -> str: ...

    def read_issue_context(self, issue_id: str) -> str: ...

    def repo_root(self) -> Path: ...
