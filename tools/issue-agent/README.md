# issue-agent

모노레포 하위 패키지입니다. 루트 uv workspace에서 함께 관리합니다.

## 로컬 기준 사용법 (현재 동작 기준)

### 0) 사전 조건

- 작업 디렉터리: 저장소 루트
- Python/uv 사용 가능
- 대상 이슈 파일 존재
    - `issues/<issue-id>/PLAN.md`
    - `issues/<issue-id>/PROGRESS.md`

### 1) CLI 확인

```bash
uv run --package issue-agent python -m main.cli --help
```

### 2) Plan 상태 생성 (Issue 단계)

```bash
uv run --package issue-agent python -m main.cli local-workflow --issue-id 10 --action plan --mode fix
```

- `--mode`: `fix` 또는 `feat`
- 실행 후 상태 파일: `.issue-agent/local-states.json`

### 3) 멀티 에이전트 실행 (MR/PR 단계)

```bash
uv run --package issue-agent python -m main.cli local-workflow --issue-id 10 --action proceed
```

- 실행 순서: `dev -> review -> qa -> docs`
- 완료 시 `current_stage: completed`로 갱신

### 4) 피드백 반영 재계획

```bash
uv run --package issue-agent python -m main.cli local-workflow --issue-id 10 --action differently --instruction "split rollout by module"
```

- Plan/Progress를 재생성하고 stage를 `plan`으로 되돌림

### 5) 상태 확인

```bash
cat .issue-agent/local-states.json
cat issues/10/PROGRESS.md
```

## 참고: 자동 루프 스크립트

`tools/issue-agent/scripts/local-workflow-loop.sh`는 테스트 -> plan 보정 -> Ralphy -> proceed 순으로 실행합니다.

## 트러블슈팅

- `No module named issue_agent` 오류가 나면
    - `python -m issue_agent.cli` 대신 `python -m main.cli`를 사용합니다.
- `Missing issues/<id>/PLAN.md` 또는 `PROGRESS.md`가 나오면
    - 먼저 해당 이슈 디렉터리와 파일을 준비합니다.
