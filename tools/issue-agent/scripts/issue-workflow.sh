#!/usr/bin/env bash
set -euo pipefail

ISSUE_ID="${1:-}"
if [[ -z "${ISSUE_ID}" ]]; then
  echo "Usage: $0 <issue-id>"
  exit 1
fi

PLAN_PATH="issues/${ISSUE_ID}/PLAN.md"
PROGRESS_PATH="issues/${ISSUE_ID}/PROGRESS.md"

if [[ ! -f "${PLAN_PATH}" ]]; then
  echo "Missing ${PLAN_PATH}"
  exit 1
fi

if [[ ! -f "${PROGRESS_PATH}" ]]; then
  echo "Missing ${PROGRESS_PATH}"
  exit 1
fi

echo "[1/3] Validate current python workflow"
uv run --package issue-agent python -m unittest discover -s tools/issue-agent/src/test -v

echo "[2/3] Run Ralphy with issue-scoped plan"
ralphy --prd "${PLAN_PATH}" --codex

echo "[3/3] Reminder: update ${PROGRESS_PATH} with executed task result"
