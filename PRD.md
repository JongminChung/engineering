# PRD: AI Code Generation Pipeline

## Overview

이슈 기반 코드 생성 파이프라인. OpenAI Codex Multi-Agent 아키텍처(Agent Roles + Skills)를 기반으로, Markdown 파일로 컨텍스트와 상태를 관리한다.

---

## Goals

- 이슈/태스크가 주어지면 자동으로 분석 → 계획 → 코드 생성까지 수행
- 세션이 끊겨도 재시작 시 컨텍스트를 완전히 복원 가능
- 모든 상태를 Markdown 파일로 관리하여 사람이 읽고 수정 가능

---

## System Architecture

### Multi-Agent 구조 (Codex Agent Roles 기반)

Codex는 `[agents]` 섹션에 정의된 **Agent Role**을 기반으로 Sub-Agent를 Spawn한다. 각 Role은 고유한 모델, 권한(sandbox), 지시사항을 가진다.

```
RALPH (Orchestrator)
    ├── planner   → PLAN.md 작성, 태스크 분해
    ├── explorer  → 코드베이스 탐색 (read-only)
    ├── coder     → 코드 생성/수정 (feat, fix, refactor)
    ├── tester    → 테스트 작성 및 검증
    └── reviewer  → 코드 리뷰, 보안/품질 검사
```

**병렬 실행:** 독립적인 태스크는 여러 Sub-Agent가 동시에 실행. 완료 후 RALPH가 결과를 통합.

### Agent Role 정의

| Role       | 모델                   | Sandbox   | 역할                    |
| ---------- | ---------------------- | --------- | ----------------------- |
| `planner`  | codex (high reasoning) | read-only | 이슈 분석, PLAN.md 작성 |
| `explorer` | codex-spark (medium)   | read-only | 코드베이스 탐색         |
| `coder`    | codex (medium)         | default   | 코드 생성/수정          |
| `tester`   | codex (medium)         | default   | 테스트 작성             |
| `reviewer` | codex (high reasoning) | read-only | 리뷰, 보안/품질 검사    |

### Skills 구성

Skills는 `.codex/skills/` 또는 `.agent/skills/`에 Markdown으로 정의.

| Skill           | 담당 Agent     | 설명                    |
| --------------- | -------------- | ----------------------- |
| `file_read`     | all            | Markdown 파일 파싱      |
| `file_write`    | planner, coder | Markdown 파일 업데이트  |
| `code_generate` | coder          | 요구사항 기반 코드 생성 |
| `code_review`   | reviewer       | 코드 품질/보안 검토     |
| `test_write`    | tester         | 유닛/통합 테스트 작성   |
| `git_commit`    | coder          | 변경사항 커밋           |

---

## File Structure

```
project/
├── AGENTS.md                   # 전체 시스템 안내 (세션 시작 시 최우선 참조)
├── PLAN.md                     # 현재 이슈 분석 및 실행 계획
├── PROGRESS.md                 # 태스크별 진행 상황 추적
└── .codex/
    ├── config.toml             # Codex 설정 (agent roles 정의)
    └── agents/
        ├── planner.toml
        ├── explorer.toml
        ├── coder.toml
        ├── tester.toml
        └── reviewer.toml
```

---

## Configuration Spec

### `.codex/config.toml`

```toml
[features]
multi_agent = true

[agents]
max_threads = 5

[agents.planner]
description = "이슈를 분석하고 PLAN.md를 작성하는 계획 전담 에이전트."
config_file = "agents/planner.toml"

[agents.explorer]
description = "코드베이스를 탐색하고 관련 파일/구조를 파악하는 에이전트. 읽기 전용."
config_file = "agents/explorer.toml"

[agents.coder]
description = "feat, fix, refactor 등 실제 코드를 생성하고 수정하는 에이전트."
config_file = "agents/coder.toml"

[agents.tester]
description = "유닛/통합 테스트를 작성하고 검증하는 에이전트."
config_file = "agents/tester.toml"

[agents.reviewer]
description = "보안, 코드 품질, 버그, 유지보수성을 검토하는 에이전트."
config_file = "agents/reviewer.toml"
```

### `.codex/agents/planner.toml`

```toml
model_reasoning_effort = "high"
sandbox_mode = "read-only"
developer_instructions = """
항상 AGENTS.md → PLAN.md → PROGRESS.md 순서로 읽어 컨텍스트를 파악하라.
이슈를 분석하여 태스크로 분해하고 PLAN.md를 작성하라.
각 태스크에 담당 Agent Role과 Type(feat/fix/refactor/test/docs)을 명시하라.
"""
```

### `.codex/agents/explorer.toml`

```toml
model_reasoning_effort = "medium"
sandbox_mode = "read-only"
developer_instructions = """
코드베이스를 탐색하여 관련 파일, 의존성, 구조를 파악하라.
결과를 PLAN.md의 '코드베이스 분석' 섹션에 기록하라.
"""
```

### `.codex/agents/coder.toml`

```toml
model_reasoning_effort = "medium"
developer_instructions = """
PLAN.md의 태스크를 기반으로 코드를 생성/수정하라.
작업 완료 시 PROGRESS.md를 업데이트하라.
커밋 메시지는 Conventional Commits 형식(feat/fix/refactor)을 따르라.
"""
```

### `.codex/agents/tester.toml`

```toml
model_reasoning_effort = "medium"
developer_instructions = """
coder가 생성한 코드에 대한 유닛/통합 테스트를 작성하라.
테스트 실행 후 결과를 PROGRESS.md에 기록하라.
"""
```

### `.codex/agents/reviewer.toml`

```toml
model_reasoning_effort = "high"
sandbox_mode = "read-only"
developer_instructions = """
보안, 코드 품질, 버그, 유지보수성 관점에서 코드를 검토하라.
문제 발견 시 재현 방법과 함께 구체적인 수정 방안을 제시하라.
검토 결과를 PROGRESS.md에 기록하라.
"""
```

---

## Markdown File Specs

### AGENTS.md

```markdown
# AGENTS

## 세션 시작 규칙

새 세션 시작 시 반드시 아래 순서로 파일을 읽을 것:

1. AGENTS.md (현재 파일) — 시스템 구조 파악
2. PLAN.md — 현재 이슈 및 태스크 파악
3. PROGRESS.md — 진행 상황 확인 후 미완료 태스크부터 재개

## 에이전트 구성

- **planner**: 이슈 분석, PLAN.md 작성
- **explorer**: 코드베이스 탐색 (read-only)
- **coder**: 코드 생성/수정 (feat, fix, refactor)
- **tester**: 테스트 작성 및 검증
- **reviewer**: 코드 리뷰, 보안/품질 검사

## 공통 규칙

- 모든 상태 변경은 반드시 PROGRESS.md에 기록
- 계획 변경은 PLAN.md에 반영
- 커밋은 Conventional Commits 형식 준수
```

### PLAN.md

```markdown
# PLAN

## 이슈

[이슈 내용]

## 분석 (planner)

[이슈 분석 결과]

## 코드베이스 분석 (explorer)

[관련 파일 및 구조]

## 태스크 목록

- [ ] Task 1: [설명] | Agent: coder | Type: feat
- [ ] Task 2: [설명] | Agent: tester | Type: test
- [ ] Task 3: [설명] | Agent: reviewer | Type: review

## 의존성

- Task 2 → Task 1 완료 후 시작
- Task 1, Task 3 → 병렬 실행 가능
```

### PROGRESS.md

```markdown
# PROGRESS

## 현재 상태

- 전체 진행률: 0/3
- 현재 단계: PLAN

## 태스크 진행

| Task   | Status    | Agent    | 비고           |
| ------ | --------- | -------- | -------------- |
| Task 1 | ⏳ 진행중 | coder    | -              |
| Task 2 | ⬜ 대기   | tester   | Task 1 완료 후 |
| Task 3 | ✅ 완료   | reviewer | -              |

## 로그

- 2024-01-01 10:00 — planner: PLAN.md 작성 완료
- 2024-01-01 10:05 — explorer: 코드베이스 분석 완료
```

---

## Workflow

### 1. 세션 시작 / 재시작

```
AGENTS.md 읽기 → PLAN.md 읽기 → PROGRESS.md 읽기
→ 미완료 태스크 파악 → 해당 Agent Role로 재개
```

### 2. 신규 이슈 처리

```
이슈 입력
→ [planner + explorer 병렬 Spawn]
    planner: 이슈 분석 → PLAN.md 작성
    explorer: 코드베이스 탐색 → PLAN.md 코드베이스 섹션 보완
→ RALPH: 결과 통합, 태스크 분배, PROGRESS.md 초기화
→ [coder + reviewer 병렬 Spawn (독립 태스크)]
    coder: 코드 생성/수정
    reviewer: 기존 코드 리뷰
→ tester: 테스트 작성
→ 전체 완료 → PROGRESS.md 최종 업데이트 → 커밋
```

### 3. 태스크 타입

| Type       | 설명           |
| ---------- | -------------- |
| `feat`     | 신규 기능 구현 |
| `fix`      | 버그 수정      |
| `refactor` | 코드 개선      |
| `test`     | 테스트 작성    |
| `docs`     | 문서화         |

---

## Non-Goals

- UI/대시보드 제공 (Markdown 파일로 대체)
- 모델 학습/파인튜닝
- Codex 외 프레임워크 지원 (초기 버전 기준)

---

## Open Questions

- Sub-Agent 실패 시 retry/fallback 전략
- PROGRESS.md 병렬 쓰기 충돌 해결 전략
- GitHub Issues/Linear 등 외부 이슈 트래커 연동 범위

## Decisions (2026-02-19)

### 1. Sub-Agent 실패 처리

- 기본 재시도 정책: 동일 태스크 기준 최대 2회 재시도(지수 백오프 5초, 15초).
- 2회 연속 실패 시 `reviewer`가 실패 원인을 요약하고 `planner`가 태스크를 재분해한다.
- 재분해 후에도 실패하면 해당 태스크를 `blocked`로 표시하고 다음 독립 태스크로 진행한다.

### 2. PROGRESS.md 동시성 제어

- 단일 작성자 원칙: `RALPH`만 `PROGRESS.md`를 최종 기록한다.
- Sub-Agent는 직접 파일을 수정하지 않고 결과를 메시지로 반환한다.
- `RALPH`는 태스크 단위로 append-only 로그를 기록하고, 상태 테이블은 마지막에 한 번만 갱신한다.

### 3. 외부 이슈 트래커 연동 범위 (MVP)

- 1차 범위: 로컬 Markdown(`AGENTS.md`, `PLAN.md`, `PROGRESS.md`)만 사용.
- 2차 범위: GitHub Issues read-only 동기화(제목/본문/라벨).
- 3차 범위: GitHub Issues 양방향 동기화 및 Linear 확장.

---

## Acceptance Criteria

- 신규 이슈 입력 시 `PLAN.md`와 `PROGRESS.md`가 자동 초기화된다.
- 독립 태스크가 병렬 실행되고, 충돌 없이 결과가 통합된다.
- 세션 중단 후 재시작 시 3개 문서만으로 동일 컨텍스트를 복원할 수 있다.
- 실패 태스크는 재시도/차단 상태가 `PROGRESS.md`에 추적 가능하게 기록된다.
- 최종 산출물에는 코드 변경, 테스트 결과, 리뷰 요약이 모두 포함된다.

---

## Milestones

### M1. Workflow Core (완료 기준: 로컬 수동 실행 가능)

- Agent Roles 정의 및 spawn/orchestration 루프 구현
- `PLAN.md`, `PROGRESS.md` 생성/갱신 자동화
- 단일 태스크 루프(dev → review → qa → docs) 동작 검증

### M2. Reliability (완료 기준: 실패/재시도 안정화)

- 재시도/백오프/blocked 처리 구현
- 병렬 태스크 통합 시 상태 일관성 검증
- 회귀 방지를 위한 최소 테스트 시나리오 확보

### M3. Tracker Integration (완료 기준: GitHub read-only 연동)

- 이슈 메타데이터 가져오기(제목/본문/라벨)
- 로컬 `PLAN.md` 초기화 입력으로 반영
- 연동 실패 시 로컬 입력 모드로 자동 fallback

---

## Risks and Mitigations

- 병렬 태스크 증가로 컨텍스트 오염 가능
    - 대응: 태스크별 입력 범위 최소화, 완료 시 정형화된 결과 스키마 강제
- 장기 실행 시 로그 비대화
    - 대응: `PROGRESS.md`는 최근 N개 로그만 유지하고 상세 로그는 이슈 하위 파일로 분리
- 테스트 실행 시간 증가
    - 대응: 변경 범위 기반 최소 테스트를 기본으로 하고, 전체 테스트는 머지 전 게이트로 분리

---

## Rollout Plan

1. 단일 저장소에서 M1 기능을 적용하고 수동 이슈 3건으로 반복 검증
2. 실패 케이스(명령 실패, 테스트 실패, 파일 충돌)를 주입해 M2 안정성 확인
3. GitHub read-only 연동을 붙여 M3 검증 후 점진 배포
