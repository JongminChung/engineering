# RFC: Logging system (log4j2 + journald + fluent-bit + ES)

## 배경

- [ECS Log](https://www.elastic.co/docs/reference/ecs/ecs-log)
 	- log4j2 format: json
- 기존 환경
 	- rsyslog -> 로그 중앙화(파일로 관리) -> (filebeat) -> ES

## 목표

- 감사로그 수준으로 파일을 중앙화 저장해야 함.
- ES 전송 실패 시에도 파일 유실을 최소화해야 함.
- 가능한 기존 운영 방식(파일 기반 수집/보관)과의 차이를 최소화함.

## 비목표

- 전체 애플리케이션 로깅 구조의 전면 교체.
- ES 스키마/인덱스 전략의 변경.

## 첨부 문서

- [구성도/플로우 다이어그램](rfc-logging-flow-diagrams.md)
- [운영 체크리스트](rfc-logging-ops-checklist.md)
- [운영 절차/복구 플레이북](rfc-logging-ops-playbook.md)
- [PoC docker-compose](docker-compose.yml)
- [PoC 검증 절차](rfc-logging-poc-validation.md)

## 제안안 (3안)

### 안 1) log4j2 -> journald -> fluent-bit -> Proxy(파일 저장 + ES 전송) -> ES

**개요**

- journald에 수집된 로그를 fluent-bit로 읽어 Proxy에 파일로 저장하면서 동시에 ES로 전송한다.

**장점**

- 파일 기반 감사로그 요구사항을 가장 직접적으로 충족.
- ES 장애 시에도 Proxy에 파일이 남아 유실 위험이 낮다.

**단점/리스크**

- Proxy 운영(스토리지/로테이션/백업) 비용이 증가.
- 구성 요소가 늘어 장애 지점이 증가.

**운영 영향**

- journald 보존 정책 + fluent-bit 파서/필터 + Proxy 파일 정책을 함께 관리해야 함.

### 안 2) log4j2 -> 파일(app log) -> filebeat -> ES (+ 파일 중앙 스토리지)

**개요**

- log4j2가 직접 파일을 생성하고 filebeat로 ES 전송.
- 파일은 중앙 스토리지(NAS/S3 등)로 별도 중앙화한다.

**장점**

- 기존 rsyslog/filebeat 방식과 유사해 변화 최소화.
- 감사로그 파일을 직접 보관/중앙화 가능.

**단점/리스크**

- 애플리케이션 파일 로테이션 정책 관리 필요.
- journald 기반 표준화/관측성의 이점을 활용하지 못함.

**운영 영향**

- 파일 수집 경로(로컬 -> 중앙 스토리지)와 보관 정책을 별도로 관리.

### 안 3) log4j2 -> journald -> fluent-bit -> ES + journald 파일 보관(중앙화)

**개요**

- ES 전송은 journald -> fluent-bit 경로로 수행.
- journald 데이터를 별도 파일로 내보내 중앙 스토리지에 보관.

**장점**

- 파이프라인이 비교적 단순.
- journald 기반 표준화 유지 가능.

**단점/리스크**

- journald 보존/로테이션/forwarding 정책을 잘못 설정하면 유실 위험이 커짐.
- 중앙 파일 보관을 위한 추가 작업(Export/Forward)이 필요.

**운영 영향**

- journald 내보내기/보관 자동화가 핵심 운영 포인트.

## 비교 요약

- 감사로그 파일 보존 안정성: 안 1 > 안 2 > 안 3
- 운영 단순성: 안 3 > 안 2 > 안 1
- 기존 환경 변화 최소: 안 2 > 안 1 > 안 3
- 장애 내성(ES 장애 시): 안 1 > 안 2 > 안 3

## 결정 기준 (초안)

- 파일 보존이 절대적이면 안 1을 우선 검토.
- 변화 최소/운영 단순을 우선하면 안 2.
- journald 기반 표준화를 유지하면서 단순화하고 싶다면 안 3.

## 결정 필요 사항

- 파일 중앙화 대상 스토리지(NAS/S3/로컬 디스크).
- 감사로그 보관 기간/용량 기준.
- ES 장애 허용 범위(전송 지연 허용 시간, 재전송 요구).

## PoC 실행 가이드 (안별)

### 안 1) journald -> fluent-bit -> Proxy(파일 + ES)

- 최소 범위 실행: `docker-compose -f docker-compose.option1.min.yml up --build`
- 전체 실행: `docker-compose -f docker-compose.option1.yml up --build`
- 파일 확인: `poc/data/proxy`에 JSON 로그 파일 생성 여부 확인
- ES 확인: `curl http://localhost:9200/_cat/indices?v`에서 `log4j2-poc` 생성 여부 확인

### 안 2) app file -> filebeat -> ES (+ 중앙 스토리지)

- 실행: `docker-compose -f docker-compose.option2.yml up --build`
- 파일 확인: `app-logs` 볼륨에 로그 파일 생성 여부 확인
- 중앙 스토리지 확인: `central-storage` 볼륨에 복사 여부 확인
- ES 확인: `curl http://localhost:9200/_cat/indices?v`에서 `log4j2-poc` 생성 여부 확인

### 안 3) journald -> fluent-bit -> ES + journald 파일 보관

- 최소 범위 실행: `docker-compose -f docker-compose.option3.min.yml up --build`
- 전체 실행: `docker-compose -f docker-compose.option3.yml up --build`
- 중앙 스토리지 확인: `central-storage` 볼륨의 `journal-export.log` 생성 여부 확인
- ES 확인: `curl http://localhost:9200/_cat/indices?v`에서 `log4j2-poc` 생성 여부 확인
