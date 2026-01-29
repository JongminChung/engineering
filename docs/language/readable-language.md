# Readable Language

## Python

오픈소스 코드를 읽고 이해하는 관점에서 Python의 기본 구성과 관용 패턴을 정리한다.

- 실행 모델: 인터프리터 방식이며 모듈 단위로 로드된다. `python -m 패키지`로 엔트리포인트가 실행되기도 한다.
- 패키지/모듈 구조: 폴더가 패키지이며 `__init__.py`가 있으면 패키지로 인식된다. 상대/절대 import가 혼재한다.
- 타입 힌트: `typing` 기반의 타입 힌트는 선택사항이며, 런타임 강제가 아니다. `mypy`, `pyright`로 정적 검사한다.
- 가상환경: `venv`/`virtualenv`가 표준이며 의존성 파일은 `requirements.txt`나 `pyproject.toml`(Poetry)로 관리한다.

**읽을 때 자주 보는 문법**

- 컨텍스트 매니저: `with open(...) as f:` 형태로 리소스 관리.
- 데코레이터: `@dataclass`, `@classmethod`, `@property` 등 함수/클래스 동작을 감싼다.
- 리스트/딕셔너리 컴프리헨션: `[x for x in xs if cond]`, `{k: v for ...}`.
- 제너레이터: `yield`로 lazy iteration을 구현하며 대규모 데이터 처리에 많이 쓴다.
- 예외 처리: `try/except/finally`가 일반적이며, 예외 타입이 넓게 잡히는지 확인한다.

**오픈소스 코드 읽기 팁**

- 엔트리포인트 찾기: `pyproject.toml`, `setup.cfg`, `setup.py`의 `console_scripts`를 확인한다.
- 설정/환경 변수: `os.environ`, `dotenv` 사용 여부를 확인한다.
- 비동기: `async def`, `await`, `asyncio` 기반인지 확인한다.
- 테스트: `pytest`가 사실상 표준이며 `tests/` 구조가 일반적이다.

Java/TS/Kotlin 관점에서의 대응

- 클래스보다 함수 중심 구조가 흔하다. 유틸 함수 파일이 많다.
- 인터페이스 대신 덕 타이핑에 의존하는 경우가 많아 실제 사용처를 따라가야 한다.
- 모듈간 순환 import가 존재할 수 있어 import 순서가 중요할 때가 있다.

순환 import 주의 사항

- A가 B를 import하고 B가 다시 A를 import하면 로딩 순서에 따라 `AttributeError`가 발생할 수 있다.
- 오픈소스 코드에서 문제가 생기면 import를 함수 내부로 이동하거나, 타입 힌트만 `typing.TYPE_CHECKING` 블록으로 분리해 해결하는 경우가 많다.

덕 타이핑 예시

```python
def read_all(source):
    return source.read()

class MemoryBuffer:
    def __init__(self, data: str):
        self._data = data
    def read(self) -> str:
        return self._data

read_all(MemoryBuffer("hello"))
```

- `read_all`은 타입 선언 없이도 `read()`만 있으면 동작한다.
- 타입 힌트를 써도 런타임 강제는 아니며, 정적 검사기에서만 확인된다.

Protocol 기반 예시

```python
from typing import Protocol

class Readable(Protocol):
    def read(self) -> str: ...

def read_all(source: Readable) -> str:
    return source.read()
```

- `Protocol`은 필요한 메서드만 정의해 구조적 타이핑 계약을 만든다.
- 런타임에서는 강제되지 않지만 `mypy`, `pyright`가 타입 불일치를 잡는다.

## Go

오픈소스 코드를 읽고 이해하는 관점에서 Go의 기본 구성과 관용 패턴을 정리한다. Go 1.20+ 기준으로 설명한다.

- 실행 모델: 컴파일 언어이며 `main` 패키지의 `func main()`이 엔트리포인트다.
- 모듈/패키지: `go.mod`가 모듈 루트이며 import 경로는 모듈 경로와 폴더 구조가 동일하다.
- 타입 시스템: 정적 타입, 인터페이스는 구조적 타이핑(implicit)이다.
- 의존성: `go.mod`와 `go.sum`으로 관리한다. `go list`/`go mod`로 조회한다.
- 포맷/정적분석: `gofmt`가 표준이며 `go vet`, `golangci-lint`를 자주 본다.

읽을 때 자주 보는 문법

- 오류 처리: `if err != nil { return ... }` 패턴이 표준이다.
- 다중 반환: `func f() (T, error)` 형태가 많다.
- 고루틴/채널: `go f()`로 비동기 실행, `chan`으로 통신한다.
- 구조체와 메서드: `type Foo struct {}`와 `func (f *Foo) Method()` 패턴.
- 인터페이스: 작은 인터페이스를 조합하는 경우가 많다. `io.Reader`, `context.Context` 등을 확인한다.

오픈소스 코드 읽기 팁

- 모듈 루트 찾기: `go.mod` 위치를 먼저 확인한다.
- 엔트리포인트 찾기: `package main`과 `func main()`을 찾는다.
- 컨텍스트 전파: `context.Context`가 주요 함수 시그니처에 포함되어 있으면 취소/타임아웃 흐름을 추적한다.
- 로깅: `log`, `zap`, `zerolog` 등 라이브러리를 확인한다.
- 테스트: `_test.go` 파일과 `TestXxx` 함수를 확인한다.

Java/TS/Kotlin 관점에서의 대응

- 클래스 기반이 아니라 패키지/함수 중심이며, 명시적 생성자보다 `NewXxx` 팩토리 함수가 많다.
- 런타임 DI보다는 직접 의존성 주입(함수 인자 전달)을 흔히 사용한다.
- 예외가 없고 에러 반환 기반이라 호출 스택을 따라가며 `err` 흐름을 본다.

## Java 기준 매핑

Java에 익숙한 관점에서 Python/Go를 읽을 때의 대응 개념을 정리한다.

- 패키지/모듈: `package`(Java) ≈ `package`(Go), `패키지 디렉터리`(Python). Python은 패키지 경계가 느슨해 상대 import를 주의한다.
- 엔트리포인트: `public static void main`(Java) ≈ `func main`(Go), `python -m`/`console_scripts`(Python).
- 인터페이스: Java `interface` ≈ Go의 implicit interface, Python은 `Protocol`/덕 타이핑으로 동작 계약을 표현한다.
- 예외/에러: Java 예외 체계 ≈ Go의 `error` 반환, Python은 예외 기반이지만 `try/except` 범위가 넓다.
- DI/구성: Java(Spring) ≈ Go는 함수 인자 전달, Python은 전역 설정/팩토리/DI 프레임워크가 혼재한다.
- 빌드/의존성: Gradle/Maven ≈ `go mod`, Python은 `pyproject.toml`/`requirements.txt` 기반이다.
