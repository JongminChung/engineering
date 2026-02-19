#!/usr/bin/env bash
set -euo pipefail

export PATH="$HOME/.bun/bin:$PATH"

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

echo "[Workflow] issue=${ISSUE_ID}"
echo "[Stage 0] validate tests"
uv run --package issue-agent python -m unittest discover -s tools/issue-agent/src/test -v

echo "[Stage 1] ensure local workflow state (plan)"
STATE_PATH=".issue-agent/local-states.json"
if uv run --package issue-agent python - <<PY
import json
from pathlib import Path
issue = "${ISSUE_ID}"
path = Path("${STATE_PATH}")
if not path.exists():
    raise SystemExit(1)
data = json.loads(path.read_text(encoding="utf-8"))
raise SystemExit(0 if issue in data else 1)
PY
then
  echo "state already exists for issue ${ISSUE_ID}; skipping plan bootstrap"
else
  uv run --package issue-agent python -m issue_agent.cli local-workflow \
    --issue-id "${ISSUE_ID}" \
    --action plan \
    --mode fix
fi

echo "[Stage 2] run Ralphy (config-free)"
if command -v ralphy >/dev/null 2>&1; then
  ralphy --prd "${PLAN_PATH}" --codex
else
  echo "ralphy is not installed; skipping execution"
fi

echo "[Stage 3] execute multi-agent stages"
uv run --package issue-agent python -m issue_agent.cli local-workflow \
  --issue-id "${ISSUE_ID}" \
  --action proceed

echo "[Stage 4] next action"
echo "- open ${PLAN_PATH} and pick the next pending task"
echo "- if needed: uv run --package issue-agent python -m issue_agent.cli local-workflow --issue-id ${ISSUE_ID} --action differently --instruction \"...\""
echo "- update ${PROGRESS_PATH}"
