---
name: ralphy-orchestration
description: Ralphy 실행 루프를 이슈 단위로 오케스트레이션하는 스킬
---

# Ralphy Orchestration Skill

## 목적

- 이슈 단위 PLAN/PROGRESS를 기준으로 Ralphy 실행 루프를 안정적으로 수행합니다.

## 입력

- `issues/{issue-id}/PLAN.md`
- `issues/{issue-id}/PROGRESS.md`

## 실행 절차

1. PLAN에서 첫 번째 pending 태스크를 선택한다.
2. `ralphy --prd issues/{issue-id}/PLAN.md --codex`를 실행한다.
3. 결과를 PROGRESS의 Task Queue/Decision Log에 기록한다.
4. 다음 pending 태스크로 반복한다.

## 멀티에이전트 분업

- dev: 코드 변경/테스트 보강
- review: 회귀/리스크 점검
- qa: 시나리오 검증
- docs: 문서/요약 정리

## 완료 기준

- PLAN 체크리스트가 모두 done으로 전환
- PROGRESS에 최종 실행 로그/결정 내역 기록
