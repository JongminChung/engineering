# 01-시작하기

Go를 시작할 때 필요한 기본 개념과, 소스 코드로 Go를 빌드하는 흐름을 정리합니다.

## 기본 프로젝트 만들기

```bash
mkdir go-sandbox
cd go-sandbox
go mod init go-sandbox
```

```go
package main

import "fmt"

func main() {
    fmt.Println("hello, go")
}
```

- 참고: [표준 프로젝트 레이아웃](https://github.com/golang-standards/project-layout)

## 실행과 빌드

```bash
# go-sandbox/helloworld 디렉터리에서 실행
go run .
# hello, go

go build -o bin/hello .
./bin/hello
# hello, go
```

## 변수 선언

`var`로 타입을 명시하거나, `:=`로 타입을 추론해 선언할 수 있습니다. 선언만 하면 기본값이 들어갑니다(숫자 0, bool false, string "").

```bash
go run 01-시작하기/main.go
```

## 상수 선언과 타입 추론

`const`는 컴파일 타임 상수이며, 선언 시 타입을 생략하면 사용 위치에서 타입이 결정됩니다.

```go
const maxRetry = 3          // untyped 상수
const pi float64 = 3.14159  // typed 상수

count := maxRetry          // int로 추론
ratio := pi / 2            // float64 유지
```

## 명시적 형변환

Go는 암시적 형변환을 허용하지 않습니다. 타입이 다르면 연산/비교가 불가능하며, 의도를 드러내기 위해 명시적으로 변환해야 합니다.

```go
func valueOfPi(multiplier uint) float32 {
    return 3.14159 * float32(multiplier)
}

var a int = 1
var b uint = 2
// if a < b { } // 컴파일 에러: mismatched types
```

## 제네릭

Go 1.18부터 타입 파라미터를 사용할 수 있으며, 제약(constraint)을 통해 허용 타입을 제한합니다.

```go
type Number interface {
    ~int | ~float64
}

func Sum[T Number](values []T) T {
    var total T
    for _, v := range values {
        total += v
    }
    return total
}

fmt.Println(Sum([]int{1, 2, 3}))
fmt.Println(Sum([]float64{1.5, 2.5}))
```

## 구조체

구조체는 필드를 묶는 값 타입입니다. 리터럴로 생성하고, 포인터로 접근하면 원본을 수정할 수 있습니다.

```go
type Profile struct {
    Name string
    Age  int
}

func (p Profile) Greeting() string {
    return fmt.Sprintf("%s(%d)", p.Name, p.Age)
}

profile := Profile{Name: "Gopher", Age: 2}
profilePtr := &Profile{Name: "Gopher", Age: 3}
profilePtr.Age++

fmt.Println(profile.Greeting(), profilePtr.Greeting())
```

## 포인터 (Go vs C)

- Go 포인터는 참조 전달은 가능하지만 포인터 연산과 임의 메모리 접근은 금지됩니다.
- 포인터 필드는 자동 역참조되며, `nil` 접근은 런타임 패닉이므로 직접 체크해야 합니다.
- 메모리는 가비지 컬렉션으로 관리되고, `unsafe` 없이 주소 연산을 할 수 없습니다.

```go
type Counter struct {
    Value int
}

func newCounter(value int) *Counter {
    return &Counter{Value: value}
}

counter := Counter{Value: 1}
ptr := &counter
ptr.Value += 2 // 자동 역참조

var empty *Counter
fmt.Println(ptr.Value, empty == nil)

// ptr++           // 컴파일 에러: 포인터 연산 불가
// *(*int)(0x1234) // 컴파일 에러: 임의 주소 접근 불가
```

## 반복문 형태

Go는 `for` 하나로 for-in, while, do-while 패턴을 모두 표현합니다.

```go
items := []string{"go", "java", "rust"}
for i, item := range items { // for-in
    fmt.Println(i, item)
}

for i := range items {
    fmt.Println(i) // index만 필요할 때
}

for _, item := range items {
    fmt.Println(item) // value만 필요할 때
}

count := 0
for { // do-while/repeat-while 패턴
    count++
    fmt.Println("count", count)
    if count >= 2 {
        break
    }
}
```

## 자바와 타입/배열 차이

- Go의 배열은 길이가 타입의 일부인 고정 길이 값 타입이며, 복사 시 값 전체가 복사됩니다.
- Go의 슬라이스는 길이가 늘어날 수 있고 참조처럼 동작해 자바 배열과 유사합니다.
- `nil`은 포인터, 슬라이스, 맵, 채널, 인터페이스 같은 참조 타입에만 가능합니다.

Go는 모든 타입이 일급 시민이며, 프리미티브/래퍼 구분이 없습니다.

```go
var a int = 10           // 그냥 int
var b *int = &a          // 포인터가 필요하면 명시적으로

// 제네릭에서도 int 직접 사용 가능 (Go 1.18+)
func Sum[T int | float64](values []T) T {
    var total T
    for _, v := range values {
        total += v
    }
    return total
}
```

| 특성            | Java                             | Go                                        |
| --------------- | -------------------------------- | ----------------------------------------- |
| **기본 타입**   | `int`, `long`, `double` 등 8개   | `int`, `int8`, `int64`, `float32` 등 다양 |
| **래퍼 클래스** | `Integer`, `Long` 등 필요        | ❌ 없음 (불필요)                          |
| **null 가능**   | 래퍼만 가능 (`Integer` → `null`) | 포인터만 가능 (`*int` → `nil`)            |
| **제네릭 사용** | 래퍼 클래스만 가능               | 모든 타입 직접 사용                       |
| **박싱/언박싱** | 자동 발생 (성능 오버헤드)        | ❌ 없음                                   |
| **제로값**      | 명시적 초기화 필요               | 자동 제로값 (`int` → `0`)                 |

Go는 Slice라는 독특한 구조를 사용합니다. 배열을 기반으로 하지만 동적으로 확장됩니다.

```go
// Go - Slice (동적 배열)
slice := []string{"Hello", "World"}
slice = append(slice, "Go")   // 동적 추가
slice[0]                       // "Hello"
len(slice)                     // 3
cap(slice)                     // capacity (내부 배열 크기)

// 배열은 고정 크기 (거의 사용 안 함)
var arr [10]string            // 크기가 타입의 일부!
```

**Slice의 내부 구조**

```go
// Slice는 사실 이런 구조체
type slice struct {
    array unsafe.Pointer  // 실제 배열을 가리키는 포인터
    len   int             // 현재 길이
    cap   int             // 용량 (capacity)
}
```

| 특성         | Java `ArrayList`     | Go `Slice`                      |
| ------------ | -------------------- | ------------------------------- |
| **타입**     | 클래스 (참조 타입)   | 내장 타입 (값처럼 동작)         |
| **선언**     | `new ArrayList<>()`  | `[]T{}` 또는 `make([]T, len)`   |
| **추가**     | `list.add(x)`        | `slice = append(slice, x)`      |
| **길이**     | `list.size()`        | `len(slice)`                    |
| **용량**     | 내부적으로 관리      | `cap(slice)`로 직접 확인        |
| **슬라이싱** | `list.subList(1, 3)` | `slice[1:3]` (매우 간결)        |
| **메모리**   | 객체 오버헤드 있음   | 24바이트 헤더만 (경량)          |
| **nil 체크** | `list == null`       | `slice == nil`                  |
| **빈 값**    | `new ArrayList<>()`  | `[]T{}` 또는 `nil` (둘 다 가능) |

## 기본 개념 요약

- 모듈은 `go.mod`가 있는 디렉터리를 기준으로 합니다.
- 패키지는 디렉터리 단위로 구성합니다.
- `gofmt`로 포맷을 일관되게 유지합니다.
- `go test ./...`로 전체 테스트를 실행합니다.
