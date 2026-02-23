# 시스템 아키텍처 설계

## 개요

운영 실시간 확인 흐름(`journald`)은 유지하면서, 중앙 분석/검색/예외 추적은 OTLP 기반으로 통합한다.
설계 범위는 로그 중앙화와 예외 추적(트레이스/메트릭 연계)까지 포함한다.

## 목표/비목표

### 목표

- 로그, 트레이스, 메트릭 수집 경로를 OTLP 표준으로 정렬한다.
- 운영자 즉시 확인 경로(`journald`)와 중앙 분석 경로(OpenSearch/APM)를 동시에 보장한다.
- Collector 중심 구조로 백엔드(OpenSearch, SigNoz, LGTM) 교체 가능성을 유지한다.

### 비목표

- 애플리케이션 비즈니스 로직/도메인 예외 처리 정책 자체를 정의하지 않는다.
- SIEM 규칙, 보안 관제 정책 세부 구현은 다루지 않는다.
- 알림 이메일 방식에 대해서는 다루지 않는다.
- 장애 상황에서 유실을 최소화하도록 큐/재시도 기반 전달 구조는 다루지 않는다.

## 사용기술

| 분류           | 기술                          | 용도                                   |
| -------------- | ----------------------------- | -------------------------------------- |
| 관측성 표준    | OpenTelemetry (OTLP)          | 로그/트레이스/메트릭 수집 및 전송 표준 |
| 수집/라우팅    | OTel Collector                | 수집, 변환, 라우팅, 다중 Export        |
| 로그 검색/집계 | OpenSearch                    | 인덱싱, 검색, 집계                     |
| 시각화         | OpenSearch Dashboards         | 로그 조회, 시각화, 운영 분석           |
| APM 백엔드     | LGTM                          | 예외/트레이스 분석, 서비스 맵          |
| 로컬 운영 확인 | systemd-journald (`journald`) | 호스트 실시간 로그 확인                |
| 런타임         | systemd service, podman/OCI   | 애플리케이션 실행 환경                 |

## 시스템 컨텍스트

관측성 플랫폼(System of Interest)은 Edge 서비스에서 발생한 로그/트레이스/메트릭을 수집하고,
로그 분석(OpenSearch)과 예외 분석(APM)으로 분기 전달한다.

운영팀은 `journald`와 대시보드를 병행 사용하고, 개발팀/관리자는 대시보드 중심으로 분석한다.

```plantuml
!include <C4/C4_Context>

title Observability Platform - System Context

Person(ops, "Ops", "운영팀")
Person(dev, "Developer", "개발팀")

System(obs, "Observability Platform", "OTLP 기반 수집/변환/라우팅")
System_Ext(edgeApps, "Edge Applications", "Native/Container 애플리케이션")
System_Ext(opensearchCluster, "OpenSearch Cluster", "로그 인덱싱/검색")
System_Ext(fileStorage, "File Storage", "장기 보관")
System_Ext(apmBackend, "APM Backend", "SigNoz 또는 LGTM")
System_Ext(hostOps, "Host Logging", "journald/journalctl")

Rel(edgeApps, obs, "sends logs/traces/metrics", "OTLP")
Rel(obs, opensearchCluster, "exports logs", "OTLP/HTTP or Bulk API")
Rel(obs, fileStorage, "exports logs", "File Exporter")
Rel(obs, apmBackend, "exports traces/logs/metrics", "OTLP/HTTP")
Rel(edgeApps, hostOps, "writes runtime logs", "stdout/stderr")
Rel(ops, hostOps, "queries", "journalctl")
Rel(ops, obs, "operates/monitors", "Dashboard/CLI")
Rel(dev, obs, "searches/analyzes logs", "Web UI")
```

## 컨테이너 다이어그램 1: 시스템 구조도

```plantuml
!include <C4/C4_Container>

title Observability Platform - Integrated Container View

Person(ops, "Ops")
Person(dev, "Developer")

System_Boundary(host, "Host (Edge Server)") {
  Container(nativeProc, "Native Process", "systemd service", "애플리케이션 로그/트레이스/메트릭 생성")
  Container(podCtr, "Podman Container", "podman/OCI", "컨테이너 앱 로그/트레이스/메트릭 생성")
  Container(journald, "journald", "systemd-journald", "실시간 운영 조회")
}

System_Boundary(obs, "Central Observability") {
  Container(centralCollector, "OTel Collector", "OpenTelemetry", "중앙 수집/변환/라우팅")
  Container(apmCollector, "OTel Collector (APM)", "OpenTelemetry", "APM 전용 파이프라인")
  Container(fileStore, "File Store", "Local FS / NFS", "로그 장기 보관")
}

System_Boundary(search, "OpenSearch") {
  Container(opensearch, "OpenSearch", "OpenSearch", "로그 인덱싱/검색")
  Container(dashboards, "Dashboards", "OpenSearch Dashboards", "로그 시각화/조회")
}

System_Boundary(apm_boundary, "APM") {
  Container(apm, "APM", "LGTM, Signoz", "예외/트레이스 분석")
}

Rel(nativeProc, journald, "writes", "stdout/stderr")
Rel(podCtr, journald, "writes", "stdout/stderr")
Rel(nativeProc, centralCollector, "sends logs/traces/metrics", "OTLP")
Rel(podCtr, centralCollector, "sends logs/traces/metrics", "OTLP")
Rel(centralCollector, opensearch, "exports logs", "OTLP/HTTP or Bulk API")
Rel(centralCollector, fileStore, "exports logs", "File Exporter")
Rel(centralCollector, apmCollector, "forwards traces/logs/metrics", "OTLP")
Rel(apmCollector, apm, "exports", "OTLP/HTTP")
Rel(dashboards, opensearch, "queries", "REST API")

Rel(ops, journald, "queries", "journalctl CLI")
Rel(dev, dashboards, "views", "HTTPS")
```

## 컨테이너 다이어그램 2: 로그 수집/검색 세부

```plantuml
!include <C4/C4_Container>

title Logging Flow - Container View

Person(ops, "Ops")
Person(dev, "Developer")

System_Boundary(host, "Host (Edge Server)") {
  Container(nativeProc, "Native Process", "systemd service", "로그 생성")
  Container(podCtr, "Podman Container", "podman/OCI", "로그 생성")
  Container(journald, "journald", "systemd-journald", "실시간 운영 조회")
}

System_Boundary(logging, "Central Logging") {
  Container(collector, "OTel Collector", "OpenTelemetry", "로그 수집/변환/라우팅")
  Container(fileStore, "File Store", "Local FS / NFS", "원본 로그 보관")
}

System_Boundary(search, "OpenSearch Cluster") {
  Container(opensearch, "OpenSearch", "OpenSearch", "검색/집계")
  Container(dashboards, "Dashboards", "OpenSearch Dashboards", "조회/시각화")
}

Rel(nativeProc, journald, "writes", "stdout/stderr")
Rel(podCtr, journald, "writes", "stdout/stderr")
Rel(nativeProc, collector, "sends logs", "OTLP/gRPC")
Rel(podCtr, collector, "sends logs", "OTLP/gRPC")
Rel(collector, opensearch, "exports", "OTLP/HTTP or Bulk API")
Rel(collector, fileStore, "exports", "File Exporter")
Rel(dashboards, opensearch, "queries", "REST API")
Rel(ops, journald, "queries", "journalctl")
Rel(dev, dashboards, "analyzes", "HTTPS")
```

## 컨테이너 다이어그램 3: 예외 추적 세부

```plantuml
!include <C4/C4_Container>

title Exception Tracking Flow - Container View

Person(devops, "Devops")

System_Boundary(host, "Host (Edge Server)") {
  Container(app, "App", "Java / Container", "예외 및 trace/span 생성")
}

System_Boundary(pipeline, "Tracing Pipeline") {
  Container(centralCollector, "OTel Collector", "OpenTelemetry", "수집/전처리")
  Container(apmCollector, "OTel Collector (APM)", "OpenTelemetry", "APM 전달 파이프라인")
}

System_Boundary(apm, "APM Backend") {
  Container(sigNoz, "SigNoz", "APM Backend", "예외/트레이스 분석")
}

Rel(app, centralCollector, "sends logs/traces/metrics", "OTLP")
Rel(centralCollector, apmCollector, "forwards", "OTLP")
Rel(apmCollector, sigNoz, "exports", "OTLP/HTTP")
Rel(devops, sigNoz, "queries", "Web UI")
```

## 각 구성요소 설명

| 구성요소                          | 책임                             | 입력                        | 출력                                       | 운영 포인트                                  |
| --------------------------------- | -------------------------------- | --------------------------- | ------------------------------------------ | -------------------------------------------- |
| Native Process / Podman Container | 애플리케이션 텔레메트리 생성     | 비즈니스 이벤트             | 로그/트레이스/메트릭(OTLP), stdout/stderr  | 로깅 레벨, appender/SDK 설정 일관성          |
| journald                          | 호스트 실시간 로그 보관/조회     | stdout/stderr               | `journalctl` 조회 결과                     | 운영자 즉시 대응 경로 유지                   |
| OTel Collector (Central)          | 수집/변환/라우팅, 다중 Export    | OTLP logs/traces/metrics    | OpenSearch, File Store, APM Collector 전달 | queue/retry/backpressure, drop rate 모니터링 |
| OTel Collector (APM)              | APM 전용 전달 최적화             | 중앙 Collector 전달 데이터  | APM Backend 적재                           | export timeout/retry, 파이프라인 지연 감시   |
| OpenSearch                        | 로그 인덱싱/검색/집계            | Collector export 로그       | 검색 인덱스/집계 결과                      | 인덱싱 실패율, 지연, ILM/보존 정책           |
| OpenSearch Dashboards             | 로그 시각화/탐색 UI              | OpenSearch REST 응답        | 대시보드/검색 결과                         | 접근 제어, 쿼리 성능                         |
| File Store (Local FS/NFS)         | 장기 보관/백업                   | Collector export 로그       | 원본 보관 데이터                           | 보존 기간, 용량/회전 정책                    |
| APM (APM Backend)                 | 예외, trace/span, 서비스 맵 분석 | APM Collector export 데이터 | 예외 분석 화면/알림 기반 데이터            | 인덱싱 지연, 조회 성능, 백엔드 교체 가능성   |

## 인터페이스 요약

| Source                   | Target                   | Data                | Protocol                    |
| ------------------------ | ------------------------ | ------------------- | --------------------------- |
| App                      | OTel Collector (Central) | logs/traces/metrics | OTLP                        |
| OTel Collector (Central) | OpenSearch               | logs                | OpenSearch Exporter (HTTPS) |
| OTel Collector (Central) | File Store               | logs                | File Exporter               |
| OTel Collector (Central) | OTel Collector (APM)     | traces/logs/metrics | OTLP                        |
| Runtime                  | journald                 | logs                | stdout/stderr               |
