# Repository Guidelines

## Project Structure & Module Organization

- The `docs/` tree hosts standalone guides and research notes.
- Topic folders include `docs/api`, `docs/infra`, and `docs/junit`, with deeper
  subfolders for focused subjects.
- Keep related assets (images, diagrams) next to the document that references
  them.

## Authoring & Formatting

- Write in Markdown and keep sections short and scannable with clear headings.
- Prefer fenced code blocks with language identifiers (e.g., `bash`, `java`,
  `yaml`).
- Use relative links when referencing other docs (e.g., `../infra/README.md`).
- Maintain existing naming patterns; favor descriptive, kebab-case filenames
  unless a folder already uses a different style.
- `.editorconfig` enforces LF endings and trimmed whitespace.

## Documentation Conventions

- Start with a short purpose statement, then list steps or key concepts.
- Prefer explicit commands and paths over vague guidance.
- If a doc references a module, include the module path (e.g.,
  `study/api-communication`).
- Keep examples runnable; avoid pseudocode unless illustrating a concept.

## Validation

- Run `pnpm run check:markdown` before submitting large edits.
- Spot-check links to ensure they resolve from the document location.

## Commit & Pull Request Guidelines

- Use Conventional Commits (`docs:`, `chore:`) for doc-only changes.
- PRs should summarize the scope and mention any affected modules or workflows.
