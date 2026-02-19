---
name: workflow-core
description: 트리거 독립형 AI 워크플로우 핵심 실행 규약 스킬
---

# Workflow Core Skill

## Purpose

- Build and run a trigger-independent AI workflow using Ralphy + Skills + Multi-Agent.

## Inputs

- `issues/{issue-id}/PLAN.md`
- `issues/{issue-id}/PROGRESS.md`
- local repository context

## Execution Contract

- Process one pending task per loop.
- Keep changes minimal and update PROGRESS after each loop.
- Validate with tests before marking task done.

## Stage Responsibilities

- dev: implement the selected task
- review: find risk/regression
- qa: verify scenarios and tests
- docs: update summary and follow-ups
