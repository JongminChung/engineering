# API 통신 학습 모듈

REST와 gRPC를 같은 도메인(주문 서비스)에 적용해보며 확장성, 성능, 보안 요소를 체험할 수 있는 학습용 모듈입니다. 하나의 `OrderService` 계층을 REST Controller와 gRPC
Service가 공유하도록 설계되어, 프로토콜이 달라도 어떻게 동일한 도메인 정책을 재사용하는지 확인할 수 있습니다.

## 핵심 학습 포인트

- **보안**: API Key 기반 멀티 테넌트 인증(`ApiKeyAuthFilter`, `GrpcApiKeyServerInterceptor`)과 Trace ID 전파.
- **확장성 & 성능**:
  - 슬라이딩 윈도우 레이트 리미터(`SlidingWindowRateLimiter`)로 테넌트/클라이언트별 요청 제어
  - 읽기 모델 캐시(`OrderReadModelCache`)로 HOT 데이터 재사용
  - 요청 메트릭(`RequestMetrics`)으로 성공/실패/캐시 적중률/평균 지연 관찰
- **프로토콜 비교**: REST(`OrderController`)와 gRPC(`OrderGrpcService`) 엔드포인트를 비교하면서, 헤더/메타데이터 처리와 예외 매핑 차이를 학습

## API 개요

### REST (JSON)

| 메서드    | 경로                    | 설명       |
|--------|-----------------------|----------|
| `POST` | `/api/v1/orders`      | 주문 생성    |
| `GET` | `/api/v1/orders/{id}` | 주문 단건 조회 |

필수 헤더

- `X-API-KEY`, `X-TENANT-ID`, `X-CLIENT-ID`
- `X-TRACE-ID`는 선택이지만, 없으면 필터에서 UUID를 생성하여 응답 헤더에도 전달합니다.

### REST (Protobuf)

- 엔드포인트: `POST /api/v1/orders/proto`, `GET /api/v1/orders/{id}/proto`
- Content-Type / Accept: `application/x-protobuf`
- 메시지 타입: `orders.proto` 내 `CreateOrderRequest`, `OrderMessage`
- Spring MVC는 `ProtobufHttpMessageConverter`를 통해 HTTP 본문과 Proto 메시지를 직렬화/역직렬화합니다.

JSON 대신 Proto를 쓰더라도 REST 호출 경로, API Key 인증, 레이트 리미팅 동작은 동일합니다. gRPC를 도입하기 전에 Proto 포맷만 먼저 도입하는 시나리오를 연습할 수 있습니다.

### gRPC

- 프로토 정의: `src/main/proto/orders.proto`
- 서비스 구현: `OrderGrpcService`
- Metadata 키: `x-api-key`, `x-tenant-id`, `x-client-id`, `x-trace-id`
- `GrpcApiKeyServerInterceptor`가 메타데이터를 검증 후 `GrpcRequestContext`에 바인딩합니다.

## 설정 (`application.yml`)

```yaml
app:
  security:
    clients:
      - tenant-id: tenant-alpha
        client-id: web-client
        hashed-api-key: 024f6c9525465fbec0047e2686f02a413c52241fde8af273148c419fa18fb312
  study:
    rate-limit:
      max-requests: 5
      window: 1m
    cache:
      ttl: 45s
```

- `hashed-api-key`에는 실제 API Key를 SHA-256으로 해싱한 값을 넣어야 합니다. 기본 값은 학습용 `local-api-key`입니다.
- `app.study` 설정으로 캐시 TTL, 레이트 리밋 윈도우/쿼터를 조정하여 성능 실험을 해볼 수 있습니다.

## 테스트

| 테스트 클래스                               | 설명                                           |
|:--------------------------------------|:---------------------------------------------|
| `orders/api/OrderControllerTest`      | Spring MVC + Security 필터 + Rate Limit 플로우 검증 |
| `orders/api/OrderProtoControllerTest` | REST에서 Proto 본문 통신 및 캐시/보안 흐름 검증             |
| `grpc/OrderGrpcServiceTest`           | InProcess gRPC 서버를 띄워 Metadata 인증과 응답 검증     |
| `orders/service/OrderServiceTest`     | 캐시 적중, 레이트 리밋, 메트릭 동작 검증                     |

실행 명령:

```bash
./gradlew :study:api-communication:test
```

## 디렉터리 구조

```
study/api-communication
├── build.gradle.kts
├── README.md
├── src
│   ├── main
│   │   ├── java/io/github/jongminchung/study/apicommunication
│   │   │   ├── config        # Bean 설정 및 Properties
│   │   │   ├── context       # 헤더/컨텍스트 정의
│   │   │   ├── metrics       # 요청 지표 수집
│   │   │   ├── orders        # 도메인, 서비스, API, 캐시, 저장소
│   │   │   ├── ratelimit     # 레이트 리미터 구현
│   │   │   └── security      # REST & gRPC 인증
│   │   └── proto/orders.proto
│   └── test/java/...         # REST/gRPC/Service 테스트
```

이 구조를 통해 REST ↔ gRPC 경로는 다르지만 인증, 레이트 리미팅, 캐시, 도메인 로직을 공통으로 사용하게 됩니다. 프로토콜별 차이를 확인하면서 서비스 품질 목표(확장성, 성능, 보안)를 어떻게 고민해야
하는지 실습해보세요.
