# AGENTS.md

## 구조

- Spring Boot Modulith PoC 앱은 `study/cloud`에 위치합니다.
- 도메인 모듈 경계를 유지하며 모듈 간 의존성은 최소화합니다.
- Kafka/PostgreSQL/보안 관련 코드는 목적별 패키지로 분리합니다.

## 코딩 규칙

- DTO는 기본적으로 Java `record`를 사용합니다.
- 상태를 관리하는 도메인만 `class`를 허용합니다.
- Fluent 스타일이 필요한 DTO는 `class` + `@Accessors(fluent = true)` 패턴을 사용합니다.
- 패키지/클래스 네이밍은 Spring 표준을 따릅니다.

## 테스트 규칙

- 테스트는 JUnit Jupiter 기반으로 작성합니다.
- 통합 테스트는 Testcontainers를 우선 사용합니다.
- Modulith 경계는 `ApplicationModules.verify()`로 검증합니다.

## 명령어

- 테스트 실행: `./gradlew :study:cloud:test`
- 로컬 실행: `./gradlew :study:cloud:bootRun`

## 표준/보안

- AWS 문서 및 RFC 표준을 우선 기준으로 삼습니다.
- 비표준 동작은 문서화하고 테스트로 검증합니다.
