# 시스템 아키텍처 설계

## 개요

이 시스템은 클라우드 애플리케이션에서 발생하는 시그널 (로그/트레이스/메트릭)을 수집, 저장하고 이를 기반으로 검색 및 알림을 제공하는 DX 경험을 상승시키는 목적의 시스템이다.

핵심 설계 방향은 다음과 같다.

- OpenSearch 클러스터를 활용한다.
- Logs, Traces, Metrics를 시각화하고 관측 가능성을 보장한다.
- 운영 실시간 확인 흐름은 jounrald 를 유지한다.
- 수집 표준은 OTLP를 우선으로 하며, OTLP Collector를 통해 수집/버퍼링/라우팅 한다.

## 목표/비목표

### 목표

- 로그, 트레이스, 메트릭 수집 경로를 OTLP 표준으로 입력을 받는다.
- 운영자 즉시 확인 경로(`journald`)와 중앙 분석 경로(OpenSearch)를 동시에 보장한다.
- Collector + Data Prepper 중심 구조로 OpenSearch 백엔드 적재 경로를 표준화한다.

### 비목표

- 애플리케이션 비즈니스 로직/도메인 예외 처리 정책 자체를 정의하지 않는다.
- SIEM 규칙, 보안 관제 정책 세부 구현은 다루지 않는다.
- 알림 이메일 방식에 대해서는 다루지 않는다.
- 장애 상황에서 유실을 최소화하도록 큐/재시도 기반 전달 구조는 다루지 않는다.

## 사용기술

| 분류                     | 기술                        | 용도                                     |
| ------------------------ | --------------------------- | ---------------------------------------- |
| 관측성 표준              | OpenTelemetry (OTLP)        | 로그/트레이스/메트릭 수집 및 전송 표준   |
| 수집/라우팅              | OTel Instrumentation        | Signal 수집 및 로그/트레이스/메트릭 전송 |
| OpenSearch 데이터 수집기 | Data Prepper                | OTLP 수신, 전처리, OpenSearch 적재       |
| 백엔드                   | OpenSearch                  | 인덱싱, 검색, 집계                       |
| 시각화                   | OpenSearch Dashboards       | 로그 조회, 시각화, 운영 분석             |
| 런타임                   | systemd service, podman/OCI | 애플리케이션 실행 환경                   |

## 시스템 컨텍스트

관측성 플랫폼(System of Interest)은 Edge 서비스에서 발생한 로그/트레이스/메트릭을 수집하고,
로그 분석 및 APM 분석 데이터를 OpenSearch로 전달한다(APM 경로는 Data Prepper 경유).

운영팀은 `journald`와 대시보드를 병행 사용하고, 개발팀/관리자는 대시보드 중심으로 분석한다.

```plantuml
!include <C4/C4_Context>

title Observability Platform - System Context

Person(ops, "Ops", "운영팀")
Person(dev, "Developer", "개발팀")

System(obs, "Observability Platform", "OTLP 기반 수집/변환/라우팅")
System_Ext(edgeApps, "Edge Applications", "Native/Container 애플리케이션")
System_Ext(opensearchCluster, "OpenSearch Cluster", "로그 인덱싱/검색")
System_Ext(dataPrepper, "Data Prepper", "OTLP 수신/전처리/적재")
System_Ext(hostOps, "Host Logging", "journald/journalctl")

Rel(edgeApps, obs, "sends logs/traces/metrics", "OTLP")
Rel(obs, dataPrepper, "exports traces/logs/metrics", "OTLP/HTTP")
Rel(dataPrepper, opensearchCluster, "indexes traces/logs/metrics", "OpenSearch API")
Rel(edgeApps, hostOps, "writes runtime logs", "stdout/stderr")
Rel(ops, hostOps, "queries", "journalctl")
Rel(ops, obs, "operates/monitors", "Dashboard/CLI")
Rel(dev, obs, "searches/analyzes logs", "Web UI")
```

## 컨테이너 다이어그램 1: 시스템 구조도

`Data Prepper` 의 목표는 OpenSearch 전용의 OTEL Collector로 구성한다.

중앙 Otel Collector를 제거하고 직접 OTel Instrumentation 애플리케이션이 수집한 시그널들을 전송해도 된다.

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
  Container(dataPrepper, "Data Prepper", "OpenSearch Data Prepper", "APM 데이터 전처리/적재")
}

System_Boundary(search, "OpenSearch") {
  Container(opensearch, "OpenSearch", "OpenSearch", "계측 데이터 백엔드")
  Container(dashboards, "Dashboards", "OpenSearch Dashboards", "계측 데이터 대시보드")
}

Rel(nativeProc, journald, "writes", "stdout/stderr")
Rel(podCtr, journald, "writes", "stdout/stderr")
Rel(nativeProc, centralCollector, "sends logs/traces/metrics", "OTLP")
Rel(podCtr, centralCollector, "sends logs/traces/metrics", "OTLP")
Rel(centralCollector, dataPrepper, "sends logs/traces/metrics", "OTLP")
Rel(dataPrepper, opensearch, "indexes traces/logs/metrics", "OpenSearch API")
Rel(dashboards, opensearch, "queries", "REST API")

Rel(ops, journald, "queries", "journalctl CLI")
Rel(dev, dashboards, "views", "HTTPS")
```

## 각 구성요소 설명

| 구성요소                          | 책임                          | 입력                          | 출력                                       | 운영 포인트                                  |
| --------------------------------- | ----------------------------- | ----------------------------- | ------------------------------------------ | -------------------------------------------- |
| Native Process / Podman Container | 애플리케이션 텔레메트리 생성  | 비즈니스 이벤트               | 로그/트레이스/메트릭(OTLP), stdout/stderr  | 로깅 레벨, appender/SDK 설정 일관성          |
| journald                          | 호스트 실시간 로그 보관/조회  | stdout/stderr                 | `journalctl` 조회 결과                     | 운영자 즉시 대응 경로 유지                   |
| OTel Collector (Central)          | 수집/변환/라우팅, 다중 Export | OTLP logs/traces/metrics      | OpenSearch, File Store, APM Collector 전달 | queue/retry/backpressure, drop rate 모니터링 |
| OTel Collector (APM)              | APM 전용 전달 최적화          | 중앙 Collector 전달 데이터    | Data Prepper 전달                          | export timeout/retry, 파이프라인 지연 감시   |
| Data Prepper                      | APM 데이터 수신/전처리/적재   | APM Collector export 데이터   | OpenSearch APM 인덱스                      | pipeline delay, buffer 사용량, drop 감시     |
| OpenSearch                        | 로그/APM 인덱싱/검색/집계     | Collector/Data Prepper 데이터 | 검색 인덱스/집계 결과                      | 인덱싱 실패율, 지연, ILM/보존 정책           |
| OpenSearch Dashboards             | 로그 시각화/탐색 UI           | OpenSearch REST 응답          | 대시보드/검색 결과                         | 접근 제어, 쿼리 성능                         |
| File Store (Local FS/NFS)         | 장기 보관/백업                | Collector export 로그         | 원본 보관 데이터                           | 보존 기간, 용량/회전 정책                    |
