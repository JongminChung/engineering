# 애플리케이션 상세 대시보드 구현 가이드 (OpenSearch & OTLP)

본 문서는 OTLP를 통해 수집된 애플리케이션의 Logs, Traces, Metrics 데이터를 OpenSearch Dashboards에서 어떻게 구체적으로 시각화하고 구현할지에 대한 기술적인 가이드를 제공한다.

---

## 1. 전제 조건 및 데이터 구조

OpenTelemetry Instrumentation이 적용된 애플리케이션에서 다음과 같은 데이터가 유입되고 있어야 한다.

- **Metrics:** `v1.metrics-*` (예: `jvm.memory.used`, `http.server.duration`)
- **Traces:** `otel-v1-apm-span-*` (Span 데이터)
- **Logs:** `logs-otel-v1-*` (MDC에 `trace_id`, `span_id` 포함 권장)

---

## 2. 시그널별 구체적 시각화 구현 (Metrics, Traces, Logs)

### ① Metrics: RED Metrics 및 런타임 상태 (Line & Gauge)

가장 먼저 확인해야 할 골든 시그널이다. 런타임(JVM 등) 지표는 Phase 2 단계에서 필요시 추가한다.

| 위젯 명칭       | 데이터 소스 (Metric Name)          | 시각화 방식  | 쿼리/필터 예시 (DSL/Lucene)                       | 필수 여부 |
| :-------------- | :--------------------------------- | :----------- | :------------------------------------------------ | :-------- |
| **Throughput**  | `http.server.duration_count`       | Stacked Area | `service.name: "my-app"`                          | **필수**  |
| **P99 Latency** | `http.server.duration` (Histogram) | Line Chart   | `service.name: "my-app"` (Percentile: 99)         | **필수**  |
| **Error Rate**  | `http.server.duration_count`       | Gauge        | `http.response.status_code: [500 TO 599]` / Total | **필수**  |
| **JVM Heap**    | `jvm.memory.used` (선택적)         | Line Chart   | `type: "heap"`, `service.name: "my-app"`          | 선택      |
| **GC Pause**    | `jvm.gc.pause_sum` (선택적)        | Bar Chart    | `service.name: "my-app"`                          | 선택      |

### ② Traces: API 성능 분포 (Heatmap & Scatter Plot)

트레이스 데이터를 통해 특정 요청의 지연 원인을 시각적으로 파악한다.

- **Latency Heatmap:** `traceId`와 `durationInNanoseconds`를 기반으로 시간대별 응답 속도 분포를 시각화한다.
    - **Y축:** Duration (ms)
    - **X축:** @timestamp
    - **용도:** 특정 시간대에 지연이 몰리는지, 혹은 산발적으로 발생하는지 확인.
- **Service Map:** Data Prepper가 생성한 `service-map` 인덱스를 사용하여 서비스 간 호출 관계와 에러 전파 경로를 확인한다.

### ③ Logs: 실시간 스트림 (Data Table)

장애 발생 시 '지금 무슨 일이 일어나는가'를 확인하기 위한 위젯이다.

- **Live Tail Widget:** `Data Table` 위젯을 사용하여 최근 15분의 로그를 5초 주기로 갱신한다.
    - **필드 구성:** `@timestamp`, `log.level`, `message`, `trace_id`
    - **Trace-Log Correlation:** `trace_id` 필드에 **Index Pattern Format**을 적용하여 클릭 시 Trace Analytics 화면으로 이동하도록 `URL` 템플릿을 설정한다.
        - 예: `https://opensearch-dashboards/app/observability-dashboards#/trace_analytics/traces/{{value}}`

---

## 3. 대시보드 인터랙션 설계 (Drill-down)

효율적인 운영을 위해 '전체'에서 '상세'로 이어지는 클릭 흐름을 구성한다.

1.  **Global Overview (전체 현황):**
    - `Top 5 Error Services` 위젯에서 특정 서비스명을 클릭한다.
2.  **Service Deep-dive (서비스 상세):**
    - 클릭 시 `service.name` 필터가 적용된 상세 대시보드로 이동한다.
    - 해당 시점의 `JVM Heap`과 `Error Log`를 동시에 비교하여 메모리 부족(OOM)인지 로직 에러인지 판단한다.
3.  **Trace Analysis (원인 분석):**
    - 로그 테이블의 `trace_id`를 클릭하여 전체 분산 트레이스 스팬(Span)을 확인하고 병목 지점을 특정한다.

---

## 4. 구체적인 위젯 배치도 (Mock-up)

```text
+-----------------------------------------------------------------------+
| [Filter Bar] service.name: [order-service v]  Time: [Last 15m v]      |
+-----------------------------------------------------------------------+
| <Section 1: App Health (RED)>                                         |
| [ RPS (Line) ] [ Error Rate (Gauge) ] [ P99 Latency (Line) ]          |
+-----------------------------------------------------------------------+
| <Section 2: Runtime (JVM)>                                            |
| [ Heap Usage (Area) ] [ GC Count (Bar) ] [ Thread Count (Line) ]      |
+-----------------------------------------------------------------------+
| <Section 3: Root Cause (Logs & Traces)>                               |
| [ Log Level Dist (Bar) ] [ Latency Heatmap (Heatmap) ]                |
| +-------------------------------------------------------------------+ |
| | @timestamp | level | message | trace_id (Link)                    | |
| | 12:01:05   | ERROR | DB Timeout | 8a3f... (Click)                 | |
| +-------------------------------------------------------------------+ |
+-----------------------------------------------------------------------+
```

---

## 5. 결론

본 가이드에 따라 위젯을 구성하면, 서비스의 물리적 상태와 논리적 상태(RED), 그리고 세부 증거(Logs/Traces)를 하나의 화면에서 유기적으로 분석할 수 있다. **초기 구축 단계에서는 언어 중립적인 RED 지표에 집중하고, 특정 언어(JVM 등)에 종속적인 메트릭은 운영 성숙도에 따라 단계적으로 도입하는 것을 권장한다.** 구체적인 레이아웃 배치는 [대시보드 구성 청사진](./dashboard-composition-blueprint.md)을 참고한다.
