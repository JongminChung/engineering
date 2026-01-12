# AGENTS.md

## 구조

- 분산 락 라이브러리와 Spring 통합, Spring Boot 스타터로 구성됩니다.
- 서브모듈:
  - `distributed-lock/core` — 코어 API, 정책, 키 전략
  - `distributed-lock/provider-jdbc` — JDBC 기반 락 프로바이더
  - `distributed-lock/provider-redis` — Redisson 기반 락 프로바이더
  - `distributed-lock/spring` — Spring AOP/SpEL 통합
  - `distributed-lock/spring-boot-autoconfigure` — 자동 설정
  - `distributed-lock/spring-boot-starter` 및 `distributed-lock/spring-boot-starter-*` — 스타터
  - `distributed-lock/test` — 테스트 유틸
- `**/build/` 산출물은 커밋하지 않습니다.

## 명령어

- 코어 테스트: `./gradlew :distributed-lock:core:test`
- JDBC 프로바이더 테스트: `./gradlew :distributed-lock:provider-jdbc:test`
- Redis 프로바이더 테스트: `./gradlew :distributed-lock:provider-redis:test`
- Spring 통합 테스트: `./gradlew :distributed-lock:spring:test`
- 자동 설정 테스트: `./gradlew :distributed-lock:spring-boot-autoconfigure:test`
- 테스트 유틸 테스트: `./gradlew :distributed-lock:test:test`
- 의존성 확인: `./gradlew :distributed-lock:dependencies:dependencies`

## 코딩 규칙

- `.editorconfig`과 Spotless 포맷을 따릅니다.
- Java 네이밍: 클래스는 PascalCase, 메서드는 camelCase입니다.
- 패키지는 `io.github.jongminchung.distributedlock`를 사용합니다.

## 테스트 규칙

- JUnit Jupiter + AssertJ를 사용합니다.
- 통합 테스트는 Testcontainers(MySQL/Redis)를 사용할 수 있으며 Docker가 필요합니다.
- 프로바이더의 실제 동작을 우선 검증하고, 과도한 목킹은 피합니다.

## 의존성/호환

- Spring 의존성은 Spring Boot BOM을 기준으로 합니다.
- Redisson API 버전은 `libs.versions.toml`과 일치해야 합니다.
- 자동 설정은 프로바이더를 `compileOnly`로 유지합니다.

## 보안

- 비밀정보는 커밋하지 않습니다.
- Testcontainers 사용 시 로컬 Docker 실행 여부를 확인합니다.
