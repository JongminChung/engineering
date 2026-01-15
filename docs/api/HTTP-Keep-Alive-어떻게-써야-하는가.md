# HTTP/1.1 Keep-Alive, 어떻게 써야 하는가

## LB·WAF·프록시 환경에서의 현실적인 표준 해석과 운영 가이드

HTTP/1.1에서 **keep-alive(지속 연결)** 는 성능 최적화의 기본이다.
하지만 실제 운영 환경에서는 다음과 같은 질문이 항상 따라온다.

- 클라이언트는 서버와 직접 연결되는가, 아니면 LB/WAF와 연결되는가?
- keep-alive는 누가 끊는가?
- 각 구간의 timeout이 서로 다르면 어떻게 되는가?
- “표준적으로 올바른 설정”이 존재하는가?

이 글에서는 **RFC 기준에서 keep-alive가 무엇을 보장하고, 무엇을 보장하지 않는지**를 먼저 짚고,
그 위에서 **현실적인 운영 표준안(권장 패턴)** 을 정리한다.

---

## 1. HTTP/1.1에서 keep-alive는 무엇인가

HTTP/1.1에서는 **연결 유지가 기본(default)** 이다.

- HTTP/1.0: 명시적으로 `Connection: keep-alive` 필요
- HTTP/1.1: 명시적으로 `Connection: close` 하지 않는 한 연결 유지

즉, HTTP/1.1에서 keep-alive는 “옵션”이 아니라 **기본 동작**이다.

### RFC 관점 정리

- HTTP는 **연결 유지 여부를 협상하지 않는다**
- 클라이언트와 서버는 언제든지 연결을 종료할 수 있다
- keep-alive는 _힌트(hint)_ 이지 _계약(contract)_ 이 아니다

> HTTP는 “이 연결을 얼마나 오래 유지할지”를 약속하지 않는다.

**[관련 RFC]**

- RFC 9112 — HTTP/1.1 Message Syntax and Routing
- RFC 9110 — HTTP Semantics

---

## 2. keep-alive에 대해 흔히 오해하는 것들

### 오해 1: keep-alive면 연결이 오래 유지된다

❌ 잘못된 이해다.

- keep-alive는 **재사용 가능함을 의미할 뿐**
- 실제 종료 시점은 **중간자(LB/WAF/프록시)** 가 결정할 수 있다

Client ── keep-alive ── LB ── keep-alive ── WAF ── keep-alive ── Server

이 구조에서:

- Client ↔ LB
- LB ↔ WAF
- WAF ↔ Server

👉 **각 구간은 서로 다른 keep-alive timeout을 가진다**

---

### 오해 2: keep-alive timeout은 HTTP 표준에 정의되어 있다

❌ 아니다.

RFC는 다음만 정의한다.

- 연결은 재사용될 수 있다
- 어느 쪽이든 언제든지 닫을 수 있다

다음은 **모두 구현체/운영 정책**이다.

- keep-alive timeout 값
- 최대 요청 수
- idle connection 정리 기준

> keep-alive는 표준이 아니라 **운영의 문제**다.

---

## 3. 현실적인 문제 시나리오

### 시나리오 1: 중간자가 먼저 연결을 끊는 경우

예시:

- Client keep-alive: 60초
- LB keep-alive: 30초
- Server keep-alive: 120초

Client ----(60s)---- LB ----(30s)---- Server

30초 동안 요청이 없으면:

- LB가 연결을 끊음
- Client는 이를 모른 채 기존 연결에 요청 전송
- 결과: `Connection reset by peer`

👉 **정상적인 동작**이며, 버그가 아니다.

---

### 시나리오 2: 서버가 연결을 먼저 정리하는 경우

- 서버의 idle connection 정리 정책 발동
- Client는 keep-alive 연결이 살아있다고 가정
- 다음 요청에서 소켓 에러 발생

👉 이것 역시 **정상적인 HTTP/1.1 동작**

---

## 4. HTTP/1.1 keep-alive의 표준적 해석

### RFC가 말하는 핵심 원칙

1. 연결은 언제든지 종료될 수 있다
2. 클라이언트는 연결 종료를 **항상 가정해야 한다**
3. 서버는 연결 유지에 대해 **보장하지 않는다**

즉,

> keep-alive는 “성능 최적화 힌트”이지
> “연결 안정성 보장 장치”가 아니다.

---

## 5. 실무에서의 표준안 (권장 패턴)

### 1) 클라이언트는 항상 재연결 가능해야 한다

- keep-alive 연결 실패는 **예외 상황이 아니다**
- 다음을 전제로 설계해야 한다:
    - 소켓 에러
    - RST
    - timeout

👉 **재시도 로직은 필수**

---

### 2) keep-alive timeout은 “짧은 쪽에 맞춘다”

권장 원칙:

Client timeout <= LB timeout <= Server timeout

이유:

- 중간자가 먼저 끊으면 클라이언트 오류 발생
- 클라이언트가 가장 보수적으로 행동하는 것이 안전

---

### 3) 커넥션 재사용과 요청 재시도를 분리한다

- keep-alive 실패 ≠ 요청 실패
- 요청은 **멱등성 기준**으로 재시도 가능해야 한다

예:

- GET / PUT → 자동 재시도 가능
- POST → Idempotency-Key 필요

---

### 4) keep-alive는 성능 최적화로만 취급한다

❌ “연결이 살아있을 것이다”라는 가정
⭕ “살아있으면 좋고, 아니면 다시 맺는다”

---

### 5) 관측 지표를 반드시 분리한다

다음은 반드시 분리해서 봐야 한다.

- TCP 연결 수
- keep-alive 재사용률
- connection reset / timeout 비율
- 요청 실패율

👉 keep-alive 이슈를 **장애로 오인하지 않기 위해** 중요

---

## 6. LB / WAF / 프록시 환경에서의 권장 체크리스트

### 설정 관점

- [ ] 각 hop의 keep-alive timeout 문서화
- [ ] idle connection 정리 기준 명확화
- [ ] LB ↔ Server 구간의 timeout > Client ↔ LB

### 애플리케이션 관점

- [ ] 커넥션 오류 발생 시 재시도 가능
- [ ] POST 요청 멱등성 설계
- [ ] Request ID 기반 추적

---

## 7. 정리

HTTP/1.1 keep-alive는 다음과 같이 이해해야 한다.

- **기본 동작이지만, 보장되지 않는다**
- **중간자가 있는 순간 timeout은 하나가 아니다**
- **끊어지는 것이 정상이며, 실패가 아니다**

그리고 실무 표준안은 명확하다.

> “keep-alive를 신뢰하지 말고,
> 재연결과 재시도를 전제로 설계하라.”

HTTP/2, HTTP/3로 가더라도
**이 철학은 변하지 않는다.**
