# Spring Modulith PoC 작업 일지

## 작업 범위

- `study:cloud` 모듈로 통합 진행함.
- 기존 `study:api-communication`, `study:infra`는 통합 대상에서 제외하고 Gradle 설정에서 모듈 포함만
  제거함.
- Users/Auth/IAM 모듈 경계로 PoC 구조 잡음.

## 결정 사항

- 인증/인가 흐름을 `auth` 모듈로 분리함.
- 정책 결정(PDP)과 정책 집행(PEP)을 `iam` 모듈에서 분리 구현함.
- Kafka/PostgreSQL 연동은 추후 단계로 남김.

## 변경 영향

- `settings.gradle.kts`에서 `study:api-communication`, `study:infra` 포함 제거됨.
- 신규 모듈에서 Modulith 검증 테스트 추가됨.
- 문서에 v2 변경사항 요약 추가됨.
