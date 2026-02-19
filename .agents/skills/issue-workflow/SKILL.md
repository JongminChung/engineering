---
name: issue-workflow
description: 이슈 단위 PLAN/PROGRESS 루프를 운영하는 워크플로우 스킬
---

# Issue Workflow Skill

## 목적

- 이슈 단위 PLAN/PROGRESS를 기준으로 반복 가능한 작업 루프를 운영한다.

## 현재 우선순위

- 트리거 개발(GitLab/GitHub)보다 워크플로우 자체(Ralphy + Skills + Multi-Agent)를 먼저 완성한다.

## 실행 절차

1. `issues/{issue-id}/PLAN.md`에서 pending 태스크 1개를 선택한다.
2. `dev -> review/qa -> docs` 단계로 수행한다.
3. 결과를 `issues/{issue-id}/PROGRESS.md`에 반영한다.
4. 다음 pending 태스크로 반복한다.

## 완료 기준

- PLAN 체크리스트 진행률이 증가한다.
- PROGRESS의 Current Stage/Task Queue/Decision Log가 최신 상태로 유지된다.
