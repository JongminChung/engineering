# Repository Guidelines

## Project Structure & Module Organization

- This module provides a distributed lock library with Spring integration and
  Spring Boot starters.
- Submodules:
  - `distributed-lock/core` — core lock APIs, policies, and key strategies.
  - `distributed-lock/provider-jdbc` — JDBC-based lock provider.
  - `distributed-lock/provider-redis` — Redisson-based lock provider.
  - `distributed-lock/spring` — Spring AOP/SpEL integration.
  - `distributed-lock/spring-boot-autoconfigure` — auto-configuration for
    providers.
  - `distributed-lock/spring-boot-starter` and
    `distributed-lock/spring-boot-starter-*` — starters.
  - `distributed-lock/test` — shared test utilities.
- Build output is generated under `**/build/` and must not be committed.

## Build, Test, and Development Commands

- `./gradlew :distributed-lock:core:test` — run core tests.
- `./gradlew :distributed-lock:provider-jdbc:test` — run JDBC provider tests.
- `./gradlew :distributed-lock:provider-redis:test` — run Redis provider tests.
- `./gradlew :distributed-lock:spring:test` — run Spring integration tests.
- `./gradlew :distributed-lock:spring-boot-autoconfigure:test` — run auto-config
  tests.
- `./gradlew :distributed-lock:test:test` — run test-utility module tests.
- `./gradlew :distributed-lock:dependencies:dependencies` — inspect module
  dependency graph.

## Coding Style & Naming Conventions

- Follow `.editorconfig` and Spotless formatting.
- Use standard Java naming: PascalCase for classes, camelCase for methods.
- Package names follow `io.github.jongminchung.distributedlock`.

## Testing Guidelines

- Tests use JUnit Jupiter and AssertJ.
- Integration tests may use Testcontainers (MySQL/Redis) and require Docker.
- Keep tests focused on behavior; prefer real provider behavior over mocks.

## Dependencies & Compatibility

- Spring Boot BOM is used for Spring dependencies.
- Redisson API versions must match the configured `libs.versions.toml`.
- Keep provider modules optional for auto-configuration via `compileOnly`.

## Commit & Pull Request Guidelines

- Use Conventional Commits (e.g., `feat:`, `fix:`, `test:`).
- Document behavior changes in `distributed-lock/README.md` when relevant.

## Security & Configuration Tips

- Never commit secrets (tokens, passwords).
- For local testing, ensure Docker is running when Testcontainers is used.
