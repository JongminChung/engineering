# 성능 테스트 PRD

k6 부하 테스트와 Prometheus/Grafana 관측을 결합해 API/웹/DB 성능을 같은 타임라인에서 분석한다.

## 아키텍처 요약

### 핵심 구성

- 테스트: [k6-operator](https://github.com/grafana/k6-operator)
- 수집/저장: Prometheus
- 시각화: Grafana
- 인프라: Kubernetes

### 흐름

1. k6가 API/웹 요청을 발생
2. Prometheus가 시스템/DB 메트릭 수집
3. Grafana에서 k6 결과와 메트릭을 함께 분석

### k6/관측

- Grafana 대시보드: 기본 제공 템플릿 사용(특정 ID 없음)
- 메타데이터: 기존 PostgreSQL 저장

## 인프라/네트워크

### DB 구성

- 현재: PostgreSQL 단일 인스턴스
- 데이터 경로: `PGDATA=/var/lib/postgresql/data/pgdata` (PVC 루트 `lost+found` 충돌 회피)
- 향후: CloudNativePG로 확장

### 외부 접속

- `grafana.jamie.kr` → nginx-service:443 → Grafana (추후 ADMIN 페이지 일부로 귀속하고 접근 안되게 구성)
- `postgres.jamie.kr` → nginx-service:5432 → PostgreSQL
- OpenStack LB는 L4 전달, Host 기반 분기는 Nginx에서 처리
- Ingress Controller 미사용

### 시크릿 관리

- 실제 값은 Kubernetes Secret에만 저장
- 확인 방법: `./credentials.md`

## 운영 가이드

### 적용 로그(요약)

- `namespaces.yml`, `postgres.yml`, `postgres_exporter.yml`, `monitoring.yml`, `nginx_service.yml` 적용
- Nginx Service annotation 타입 오류 → 문자열로 수정

## 로드맵

### MVP 실행 단계

#### 1. k6-operator 설치(Helm)

- monitoring 네임스페이스에 Helm 설치
- CRD 설치 확인 및 기본 values 정리

#### 2. DB 스키마 정의

- 테이블: perf_tests
    - id, name, description, script_path, created_at, updated_at
- 마이그레이션 방식 결정(앱 초기화 시 SQL 실행 or 별도 마이그레이션 도구)

#### 3. 스크립트 저장 PVC 설계

- PVC 생성 + 앱 서버에 마운트
- 업로드 시 /scripts/<test-name>/script.js로 저장

### 확장 기능

#### 4. 편의성 레이어 구축

##### 4.1 테스트 시나리오 템플릿 라이브러리

- **목적**: 자주 쓰는 패턴을 재사용 가능하게 템플릿화
- **구현**:
    - DB 테이블: `test_templates`
        - id, name, category, description, script_template, default_config(JSON)
    - 템플릿 카테고리: API 부하, WebSocket, gRPC, DB 쿼리, 복합 시나리오
    - 변수 치환 방식: `${TARGET_URL}`, `${VUS}`, `${DURATION}` 등
- **결과물**: 템플릿 선택 → 파라미터 입력 → 즉시 실행 가능

##### 4.2 웹 기반 관리 UI

- **기능**:
    - 테스트 생성/실행/중지/삭제
    - 실시간 진행 상황 모니터링 (k6 Pod 상태)
    - 테스트 이력 조회 및 비교
    - 템플릿 관리 인터페이스
- **구현 방식**:
    - k6-operator CRD를 직접 호출해 실행/중지 트리거
    - 실행 요청과 결과 메타데이터를 DB에 저장
- **API 경로**:
    - `GET /api/test-templates` / `POST /api/test-templates`
    - `GET /api/test-templates/:templateId` / `PUT /api/test-templates/:templateId` / `DELETE /api/test-templates/:templateId`
    - `GET /api/perf-tests` / `POST /api/perf-tests`
    - `GET /api/perf-tests/:testId` / `PUT /api/perf-tests/:testId` / `DELETE /api/perf-tests/:testId`
    - `GET /api/perf-tests/:testId/runs` / `POST /api/perf-tests/:testId/runs`
    - `GET /api/test-runs/:runId` / `DELETE /api/test-runs/:runId`
- **기술 스택**:
    - Fullstack: pnpm + TanStack start
    - WebSocket: 실시간 로그 스트리밍
- **배포**: `perftest.jamie.kr` → nginx-service → UI 서비스

##### 4.3 결과 분석 대시보드

- **Grafana 확장**:
    - 커스텀 대시보드: k6 메트릭 + 시스템 메트릭 통합 뷰
    - 테스트별 자동 annotation (시작/종료 시점 표시)
    - 히스토리 비교: 이전 실행 vs 현재 실행
- **DB 저장**:
    - 테이블: `test_results`
        - test_id, run_id, start_time, end_time, summary (JSON), status
    - 트렌드 분석용 요약 통계 저장

## 참고

- https://tech.kakaopay.com/post/perftest_zone/
- https://grafana.com/docs/k6/latest/
- https://cloudnative-pg.io/docs/1.28/

### 고도화 관련 참고

- https://grafana.com/docs/k6/latest/using-k6-browser/
- https://openai.com/index/scaling-postgresql/
