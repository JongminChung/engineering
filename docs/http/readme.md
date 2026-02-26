# HTTP & API Standard Guide

이 문서는 현대적인 웹 애플리케이션 개발을 위한 HTTP 프로토콜의 이해와 RFC 표준 기반의 API 설계 가이드를 정리합니다. Cloud Native 환경에서의 안정성, 보안, 성능 최적화 전략을 포함합니다.

---

## 1. HTTP 프로토콜의 진화

HTTP는 단순한 전송 프로토콜이 아니라 **API의 계약(Contract)**입니다.

| 버전         | 핵심 변화                                       | 해결한 문제                    | 관련 RFC |
| :----------- | :---------------------------------------------- | :----------------------------- | :------- |
| **HTTP/1.1** | 지속 연결(Keep-Alive), 파이프라이닝             | 연결 반복 생성 비용 절감       | RFC 9112 |
| **HTTP/2**   | 멀티플렉싱, 헤더 압축(HPACK), 바이너리 프레이밍 | HOL Blocking 해결, 전송 효율   | RFC 7540 |
| **HTTP/3**   | QUIC 기반(UDP), 0-RTT, 손실 격리                | 패킷 손실 시 지연 방지, 이동성 | RFC 9114 |

---

## 2. HTTP Semantics (RFC 9110)

### 2.1 메서드 속성 및 의미

| 메서드     | 의미             | Safe | Idempotent | 비고                                 |
| :--------- | :--------------- | :--: | :--------: | :----------------------------------- |
| **GET**    | 리소스 조회      |  O   |     O      | 캐시 가능                            |
| **POST**   | 데이터 처리/제출 |  X   |     X      | 비멱등 작업의 기본                   |
| **PUT**    | 리소스 전체 대체 |  X   |     O      | 리소스 존재 시 대체, 없을 시 생성    |
| **PATCH**  | 리소스 일부 수정 |  X   |     △      | [RFC 6902], [RFC 7396] 참조          |
| **DELETE** | 리소스 삭제      |  X   |     O      | 삭제 후 재호출 시에도 성공(204) 처리 |

### 2.2 주요 상태 코드

- **201 Created:** 리소스 생성 성공 (Location 헤더 포함 권장)
- **204 No Content:** 요청 성공했으나 응답 본문 없음 (주로 DELETE)
- **304 Not Modified:** 캐시가 유효하여 본문 전송 생략 (RFC 9111)
- **409 Conflict:** 리소스 상태 충돌 (낙관적 락 실패 등)
- **429 Too Many Requests:** 레이트 리밋 초과 (RFC 9456)

---

## 3. API 설계 및 데이터 표준

### 3.1 멱등성 보장 (Idempotency)

- **Idempotency-Key:** POST 요청의 중복 처리를 방지하기 위해 클라이언트가 발급한 고유 키를 헤더에 포함.
- 서버는 해당 키로 처리 이력을 조회하여 동일 결과 반환.

### 3.2 부분 수정 전략 (Patching)

- **JSON Patch (RFC 6902):** `add`, `remove`, `replace` 등의 연산을 기술하는 방식. 정교한 제어 가능.
- **JSON Merge Patch (RFC 7396):** 전송된 JSON 구조대로 덮어쓰는 방식. 직관적이나 리스트 처리나 null(삭제) 처리에 주의 필요.

### 3.3 동시성 제어 (Concurrency)

- **ETag (RFC 9110):** 리소스 버전 식별자. `If-Match` 헤더와 조합하여 수정 중 충돌 방지.
- **낙관적 락:** DB의 `version` 필드 활용. 충돌 시 409 응답.

### 3.4 오류 응답 표준 (RFC 9457)

오류 발생 시 일관된 구조(`application/problem+json`)로 응답합니다.

- `type`: 오류 유형 식별 URI
- `title`: 오류에 대한 짧은 요약
- `status`: HTTP 상태 코드
- `detail`: 구체적인 발생 원인
- `instance`: 오류가 발생한 구체적 리소스 경로

### 3.5 레이트 리밋 표준 (RFC 9456)

- `RateLimit-Limit`: 허용된 총 요청 수
- `RateLimit-Remaining`: 남은 요청 수
- `RateLimit-Reset`: 초기화까지 남은 시간 (초)

---

## 4. 보안 및 인증 표준

### 4.1 OAuth 2.0 & JWT

- **Core (RFC 6749):** 권한 부여 코드(Auth Code) 플로우 권장.
- **JWT (RFC 7519):** 클레임 기반 토큰. `iss`, `aud`, `exp` 필드 필수 준수.
- **PKCE (RFC 8252):** 네이티브/모바일 앱에서 안전한 리디렉션 구현 필수.

### 4.2 전송 보안 강화

- **mTLS (RFC 8705):** 인증서 기반의 상호 TLS 인증으로 보안 수준 강화.
- **Message Signatures:** 요청/응답 메시지 자체에 서명하여 위변조 방지 (IETF 초안 참조).

---

## 5. 커넥션 및 성능 전략

### 5.1 Keep-Alive와 네트워크 안정성

- **구조:** `Client ── LB ── Proxy ── Server`
- **Timeout 원칙:** `Client <= LB <= Server`. 중간 장비가 먼저 끊지 않도록 설정.
- **Connection Reset:** 유휴 연결 재사용 시 발생 가능하므로, 클라이언트의 재시도 로직은 필수.

### 5.2 재시도 및 백오프

- **Safe/Idempotent 메서드:** 자동 재시도 허용.
- **비멱등 메서드:** Idempotency-Key가 있을 때만 재시도.
- **Backoff:** 지수 백오프 + 지터(Jitter)를 적용하여 thundering herd 현상 방지.

### 5.3 페이지네이션 & 국제화

- **Pagination:** 고성능 처리를 위해 Cursor-based 방식 권장.
- **국제화 (RFC 4647):** `Accept-Language` 헤더와 매칭 알고리즘을 통한 다국어 대응.

---

## 6. Cloud Native 로드맵

1. **인프라:** LB/Gateway를 통한 TLS Termination 및 HTTP/2/3 활성화.
2. **거버넌스:** RFC 9457(오류), RFC 9456(제한) 표준 강제. 공통 SDK 제공.
3. **고도화:** Service Mesh 도입을 통한 mTLS 및 트래픽 정책 중앙 관리.
