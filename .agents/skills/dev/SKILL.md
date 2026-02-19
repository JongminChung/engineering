---
name: dev
description: 현재 PLAN 태스크 1개를 구현하고 최소 검증을 수행하는 개발 스킬
---

# Dev Agent Skill

- 목적: PLAN의 현재 태스크 1개를 구현하고 최소 테스트를 추가합니다.
- 입력: `issues/{issue-id}/PLAN.md`, `issues/{issue-id}/PROGRESS.md`, MR 지시사항
- 출력: 구현 커밋 + PROGRESS 상태 업데이트
- 규칙: 한 번에 한 태스크만 처리하고, 완료 후 Decision Log를 기록합니다.
