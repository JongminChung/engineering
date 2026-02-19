---
name: docs
description: 변경사항을 문서화하고 이슈/머지요청 맥락을 정리하는 문서화 스킬
---

# Docs Agent Skill

- 목적: 최종 변경을 문서화하고 이슈/머지요청 맥락을 정리합니다.
- 입력: 변경 목록, 검증 결과, `issues/{issue-id}/PROGRESS.md`
- 출력: MR 요약, 운영 영향, 릴리즈 노트
- 규칙: PROGRESS Decision Log와 일치하도록 변경 이유를 정리합니다.
