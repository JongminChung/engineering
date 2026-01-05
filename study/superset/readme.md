# Apache Superset PoC/학습 정리

## 목표

- Superset의 핵심 기능(데이터 연결, 차트/대시보드, 권한)을 직접 체험
- 우리 팀/프로덕트에 적용 가능한지 빠르게 판단할 수 있는 PoC 시나리오 정의

## 범위

- 오픈소스 Superset을 로컬에서 실행해 기본 흐름을 확인
- 데이터 소스 연결, 차트 구성, 대시보드 공유, 권한/역할 테스트
- 운영 배포 전 검토 포인트(보안, 성능, 거버넌스) 체크

## 로컬 PoC 빠른 시작(공개 데이터 포함)

- 방식: Superset + 데이터 소스(Postgres) + 로더를 Docker Compose로 동시 기동
- 이유: 공개 데이터셋을 자동 적재해 바로 시각화 PoC 가능

### 1) 환경 준비

- Docker / Docker Compose 설치

### 2) 실행(데이터 소스까지 자동 기동)

```bash
cd study/superset
docker compose up -d superset_db redis dataset_db dataset_loader superset-init
docker compose up -d superset
```

- 접속: <http://localhost:8088>
- 기본 계정: admin / admin (필요 시 `study/superset/docker-compose.yml`에서 변경)
- 첫 기동 시 공개 데이터 다운로드로 시간이 소요될 수 있음

### 3) 데이터 소스 연결

- Superset > Settings > Database Connections > + Database
- SQLAlchemy URI:
  - `postgresql+psycopg2://analytics:analytics@dataset_db:5432/analytics`
- 테이블: `stocks`

### 4) 확인 포인트

- 데이터 소스 연결: 연결 문자열, 계정 권한 범위
- 차트 생성: 시각화 타입/필터/집계 성능
- 대시보드: 필터 상호작용, 공유 링크, 퍼머링크
- 권한: 역할 분리(읽기/작성/관리), 데이터셋 접근 제한

## 선정 PoC 유즈케이스: 주가 기반 성과 대시보드

- 데이터: 공개 주가 시계열 데이터셋(여러 기업)
- 목표: 기간별 성과(수익률)와 변동성 비교로 비즈니스 성과를 수치화
- 기대 확인: 필터, 드릴다운, 대시보드 공유, 성능 체감

## 직접 만들어보는 과제(Hands-on)

1. `stocks` 데이터셋 연결 확인
2. 종목별 가격 추이 라인 차트(`date`, `price`, `symbol`)
3. 기간별 수익률(계산 컬럼/지표)
4. 종목별 변동성(표준편차) 바 차트
5. 대시보드 1개 구성 후 공유 링크 확인

## PoC 실습 절차(직접 구성)

### 1) 데이터베이스 연결

- Superset 접속: <http://localhost:8088> (admin/admin)
- Settings → Database Connections → + Database
- SQLAlchemy URI:
  - `postgresql+psycopg2://analytics:analytics@dataset_db:5432/analytics`
- Save

### 2) 데이터셋 등록

- Data → Datasets → + Dataset
- Database: `analytics`
- Schema: `public`
- Table: `stocks`
- Add

### 3) 차트 3~4개 구성

1. 종목별 가격 추이(라인)
   - Dataset: `stocks`
   - Chart type: Line Chart
   - Time column: `date`
   - Time grain: Day 또는 Month
   - Metric: `AVG(price)`
   - Series: `symbol`
2. 기간 수익률(%) 비교
   - Chart type: Bar Chart
   - Metric(예시 SQL): `(MAX(price) - MIN(price)) / NULLIF(MIN(price), 0) * 100`
   - Group by: `symbol`
3. 종목별 변동성
   - Chart type: Bar Chart
   - Metric: `STDDEV(price)`
   - Group by: `symbol`
4. 종목별 평균 가격 비교
   - Chart type: Bar Chart
   - Metric: `AVG(price)`
   - Group by: `symbol`

### 4) 대시보드 구성

- Dashboards → + Dashboard → Create
- Add Charts로 위 차트 추가
- 필터(선택): `symbol`, `date` 범위
- Save → Share

## 오픈소스 활용 가이드

- Superset은 시각화 레이어로 쓰고, 데이터 적재/정제는 별도 파이프라인에서 처리
- 데이터 소스는 최소 권한 계정으로 연결
- 운영 환경은 SSO, 감사 로그, 백업/복구 정책 검토
- 대규모 사용 시 메타데이터 DB, 캐시, 워커 확장 전략 고려

## PoC 체크리스트

- 기능: 연결/차트/대시보드/권한 흐름 완료
- 성능: 1~2초 내 기본 차트 렌더링 가능 여부
- 사용성: 비개발자가 차트 생성 가능한지
- 운영성: 알림/공유/접근 제어 가능 여부

## 다음 단계 제안

- 위 유즈케이스 중 1~2개를 선정해 실제 데이터로 PoC 진행
- 결과에 따라 도입 범위(팀/전사), 운영 구조, 비용 산정
