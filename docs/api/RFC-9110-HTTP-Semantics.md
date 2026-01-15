# RFC 9110: HTTP Semantics

## 1. 개요

**RFC 9110**은 HTTP의 _의미론(Semantics)_ 을 정의하는 표준 문서로,
HTTP 요청·응답의 의미, 메서드, 상태 코드, 헤더 필드, URI 스킴 등을 규정한다.

- HTTP의 공통 개념과 동작 규칙 정의
- 기존 RFC 723x 계열 문서를 통합·대체
- HTTP 버전(1.1, 2, 3)에 독립적인 의미 규정

---

## 2. HTTP 아키텍처

### 2.1 기본 특성

- **무상태(stateless)** 프로토콜
- **요청(Request) / 응답(Response)** 모델
- 애플리케이션 계층 프로토콜

### 2.2 주요 구성 요소

- **Client**: 요청을 생성
- **Server**: 요청을 처리하고 응답 생성
- **Intermediary**: 프록시, 게이트웨이, 터널
- **Cache**: 응답을 저장하여 재사용

---

## 3. 리소스와 표현

- **Resource**: URI로 식별되는 개념적 대상 (예: 사용자 123, '무엇')
    - **Representation**: 리소스의 현재 상태를 표현한 데이터 (예: 소프트웨어적으로 추상화한 표현, '어떻게 전달되느냐')
        - 데이터 본문 + 메타데이터(헤더)
        - JSON, HTML,
        - 에:
            - ```text
              HTTP/1.1 200 OK
              Content-Type: application/json
              ETag: "v1"

              {
                "id": 123,
                "name": "Jamie",
                "email": "jamie@example.com"
              }
              ```

---

## 4. HTTP 메시지

### 4.1 요청 메시지

- 메서드 (Method)
- 요청 대상 (Target URI)
- 헤더 필드
- 메시지 본문 (선택)

### 4.2 응답 메시지

- 상태 코드 (Status Code)
- 헤더 필드
- 메시지 본문 (선택)

---

## 5. HTTP 메서드

| 메서드  | 의미                       |
| ------- | -------------------------- |
| GET     | 리소스 조회                |
| HEAD    | GET과 동일하나 본문 없음   |
| POST    | 데이터 제출 또는 처리 요청 |
| PUT     | 리소스 전체 대체           |
| DELETE  | 리소스 삭제                |
| CONNECT | 터널 생성                  |
| OPTIONS | 서버 기능 조회             |
| TRACE   | 요청 경로 테스트           |

### 메서드 속성

- **Safe**: 서버 상태를 변경하지 않음 (GET, HEAD)
- **Idempotent**: 여러 번 수행해도 결과 동일 (GET, PUT, DELETE 등)

---

## 6. 상태 코드 (Status Codes)

### 상태 코드 범주

| 범위 | 의미            |
| ---- | --------------- |
| 1xx  | 정보            |
| 2xx  | 성공            |
| 3xx  | 리다이렉션      |
| 4xx  | 클라이언트 오류 |
| 5xx  | 서버 오류       |

### 대표 상태 코드

- `200 OK`
- `201 Created`
- `204 No Content`
- `301 Moved Permanently`
- `304 Not Modified`
- `400 Bad Request`
- `401 Unauthorized`
- `403 Forbidden`
- `404 Not Found`
- `500 Internal Server Error`
- `503 Service Unavailable`

---

## 7. 헤더 필드

- 요청 및 응답 메타데이터 전달
- 의미는 컨텍스트(요청/응답)에 따라 달라질 수 있음

예시:

- `Host`
- `User-Agent`
- `Accept`
- `Content-Type`
- `Authorization`
- `Cache-Control`

---

## 8. URI와 스킴

- HTTP는 URI를 통해 리소스를 식별
- 주요 스킴:
    - `http`
    - `https`
- `userinfo` 사용은 보안상의 이유로 권장되지 않음

---

## 9. 캐싱과 조건부 요청

### 9.1 캐싱

- 응답 재사용을 통해 성능 향상
- `Cache-Control`, `Expires` 헤더 사용

### 9.2 조건부 요청

- 리소스 변경 여부에 따라 응답 제어
- 주요 검증자:
    - `ETag`
    - `Last-Modified`

---

## 10. 인증(Authentication)

- 챌린지-응답 방식
- 주요 헤더:
    - `WWW-Authenticate`
    - `Authorization`

---

## 11. 확장성

- 새로운 메서드, 상태 코드, 헤더 필드 확장 가능
- IANA 등록을 통해 표준화

---

## 12. 정리

RFC 9110은 HTTP의 **의미와 규칙을 정의하는 핵심 명세**로서:

- HTTP 동작의 일관성 보장
- 버전 간 의미 분리
- 현대 HTTP 스택의 기준 문서 역할 수행
