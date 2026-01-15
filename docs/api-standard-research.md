# API 표준 연구 노트

## 1. 문서 목적

- RESTful 등의 일반적 원칙을 제외하고, 실제 RFC 표준을 근거로 API 설계·운영 시 고려해야 할 영역을 식별한다.
- 각 표준의 범위, 핵심 죄항, 적용 아이디어를 정리해 조직 내 API 정책 문서의 근거로 활용한다.

## 2. 핵심 표준 요약

| 영역                    | RFC                                                                                                                       | 핵심 포인트                                                                                       | 적용 아이디어                                                                                                                |
| ----------------------- | ------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| HTTP 의미 체계          | [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html)                                                                   | 메서드 안전성·멱등성, 상태 코드 의미,<br>콘텐츠 협상, 캐시 제어 의미를 정의한다.                  | API 장애 복구 시 재시도 정책을<br>`/GET`/`PUT` 멱등성에 매칭하고,<br>`Vary`·`Content-Encoding` 정책을<br>공식 의미에 맞춘다. |
| 캐시 & 조건부 요청      | [RFC 9111](https://www.rfc-editor.org/rfc/rfc9111.html)                                                                   | `ETag`, `If-None-Match`,<br>`Cache-Control` 등의 조건부<br>처리 흐름과 중계 캐시 동작을 명시한다. | CDN/프록시 캐시 기준을 문서화하고,<br>304 응답 핸들링 테스트 케이스를<br>작성한다.                                           |
| HTTP/2 전송             | [RFC 7540](https://www.rfc-editor.org/rfc/rfc7540.html)                                                                   | 스트림 다중화, 헤더 압축(HPACK),<br>우선순위 및 플로우 컨트롤 정의.                               | 모바일/고지연 환경 API 호출에서<br>HOL(Head-of-Line) 블로킹을<br>줄이도록 Gateway 업그레이드<br>계획을 수립한다.             |
| HTTP/3 전송             | [RFC 9114](https://www.rfc-editor.org/rfc/rfc9114.html)                                                                   | QUIC 기반 스트림, 0-RTT,<br>패킷 손실 회복 방식을 설명한다.                                       | 실시간 API/웹후크 재전송 실패율을<br>줄이기 위해 HTTP/3 지원<br>로드맵을 수립한다.                                           |
| JSON                    | [RFC 8259](https://www.rfc-editor.org/rfc/rfc8259.html)                                                                   | 숫자·문자열 범위, 유니코드 처리,<br>`null` 처리 등 문법을 정식 규정.                              | API 스키마 밸리데이터가<br>RFC 준수 여부(예: NaN,<br>Infinity 금지)를 검사하도록 한다.                                       |
| CBOR                    | [RFC 8949](https://www.rfc-editor.org/rfc/rfc8949.html)                                                                   | JSON 대비 효율적인 이진 직렬화<br>규약. 태그 시스템으로 확장성을 정의.                            | IoT/엣지 API에 CBOR 옵션을<br>제공하여 전송량을 줄이고, 서버는<br>컨텐츠 협상으로 포맷을 전환한다.                           |
| JSON Patch              | [RFC 6902](https://www.rfc-editor.org/rfc/rfc6902.html)                                                                   | JSON 문서의 경로 기반 연산<br>(`add`, `remove`, `replace`)을<br>정의한다.                         | 대용량 리소스 부분 수정 API에<br>패치 문서를 채택하고, 연산 실패 시<br>409/422 응답 정책을 표준화한다.                       |
| JSON Merge Patch        | [RFC 7396](https://www.rfc-editor.org/rfc/rfc7396.html)                                                                   | 덮어쓰기 기반 부분 업데이트를<br>간단한 JSON 문법으로 정의.                                       | 설정 리소스 업데이트 API에서<br>직관적 Merge Patch를 허용하고,<br>`null` 삭제 정책을 문서화한다.                             |
| 오류 응답               | [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html)                                                                   | `type`, `title`, `detail`,<br>`instance` 필드 구조로<br>HTTP 오류를 표현.                         | 공통 오류 미들웨어에서 RFC 9457<br>스펙을 강제하고, `instance` URI로<br>추적 ID를 연결한다.                                  |
| RateLimit 헤더          | [RFC 9456](https://www.rfc-editor.org/rfc/rfc9456.html)                                                                   | `RateLimit-Limit`,<br>`RateLimit-Remaining`,<br>`RateLimit-Reset` 헤더 의미와<br>단위를 표준화.   | 전사 Rate Limit 정책 문서에<br>헤더 필드 정의와 단위(초/분)를<br>명시하고 모니터링 지표를 연계한다.                          |
| OAuth 2.0 코어          | [RFC 6749](https://www.rfc-editor.org/rfc/rfc6749.html)                                                                   | 권한부여 그랜트, 토큰 발급<br>플로우의 공식 규격.                                                 | API Gateway와 Authorization<br>Server 사이의 redirect<br>URI·scope 검증 로직을 명확히<br>정의한다.                           |
| Bearer 토큰 전달        | [RFC 6750](https://www.rfc-editor.org/rfc/rfc6750.html)                                                                   | HTTP `Authorization` 헤더로<br>Bearer 토큰을 전달하는 법과<br>오류 응답 형식을 규정.              | 클라이언트 SDK가<br>`WWW-Authenticate` 파라미터를<br>파싱해 재인증 로직을 구현하게 한다.                                     |
| 토큰 인트로스펙션       | [RFC 7662](https://www.rfc-editor.org/rfc/rfc7662.html)                                                                   | 토큰 활성 상태를 확인하는<br>엔드포인트 규격.                                                     | Resource Server가 Access<br>Token을 검증할 때 인트로스펙션<br>캐싱 전략을 설계한다.                                          |
| mTLS 확장               | [RFC 8705](https://www.rfc-editor.org/rfc/rfc8705.html)                                                                   | OAuth 클라이언트 인증을 mTLS로<br>강화하는 프로파일.                                              | 금결원/금융권 API 연동 시 mTLS<br>바인딩 토큰을 요구하도록 정책화.                                                           |
| 네이티브 앱 OAuth       | [RFC 8252](https://www.rfc-editor.org/rfc/rfc8252.html)                                                                   | 모바일/데스크톱 애플리케이션을 위한<br>안전한 리디렉션, PKCE 요구.                                | SDK 가이드에 앱 스킴 등록·PKCE<br>필수화 절차를 포함한다.                                                                    |
| JWT                     | [RFC 7519](https://www.rfc-editor.org/rfc/rfc7519.html)                                                                   | JSON 기반 클레임 집합, 만료,<br>audience 처리 정의.                                               | API 토큰 구조에 `iss`, `aud`,<br>`exp` 필드를 표준대로 포함시키고<br>clock skew 허용치를 명시한다.                           |
| JWS/JWE                 | [RFC 7515](https://www.rfc-editor.org/rfc/rfc7515.html)                                                                   | 토큰 서명(JWS)·암호화(JWE) 구조,<br>헤더 파라미터 규정.                                           | 토큰 검증 라이브러리 선택 시<br>`alg=none` 차단, 키 회전 전략을<br>문서화한다.                                               |
| Problem Details 확장    | [RFC 7807](https://www.rfc-editor.org/rfc/rfc7807.html) → [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html)         | 최신 스펙은 JSON+XML 동시 정의와<br>확장 필드 규칙을 포함한다.                                    | 서비스별 오류 코드 매핑표를<br>`type` URI로 관리하고 버전 업<br>전략을 정한다.                                               |
| HTTP Message Signatures | [draft-ietf-httpbis-message-signatures-19](https://www.ietf.org/archive/id/draft-ietf-httpbis-message-signatures-19.html) | HTTP 헤더/바디 서명 규격<br>(서명입력 구성, 키 식별,<br>검증 절차)을 제공.                        | 전자정부·금융 API에 요청<br>위·변조 방지를 강제하고,<br>응답 서명 도입 가능성을 평가한다.                                    |
| Web Push                | [RFC 8030](https://www.rfc-editor.org/rfc/rfc8030.html)                                                                   | 구독 관리, 푸시 서비스와<br>애플리케이션 서버간 신뢰 모델을<br>정의.                              | 고신뢰 웹후크 전달이 필요할 때<br>주제(Topic) 모델과 재전송<br>절차를 차용한다.                                              |
| 언어 협상               | [RFC 4647](https://www.rfc-editor.org/rfc/rfc4647.html)                                                                   | `Accept-Language` 필터링·매칭<br>알고리즘을 명시한다.                                             | 다국어 API 응답 시 언어 선택<br>정책을 공식화하고, fallback<br>우선순위를 테스트에 포함한다.                                 |

## 3. 영역별 심화 메모

### 3.1 HTTP 의미 체계와 캐싱

- `RFC 9110`과 `RFC 9111`을 근거로 메서드 멱등성, 조건부 요청, 캐시 유효성 검사를 문서화하면 API 재시도·Failover
  정책을 명확히 설명할 수 있다.
- CDN, API Gateway, Edge Cache가 같은 규칙을 따르도록 `Cache-Control` 디렉티브와 `Vary` 조합을 정책
  문서에 포함해야 한다.

### 3.2 전송 계층(HTTP/2/3)

- `RFC 7540`은 헤더 압축과 스트림 우선순위 제어를 통해 동시 다중 API 호출의 지연을 줄일 수 있는 근거를 제공한다.
- `RFC 9114`는 QUIC의 손실 복원 메커니즘과 0-RTT 재연결을 정의하므로, 모바일/해외 네트워크에서 API 호출 실패율을 줄이려면
  해당 RFC를 근거로 HTTP/3 지원을 추진한다.

### 3.3 데이터 표현과 부분 수정

- JSON (`RFC 8259`) 스펙을 준수하지 않으면 NaN/Infinity 같은 비표준 값이 파서 호환성을 깨뜨릴 수 있다.
- JSON Patch (`RFC 6902`), Merge Patch (`RFC 7396`)를 비교해 리소스별 최적 패치 전략을 정의하고, 실패
  시 응답 코드를 표준화한다.

### 3.4 오류·제한 응답 표준화

- `RFC 9456`과 `RFC 9457` 조합으로 Rate Limit 및 오류 응답 구조를 통일하면, 클라이언트 SDK가 자동으로
  재시도·사용자 안내를 구현할 수 있다.
- API 사양 문서에 `type` URI 목록, Rate Limit 단위(초/분), `Retry-After` 연계 규칙을 명시한다.

### 3.5 인증·인가 생태계

- OAuth 2.0 코어 (`RFC 6749`, `RFC 6750`) 외에도 `RFC 7662`, `RFC 8705`, `RFC 8252`를
  참조하여 토큰 검증, mTLS, 네이티브 앱 요구사항을 다층적으로
  설계한다.
- JWT (`RFC 7519`)와 JWS/JWE (`RFC 7515`) 조합으로 토큰 구조·암호화를 정의하며, 키 회전과 클레임 네임스페이스
  규칙을 명시한다.

### 3.6 메시지 서명과 고신뢰 전달

- HTTP Message Signatures IETF 초안은 고보안 API에서 요청/응답 무결성을 보장하기 위한 최신 표준 동향이므로 PoC를
  통해 구현 피드백을 준비한다.
- Web Push (`RFC 8030`)는 안정적인 이벤트 전달 모델을 정의하므로 고신뢰 웹후크나 비동기 콜백을 설계할 때 참조한다.

### 3.7 국제화 대응

- `RFC 4647`의 언어 필터링 알고리즘을 적용해 `Accept-Language`와 `Content-Language` 헤더 처리 일관성을
  확보한다.
- API 문서에 지원 언어 태그 목록과 fallback 순서를 명시하여 QA 자동화가 가능하게 한다.

## 4. 실행 제안

1. API 정책 문서에 위 RFC 번호를 근거 조항으로 링크하고, 각 기능(오류, Rate Limit, 패치 등)의 준수 여부 체크리스트를
   만든다.
2. Gateway/Proxy 업그레이드 로드맵에 HTTP/2→HTTP/3 전환, 메시지 서명 PoC 등 비기능 요구사항을 반영한다.
3. OAuth·JWT·RateLimit·Problem Details 등 보안/거버넌스 영역은 중앙 플랫폼 팀에서 템플릿을 제공하고, 서비스
   팀은 준수 여부만 체크하도록 한다.
