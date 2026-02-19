# issue-agent

모노레포 하위 패키지입니다. 루트 uv workspace에서 함께 관리합니다.

## from repository root

```bash
uv run --package issue-agent python -m issue_agent.cli --help
```

## from package directory

```bash
cd tools/issue-agent
uv run python -m issue_agent.cli --help
```

## local workflow (config-free)

```bash
# 1) Plan only
uv run --package issue-agent python -m issue_agent.cli local-workflow --issue-id 10 --action plan --mode fix

# 2) Execute stages (dev -> review/qa -> docs)
uv run --package issue-agent python -m issue_agent.cli local-workflow --issue-id 10 --action proceed

# 3) Re-plan with differently feedback
uv run --package issue-agent python -m issue_agent.cli local-workflow --issue-id 10 --action differently --instruction "split rollout by module"
```
