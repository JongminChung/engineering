# OTLP 데이터 모델 및 분산 계측 가이드

본 문서는 **OTLP(OpenTelemetry Protocol)**의 핵심 데이터 구조인 3단계 계층 모델을 이해하고, 실제 분산 환경에서 자동 계측 라이브러리와 수동 SDK가 어떻게 협력하여 데이터를 전송하는지 다룹니다.

---

## 1. OTLP 3단계 계층 구조 (Hierarchical Array Model)

OTLP는 네트워크 전송 효율을 극대화하기 위해 데이터를 '리소스'와 '스코프' 단위로 그룹화하여 처리합니다. 특히 **Scope**는 데이터를 생성한 주체(라이브러리)를 식별하는 핵심 계층입니다.

| 계층         | Proto 필드명                 | 의미                                          | 비고                  |
| :----------- | :--------------------------- | :-------------------------------------------- | :-------------------- |
| **Resource** | `ResourceLogs/Spans/Metrics` | **어디서?** (서비스명, 호스트, 컨테이너 정보) | 전역 속성             |
| **Scope**    | `ScopeLogs/Spans/Metrics`    | **누가?** (계측 라이브러리명, 버전)           | Instrumentation Scope |
| **Data**     | `LogRecord / Span / Metric`  | **무엇을?** (메시지, 실행시간, 지표값 등)     | 개별 시그널           |

---

## 2. Trace와 Span: 분산 추적의 핵심

Trace는 클라이언트의 요청이 시스템을 통과하는 전체 여정이며, **Span**은 그 여정 중 특정 작업의 단위입니다.

- **Trace ID:** 전체 요청을 식별하는 고유 ID.
- **Span ID:** 개별 작업(메서드 호출, DB 쿼리 등)을 식별하는 ID.
- **Parent Span ID:** 현재 작업의 부모가 누구인지 기록하여 계층 구조를 형성합니다.
- **Span Kind:** 내부 작업(Internal), 서버 수신(Server), 클라이언트 호출(Client) 등의 역할을 정의합니다.

---

## 3. 계측 지원 라이브러리의 역할 (Instrumentation Libraries)

애플리케이션에 추가되는 각 계측 라이브러리는 고유한 **Scope Name**을 가지며, 해당 기술 스택의 데이터를 자동으로 수집합니다.

- **`io.opentelemetry.spring-webmvc`**: HTTP 요청/응답 및 컨트롤러 실행 추적
- **`io.opentelemetry.jdbc`**: SQL 쿼리 실행 및 DB 연결 추적
- **`io.opentelemetry.runtime-telemetry`**: JVM 가비지 컬렉션, 메모리 지표 수집

---

## 4. 분산 환경 계측 시나리오 (OrderService → InventoryService)

자동 계측 라이브러리가 인프라 계층을 담당하고, 개발자가 작성한 코드가 비즈니스 맥락을 추가합니다.

### 4.1 OrderService (Spring Web + JDBC + Metrics)

서비스에 유입되는 HTTP 요청은 Spring Web 라이브러리가, 비즈니스 로직은 수동 Span이, DB 호출은 JDBC 라이브러리가 각각 계측합니다.

```java
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final Tracer tracer = GlobalOpenTelemetry.getTracer("order-service-custom");
    private static final LongCounter orderCounter = GlobalOpenTelemetry.getMeter("order-metrics")
            .counterBuilder("order.total").build();

    public void createOrder(String orderId) {
        // [1. 자동 계측] io.opentelemetry.spring-webmvc에 의해 이미 부모 Span이 생성된 상태

        // [2. 수동 계측] 비즈니스 로직을 위한 자식 Span 생성
        Span span = tracer.spanBuilder("process-order-logic").startSpan();

        try (Scope scope = span.makeCurrent()) {
            MDC.put("order.id", orderId);
            orderCounter.add(1); // [3. 지표 계측] 주문 수량 기록

            log.info("주문 처리 시작"); // [4. 로그 계측] SLF4J 브릿지를 통해 Span ID와 결합

            // [5. 자동 계측] io.opentelemetry.jdbc 라이브러리가 아래 DB 호출을 자동으로 추적
            saveToDatabase(orderId);

            // [6. 전파] Agent가 HTTP 헤더에 Trace Context를 주입하여 InventoryService로 전달
            callInventoryService(orderId);

        } finally {
            span.end();
            MDC.clear();
        }
    }
}
```

**SLF4J 연동 방식:**

위 코드의 `log.info()`는 별도의 OTel API 호출 없이도 로그 프레임워크에 설정된 Appender를 통해 처리됩니다. 이 과정에서 현재 활성화된 **Trace ID**와 **MDC(order.id)** 정보가 추출되어 OTLP 로그 패킷의 속성으로 자동 결합됩니다.

> ⚠️ **실무 주의사항: MDC 필드 캡처 설정**
> 로그 프레임워크(Log4j2/Logback) 설정 파일에서 `OpenTelemetryAppender`를 정의할 때,
> `captureMdcAttributes` 속성을 통해 OTLP Attributes로 변환할 MDC 키 목록을 반드시 명시해야 합니다.
> (예: `<captureMdcAttributes>order.id,user.role</captureMdcAttributes>` 또는 전체 허용 시 `*`)
> 이 설정이 누락되면 `MDC.put()`으로 넣은 데이터가 OTLP 수집기로 전송되지 않습니다.

### 4.2 InventoryService (재고 관리)

재고 서비스는 전달받은 Trace ID를 기반으로 로그를 남기며, 현재 재고 상태를 관찰합니다.

```java
public class InventoryService {
    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private static int stockCount = 100;

    static {
        // Metric: 현재 재고량을 주기적으로 관찰하는 Gauge
        GlobalOpenTelemetry.getMeter("inventory-service")
            .gaugeBuilder("inventory.stock")
            .buildWithCallback(m -> m.record(stockCount));
    }

    public void checkInventory(String orderId) {
        // 상위 서비스(OrderService)로부터 전파된 Trace Context 내에서 로그가 생성됨
        log.info("재고 확인 중: {}", orderId);
        if (stockCount > 0) stockCount--;
    }
}
```

---

## 5. OTLP 데이터 구조 예시 (Scope 구분)

수집된 데이터는 어떤 라이브러리가 만들었느냐에 따라 서로 다른 `scopeSpans` 배열에 담깁니다.

### 5.1 Traces: 여러 Scope의 결합

하나의 Trace 내에 Spring Web(자동), Custom SDK(수동), JDBC(자동) 계측 데이터가 공존합니다.

```json
{
    "resourceSpans": [
        {
            "resource": { "attributes": [{ "key": "service.name", "value": "order-service" }] },
            "scopeSpans": [
                {
                    "scope": { "name": "io.opentelemetry.spring-webmvc", "version": "1.25.0" },
                    "spans": [
                        {
                            "name": "POST /orders",
                            "kind": "SPAN_KIND_SERVER",
                            "traceId": "4bf921150b7ee7293054487170572338",
                            "spanId": "af71415174092b7a"
                        }
                    ]
                },
                {
                    "scope": { "name": "order-service-custom" },
                    "spans": [
                        {
                            "name": "process-order-logic",
                            "parentSpanId": "af71415174092b7a",
                            "traceId": "4bf921150b7ee7293054487170572338",
                            "spanId": "00f067aa0ba902b7"
                        }
                    ]
                },
                {
                    "scope": { "name": "io.opentelemetry.jdbc" },
                    "spans": [
                        {
                            "name": "INSERT INTO orders",
                            "kind": "SPAN_KIND_CLIENT",
                            "traceId": "4bf921150b7ee7293054487170572338",
                            "parentSpanId": "00f067aa0ba902b7"
                        }
                    ]
                }
            ]
        }
    ]
}
```

### 5.2 Metrics: 통계 데이터

지표 라이브러리에 의해 수집된 시스템 수치입니다.

```json
{
  "resourceMetrics": [{
    "scopeMetrics": [{
      "scope": { "name": "order-metrics" },
      "metrics": [{
        "name": "order.total",
        "sum": { "dataPoints": [{ "asInt": "1", "attributes": [...] }] }
      }]
    }]
  }]
}
```

---

## 6. 핵심 요약 및 설계 철학

- **Batching (묶음 배송):** OTLP의 계층 구조는 데이터를 Resource와 Scope 단위로 묶어 전송 오버헤드를 최소화합니다.
- **Trace-Centric (맥락 중심):** `traceId`를 공통 분모로 삼아 "어떤 요청에서 어떤 로그가 남았고, 당시의 시스템 지표(Metric)는 어떠했는가"를 통합 분석합니다.
- **Context Propagation:** 서비스 간 이동 시 Trace 정보를 자동으로 전달하여 분산 시스템의 가시성을 확보합니다.
