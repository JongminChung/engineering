# AGENTS.md

## 범위

- `frontend/` 내 TypeScript 유틸/실험 코드에 대한 규칙입니다.

## 코딩/포맷

- TypeScript는 `tsconfig.json`을 기준으로 작성합니다.
- 포맷은 루트의 `biome.json`과 `.editorconfig`를 따릅니다.
- 실행 환경은 `package.json`에 정의된 의존성을 기준으로 합니다.

## 작업 원칙

- 간단한 스크립트/유틸은 파일 단위로 명확히 분리합니다.
- 외부 서비스 접근이 필요하면 호출 부를 분리하고, 기본값은 안전하게 둡니다.
