# Repository Guidelines

## Project Structure & Module Organization

- Spring Boot Modulith PoC 앱은 `study/cloud`에 위치함.
- 도메인 모듈 경계를 유지하며, 모듈 간 의존성은 최소화함.
- Kafka/PostgreSQL/보안 관련 코드는 목적별 패키지로 분리함.

## Coding Style & Naming Conventions

- DTO는 기본적으로 Java `record`를 사용함.
- 상태를 관리하는 도메인인 경우에만 `class`를 허용함.
- `FluentDtoJsonTest`처럼 Fluent 스타일이 필요한 DTO는 `class` +
	`@Accessors(fluent = true)` 패턴을 사용함.
- 패키지/클래스 네이밍은 Spring 표준을 따름.

## Testing Guidelines

- 테스트는 JUnit Jupiter 기반으로 작성함.
- 통합 테스트는 Testcontainers를 우선 사용함.
- Modulith 경계는 `ApplicationModules.verify()`로 검증함.

## Build & Run

- `./gradlew :study:cloud:test`로 테스트 실행함.
- `./gradlew :study:cloud:bootRun`으로 로컬 실행함.

## Security & Standards

- AWS 문서 및 RFC 표준을 우선 기준으로 삼음.
- 비표준 동작은 문서화하고 테스트로 검증함.
