# OTel Java Instrumentation

## OTel Java[^1]

- **역할**: OpenTelemetry의 **핵심 API 및 SDK** 구현체
- **제공 기능**:
    - Traces, Metrics, Logs API
    - Context 전파 메커니즘
    - 데이터 수집 및 처리 SDK
    - Exporter (OTLP, Zipkin, Prometheus 등)
- **사용 방식**: 개발자가 **코드에 직접 통합**하여 텔레메트리 수집

---

## OTel Java Instrumentation[^2]

- **역할**: **자동 계측 에이전트**
- **제공 기능**:
    - Java Agent를 통한 바이트코드 조작
    - 인기 프레임워크/라이브러리 자동 계측 (Spring, JDBC, HTTP 클라이언트 등)
    - 코드 수정 없이 텔레메트리 수집
- **사용 방식**: `-javaagent` 플래그로 JVM 실행 시 적용

## 관계도

```text
OpenTelemetry Specification (언어 중립적 표준)
           ↓
┌──────────────────────────────────┐
│   opentelemetry-java (Core)      │ ← API/SDK 구현
│   - API, SDK, Exporters          │
└──────────────────────────────────┘
           ↓ (의존)
┌────────────────────────────────────┐
│ opentelemetry-java-instrumentation │ ← 자동 계측 확장
│   - Java Agent                     │
│   - Library Instrumentations       │
└────────────────────────────────────┘
```

## 자동 계측 과정

```java
// 사용자 코드 (수정 없음)
@GetMapping("/hello")
public String hello() {
    restTemplate.getForObject("http://api.example.com", String.class);
    return "Hello";
}
```

**Java Agent가 하는 일:**

1. 바이트코드 조작 (ByteBuddy 사용)
    - `RestTemplate` 메서드 호출을 감지
    - 자동으로 계측 코드 주입
2. 주입되는 코드 (실제로는 이렇게 동작):
    - ```java
      // Agent가 바이트코드 레벨에서 자동 추가
      Span span = tracer.spanBuilder("GET /api")  // ← opentelemetry-java SDK 사용
          .startSpan();
      try (Scope scope = span.makeCurrent()) {
          // 원본 코드 실행
          restTemplate.getForObject(...);
      } finally {
          span.end();
      }
      ```

[^1]: https://github.com/open-telemetry/opentelemetry-java

[^2]: https://github.com/open-telemetry/opentelemetry-java-instrumentation
