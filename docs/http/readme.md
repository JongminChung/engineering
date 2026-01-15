# HTTP

**_Table of Contents_**

<!-- TOC -->

- [HTTP](#http)
    - [Chunked responses](#chunked-responses)
    - [Chunked를 무조건 써야 하는 거 아니야?](#chunked를-무조건-써야-하는-거-아니야)
    - [HTTP/1.1 파이프라이닝이 제공된다 했는데 진짜로?](#http11-파이프라이닝이-제공된다-했는데-진짜로)
        - [왜 파이프라이닝을 안 쓸까?](#왜-파이프라이닝을-안-쓸까)
        - [브라우저의 대안: 연결 병렬화 (Parallel Connections)](#브라우저의-대안-연결-병렬화-parallel-connections)
    - [Keep-Alive는 항상 지켜져야 되는거 아니야?](#keep-alive는-항상-지켜져야-되는거-아니야)
    _ [브라우저나 React에서 커넥션을 자동으로 재사용하나?](#브라우저나-react에서-커넥션을-자동으로-재사용하나)
    _ [서버 사이드에서의 커넥션 Keep-Alive 자동을 재사용하나?](#서버-사이드에서의-커넥션-keep-alive-자동을-재사용하나)
    _ [1. 커넥션 풀을 사용하지 않을 때의 문제 (Short-lived Connections)](#1-커넥션-풀을-사용하지-않을-때의-문제-short-lived-connections)
    _ [2. 왜 서버 사이드에서는 "자동"이 아닌가?](#2-왜-서버-사이드에서는-자동이-아닌가)
    _ [3. 주요 환경별 커넥션 풀 설정](#3-주요-환경별-커넥션-풀-설정)
    _ [애플리케이션 레벨의 연결 관리 및 재시도 전략](#애플리케이션-레벨의-연결-관리-및-재시도-전략)
    _ [1. 왜 애플리케이션 레벨 검증이 필요한가?](#1-왜-애플리케이션-레벨-검증이-필요한가)
    _ [2. DB vs HTTP의 검증 방식 차이](#2-db-vs-http의-검증-방식-차이)
    _ [3. 재시도 전략 (Retry Strategy)](#3-재시도-전략-retry-strategy)
    _ [4. Retry 패턴과 지수 백오프 (Exponential Backoff)](#4-retry-패턴과-지수-백오프-exponential-backoff)
        <!-- TOC -->

## Chunked responses

청크는 “텍스트”가 아님

- 바이너리 안전(binary-safe)
- Content-Type에 전혀 의존하지 않음

```text
# 응답 헤더
HTTP/1.1 200 OK
Content-Type: text/plain
Transfer-Encoding: chunked

# 응답 본문 (청크 단위)

4
Wiki
5
pedia
E
 in chunks.
0
```

**구조 설명**

```text
[chunk size (hex)]
[chunk data]
```

- 4: "Wiki" (4 bytes)
- 5: "pedia" (5 bytes)
- E: " in chunks." (11 bytes)
- 0: End of response

**클라이언트가 받는 최종 데이터**

```text
Wikipedia in chunks.
```

## Chunked를 무조건 써야 하는 거 아니야?

HTTP/1.1 chunked는 framing 비용이 있음.

1. 작은 청크가 많을수록 오버헤드 증가함.
   특히 **소형 JSON API**에서는 손해
2. 중간 장비/캐시 친화적이지 않음.
   CDN/Reverse Proxy에서는 Content-Length가 있어야 캐시 최적화 쉬움.
    - **데이터 무결성:** 전체 데이터 크기를 미리 알아야 전송 중단 시 불완전한 캐싱을 방지할 수 있음.
    - **리소스 할당:** 응답 크기에 맞춰 메모리나 디스크 공간을 효율적으로 사전 예약 가능함.
    - **부분 요청 처리:** 대용량 파일의 `Range` 요청(일부분만 가져오기) 지원이 용이함.

## HTTP/1.1 파이프라이닝이 제공된다 했는데 진짜로?

실제 브라우저 동작 (HTTP/1.1)

1. HTML 요청
2. HTML 수신 중 파싱
3. <img>, <script> 발견
4. 여러 TCP 연결을 병렬로 열어 요청
    - 보통 host당 6개 내외

👉 **연결 병렬화(Parallel Connections), 파이프라이닝 아님**

### 왜 파이프라이닝을 안 쓸까?

1. **HOL (Head-of-Line) Blocking**: 파이프라이닝은 하나의 커넥션에서 여러 요청을 보내지만, 응답은 반드시 **요청
   순서대로** 받아야 함. 첫 번째 요청의 처리가 늦어지면 뒤의 응답들이 모두 대기하게 됨.
2. **중간 장비(Proxy/Firewall) 문제**: 많은 프록시나 방화벽이 파이프라이닝을 제대로 지원하지 못해 연결이 끊기거나 오작동하는
   경우가 많음.
3. **복잡한 오류 처리**: 중간에 응답이 실패했을 때, 어디까지 재시도해야 하는지 결정하기 매우 복잡함.

### 브라우저의 대안: 연결 병렬화 (Parallel Connections)

- **현재 상황:** 대부분의 현대 브라우저(Chrome, Firefox 등)는 파이프라이닝을 기본적으로 비활성화했거나 완전히 제거함.
- 파이프라이닝의 위험성 대신, 여러 개의 TCP 연결을 동시에 열어서 리소스를 요청함.
- 도메인(Host)당 동시 연결 수는 보통 6개로 제한됨.

> **참고:** 이 모든 제약(HOL blocking, 연결 개수 제한)은 HTTP/2의 **멀티플렉싱(Multiplexing)**이
> 등장하면서 근본적으로 해결됨.

## Keep-Alive는 항상 지켜져야 되는거 아니야?

HTTP/1.1부터는 `Connection: keep-alive`가 기본(default)으로 설정됨.

- **연결 재사용:** 한 번 맺은 TCP 연결을 끊지 않고 여러 개의 HTTP 요청/응답을 주고받을 수 있어, TCP 핸드셰이크(
  Handshake) 비용을 줄여줌.
- **명시적 종료:** 연결을 끊고 싶다면 `Connection: close` 헤더를 명시적으로 보내야 함.
- **제약 사항:** 서버나 클라이언트 설정(Timeout, 최대 요청 수)에 따라 언제든지 끊길 수 있으므로 "항상" 유지되는 것은
  아님. 따라서 애플리케이션 레벨에서는 연결이 언제든 끊길 수 있음을 전제로 설계해야 함(**Best-effort**).

### 브라우저나 React에서 커넥션을 자동으로 재사용하나?

결론부터 말하면 **브라우저가 알아서 다 해줌.**

1. **브라우저의 연결 관리 (Connection Pooling):**
    - 브라우저는 내부적으로 **커넥션 풀(Connection Pool)**을 관리함.
    - React나 일반 JavaScript에서 `fetch`나 `axios`로 요청을 보낼 때, 브라우저는 이미 해당 도메인(Origin)
      과 맺어놓은 유휴(Idle) 커넥션이 있는지 확인하고 있다면 이를 재사용.
    - 이때 **TCP 핸드셰이크뿐만 아니라 TLS 핸드셰이크 과정도 생략**되므로 성능 이득이 매우 큼.
2. **Axios의 경우:**
    - `axios`는 브라우저 환경에서 `XMLHttpRequest` 또는 `fetch` API를 사용하여 통신함.
    - 따라서 개발자가 `axios`를 쓰더라도 실제 네트워크 연결 제어권은 브라우저에게 있으며, 브라우저는 `Keep-Alive`를
      기본적으로 활용하여 연결을 재사용함.
    - 즉, **`axios`를 쓸 때도 별도의 설정 없이 브라우저의 커넥션 풀링 혜택을 그대로 받음.**
3. **개발자의 역할:**
    - 프론트엔드 개발자는 보통 커넥션 재사용 로직을 직접 구현할 필요가 없음. HTTP/1.1 이상의 프로토콜을 사용하면 브라우저와 서버가
      알아서 `Keep-Alive`를 통해 최적화함.
4. **주의사항 (Node.js/Backend):**
    - 브라우저 환경과 달리 **Node.js(서버 사이드)** 환경에서 `axios`를 쓸 때는 주의가 필요함.
    - Node.js의 `http.Agent`는 기본적으로 `keepAlive: false`인 경우가 많으므로, 백엔드 간 통신 시에는
      `axios` 인스턴스 설정에 `httpAgent`와 `httpsAgent`를 통해 커넥션 풀을 활성화해야 함.

> **결론:** `Keep-Alive`는 "반드시 유지된다"는 신뢰보다는 **"유지될 수 있다면 최대한 활용한다"**는 관점으로 보아야 하며,
> 브라우저 환경에서는 이 복잡한 과정을 자동으로 처리해 줌.

> **참고:** HTTP/2와 HTTP/3에서는 별도의 `Connection` 헤더 없이도 프로토콜 수준에서 연결 유지가 기본이며,
> 멀티플렉싱을 통해 더 효율적으로 관리됨.

### 서버 사이드에서의 커넥션 Keep-Alive 자동을 재사용하나?

서버 사이드(Node.js, Spring, Go 등)에서는 브라우저와 달리 **커넥션 풀을 명시적으로 설정하지 않으면 매번 새로운 TCP 연결을
맺을 가능성이 높음.**

#### 1. 커넥션 풀을 사용하지 않을 때의 문제 (Short-lived Connections)

만약 백엔드 서비스 간 통신(Service-to-Service)에서 커넥션 풀을 사용하지 않는다면:

- **매 요청마다 핸드셰이크 발생:** 모든 HTTP 요청마다 `TCP 3-Way Handshake`와 `TLS Handshake`가
  일어남.
- **리소스 낭비:** 연결을 맺고 끊는 과정에서 CPU와 네트워크 자원을 소모하며, 특히 고부하 상황에서는 `TIME_WAIT` 상태의
  소켓이 쌓여 **포트 고갈(Port Exhaustion)** 문제가 발생할 수 있음.
- **지연 시간(Latency) 증가:** 매번 연결을 새로 만들기 때문에 응답 속도가 현저히 느려짐.

#### 2. 왜 서버 사이드에서는 "자동"이 아닌가?

- **브라우저:** 사용자 한 명의 요청을 처리하는 것이 목적이므로, 연결을 유지하는 것이 대부분의 경우 이득임.
- **서버 사이드:** 서버는 수많은 목적지(외부 API, DB, 다른 마이크로서비스)와 통신합니다. 모든 연결을 무한정 유지하면 서버의 파일
  디스크립터(File Descriptor)가 고갈될 수 있기 때문에, 개발자가 서비스의 특성에 맞게 **풀의 크기, 유휴 연결 유지 시간(
  Idle Timeout)** 등을 직접 제어해야 함.

#### 3. 주요 환경별 커넥션 풀 설정

- **Node.js:** 기본 `http.Agent`는 `keepAlive: false`가 기본값인 경우가 많음(버전별로 상이). 성능을
  위해서는 `new http.Agent({ keepAlive: true })`를 명시적으로 설정하거나 `axios` 등의 라이브러리에서 전역
  에이전트를 설정해야 함.
- **Java/Spring:** `RestTemplate`이나 `WebClient` 사용 시 Apache HttpClient나 Reactor
  Netty의 커넥션 풀 설정을 통해 관리함.
- **Go:** `http.Client`의 `Transport` 설정에서 `MaxIdleConns` 등을 통해 커넥션 풀을 기본적으로
  지원하지만, 세부 튜닝이 필요할 수 있음.

### 애플리케이션 레벨의 연결 관리 및 재시도 전략

#### 1. 왜 애플리케이션 레벨 검증이 필요한가?

커넥션 풀에 들어있는 연결이 항상 살아있다고 신뢰할 수 없는 이유는 다음과 같습니다:

- **TCP Keep-Alive의 한계:** OS 레벨의 TCP Keep-Alive는 기본 주기가 매우 길고(보통 2시간), OS 설정에
  의존적이어서 연결 끊김을 즉시 감지하기 어렵습니다.
- **FIN/RST 패킷 유실:** 네트워크 장애나 갑작스러운 서버 종료 시 종료 패킷(FIN/RST)이 클라이언트에 전달되지 않을 수
  있습니다.
- **중간 장비의 개입:** 로드 밸런서(LB), NAT 게이트웨이, 방화벽(FW) 등은 유휴 상태인 세션을 아무런 통보 없이 조용히 제거(
  Silent drop)할 수 있습니다.

따라서 **"연결을 사용하기 직전에 살아있는지 확인한다"**는 **Application-level validation** 전략이 필요합니다.

```text
connection pool
↓
borrow connection
↓
validate (lightweight check)
↓
OK → use
Fail → drop + reconnect
```

#### 2. DB vs HTTP의 검증 방식 차이

- **DB Connection Pool (JDBC 등):**
    - `Validation Query`(예: `SELECT 1`)를 사용하여 풀에서 꺼낸 커넥션이 유효한지 검증합니다.
    - **비용 vs 안정성:** 매번 검증하면 성능 손해(Latency 증가)가 있지만, 장애 확률을 획기적으로 낮춥니다. 보통 유휴
      커넥션을 검사하거나, 가져올 때(Borrow) 가벼운 체크를 수행합니다.
- **HTTP Client:**
    - 대부분의 HTTP 클라이언트는 DB처럼 꺼낼 때마다 `HEAD` 요청 등을 보내 검증하지 않습니다. (추가 왕복 비용이 너무 크기
      때문)
    - 대신, **실패 시 자동 재연결 및 재시도(Reconnect + Retry)** 전략을 기본으로 택합니다.

#### 3. 재시도 전략 (Retry Strategy)

재시도 로직이 없다면 `Keep-Alive`는 언제 끊길지 모르는 "시한폭탄"과 같습니다. 하지만 무분별한 재시도는 시스템을 더 위험하게 만들
수 있습니다.

**A. 멱등성(Idempotency) 이해**

- **멱등성:** 동일한 요청을 여러 번 실행해도 결과가 동일하고 서버의 상태가 추가적으로 변경되지 않는 특성.
- **재시도 가능 메서드:** `GET`, `HEAD`, `OPTIONS`, `TRACE` (일반적으로 안전). `PUT`,
  `DELETE` (멱등성이 보장되도록 설계된 경우 재시도 가능).
- **재시도 주의 메서드:** `POST` (새 리소스를 생성하므로 단순 재시도 시 중복 생성 위험).

**B. 언제 Retry 해야 하는가? (대상 에러)**

- **네트워크 계층 에러:**
    - `Connection reset by peer (RST)`: 서버가 연결을 강제로 끊었을 때.
    - `Broken pipe (EPIPE)`: 이미 끊긴 연결에 데이터를 쓰려고 할 때.
    - `Connection timed out`: 응답이 없거나 물리적 경로 문제.
- **HTTP 상태 코드:**
    - `502 Bad Gateway`, `503 Service Unavailable`, `504 Gateway Timeout`: 서버의
      일시적 장애나 과부하.
    - `429 Too Many Requests`: 서버가 Rate Limit을 걸었을 때 (이 경우 백오프 필수).

**C. Retry 금지 대상**

- `4xx` (Client Error): `400`, `401`, `403`, `404` 등 논리적 오류는 재시도해도 결과가 달라지지 않음.
- **Business Error:** 애플리케이션 로직에서 발생한 오류.
- **Partial Success:** 요청의 일부만 처리되었을 가능성이 있는 경우 (중복 처리 위험).

#### 4. Retry 패턴과 지수 백오프 (Exponential Backoff)

실패 직후 즉시 재시도하는 것은 서버에 "폭격"을 가하는 것과 같습니다. 실패할수록 대기 시간을 늘리는 전략이 필요합니다.

```text
request
↓
fail?
↓
is idempotent? (or safe to retry)
↓
retry count < limit?
↓
calculate backoff
↓
wait & retry
```

- **지수 백오프 (Exponential Backoff):** 재시도 횟수에 따라 대기 시간을 기하급수적으로 늘림.
    - 1차 시도: 100ms 대기
    - 2차 시도: 300ms 대기
    - 3차 시도: 1s 대기
- **지터 (Jitter):** 여러 클라이언트가 동시에 재시도하여 발생하는 병목(Thundering Herd)을 방지하기 위해 백오프 시간에
  약간의 무작위 값(Randomness)을 추가하는 것이 좋습니다.
