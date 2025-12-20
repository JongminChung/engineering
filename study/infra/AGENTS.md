# Repository Guidelines

## Project Structure & Module Organization
- This module is test-focused; source code lives under `study/infra/src/test/java`.
- Kafka tests live in `io/github/jongminchung/study/infra/kafka` with phase folders (e.g., `phase1`).
- PostgreSQL tests live in `io/github/jongminchung/study/infra/postgresql`.
- Local Kafka helpers (optional) are in `study/infra/docker-compose.kafka.yml`.
- Build output is generated in `study/infra/build/` and should not be committed.

## Build, Test, and Development Commands
- `./gradlew :study:infra:test` — run all infra tests (JUnit Jupiter + Testcontainers).
- `./gradlew :study:infra:test --tests "...KafkaBasicTest"` — run a single test class.
- `./gradlew :study:infra:dockerComposeUp` — start Kafka with Docker Compose.
- `./gradlew :study:infra:dockerComposeLogs` — tail Kafka logs.
- `./gradlew :study:infra:dockerComposeDown` — stop Kafka.
- `./gradlew :study:infra:dockerComposeDownVolumes` — stop Kafka and wipe volumes.

## Coding Style & Naming Conventions
- Use standard Java naming: PascalCase classes, camelCase methods, and package names under `io.github.jongminchung.study.infra`.
- Formatting is enforced by Spotless (Gradle) and `.editorconfig` rules for whitespace and line endings.
- Keep test names descriptive and scoped by topic (e.g., `KafkaBasicTest`, `UserRepositoryTest`).

## Testing Guidelines
- Tests are JUnit Jupiter; Testcontainers is used for Kafka and PostgreSQL.
- Prefer integration-style tests that exercise real containers over mocks.
- Test classes live in `src/test/java` and end with `*Test`.
- If adding a new Kafka phase, create a new package `phaseX` and keep tests focused on one concept.

## Commit & Pull Request Guidelines
- Use Conventional Commits (`feat:`, `chore:`, `test:`) as enforced by commitlint.
- Follow the repository PR template and include context or screenshots when behavior changes.

## Security & Configuration Tips
- Docker must be running for Testcontainers and optional Compose tasks.
- Avoid committing credentials; use local `.env` or runtime config when experimenting.
