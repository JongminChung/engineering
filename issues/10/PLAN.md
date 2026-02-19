# PLAN

## 배경

GitLab 이슈/코멘트 기반으로 AI 작업을 자동화하되, 바로 구현하지 않고 먼저 Plan을 고정한 뒤 MR에서 실행하도록 워크플로우를 표준화한다.

## 목표

- Issue 단계에서는 `@agent fix|feat`만 허용하고 **Plan Only** 수행
- MR/PR 단계에서 `@agent proceed`로 멀티에이전트 실행
- 피드백은 승인/거절 대신 `@agent differently: ...`만 사용
- 코멘트 소음 최소화: 단계별 1회 코멘트 + 기존 봇 코멘트 upsert

## 명령 규칙

### Issue

- 허용: `@agent fix`, `@agent feat`
- 결과:
    1. 브랜치 생성 (`agent/issue-10`)
    2. `issues/10/PLAN.md`, `issues/10/PROGRESS.md`, `issues/10/process.md` 선커밋
    3. Draft MR 생성 (`Closes #<issue-id>`)
    4. Plan 코멘트 등록 (`What to do differently? ...`)

### MR/PR

- 허용: `@agent proceed`, `@agent differently: ...`, `@agent stop`
- 실행 순서: `dev -> (review || qa) -> docs`

## PLAN/PROGRESS 정책

- `PLAN.md`: 작업 오케스트레이션 중심 (Goal, Task Graph, Constraints, Done Criteria)
- `PROGRESS.md`: 상태 추적 중심 (Current Stage, Task Queue, Parallel Runs, Decision Log)
- 원인/증거/대안 비교는 이슈 본문 또는 MR 본문에 기록

## Task List

- [ ] T1. Issue 명령 정책 검증 (`@agent fix|feat`만 허용)
- [ ] T2. Plan Draft 파일 생성 (`issues/{issue-id}/PLAN.md`, `PROGRESS.md`, `process.md`)
- [ ] T3. Draft MR 자동 생성 및 이슈 연결(`Closes #<issue-id>`)
- [ ] T4. MR `@agent proceed` 실행 경로 검증 (`dev -> review||qa -> docs`)
- [ ] T5. `@agent differently` 반영 시 재계획 커밋 검증
- [ ] T6. 단계별 봇 코멘트 upsert 동작 검증
- [ ] T7. `.agents/skills` 기반 서브에이전트 실행 확인
- [ ] T8. `python-gitlab` + `.env` 기반 origin2 실동작 검증

## 완료 조건 (Acceptance Criteria)

- [ ] Issue에서 `@agent fix|feat` 명령 기반으로 동작할 수 있도록 구성
- [ ] Issue 트리거 시 PLAN/PROGRESS 선커밋 + Draft MR 자동 생성
- [ ] MR에서 `@agent proceed` 실행 시 단계 전환 동작
- [ ] `@agent differently` 반영 시 PLAN/PROGRESS 재생성 커밋
- [ ] 단계별 코멘트 upsert 동작 검증
- [ ] `python-gitlab` + `.env`(GITLAB_TOKEN) 기반 실제 origin2 동작 확인

## 비범위 (Out of Scope)

- 자동 머지
- 라벨 기반 분기 (현재는 코멘트 기반)
- GitHub provider 완전 구현 (스켈레톤만 유지)
