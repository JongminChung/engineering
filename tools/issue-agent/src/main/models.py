from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum
from typing import Literal

Provider = Literal["gitlab", "github"]
Command = Literal["fix", "feat", "proceed", "differently", "stop"]
Scope = Literal["issue", "mr", "pr"]
Stage = Literal["plan", "dev", "review", "qa", "docs", "completed", "blocked", "stopped"]


class Status(str, Enum):
    PENDING = "pending"
    RUNNING = "running"
    DONE = "done"
    BLOCKED = "blocked"


@dataclass(slots=True)
class NoteContext:
    provider: Provider
    scope: Scope
    project_id: str
    actor: str
    note_id: str
    issue_id: str | None = None
    mr_id: str | None = None
    body: str = ""


@dataclass(slots=True)
class ParsedCommand:
    command: Command
    instruction: str | None = None


@dataclass(slots=True)
class IssuePlanRequested:
    provider: Provider
    project_id: str
    issue_id: str
    command: Literal["fix", "feat"]
    actor: str
    note_id: str


@dataclass(slots=True)
class ExecutionRequested:
    provider: Provider
    mr_id: str
    actor: str
    note_id: str


@dataclass(slots=True)
class DifferentlyRequested:
    provider: Provider
    mr_id: str
    actor: str
    note_id: str
    instruction: str


@dataclass(slots=True)
class StageUpdated:
    mr_id: str
    stage: Stage
    status: Status
    summary: str


@dataclass(slots=True)
class TaskItem:
    id: str
    agent: Literal["dev", "review", "qa", "docs"]
    title: str
    status: Status = Status.PENDING
    result: str = ""


@dataclass(slots=True)
class WorkflowState:
    mode: Literal["fix", "feat"]
    issue_id: str
    branch: str
    mr_id: str
    current_stage: Stage = "plan"
    task_queue: list[TaskItem] = field(default_factory=list)
    decision_log: list[str] = field(default_factory=list)
    bot_comment_keys: dict[str, str] = field(default_factory=dict)
