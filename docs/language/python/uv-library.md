# uv 기반 Python 라이브러리 운영 가이드

목적: Poetry 멀티 모듈 운영 관점과 비교하면서,
uv 생태계에서 라이브러리를 설계/배포/소비하는 de facto를 정리합니다.

## 1) Python 패키징 de facto

- 메타데이터는 `pyproject.toml`(PEP 621) 기준으로 관리합니다.
- 소스는 `src/<package>/` 레이아웃을 기본으로 사용합니다.
- 배포 산출물은 `wheel` + `sdist`를 함께 제공합니다.
- 테스트는 `pytest`, 린트/포맷은 `ruff`, 타입체크는 `mypy` 또는 `pyright` 조합이 일반적입니다.

`sdist`와 `wheel`:

- `sdist`: 소스 배포본(`.tar.gz`). 설치 시 로컬 빌드가 필요할 수 있습니다.
- `wheel`: 빌드 배포본(`.whl`). 보통 설치가 더 빠르고 재현성이 좋습니다.

## 2) Node(TS) 배포 방식과 Python 대응

Node(TS)에서 보통 말하는 흐름은 대체로 맞습니다.

- `npm pack`/`npm publish`는 패키지를 `.tgz`로 배포합니다.
- 소비자는 설치 후 `package.json`의 `exports`(또는 `main`)를 통해 진입점을 해석합니다.
- `dist`는 "관례적인 빌드 출력 디렉터리"이지 Node가 강제하는 표준 키는 아닙니다.
- TypeScript 관점에서는 `dist`의 JS 산출물 + `.d.ts`(`types` 또는 export 조건)가 함께 맞아야 소비자가 제대로 사용합니다.

TS 라이브러리에서 꼭 맞춰야 할 항목:

- 런타임 산출물(JS): `import`(ESM)와 `require`(CJS) 소비 경로를 명확히 분리합니다.
- 타입 산출물(`.d.ts`): JS 엔트리와 동일한 공개 경계를 가지도록 맞춥니다.
- 엔트리 선언: `exports`에 런타임 경로를 선언하고, 타입 경로는 `types` 또는 `exports`의 조건으로 연결합니다.
- 산출물 구조: `dist/index.js`, `dist/index.cjs`, `dist/index.d.ts`처럼 파일 대응이 깨지지 않게 유지합니다.

`package.json` 예시(dual package):

```json
{
    "name": "my-lib",
    "version": "1.0.0",
    "type": "module",
    "main": "./dist/index.cjs",
    "module": "./dist/index.js",
    "types": "./dist/index.d.ts",
    "exports": {
        ".": {
            "types": "./dist/index.d.ts",
            "import": "./dist/index.js",
            "require": "./dist/index.cjs"
        }
    }
}
```

고급 예시 1: subpath exports + 타입 경로 동기화

```json
{
    "name": "my-lib",
    "version": "1.0.0",
    "type": "module",
    "exports": {
        ".": {
            "types": "./dist/index.d.ts",
            "import": "./dist/index.js",
            "require": "./dist/index.cjs"
        },
        "./react": {
            "types": "./dist/react.d.ts",
            "import": "./dist/react.js",
            "require": "./dist/react.cjs"
        },
        "./package.json": "./package.json"
    },
    "types": "./dist/index.d.ts"
}
```

의미:

- `import { x } from "my-lib"`와 `import { y } from "my-lib/react"`를 둘 다 공식 경로로 공개합니다.
- 각 공개 경로마다 JS와 `.d.ts`를 1:1로 맞춰야 합니다.
- `./package.json`을 열어두면 메타데이터를 읽는 도구와의 호환성이 좋아집니다.

고급 예시 2: `typesVersions`로 구버전 TS 호환

```json
{
    "types": "./dist/index.d.ts",
    "typesVersions": {
        "*": {
            "react": ["dist/react.d.ts"],
            "*": ["dist/index.d.ts"]
        }
    }
}
```

실무 체크리스트:

- `exports`에 연 서브패스마다 대응 `.d.ts`가 있는지 확인합니다.
- 문서 import 예제와 `exports` 공개 경로가 완전히 일치하는지 확인합니다.
- ESM/CJS 둘 다 지원하면 `import`/`require` 결과 타입이 같은지 확인합니다.

운영 시 자주 생기는 문제:

- JS 엔트리는 맞는데 `types`가 누락되어 TS 소비자가 `any`로 떨어짐
- `exports`에 서브패스(`./foo`)를 안 열어두었는데 문서에서 해당 경로 import를 안내함
- `.d.ts` 경로와 실제 JS 공개 경계가 달라 자동완성/타입체크와 런타임 동작이 불일치함

Python 대응:

- 배포는 `uv build`로 만든 `sdist`/`wheel`을 인덱스에 올립니다.
- 소비자는 설치 후 `import my_pkg`처럼 패키지 경로를 사용합니다.
- JS의 `exports`처럼 단일 강제 필드는 없고,
  `__init__.py`, `__all__`, 패키지 구조, entry points가 공개 경계를 만듭니다.

즉 "tarball 업로드 후 공개 진입점으로 소비"라는 큰 흐름은 유사하지만,
Python은 공개 경계 선언이 분산되어 있어 운영 규칙이 더 중요합니다.

## 3) Poetry 멀티 모듈 기준 비교

Poetry와 uv를 모노레포 관점에서 보면 접근이 다릅니다.

- Poetry: 각 패키지 `pyproject.toml` + path dependency 조합이 널리 사용됩니다.
- uv: workspace를 통해 루트에서 멤버를 관리하고 단일 lockfile로 동기화할 수 있습니다.

운영 기준:

- 패키지별 릴리즈 단위가 강하면 Poetry 방식(패키지별 독립)도 적합합니다.
- 모노레포 전체 동기화/재현성이 중요하면 uv workspace가 유리합니다.

## 4) 라이브러리 공개 API 규칙

Python에는 JS `exports` 같은 단일 강제 필드가 약하므로,
아래 조합으로 공개 API를 고정합니다.

- `src/<pkg>/__init__.py`에서 외부 공개 심볼만 재노출
- `__all__`로 공개 목록 고정
- README에 공식 import 경로 고정
- `_internal` 경로는 비공개 계약(호환성 보장 없음)

예시:

```python
# src/my_pkg/__init__.py
from .api import Client, create_client

__all__ = ["Client", "create_client"]
```

```python
from my_pkg import Client, create_client
```

## 5) Python에서 라이브러리를 가져오는 방법

소비자 기준 주요 방법:

- 인덱스 설치: PyPI/사설 인덱스에서 버전 지정 설치
- extras 설치: `my-pkg[redis]`처럼 선택 기능 포함 설치
- 로컬 경로 의존성: 모노레포 내부 패키지를 path로 연결
- editable 설치: 개발 중 즉시 반영이 필요할 때 사용
- git 의존성: 태그/커밋 기준으로 직접 참조

라이브러리 제공자 기준 체크:

- 기본 설치(`dependencies`)와 선택 설치(`optional-dependencies`)를 분리합니다.
- 공개 API에서 불필요한 구현 의존성 타입 노출을 줄입니다.

## 6) `__init__.py`, `__main__.py` 외에 꼭 볼 것

- `__init__.py`: 패키지 공개 API 집합(재노출, `__all__`)
- `__main__.py`: `python -m my_pkg` 실행 진입점
- `[project.scripts]`: CLI 커맨드 진입점 등록
- `[project.entry-points]`: 플러그인 확장 지점 정의
- `py.typed`: 타입 힌트 제공 패키지임을 소비자에게 알리는 마커
- namespace package: `__init__.py` 없이 패키지 분할 가능하지만 경계가 흐려질 수 있어 신중히 사용

`__main__.py` 예시:

```python
# src/my_pkg/__main__.py
from .cli import main

if __name__ == "__main__":
    raise SystemExit(main())
```

`pyproject.toml` 스크립트 예시:

```toml
[project.scripts]
my-pkg = "my_pkg.cli:main"
```

## 7) 구조 제약(ArchUnit 유사)

`_internal`은 기술적으로 import 가능하므로, CI에서 강제합니다.

- 계약 테스트: 공개 import 경로만 테스트
- 구조 테스트: `import-linter`로 외부 `_internal` import 금지

```ini
# .importlinter
[importlinter]
root_package = my_pkg

[contract:no_external_internal_import]
name = 외부 모듈의 internal 직접 import 금지
type = forbidden
source_modules =
    my_pkg.app
    my_pkg.feature
forbidden_modules =
    my_pkg._internal
```

```bash
lint-imports
```

## 8) uv 운영 기본 흐름

```bash
# 1. 의존성 동기화
uv sync

# 2. 테스트/정적 검사
uv run pytest
uv run ruff check .
uv run ruff format --check .

# 3. 배포 산출물 생성
uv build
```

릴리즈 전 최소 확인:

- 공개 import 예제가 실제로 동작하는지
- `wheel`/`sdist`가 함께 생성되는지
- extras 설치 경로가 정상 동작하는지

## 9) uv의 역할 경계

- `uv`는 의존성 해석, 가상환경, 실행, 빌드를 담당합니다.
- 공개 API/모듈 경계 강제는 `uv` 단독 기능이 아닙니다.
- 경계 강제는 패키지 구조 + 문서 + 테스트 + import-linter 조합으로 운영합니다.
