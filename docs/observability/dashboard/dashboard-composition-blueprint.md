# 관측성 대시보드 구성 청사진 (Dashboard Composition Blueprint)

본 문서는 OTLP 기반의 Logs, Traces, Metrics를 활용하여 구체적으로 어떤 위젯을 어디에 배치하고, 어떤 시각화 방식을 사용할지 정의하는 '대시보드 구성 마스터 플랜'이다.

---

## 1. 전역 현황 (Global Health) 대시보드

**목적:** 전체 시스템의 건강 상태를 한눈에 파악하고 이상 징후를 감지.

| 섹션             | 위젯 명칭               | 데이터 소스                  | 시각화 타입        | 기간/갱신 |
| :--------------- | :---------------------- | :--------------------------- | :----------------- | :-------- |
| **RED Overview** | 전체 서비스 성공률      | `http.server.duration_count` | **Gauge** (0-100%) | 15m / 30s |
|                  | 전체 RPS 추이           | `http.server.duration_count` | **Stacked Area**   | 30m / 30s |
|                  | P99 지연시간 Top 5      | `http.server.duration`       | **Horizontal Bar** | 1h / 1m   |
| **USE Overview** | CPU 사용량 Top 5 호스트 | `system.cpu.utilization`     | **Bar Chart**      | 1h / 1m   |
|                  | 메모리 사용량 (전체)    | `system.memory.usage`        | **Line Chart**     | 1h / 1m   |
|                  | 디스크 잔여량 경고      | `system.disk.utilization`    | **Pie Chart**      | 24h / 5m  |
| **Status**       | 서비스 상태 요약        | `systemd.unit.state`         | **Stat (Count)**   | 15m / 30s |

### 전역 현황 레이아웃 (Layout Design)

```plantuml
@startuml
skinparam handwritten false
skinparam defaultTextAlignment center
skinparam backgroundColor #FFFFFF

package "Global Health Dashboard" {
  node "[Section 1] RED Overview: 서비스 골든 시그널" as S1 {
    rectangle "전체 성공률 (%)\n(Gauge)" as G1
    rectangle "전체 처리량 (RPS)\n(Stacked Area Chart)" as AC1
    rectangle "P99 지연시간 Top 5\n(Bar Chart)" as BC1

    G1 -right- AC1
    AC1 -right- BC1
  }

  node "[Section 2] USE Overview: 인프라 리소스 상태" as S2 {
    rectangle "CPU Top 5 Hosts\n(Bar Chart)" as B2
    rectangle "Memory Usage Trend\n(Line Chart)" as L2
    rectangle "Disk Warn\n(Pie Chart)" as P2

    B2 -right- L2
    L2 -right- P2
  }

  node "[Section 3] Systemd & Daemon Status" as S3 {
    rectangle "Active Services\n(Stat Card)" as ST3_1
    rectangle "Inactive / Restarting\n(Stat Card - RED)" as ST3_2
    rectangle "Last Events\n(Small Table)" as T3

    ST3_1 -right- ST3_2
    ST3_2 -right- T3
  }
}

S1 -down- S2
S2 -down- S3
@enduml
```

### ASCII 레이아웃 (Conceptual View)

```text
+-----------------------------------------------------------------------+
| [Section 1] RED Overview: 서비스 골든 시그널                             |
| +-----------------+ +-------------------------+ +-------------------+ |
| | 전체 성공률 (%)  | | 전체 처리량 (RPS)       | | P99 지연시간 Top 5| |
| | (Gauge)         | | (Stacked Area Chart)    | | (Bar Chart)       | |
| +-----------------+ +-------------------------+ +-------------------+ |
+-----------------------------------------------------------------------+
| [Section 2] USE Overview: 인프라 리소스 상태                             |
| +-------------------------+ +-------------------------+ +-----------+ |
| | CPU Top 5 Hosts         | | Memory Usage Trend      | | Disk Warn | |
| | (Bar Chart)             | | (Line Chart)            | | (Pie)     | |
| +-------------------------+ +-------------------------+ +-----------+ |
+-----------------------------------------------------------------------+
| [Section 3] Systemd & Daemon Status                                   |
| +-----------------+ +-------------------------+ +-------------------+ |
| | Active Services | | Inactive / Restarting   | | Last Events       | |
| | (Stat Card)     | | (Stat Card - RED)       | | (Small Table)     | |
| +-----------------+ +-------------------------+ +-------------------+ |
+-----------------------------------------------------------------------+
```

---

**목적:** 특정 서비스의 성능 저하 원인 분석 및 실시간 장애 대응.

| 섹션            | 위젯 명칭             | 데이터 소스                  | 시각화 타입     | 기간/갱신 |
| :-------------- | :-------------------- | :--------------------------- | :-------------- | :-------- |
| **Service RED** | 엔드포인트별 RPS      | `http.server.duration_count` | **Stacked Bar** | 15m / 10s |
|                 | 응답 시간 분포        | `http.server.duration`       | **Histogram**   | 1h / 30s  |
|                 | HTTP 상태 코드 비중   | `http.server.duration_count` | **Pie Chart**   | 15m / 30s |
| **Runtime**     | JVM Heap & GC         | `jvm.memory.used`, `gc`      | **Line & Area** | 1h / 1m   |
|                 | Active Threads        | `jvm.thread.count`           | **Line Chart**  | 1h / 1m   |
| **Log/Trace**   | 에러 로그 발생 트렌드 | `logs-otel-v1`               | **Bar Chart**   | 1h / 30s  |
|                 | 실시간 로그 스트림    | `logs-otel-v1`               | **Data Table**  | 15m / 5s  |
|                 | 지연 시간 히트맵      | Traces (Span Duration)       | **Heatmap**     | 1h / 30s  |

### 웹 앱 상세 레이아웃 (Layout Design)

```plantuml
@startuml
skinparam handwritten false
skinparam defaultTextAlignment center
skinparam backgroundColor #FFFFFF

package "Web App Deep-dive (service.name: {APP})" {
  node "[Top] Web App RED Signals" as T1 {
    rectangle "RPS by Endpoint\n(Stacked Bar)" as B1
    rectangle "Latency Distribution\n(Histogram)" as H1
    rectangle "Status Code Ratio\n(Pie Chart)" as P1

    B1 -right- H1
    H1 -right- P1
  }

  node "[Middle] Runtime & Resources (JVM Analysis)" as M1 {
    rectangle "Heap vs Non-Heap Usage\n(Area Chart)" as A2
    rectangle "Thread Count & State\n(Line Chart)" as L2

    A2 -right- L2
  }

  node "[Bottom] Live Evidence (Logs & Traces)" as B1_LOG {
    rectangle "Error Trend\n(Bar Chart)" as EB1
    rectangle "Latency Heatmap\n(Heatmap)" as LH1
    rectangle "Live Log Stream (Tail -f)\n(Data Table with TraceID)" as DT1

    EB1 -right- LH1
    LH1 -down- DT1
  }
}

T1 -down- M1
M1 -down- B1_LOG
@enduml
```

### ASCII 레이아웃 (Conceptual View)

```text
+-----------------------------------------------------------------------+
| [Top] Web App RED Signals (service.name: {APP})                       |
| +-----------------+ +-------------------------+ +-------------------+ |
| | RPS by Endpoint | | Latency Distribution    | | Status Code Ratio | |
| | (Stacked Bar)   | | (Histogram)             | | (Pie Chart)       | |
| +-----------------+ +-------------------------+ +-------------------+ |
+-----------------------------------------------------------------------+
| [Middle] Runtime & Resources (JVM Analysis)                           |
| +-------------------------------------+ +---------------------------+ |
| | Heap vs Non-Heap Usage              | | Thread Count & State      | |
| | (Area Chart)                        | | (Line Chart)              | |
| +-------------------------------------+ +---------------------------+ |
+-----------------------------------------------------------------------+
| [Bottom] Live Evidence (Logs & Traces)                                |
| +-------------------------+ +---------------------------------------+ |
| | Error Trend (Bar)       | | Latency Heatmap (Visual Analysis)     | |
| +-------------------------+ +---------------------------------------+ |
| +-------------------------------------------------------------------+ |
| | TIMESTAMP | LEVEL | MESSAGE | TRACE_ID (Link)                     | |
| | (Live Data Table - Auto Refresh 5s - "Tail -f" Experience)        | |
| +-------------------------------------------------------------------+ |
+-----------------------------------------------------------------------+
```

---

## 3. 데몬/시스템 상세 (Daemon & Systemd Deep-dive)

**목적:** 백그라운드 작업 및 시스템 서비스의 안정성 감시.

| 섹션           | 위젯 명칭             | 데이터 소스                | 시각화 타입        | 기간/갱신 |
| :------------- | :-------------------- | :------------------------- | :----------------- | :-------- |
| **Job Signal** | 작업 처리 속도 (Rate) | Custom Metric / Log Count  | **Line Chart**     | 1h / 1m   |
|                | 작업 실패율           | Log Error / Span Error     | **Gauge**          | 15m / 30s |
|                | 평균 실행 시간        | Span Duration              | **Histogram**      | 1h / 1m   |
| **Systemd**    | CPU/Memory (Service)  | `systemd.service.resource` | **Area Chart**     | 1h / 1m   |
|                | 서비스 업타임         | `systemd.unit.uptime`      | **Stat (Time)**    | 24h / 5m  |
| **Logs**       | 로그 레벨 분포        | `logs-otel-v1`             | **Horizontal Bar** | 15m / 30s |
|                | 실시간 데몬 로그      | `logs-otel-v1`             | **Data Table**     | 15m / 5s  |

### 데몬 상세 레이아웃 (Layout Design)

```plantuml
@startuml
skinparam handwritten false
skinparam defaultTextAlignment center
skinparam backgroundColor #FFFFFF

package "Daemon & Systemd Deep-dive" {
  node "[Top] Daemon Job Performance" as T1 {
    rectangle "Job Rate (tps)\n(Line Chart)" as L1
    rectangle "Task Failure %\n(Gauge)" as G1
    rectangle "Avg Exec Time\n(Histogram)" as H1

    L1 -right- G1
    G1 -right- H1
  }

  node "[Middle] Resource Usage by Service" as M1 {
    rectangle "CPU Usage (Process)\n(Area Chart)" as A2
    rectangle "Memory RSS (Process)\n(Area Chart)" as AR2
    rectangle "Uptime\n(Stat Card)" as S2

    A2 -right- AR2
    AR2 -right- S2
  }

  node "[Bottom] Daemon Log Stream" as B1_LOG {
    rectangle "Log Level Distribution\n(Horizontal Bar)" as LB1
    rectangle "Daemon Log Stream\n(Data Table)" as DT1

    LB1 -down- DT1
  }
}

T1 -down- M1
M1 -down- B1_LOG
@enduml
```

### ASCII 레이아웃 (Conceptual View)

```text
+-----------------------------------------------------------------------+
| [Top] Daemon Job Performance                                          |
| +-----------------+ +-------------------------+ +-------------------+ |
| | Job Rate (tps)  | | Task Failure %          | | Avg Exec Time     | |
| | (Line Chart)    | | (Gauge)                 | | (Histogram)       | |
| +-----------------+ +-------------------------+ +-------------------+ |
+-----------------------------------------------------------------------+
| [Middle] Resource Usage by Service                                    |
| +-------------------------+ +-------------------------+ +-----------+ |
| | CPU Usage (Process)     | | Memory RSS (Process)    | | Uptime    | |
| | (Area Chart)            | | (Area Chart)            | | (Stat)    | |
| +-------------------------+ +-------------------------+ +-----------+ |
+-----------------------------------------------------------------------+
| [Bottom] Daemon Log Stream                                            |
| +-------------------------------------------------------------------+ |
| | Log Level Distribution (Horizontal Bar)                           | |
| +-------------------------------------------------------------------+ |
| | TIMESTAMP | LEVEL | MESSAGE (Log Content)                         | |
| | (Data Table - Auto Refresh 5s)                                    | |
| +-------------------------------------------------------------------+ |
+-----------------------------------------------------------------------+
```

---

## 4. 핵심 시각화 가이드 (Visualization Guide)

1. **Gauge (게이지):** '현재' 가장 위험한 수치를 보여줄 때 사용 (예: 성공률 90% 미만 시 빨간색).
2. **Stacked Area/Bar (스택형):** '전체' 대비 '부분'의 비중을 시간 흐름으로 볼 때 사용 (예: 전체 요청 중 에러 비중).
3. **Histogram (히스토그램):** 응답 시간의 '분포'를 파악하여 소수의 느린 요청(Long-tail)을 감지할 때 사용.
4. **Heatmap (히트맵):** 수천 개의 트레이스 데이터를 시간과 레이턴시 축으로 시각화하여 패턴을 찾을 때 사용.
5. **Data Table (데이터 테이블):** 원시 로그를 최신순으로 정렬하여 장애의 직접적인 증거(Evidence)를 확인할 때 사용.
