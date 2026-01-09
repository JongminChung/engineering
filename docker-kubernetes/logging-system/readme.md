# Logging System

## docker

- [docker compose](docker-compose.yml)
- configuration
  - [fluent bit config](fluent-bit/fluent-bit.conf)
  - [parser config](fluent-bit/parsers.conf)

### 체크 포인트

- 컨테이너 로그가 /var/lib/docker/containers에 실제로 생성되는지 확인 필요함
  - Docker의 log-driver가 json-file이어야 이 경로에 쌓임(대부분 기본값이긴 함)

- 외부 ES 단절 대비는 위 설정에서 두 축으로 처리함
  - 입력 위치 유지: DB /fluent-bit/state/tail-containers.db
  - 출력 실패 backlog: storage.type filesystem + storage.path
      /fluent-bit/state/storage

- 디스크 무한 적재 방지(필수)
  - 현재 예시는 기본 상한만 둔 상태라, 장애가 길면 디스크 압박 가능함
  - 운영에서는 /fluent-bit/state를 별도 디스크/파티션으로 두거나, 상한 정책을 더 강하게 잡는 게 안전함

--

## 비교1. Fluent Bit vs Fluentd log driver

**fluentd log driver 방식**

```text
App Container
  stdout/stderr
      ↓
Docker Engine
  (fluentd log driver)
      ↓  TCP/UDP
Fluentd (forward input)
      ↓
Elasticsearch
```

**fluentbit agent(tail) 방식**

```text
App Container
  stdout/stderr
      ↓
Docker json-file
  (/var/lib/docker/containers/*.log)
      ↓
Fluent Bit (tail)
      ↓
Elasticsearch
```

**핵심 차이**

- fluentd log driver: 실시간 스트리밍 / 네트워크 의존
- fluentbit: 파일 기반 / 느슨한 결합

**서비스 안정성**

| 항목         | fluentd log driver | fluentbit                 |
|------------|--------------------|---------------------------|
| fluentd 다운 | 컨테이너 로깅 경로 영향 가능   | 영향 없음(파일은 계속 쌓임)          |
| ES 단절      | fluentd buffer에 의존 | fluentbit disk buffer로 흡수 |
| 장애 전파      | 높음                 | 낮음                        |

fluentd log driver는 mode=non-blocking + fluentd-async-connect=true를 반드시 써야 함
**→ “안전하게 쓰려면 옵션으로 위험을 눌러야 하는 구조”**

fluentbit은
**→ 구조 자체가 서비스와 분리됨**

**Docker → Fluentd 구간 특성**

- Docker 엔진은
  - 컨테이너 stdout/stderr를
  - **실시간으로 fluentd에 스트리밍 전송함**
- 이 구간에는
  - ES처럼 “넉넉한 디스크 buffer”가 없음
  - Docker 엔진 내부 버퍼 + 옵션에 의존함

즉,

- Fluentd가 느리거나 다운되면
  - Docker 엔진이 로그를 못 넘김
  - 그 영향이 컨테이너 실행 경로까지 전파될 수 있음

그래서 옵션이 필요해짐.

**mode=non-blocking이 의미하는 바**

**blocking 모드(default 성격)**

- Docker가 로그를 fluentd에 넘길 때
- 수신이 안 되면 **대기(block)**
- stdout/stderr 쓰는 앱 스레드가 막힐 수 있음
  → 최악: 서비스 지연/멈춤

**non-blocking 모드**

- Docker 엔진이 로그 전송을 내부 큐로 넘기고 바로 리턴
- 큐가 꽉 차면?
  - 로그를 드롭
- 대신 앱 실행 경로는 안전

즉,

- 안정성 ↔ 로그 유실
- 이 트레이드오프를 옵션으로 강제 선택해야 하는 구조임

**fluentd-async-connect=true의 역할**

- fluentd가 아직 안 떠 있거나 재시작 중이어도
- 컨테이너 기동을 막지 않음
- 연결 실패 로그만 남기고 백그라운드에서 재시도

```bash
docker run --rm \
  --log-driver=fluentd \
  --log-opt fluentd-address=127.0.0.1:24224 \
  busybox \
  sh -c 'i=0; while true; do echo "log-$i"; i=$((i+1)); sleep 1; done'
```

```text
# 컨테이너가 바로 다운됨.
docker: Error response from daemon: failed to create task for container: failed to initialize logging driver: dial tcp 127.0.0.1:24224: connect: connection refused
```

**Docker 엔진은 Fluentd를 신뢰해야만 하는 구조라서 옵션으로 안전을 확보해야 함**
