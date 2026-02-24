# Smithy

## 개요

- Smithy는 서비스 모델(계약)을 정의하고, 그 모델로부터 서버/클라이언트/문서 등을 생성하는 IDL 기반 툴체인이다.
- OpenAPI/gRPC를 대체하기보다는 상위 수준에서 API 계약을 정의하고, 필요하면 OpenAPI/SDK/서버 구현으로 변환해 쓰는
  방향이다.
- Smithy는 모델(Shape/Service/Operation)과 프로토콜(HTTP/JSON, RPC 등)을 분리한다. 모델은 동일하게
  유지하면서 전송 프로토콜을 바꿀 수 있다.

## Smithy에서 말하는 IDL(Interface Definition Language)

IDL은 "서비스가 무엇을 제공하는지"를 언어 중립적으로 정의하는 계약이다. Smithy IDL은 다음을 핵심으로 한다.

- Service/Operation/Shape로 API의 구조를 정의한다.
- Traits로 제약(@required, @pattern), 프로토콜(@http, @restJson1), 에러(@error), 리소스(
  @resource) 등을 부여한다.
- 모델은 구현 코드와 분리되며, 동일한 모델로 코드 생성(서버/클라이언트/문서)과 검증을 수행한다.
- HTTP 세부사항(라우팅, 메서드, 직렬화)을 모델에 직접 작성하거나, 프로토콜 트레이트에 위임할 수 있다.

## IDL/프로토콜 예시 (HTTP/1.1, HTTP/2, HTTP/3, gRPC)

### HTTP/1.1 (REST+JSON, restJson1)

Smithy 모델(예시):

```smithy
$version: "2.0"
namespace example
use aws.protocols#restJson1

@restJson1
service CoffeeShop {
  version: "2024-08-23"
  operations: [GetMenu]
}

@http(method: "GET", uri: "/menu")
@readonly
operation GetMenu {
  output := { items: CoffeeItems }
}

list CoffeeItems { member: CoffeeItem }
structure CoffeeItem {
  @required
  type: String
  @required
  description: String
}
```

HTTP/1.1 요청/응답(예시):

```http
GET /menu HTTP/1.1
Host: example.com
Accept: application/json

HTTP/1.1 200 OK
Content-Type: application/json

{"items":[{"type":"LATTE","description":"..."}]}
```

### HTTP/2

Smithy 모델은 동일하게 유지하고, 전송 계층을 HTTP/2로 선택한다.
(예: 동일한 @http/@restJson1 모델을 HTTP/2로 배포)

HTTP/2 요청 헤더(의사 헤더 예시):

```http
:method: GET
:path: /menu
:scheme: https
:authority: example.com
accept: application/json
```

핵심은 "IDL은 동일하고, 전송 프로토콜 버전(HTTP/1.1 vs HTTP/2)은 런타임/서버 설정"이라는 점이다.

### HTTP/3

HTTP/3는 QUIC 기반이다. Smithy 모델은 HTTP/2와 동일하게 유지 가능하다.
전송만 HTTP/3로 바꾸면 된다.

HTTP/3 의사 헤더(HTTP/2와 동일한 형태, 전송만 QUIC):

```http
:method: GET
:path: /menu
:scheme: https
:authority: example.com
accept: application/json
```

### gRPC (HTTP/2 기반)

Smithy 모델(예시):

```smithy
$version: "2.0"
namespace example

service CoffeeShop {
  version: "2024-08-23"
  operations: [GetOrder]
}

operation GetOrder {
  input := { id: String }
  output := { id: String, status: String }
}
```

이 모델을 gRPC 프로토콜에 맞게 변환하면(예: smithy->proto 코드 생성) 아래와 유사한 proto가 된다.

```proto
syntax = "proto3";

service CoffeeShop {
  rpc GetOrder (GetOrderRequest) returns (GetOrderResponse);
}

message GetOrderRequest { string id = 1; }
message GetOrderResponse { string id = 1; string status = 2; }
```

즉, Smithy는 IDL로 계약을 정의하고, 필요 시 gRPC 프로토콜(HTTP/2) 형식으로 변환해 사용할 수 있다.

## Full Stack Application PoC (로컬 문서 기준)

참고: `study/smithy/Full Stack Application - Smithy 2.0.html`

### 0) init 이후 로컬 수정 사항

- `Makefile`에서 yarn 호출을 bun으로 변경했다.
  `study/smithy/full-stack-application/Makefile`
- 서버 start 스크립트의 yarn 호출을 bun으로 변경했다.
  `study/smithy/full-stack-application/server/package.json`
- 포트 리스닝 제약 때문에 서버를 `127.0.0.1`로 바인딩하도록 변경했다.
  `study/smithy/full-stack-application/server/src/index.ts`

- 코드 생성 산출물의 `yarn:` 스크립트를 bun으로 임시 변경했다. (`smithy build`를 다시 실행하면 덮어써짐)

- `study/smithy/full-stack-application/smithy/build/smithy/source/typescript-ssdk-codegen/package.json`
- `study/smithy/full-stack-application/smithy/build/smithy/source/typescript-client-codegen/package.json`

- 코드 생성 산출물을 bun 스크립트로 보정하는 스크립트를 추가했다.
    - `study/smithy/full-stack-application/scripts/postprocess-codegen-bun.js`
- `build-smithy`에서 코드 생성 직후 보정 스크립트를 실행하도록 변경했다.
- bun 워크스페이스로 인한 충돌을 피하려고 `bun install --ignore-workspace`를 사용했다.
- 클라이언트 SDK 타입 빌드 오류(TS2742)는 `bun install --config.node-linker=hoisted`로 해결하고,
  `build-client`에 반영했다.

### 1) 프로젝트 생성

```sh
smithy init -t full-stack-application
```

```sh
make init
```

- `make init` 단계에서 `.patches/start.patch`가 `smithy/smithy-build.json` 버전 차이로
  실패했다.
- 결과적으로 템플릿이 이미 완료 상태에 가까워, 이후 단계는 현재 상태 기준으로 진행했다.

### 2) 모델 빌드

```sh
cd smithy
smithy build
```

- `build/smithy/source` 아래에 JSON AST 및 코드 생성 결과가 생성됨.
- 경고: `aws.api#service` 트레이트 미정의 경고(튜토리얼과 동일한 정상 경고로 간주).

### 3) 서버 SDK(SSDK)와 서버 빌드

- Makefile은 yarn 대신 bun으로 교체.
- 코드 생성 산출물(package.json)의 `yarn:` 스크립트를 `bun run`으로 임시 변경 필요.

```sh
cd server/ssdk
bun install --ignore-workspace
bun build

cd ../
bun install --ignore-workspace
bun build
```

서버 실행은 로컬 포트 리스닝 권한 이슈로 `127.0.0.1` 바인딩이 필요해 `server/src/index.ts`를 조정했다.

### 4) 서버 실행 및 cURL 확인

```sh
node dist/index.js
```

```sh
curl http://127.0.0.1:3001/menu
```

응답 예시:

```json
{
    "items": [
        {
            "type": "DRIP",
            "description": "..."
        }
    ]
}
```

### 5) 클라이언트 호출 확인

```sh
node -e "const { CoffeeShop } = require('@com.example/coffee-shop-client');
const client = new CoffeeShop({ endpoint: { protocol: 'http', hostname: '127.0.0.1', port: 3001, path: '/' } });
client.getMenu()
  .then(res => (console.log('getMenu', res), client.createOrder({ coffeeType: 'DRIP' })))
  .then(res => (console.log('createOrder', res), client.getOrder({ id: res.id })))
  .then(res => console.log('getOrder', res))
  .catch(console.error);"
```

- 초기 `getOrder`는 `IN_PROGRESS`였다가, 짧은 대기 후 `COMPLETED`로 전환됨을 확인.

### 6) 웹 앱 빌드/실행

```sh
cd app
bun install --ignore-workspace
bun build
bun start
```

- `bun build` 시 Google Fonts 다운로드가 필요하여 네트워크 접근 허용 필요.
- `curl http://127.0.0.1:3000`로 HTML 반환 확인.

## PoC 중 확인된 환경 제약

- 로컬 포트 리스닝/접속에 샌드박스 제한이 있어, 실행/요청 시 권한 해제가 필요했다.
- 코드 생성 패키지의 `yarn:` 스크립트는 bun 환경에서 수동 조정이 필요했다.
- 클라이언트 SDK 타입 빌드 시 bun의 기본 node-linker에서 TS2742 오류가 발생해 `node-linker=hoisted`로
  재설치해 해결했다.

## 재현 스크립트

`study/smithy/run-poc.sh`는 아래 단계를 순서대로 실행한다.

- smithy build + codegen 보정
- 서버/클라이언트 SDK 빌드
- 서버/앱 실행 및 간단한 HTTP 확인
- 실행된 프로세스 정리
