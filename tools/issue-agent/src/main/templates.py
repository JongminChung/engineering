from __future__ import annotations

from dataclasses import dataclass
from datetime import UTC, datetime

from .models import TaskItem, WorkflowState


@dataclass(slots=True)
class PlanContext:
    issue_id: str
    mode: str
    branch: str


def _task_status(state: WorkflowState, agent: str) -> str:
    for task in state.task_queue:
        if task.agent == agent:
            return task.status.value
    return "pending"


def render_plan_markdown(ctx: PlanContext) -> str:
    return "\n".join(
        [
            "# PLAN",
            "",
            "## 배경",
            "GitLab 이슈/코멘트 기반으로 AI 작업을 자동화하되, 바로 구현하지 않고 먼저 Plan을 고정한 뒤 MR에서 실행하도록 워크플로우를 표준화한다.",
            "",
            "## 목표",
            "- Issue 단계에서는 `@agent fix|feat`만 허용하고 **Plan Only** 수행",
            "- MR/PR 단계에서 `@agent proceed`로 멀티에이전트 실행",
            "- 피드백은 승인/거절 대신 `@agent differently: ...`만 사용",
            "- 코멘트 소음 최소화: 단계별 1회 코멘트 + 기존 봇 코멘트 upsert",
            "",
            "## 명령 규칙",
            "### Issue",
            "- 허용: `@agent fix`, `@agent feat`",
            "- 결과:",
            f"  1. 브랜치 생성 (`{ctx.branch}`)",
            f"  2. `issues/{ctx.issue_id}/PLAN.md`, `issues/{ctx.issue_id}/PROGRESS.md`, `issues/{ctx.issue_id}/process.md` 선커밋",
            "  3. Draft MR 생성 (`Closes #<issue-id>`)",
            "  4. Plan 코멘트 등록 (`What to do differently? ...`)",
            "",
            "### MR/PR",
            "- 허용: `@agent proceed`, `@agent differently: ...`, `@agent stop`",
            "- 실행 순서: `dev -> (review || qa) -> docs`",
            "",
            "## PLAN/PROGRESS 정책",
            "- `PLAN.md`: 작업 오케스트레이션 중심 (Goal, Task Graph, Constraints, Done Criteria)",
            "- `PROGRESS.md`: 상태 추적 중심 (Current Stage, Task Queue, Parallel Runs, Decision Log)",
            "- 원인/증거/대안 비교는 이슈 본문 또는 MR 본문에 기록",
            "",
            "## Task List",
            "- [ ] T1. Issue 명령 정책 검증 (`@agent fix|feat`만 허용)",
            "- [ ] T2. Plan Draft 파일 생성 (`issues/{issue-id}/PLAN.md`, `PROGRESS.md`, `process.md`)",
            "- [ ] T3. Draft MR 자동 생성 및 이슈 연결(`Closes #<issue-id>`)",
            "- [ ] T4. MR `@agent proceed` 실행 경로 검증 (`dev -> review||qa -> docs`)",
            "- [ ] T5. `@agent differently` 반영 시 재계획 커밋 검증",
            "- [ ] T6. 단계별 봇 코멘트 upsert 동작 검증",
            "- [ ] T7. `.agents/skills` 기반 서브에이전트 실행 확인",
            "- [ ] T8. `python-gitlab` + `.env` 기반 origin2 실동작 검증",
            "",
            "## 완료 조건 (Acceptance Criteria)",
            "- [ ] Issue에서 `@agent fix|feat` 명령 기반으로 동작할 수 있도록 구성",
            "- [ ] Issue 트리거 시 PLAN/PROGRESS 선커밋 + Draft MR 자동 생성",
            "- [ ] MR에서 `@agent proceed` 실행 시 단계 전환 동작",
            "- [ ] `@agent differently` 반영 시 PLAN/PROGRESS 재생성 커밋",
            "- [ ] 단계별 코멘트 upsert 동작 검증",
            "- [ ] `python-gitlab` + `.env`(GITLAB_TOKEN) 기반 실제 origin2 동작 확인",
            "",
            "## 비범위 (Out of Scope)",
            "- 자동 머지",
            "- 라벨 기반 분기 (현재는 코멘트 기반)",
            "- GitHub provider 완전 구현 (스켈레톤만 유지)",
        ],
    )


def render_progress_markdown(state: WorkflowState) -> str:
    queue_lines = []
    for task in state.task_queue:
        queue_lines.append(f"- [{task.status.value}] {task.id} ({task.agent}) - {task.title}")

    parallel_runs = [
        f"- review: {_task_status(state, 'review')}",
        f"- qa: {_task_status(state, 'qa')}",
    ]

    if not state.decision_log:
        decision_log = ["- (none)"]
    else:
        decision_log = [f"- {item}" for item in state.decision_log]

    return "\n".join(
        [
            "# PROGRESS",
            "",
            "## Current Stage",
            f"- {state.current_stage}",
            "",
            "## Task Queue",
            *queue_lines,
            "",
            "## Parallel Runs",
            *parallel_runs,
            "",
            "## Decision Log",
            *decision_log,
            "",
            "## Iteration Loop (Ralphy)",
            "1. PLAN의 체크리스트에서 다음 `pending` 태스크를 선택",
            "2. 해당 태스크만 최소 변경으로 구현",
            "3. 결과를 PROGRESS에 기록하고 상태를 갱신",
            "4. 필요시 `@agent differently: ...` 지시를 반영",
            "",
            "## Next Command Hint",
            "- `@agent proceed`",
            "- `@agent differently: ...`",
            "",
            "## Last Updated",
            f"- {datetime.now(UTC).isoformat()}",
        ],
    )


def render_process_markdown(state: WorkflowState) -> str:
    return "\n".join(
        [
            "# PROCESS",
            "",
            "## Current Stage",
            f"- {state.current_stage}",
            "",
            "## Execution Policy",
            "- Issue 단계: Plan Only (`@agent fix|feat`)",
            "- MR/PR 단계: 실행 (`@agent proceed`)",
            "- 변경 요청: `@agent differently: ...`",
            "",
            "## Stage Flow",
            "- dev -> (review || qa) -> docs",
            "",
            "## Last Updated",
            f"- {datetime.now(UTC).isoformat()}",
        ],
    )


def default_tasks() -> list[TaskItem]:
    return [
        TaskItem(id="T1", agent="dev", title="Plan task 구현 및 코드 변경"),
        TaskItem(id="T2", agent="review", title="회귀/리스크 리뷰"),
        TaskItem(id="T3", agent="qa", title="시나리오 검증 및 테스트"),
        TaskItem(id="T4", agent="docs", title="문서/릴리즈 노트 반영"),
    ]
