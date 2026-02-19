from __future__ import annotations

import json
from dataclasses import dataclass
from datetime import UTC, datetime
from pathlib import Path

from .base import (
    BranchCommitResult,
    CommitAction,
    MergeRequestResult,
)
from ..models import ParsedCommand
from ..parser import parse_command


def _now_id() -> str:
    return datetime.now(UTC).strftime("%Y%m%d%H%M%S")


@dataclass(slots=True)
class LocalFsAdapter:
    root: Path

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
        self._write_files(files)
        sha = self._append_commit_log(branch=branch, commit_message=commit_message, files=files)
        return BranchCommitResult(branch=branch, commit_sha=sha)

    def commit_to_branch(
        self,
        *,
        branch: str,
        commit_message: str,
        files: list[CommitAction],
    ) -> str:
        self._write_files(files)
        return self._append_commit_log(branch=branch, commit_message=commit_message, files=files)

    def create_or_update_draft_mr(
        self,
        *,
        issue_id: str,
        source_branch: str,
        mode: str,
        title: str,
        description: str,
    ) -> MergeRequestResult:
        storage_path = self.root / ".issue-agent" / "local-mrs.json"
        storage_path.parent.mkdir(parents=True, exist_ok=True)
        if storage_path.exists():
            storage = json.loads(storage_path.read_text(encoding="utf-8"))
        else:
            storage = {}

        if issue_id in storage:
            return MergeRequestResult(mr_id=str(storage[issue_id]["mr_id"]))

        mr_id = str(issue_id)
        storage[issue_id] = {
            "mr_id": mr_id,
            "source_branch": source_branch,
            "mode": mode,
            "title": title,
            "description": description,
            "status": "draft",
            "updated_at": datetime.now(UTC).isoformat(),
        }
        storage_path.write_text(
            json.dumps(storage, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )
        return MergeRequestResult(mr_id=mr_id)

    def upsert_bot_comment(
        self,
        *,
        scope: str,
        target_id: str,
        stage_key: str,
        body: str,
    ) -> str:
        marker = f"<!-- issue-agent:{stage_key} -->"
        comments_dir = self.root / ".issue-agent" / "comments"
        comments_dir.mkdir(parents=True, exist_ok=True)
        comment_file = comments_dir / f"{scope}-{target_id}-{stage_key}.md"
        comment_file.write_text(f"{marker}\n{body}\n", encoding="utf-8")
        return str(comment_file)

    def append_issue_or_mr_note(
        self,
        *,
        scope: str,
        target_id: str,
        body: str,
    ) -> str:
        notes_dir = self.root / ".issue-agent" / "comments"
        notes_dir.mkdir(parents=True, exist_ok=True)
        notes_file = notes_dir / f"{scope}-{target_id}-notes.log"
        with notes_file.open("a", encoding="utf-8") as handle:
            handle.write(f"[{datetime.now(UTC).isoformat()}] {body}\n")
        return str(notes_file)

    def read_issue_context(self, issue_id: str) -> str:
        context_file = self.root / "issues" / str(issue_id) / "context.md"
        if not context_file.exists():
            return ""
        return context_file.read_text(encoding="utf-8")

    def repo_root(self) -> Path:
        return self.root

    def _write_files(self, files: list[CommitAction]) -> None:
        for file in files:
            target = self.root / file.path
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(file.content, encoding="utf-8")

    def _append_commit_log(
        self,
        *,
        branch: str,
        commit_message: str,
        files: list[CommitAction],
    ) -> str:
        log_path = self.root / ".issue-agent" / "commits.log"
        log_path.parent.mkdir(parents=True, exist_ok=True)
        sha = f"local-{_now_id()}"
        changed = ",".join(file.path for file in files)
        with log_path.open("a", encoding="utf-8") as handle:
            handle.write(f"{sha}\t{branch}\t{commit_message}\t{changed}\n")
        return sha
