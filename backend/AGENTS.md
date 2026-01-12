# AGENTS.md

## 범위

- `backend/` 공통 규칙입니다.
- 하위 모듈(`cloud`, `distributed-lock`, `grpc`)에 `AGENTS.md`가 있으면 해당 규칙이 우선입니다.

## 구조/코딩

- 각 모듈은 독립 Gradle 프로젝트로 운영합니다.
- 소스는 `*/src/main/java`, 테스트는 `*/src/test/java`에 둡니다.
- `**/build/` 산출물은 커밋하지 않습니다.

## 테스트

- 기본 테스트 프레임워크는 JUnit Jupiter입니다.
- 통합 테스트는 외부 의존을 최소화하고, 필요 시 Testcontainers를 사용합니다.

## 명령어

- 모듈 테스트: `./gradlew :<module-path>:test`
- 모듈 실행: `./gradlew :<module-path>:bootRun` (Spring Boot 모듈에 한함)
