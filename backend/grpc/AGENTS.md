# Repository Guidelines

## Project Structure & Module Organization

- Spring Boot app lives in `study/api-communication/src/main/java/io/github/jongminchung/study/apicommunication`.
- REST controllers, gRPC services, security, metrics, and rate limiting are organized by package (`orders`, `grpc`, `security`, `metrics`).
- Protobuf definitions are in `study/api-communication/src/main/proto`.
- Tests live in `study/api-communication/src/test/java` and cover REST, gRPC, and service logic.

## Build, Test, and Development Commands

- `./gradlew :study:api-communication:bootRun` — run the Spring Boot app locally.
- `./gradlew :study:api-communication:test` — run all module tests.
- `./gradlew :study:api-communication:test --tests "...OrderControllerTest"` — run a focused test.
- `./gradlew :study:api-communication:build` — compile, generate proto stubs, and test.

## Coding Style & Naming Conventions

- Follow standard Spring naming: `*Controller`, `*Service`, `*Repository`, `*Config`.
- Keep gRPC classes under `grpc` and REST endpoints under `orders/api`.
- Configuration properties live under `config` and map to `application.yml` keys.
- Formatting is enforced by Spotless and `.editorconfig`.

## Testing Guidelines

- Tests use JUnit Jupiter and `spring-boot-starter-test`.
- REST tests should cover filters, headers, and rate limiting behavior.
- gRPC tests use in-process servers; verify metadata authentication and error mapping.
- Keep tests deterministic; avoid real network calls.

## Commit & Pull Request Guidelines

- Use Conventional Commits per commitlint (`feat:`, `fix:`, `test:`).
- PRs should link the relevant issue and describe protocol impact (REST, proto REST, gRPC).

## Security & Configuration Tips

- API keys are expected to be hashed in `application.yml`; use the sample values for local runs.
- When changing headers or auth flow, update both REST and gRPC paths for parity.
