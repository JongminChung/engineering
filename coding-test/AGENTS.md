# AGENTS.md

## 구조

- 알고리즘 풀이 소스는 `study/coding-test/src/main/java`에 위치합니다.
- 테스트/스터디 노트는 `study/coding-test/src/test/java`에 둡니다.
- 마크다운 노트는 `study/coding-test/docs` 아래에 둡니다.
- 빌드 산출물은 `study/coding-test/build/`에 생성되며 커밋하지 않습니다.

## 명령어

- 전체 테스트: `./gradlew :study:coding-test:test`
- 단일 테스트: `./gradlew :study:coding-test:test --tests "...SolutionTest"`
- 빌드: `./gradlew :study:coding-test:build`

## 코딩 규칙

- 패키지는 문제 출처/주제를 반영합니다(예: `algorithm.codility.binary_gap`).
- 풀이 클래스명은 `Solution`을 유지합니다.
- 테스트는 주제별로 명확한 이름을 사용합니다(예: `SolutionTest`, `ArrayTest`).
- 포맷은 Spotless와 `.editorconfig`를 따릅니다.

## 테스트 규칙

- JUnit Jupiter를 사용합니다.
- 작은 입력/출력을 명시한 테스트를 선호합니다.
- 주제(배열/큐/시뮬레이션 등)에 맞춰 케이스를 구성합니다.

## 문서

- 새 카테고리 추가 시 `study/coding-test/docs`에 설명을 보강합니다.
