from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path

import gitlab
from gitlab import exceptions as gl_ex
from .base import (
    BranchCommitResult,
    CommitAction,
    MergeRequestResult,
)
from ..gitlab_api import GitLabApiError, GitLabProjectRef
from ..models import ParsedCommand
from ..parser import parse_command


@dataclass(slots=True)
class GitLabApiAdapter:
    root: Path
    base_url: str
    token: str
    project: GitLabProjectRef
    _client: gitlab.Gitlab = field(init=False, repr=False)
    _project: object = field(init=False, repr=False)

    def __post_init__(self) -> None:
        self._client = gitlab.Gitlab(url=self.base_url, private_token=self.token)
        try:
            self._project = self._client.projects.get(self.project.project_path)
        except gl_ex.GitlabError as exc:
            raise GitLabApiError(
                f"Cannot access project '{self.project.project_path}': {exc}") from exc

    def parse_command(self, note_body: str) -> ParsedCommand | None:
        return parse_command(note_body)

    def create_issue(self, title: str, description: str) -> str:
        try:
            issue = self._project.issues.create({"title": title, "description": description})
        except gl_ex.GitlabError as exc:
            raise GitLabApiError(f"Issue creation failed: {exc}") from exc
        return str(issue.iid)

    def issues_enabled(self) -> bool:
        level = str(getattr(self._project, "issues_access_level", "") or "").lower()
        legacy = bool(getattr(self._project, "issues_enabled", True))
        if level:
            return level != "disabled"
        return legacy

    def enable_issues(self) -> None:
        try:
            self._project.issues_access_level = "enabled"
            self._project.save()
        except gl_ex.GitlabError as exc:
            raise GitLabApiError(f"Enable issues failed: {exc}") from exc

    def create_branch_and_commit_plan(
        self,
        *,
        issue_id: str,
        branch: str,
        commit_message: str,
        files: list[CommitAction],
    ) -> BranchCommitResult:
        base_ref = self._resolve_base_ref()
        try:
            self._project.branches.create({"branch": branch, "ref": base_ref})
        except gl_ex.GitlabCreateError as exc:
            message = str(exc)
            if "already exists" in message:
                pass
            elif "invalid reference name" in message:
                fallback_ref = self._resolve_base_ref(force_fallback=True)
                try:
                    self._project.branches.create({"branch": branch, "ref": fallback_ref})
                except gl_ex.GitlabCreateError as fallback_exc:
                    if "already exists" not in str(fallback_exc):
                        raise GitLabApiError(
                            f"Branch creation failed (fallback): {fallback_exc}",
                        ) from fallback_exc
            else:
                raise GitLabApiError(f"Branch creation failed: {exc}") from exc

        sha = self.commit_to_branch(branch=branch, commit_message=commit_message, files=files)
        return BranchCommitResult(branch=branch, commit_sha=sha)

    def _resolve_base_ref(self, *, force_fallback: bool = False) -> str:
        if not force_fallback:
            default_branch = str(getattr(self._project, "default_branch", "") or "").strip()
            if default_branch:
                return default_branch

        try:
            branches = self._project.branches.list(get_all=True)
        except gl_ex.GitlabError as exc:
            raise GitLabApiError(f"Branch list lookup failed: {exc}") from exc

        names = [str(getattr(branch, "name", "") or "") for branch in branches]
        preferred = ["main", "master", "develop"]
        for candidate in preferred:
            if candidate in names:
                return candidate
        for candidate in names:
            if candidate:
                return candidate
        raise GitLabApiError("No available branch found for base ref")

    def commit_to_branch(
        self,
        *,
        branch: str,
        commit_message: str,
        files: list[CommitAction],
    ) -> str:
        actions = []
        for file in files:
            action = "update" if self._file_exists(branch=branch, path=file.path) else "create"
            actions.append(
                {
                    "action": action,
                    "file_path": file.path,
                    "content": file.content,
                },
            )

        try:
            commit = self._project.commits.create(
                {
                    "branch": branch,
                    "commit_message": commit_message,
                    "actions": actions,
                },
            )
        except gl_ex.GitlabError as exc:
            raise GitLabApiError(f"Commit creation failed: {exc}") from exc
        return str(commit.id)

    def _file_exists(self, *, branch: str, path: str) -> bool:
        try:
            self._project.files.get(file_path=path, ref=branch)
            return True
        except gl_ex.GitlabGetError as exc:
            if "404" in str(exc):
                return False
            raise GitLabApiError(f"File lookup failed for '{path}': {exc}") from exc

    def create_or_update_draft_mr(
        self,
        *,
        issue_id: str,
        source_branch: str,
        mode: str,
        title: str,
        description: str,
    ) -> MergeRequestResult:
        try:
            opened = self._project.mergerequests.list(state="opened", target_branch="main",
                                                      get_all=True)
        except gl_ex.GitlabError as exc:
            raise GitLabApiError(f"Merge request lookup failed: {exc}") from exc

        for candidate in opened:
            candidate_source = str(getattr(candidate, "source_branch", "") or "")
            if candidate_source == source_branch:
                return MergeRequestResult(mr_id=str(candidate.iid))

        draft_title = title if title.startswith("Draft:") else f"Draft: {title}"
        try:
            mr = self._project.mergerequests.create(
                {
                    "source_branch": source_branch,
                    "target_branch": "main",
                    "title": draft_title,
                    "description": description,
                    "remove_source_branch": False,
                },
            )
        except gl_ex.GitlabError as exc:
            raise GitLabApiError(f"Merge request creation failed: {exc}") from exc
        return MergeRequestResult(mr_id=str(mr.iid))

    def _notes_resource(self, scope: str, target_id: str):
        if scope == "mr":
            mr = self._project.mergerequests.get(target_id)
            return mr.notes
        issue = self._project.issues.get(target_id)
        return issue.notes

    def upsert_bot_comment(
        self,
        *,
        scope: str,
        target_id: str,
        stage_key: str,
        body: str,
    ) -> str:
        marker = f"<!-- issue-agent:{stage_key} -->"
        notes = self._notes_resource(scope, target_id)
        try:
            note_list = notes.list(get_all=True)
        except gl_ex.GitlabError as exc:
            raise GitLabApiError(f"Note list failed: {exc}") from exc

        for note in note_list:
            if marker in str(getattr(note, "body", "")):
                note.body = f"{marker}\n{body}"
                try:
                    note.save()
                except gl_ex.GitlabError as exc:
                    raise GitLabApiError(f"Note update failed: {exc}") from exc
                return str(note.id)

        try:
            created = notes.create({"body": f"{marker}\n{body}"})
        except gl_ex.GitlabError as exc:
            raise GitLabApiError(f"Note create failed: {exc}") from exc
        return str(created.id)

    def append_issue_or_mr_note(
        self,
        *,
        scope: str,
        target_id: str,
        body: str,
    ) -> str:
        notes = self._notes_resource(scope, target_id)
        try:
            created = notes.create({"body": body})
        except gl_ex.GitlabError as exc:
            raise GitLabApiError(f"Append note failed: {exc}") from exc
        return str(created.id)

    def read_issue_context(self, issue_id: str) -> str:
        try:
            issue = self._project.issues.get(issue_id)
        except gl_ex.GitlabError as exc:
            raise GitLabApiError(f"Issue lookup failed: {exc}") from exc
        return str(getattr(issue, "description", "") or "")

    def repo_root(self) -> Path:
        return self.root
