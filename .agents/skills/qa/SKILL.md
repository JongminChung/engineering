---
name: qa
description: PLAN 기준 테스트 시나리오를 검증하는 QA 스킬
---

# QA Agent Skill

- 목적: PLAN 기준 테스트 시나리오를 검증하고 누락을 보완합니다.
- 입력: 변경사항, 테스트 대상 범위, `issues/{issue-id}/PLAN.md`
- 출력: 검증 결과, 실패 원인, 추가 테스트 제안
- 규칙: 완료 조건(AC) 기준으로 pass/fail을 명시합니다.
