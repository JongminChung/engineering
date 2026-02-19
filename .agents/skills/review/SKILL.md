---
name: review
description: 변경사항의 회귀/결함/리스크를 점검하는 리뷰 스킬
---

# Review Agent Skill

- 목적: 현재 변경의 회귀/결함/리스크를 점검합니다.
- 입력: 변경 파일, 테스트 결과, `issues/{issue-id}/PLAN.md`
- 출력: 리뷰 피드백 + 필요 시 보완 커밋 제안
- 규칙: PLAN Acceptance Criteria와 불일치 항목을 우선 보고합니다.
