# Repository Guidelines

## Project Structure & Module Organization
- Algorithm solutions live in `study/coding-test/src/main/java`.
- Tests and study notes are in `study/coding-test/src/test/java`.
- Markdown notes live under `study/coding-test/docs` (e.g., `docs/graph/README.md`).
- Build artifacts go to `study/coding-test/build/` and are disposable.

## Build, Test, and Development Commands
- `./gradlew :study:coding-test:test` — run all algorithm tests with JUnit Jupiter.
- `./gradlew :study:coding-test:test --tests "...SolutionTest"` — run a specific test class.
- `./gradlew :study:coding-test:build` — compile and run tests for this module.

## Coding Style & Naming Conventions
- Use clear package paths that reflect the source (e.g., `algorithm.codility.binary_gap`).
- Keep solution classes named `Solution` for coding challenge submissions.
- Tests should be descriptive and placed beside the topic (e.g., `SolutionTest`, `ArrayTest`).
- Spotless and `.editorconfig` handle whitespace; keep formatting minimal and consistent.

## Testing Guidelines
- Tests use JUnit Jupiter.
- Favor small, focused tests with explicit input/output cases.
- Keep test names aligned to the concept under study (arrays, queues, simulation, etc.).

## Commit & Pull Request Guidelines
- Use Conventional Commits (`feat:`, `test:`, `chore:`).
- PRs should describe the problem solved and include sample inputs/outputs if helpful.

## Notes for Contributors
- Add new problem sets under a dedicated package to keep history navigable.
- Update or add docs in `study/coding-test/docs` when introducing a new category.
