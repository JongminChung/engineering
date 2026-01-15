# 린트/포맷 통합 가이드

이 문서는 IntelliJ IDEA와 Antigravity(VSCode) 기반 린터를 포함해 Prettier,
EditorConfig, Biome, Spotless(Java/Kotlin)를 함께 사용하는 기준과 작업 단위를
정리합니다.

## 원칙

- 단일 소스: 언어별 포맷터를 분리해 충돌을 줄입니다.
- 스코프 고정: 한 파일을 두 포맷터가 동시에 다루지 않게 합니다.
- 로컬-CLI-CI 일치: IDE는 편의, 기준은 CLI/CI로 맞춥니다.

## 도구 역할과 범위

- EditorConfig: 전 파일 공통 기본 규칙. `./.editorconfig` 기준.
- Biome: JavaScript, TypeScript, JSX, TSX, JSON, HTML, CSS 포맷/린트.
  `./biome.json` 기준.
- Prettier: Biome/Spotless 범위 외 포맷터.
  Prettier는 `.editorconfig`를 기본으로 읽습니다.
- Spotless: Java/Kotlin 포맷.
  `./build-logic/src/main/kotlin/buildlogic.formatter-conventions.gradle.kts`
  기준.
- _TBA: Python, Go, Rust_

## 언어별 매핑

- TypeScript: Biome formatter + linter.
- Java/Kotlin: Spotless formatter.
- Markdown 및 YAML: Prettier formatter.

## 충돌 방지 규칙

- Biome와 Prettier가 같은 파일을 포맷하지 않습니다.
- EditorConfig는 공통 규칙만 유지하고, 언어별 정밀 규칙은 각 포맷터 설정에 둡니다.
- Markdown은 `.editorconfig`의 `indent_style = tab`/`indent_size = 2` 규칙을 존중하도록
  Prettier 설정을 맞춥니다.

## 실행 순서(권장)

- Markdown: `prettier`.
- TypeScript: `biome check`.
- Java/Kotlin: `spotlessApply` -> `spotlessCheck`.

## IDE 설정 가이드(요약)

- IntelliJ IDEA
    - EditorConfig 지원 활성화.
    - Prettier 설정은 프로젝트의 `.prettierrc.yml`을 사용.
    - Gradle 태스크로 `spotlessApply` 실행을 등록(저장 시 혹은 수동).
    - Java/Kotlin 포맷은 IDE 내장보다 Spotless 결과를 기준으로 맞춥니다.
- VSCode(Antigravity)
    - EditorConfig 확장 활성화.
    - Biome 확장으로 TS/JS 포맷/린트.
    - Prettier는 Markdown 파일에만 적용하도록 설정.
    - Prettier 설정은 프로젝트의 `.prettierrc.yml`을 사용.

## Prettier + EditorConfig 운영 가이드

Markdown은 Prettier로만 포맷하고, `.editorconfig` 규칙을 따라가도록 설정합니다.

### Prettier 설정 예시(.prettierrc.yml)

```yaml
proseWrap: preserve
```

### Prettier 적용 범위 제한 예시(.prettierignore)

```text
*
!**/*.md
!**/*.yml
!**/*.yaml
```

## IDE 포맷 동일화

### VSCode(Antigravity) 예시

```json
{
    "editor.formatOnSave": true,
    "[markdown]": {
        "editor.defaultFormatter": "esbenp.prettier-vscode"
    },
    "[typescript]": {
        "editor.defaultFormatter": "biomejs.biome"
    },
    "[javascript]": {
        "editor.defaultFormatter": "biomejs.biome"
    }
}
```

### IntelliJ IDEA 요약

- EditorConfig 활성화.
- Markdown은 Prettier로 포맷(코드 리포맷 시 Prettier 실행).
- Java/Kotlin은 Spotless 기준을 따르도록 Gradle 태스크(예: `spotlessApply`)를 외부 툴로 등록.

## 실행 커맨드

- Markdown/YAML 포맷: `prettier --write **/*.{md,yml,yaml}`
- 스크립트: `bun run check:markdown`

## 태스크 세분화

1. 범위 확정
    - Prettier 적용 범위(Markdown만)와 Biome 범위 확정.
2. 설정 파일 정리
    - Prettier 설정 파일 도입(필요 시)과 `.editorconfig` 정합성 확인.
3. IDE 가이드 구체화
    - IntelliJ/VSCode에서 프로젝트 기준을 따르는 설정 예시 추가.
4. 실행 커맨드 문서화
    - 문서에 명시할 실행 커맨드와 스크립트(예: `bun run check`, `./gradlew spotlessApply`) 확정.
5. CI 연동
    - 변경 범위에 맞는 최소 검사 파이프라인 정의.

## 참고 파일

- EditorConfig: `./.editorconfig`
- Prettier 설정: `./.prettierrc.yml`
- Biome 설정: `./biome.json`
- Spotless 설정:
  `./build-logic/src/main/kotlin/buildlogic.formatter-conventions.gradle.kts`
