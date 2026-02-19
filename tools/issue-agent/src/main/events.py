from __future__ import annotations

from .models import NoteContext


def parse_gitlab_note_payload(payload: dict[str, object]) -> NoteContext | None:
    object_kind = str(payload.get("object_kind", ""))
    if object_kind != "note":
        return None

    object_attributes = payload.get("object_attributes")
    if not isinstance(object_attributes, dict):
        return None

    noteable_type = str(object_attributes.get("noteable_type", "")).lower()
    note_id = str(object_attributes.get("id", ""))
    body = str(object_attributes.get("note", ""))

    project = payload.get("project")
    user = payload.get("user")
    issue = payload.get("issue")
    merge_request = payload.get("merge_request")

    if not isinstance(project, dict) or not isinstance(user, dict):
        return None

    project_id = str(project.get("id", ""))
    actor = str(user.get("username", ""))

    if noteable_type == "issue" and isinstance(issue, dict):
        issue_id = str(issue.get("iid", ""))
        return NoteContext(
            provider="gitlab",
            scope="issue",
            project_id=project_id,
            actor=actor,
            note_id=note_id,
            issue_id=issue_id,
            body=body,
        )

    if noteable_type == "mergerequest" and isinstance(merge_request, dict):
        mr_id = str(merge_request.get("iid", ""))
        return NoteContext(
            provider="gitlab",
            scope="mr",
            project_id=project_id,
            actor=actor,
            note_id=note_id,
            mr_id=mr_id,
            body=body,
        )

    return None
