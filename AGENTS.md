# Repository Guidelines

## 범위와 우선순위

- 이 문서는 전역 규칙을 정의합니다. 모듈별 규칙은 각 디렉터리의 `AGENTS.md`를 우선합니다.
- `study/infra/AGENTS.md`
- `study/coding-test/AGENTS.md`
- `study/api-communication/AGENTS.md`
- `docs/AGENTS.md`

## 전역 커뮤니케이션 규칙

- 답변은 한글로 작성합니다.
- 내부 추론이나 고민은 영어로 수행하고, 결과만 한글로 전달합니다.
- 모호한 요청은 작업 전에 확인 질문을 합니다.
- 변경은 최소 단위로, 이유와 영향 범위를 함께 설명합니다.

## 유비쿼터스 제약 (전체 공통)

- 빌드 산출물(`**/build/`)과 생성물은 커밋하지 않습니다.
- 공통 스타일은 `.editorconfig`를 따릅니다.
- Java 포맷팅은 Spotless(Gradle) 기준을 따릅니다.
- 문서 포맷팅은 `pnpm run check:markdown` 기준을 따릅니다.
- 커밋 메시지는 Conventional Commits를 준수합니다.
- 보안 정보(키, 토큰, 비밀번호)는 절대 저장소에 남기지 않습니다.

## 기본 명령어

- `./gradlew build` — 전체 모듈 빌드 및 테스트 실행.
- `./gradlew test` — 전체 테스트 실행.
- `pnpm install` — 문서/포맷팅 도구 설치.
- `pnpm run check` — Biome 포맷/린트.
- `pnpm run check:markdown` — Markdown 자동 정리.
