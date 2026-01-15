# keep-alive 환경에서 `Connection reset by peer`를 어떻게 다룰 것인가

### Spring Boot · Express에서의 Retry 전략과 POST 멱등성 설계

HTTP keep-alive는 성능 최적화를 위해 필수적인 기능이지만, 실무에서는 종종 다음과 같은 오류를 마주한다.

> `Connection reset by peer`

이 글에서는

- 왜 keep-alive 환경에서 이런 오류가 발생하는지
- 애플리케이션 관점에서 **언제 Retry를 해야 하는지**
- Spring Boot와 Express(Node.js)에서 **Retry를 어떻게 가져가는 것이 안전한지**
- 특히 **POST 요청에서의 멱등성(Idempotency) 설계가 왜 중요한지**

를 정리한다.

---

## 1. keep-alive와 `Connection reset by peer`

keep-alive는 한 번 맺은 TCP 커넥션을 여러 HTTP 요청에 재사용한다.
문제는 이 “재사용” 과정에서 발생한다.

### 왜 reset이 발생하는가?

- 서버 / 로드밸런서 / 프록시가 **유휴 커넥션을 먼저 종료**
- NAT, 방화벽이 일정 시간 이후 커넥션 제거
- 클라이언트는 해당 소켓이 살아있다고 믿고 재사용
- 결과적으로 서버는 **RST 패킷**을 반환 → `Connection reset by peer`

이것은 **버그가 아니라 정상적인 네트워크 현실**에 가깝다.

### 중요한 관점

> keep-alive 환경에서는
> **“가끔 커넥션이 죽어 있는 상태로 재사용될 수 있다”**
> 는 것을 전제로 설계해야 한다.

---

## 2. Retry는 어디서 해야 하는가?

결론부터 말하면:

> **Retry는 서버가 아니라, 호출자(클라이언트)에서 수행한다.**

이유는 명확하다.

- 서버는 이미 요청을 받은 뒤일 수 있음
- 서버가 Retry를 하면 **중복 처리 위험**
- 네트워크 실패의 대부분은 **클라이언트-서버 사이**에서 발생

따라서 Retry는 **Outbound HTTP 호출을 하는 애플리케이션 레이어**의 책임이다.

---

## 3. 애플리케이션 관점에서 “언제 Retry를 해야 하는가”

### 3.1 Retry를 고려할 수 있는 경우 (권장)

#### 네트워크 / 전송 계층 오류

- `Connection reset by peer`
- connect timeout
- read timeout / socket timeout
- DNS 일시 오류 (`EAI_AGAIN` 등)
- TLS handshake 일시 실패

#### HTTP 응답 코드

- `502 Bad Gateway`
- `503 Service Unavailable`
- `504 Gateway Timeout`
- `429 Too Many Requests` (가능하면 `Retry-After` 준수)
- `408 Request Timeout` (환경에 따라)

이들은 대부분 **일시적(transient) 실패**로 분류된다.

---

### 3.2 Retry를 하면 안 되는 경우

- 대부분의 `4xx` 에러
    - `400`, `401`, `403`, `404`, `422`
- 인증/권한/검증 오류
- **POST 요청에서 멱등성이 보장되지 않은 경우**

같은 요청을 다시 보내도 **성공할 가능성이 거의 없거나**,
오히려 **중복 처리 위험**이 커진다.

---

## 4. POST 요청과 Retry의 위험성

네트워크 오류는 보통 아래 두 상황을 구분하지 못한다.

1. 요청이 서버에 **도달하지 못함**
2. 요청이 서버에 **도달했고, 처리까지 됐지만 응답만 못 받음**

POST 요청에서 (2)번 상황에서 Retry를 하면:

- 주문 2번 생성
- 결제 2번 승인
- 포인트 2번 차감

같은 치명적인 문제가 발생할 수 있다.

---

## 5. POST Retry의 전제 조건: Idempotency

### 5.1 Idempotency-Key 패턴

POST 요청에 다음과 같은 헤더를 포함한다.

```http
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
```

### 5.2 서버의 책임

- (Idempotency-Key + API Path + Method) 기준으로 요청을 식별
- 이미 처리된 요청이면:
    - 같은 응답을 그대로 반환
    - 비즈니스 로직 재실행 금지
- 보통 Redis / DB에 결과를 저장하고 TTL을 둠

이렇게 하면:

> 네트워크 오류로 인한 POST Retry가
> “중복 실행”이 아니라 “결과 재전송” 이 된다.

---

## 6. Spring Boot에서의 Retry 전략

### 6.1 Retry를 넣는 위치

- WebClient, RestTemplate, Feign 등
  외부 HTTP 호출 전용 Client Layer
- 비즈니스 로직 내부에 흩뿌리지 않는다

### 6.2 구현 옵션

- Resilience4j
    - Retry + CircuitBreaker + RateLimiter 조합에 적합
- Spring Retry
    - 간단한 어노테이션 기반 구성 가능
- Reactive 환경에서는 WebClient Filter로 중앙화

### 6.3 권장 정책 예시

- max attempts: 2~3
- backoff: exponential + jitter
- connect / read timeout 명확히 설정
- Retry 전에 timeout이 먼저 발생하도록 설계
- Circuit Breaker로 연쇄 장애 방지

## 7. Express(Node.js)에서의 Retry 전략

### 7.1 keep-alive와 Agent 설정

Node.js는 keep-alive 사용 시 Agent 설정이 매우 중요하다.

- 유휴 커넥션 관리
- socket timeout 명확히 지정

### 7.2 Retry 위치

- Express route handler 내부 ❌
- 외부 API 호출 전용 모듈 ⭕

### 7.3 POST 멱등성

- 클라이언트: Idempotency-Key 전송
- Express 서버:
    - 요청 수신 시 키 검증
    - 중복이면 저장된 응답 반환

## 8. 실무에서 바로 쓰는 Retry 규칙 요약

### HTTP Method 기준

| Method       | Retry 조건                              |
| ------------ | --------------------------------------- |
| GET / HEAD   | 네트워크 오류, 502/503/504, 429         |
| PUT / DELETE | 서버가 멱등하게 구현된 경우에 한해 허용 |
| POST         | **Idempotency-Key가 있을 때만 허용**    |

### Retry 정책

- 짧은 timeout → 소수의 retry
- 지수 백오프 + jitter
- retry budget 제한
- 관측 가능성 확보 (Request ID 기반 로그/트레이싱)

---

## 9. 관측성은 선택이 아니라 필수

Retry가 들어간 순간부터, 추적 가능성이 없으면 운영이 불가능해진다.

- Request-ID / X-Request-Id / traceparent 전파
- 재시도 시:
    - attempt 번호
    - 에러 유형
    - 최종 성공/실패 여부
- OpenTelemetry 등 분산 트레이싱 연동 권장

---

## 10. 마무리

keep-alive 환경에서의 네트워크 오류는 피할 수 없다.
중요한 것은 오류를 없애는 것이 아니라,

- 언제 Retry할지 명확히 정의하고
- Retry해도 안전하도록 멱등성을 설계하며
- 장애를 증폭시키지 않도록 제어하는 것 이다.

> Retry는 단순한 “재시도”가 아니라
> 분산 시스템 안정성의 핵심 설계 요소다.
