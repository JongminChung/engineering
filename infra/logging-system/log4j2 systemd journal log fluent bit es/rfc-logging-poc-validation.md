# RFC 첨부 D: PoC 검증 절차

## PoC 범위 (최소화)

- 불확실성이 있는 지점만 검증한다.
- 안 1, 3은 journald appender/수집 경로를 확인한다.
- 안 2는 기존 패턴과 동일하므로 PoC는 선택 사항으로 본다.

## 공통 전제

- 실행 전 `docker-compose` 버전 및 Docker 데몬 상태 확인.
- ES 검증이 필요한 경우에만 ES 컨테이너를 실행한다.

## 안 1) journald -> fluent-bit -> Proxy(파일 + ES)

### 최소 실행

- `docker-compose -f docker-compose.option1.min.yml up --build`

### 검증 절차 (최소)

- journald appender 기록 확인
    - `docker exec -it <log4j2-app 컨테이너> journalctl -u log4j2-app.service --no-pager | tail -n 5`
- proxy 파일 저장 확인
    - `poc/data/proxy` 경로에 `proxy-log*` 파일 생성 확인

### 추가 검증 (선택)

- ES 연동 확인
    - `docker-compose -f docker-compose.option1.yml up --build`
    - `curl http://localhost:9200/_cat/indices?v` 결과에 `log4j2-poc` 존재 확인
- ES 장애 시 파일 유실 여부
    - ES 컨테이너 중지 후에도 `poc/data/proxy` 파일이 계속 생성되는지 확인

### 실패 시 확인 항목

- `log4j2-app` 컨테이너 systemd 기동 여부
- `fluent-bit-edge`가 `/var/log/journal`을 읽는지 확인
- `proxy`에서 파일 생성 여부 및 ES output 에러 확인

## 안 2) app file -> filebeat -> ES (+ 중앙 스토리지)

### 실행 (선택)

- `docker-compose -f docker-compose.option2.yml up --build`

### 검증 절차 (선택)

- 파일 생성 확인
    - `app-logs` 볼륨에 `app.log` 생성 여부 확인
- 중앙 스토리지 복사 확인
    - `central-storage` 볼륨에 파일 복사 여부 확인
- ES 적재 확인
    - `curl http://localhost:9200/_cat/indices?v` 결과에 `log4j2-poc` 존재 확인

### 실패 시 확인 항목

- filebeat 컨테이너 로그에 harvest/registry 관련 에러 여부 확인
- `central-sync` 컨테이너에서 복사 오류 여부 확인

## 안 3) journald -> fluent-bit -> ES + journald 파일 보관

### 최소 실행

- `docker-compose -f docker-compose.option3.min.yml up --build`

### 검증 절차 (최소)

- 중앙 보관 파일 확인
    - `central-storage` 볼륨에 `journal-export.log` 생성 여부 확인

### 추가 검증 (선택)

- ES 연동 확인
    - `docker-compose -f docker-compose.option3.yml up --build`
    - `curl http://localhost:9200/_cat/indices?v` 결과에 `log4j2-poc` 존재 확인

### 실패 시 확인 항목

- `journal-exporter` 컨테이너의 `journalctl` 실행 오류 여부 확인
- journald 로그 보존 경로(`/var/log/journal`) 마운트 여부 확인
