# AGENTS.md

## 범위

- GitLab Runner 설치/등록 스크립트에 대한 규칙입니다.

## 작업 원칙

- 실제 Runner 등록/토큰 변경은 실행 전 확인합니다.
- 스크립트 내 민감 정보는 변수로 분리하고 커밋하지 않습니다.

## 파일

- `gitlab-runner.sh`, `runner-register.sh`는 목적별로 분리 유지합니다.
