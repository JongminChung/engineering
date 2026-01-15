# Filebeat ↔ Logstash half-open TCP / stalled connection 재연 가이드

목적
• Filebeat가 Logstash와 long-lived TCP connection을 유지하는 상황에서
• 중간 장비(NAT / LB / FW)의 idle-timeout에 의해 FIN/RST 없이 세션이 삭제되는 경우
• Filebeat가 ESTABLISHED 상태를 유지하면서 ACK 없이 stall 상태에 빠지는 현상을
로컬 환경에서 재연하는 것이 목적임

본 문서는 다음을 목표로 함
• Filebeat 재시작 전까지 복구되지 않는 상태 재연
• stalled connection 감지 / metric / reconnect 로직 검증

1. 테스트 환경 구성 개요

구성도

Filebeat -> (중간 장비 시뮬레이터) -> Logstash
• 중간 장비 역할
• idle-timeout 이후 silent drop (FIN/RST 없음) 유도
• 구현 방식
• iptables (권장)
• tc(netem)
• socat / custom proxy (선택)

⸻

1. 사전 요구 사항
   • Linux (Ubuntu 20.04 이상 권장)
   • Docker
   • Docker Compose
   • root 권한 (iptables / tc 조작용)
   • Filebeat, Logstash 동일 호스트 실행 권장

⸻

1. Docker Compose 구성

파일명: docker-compose.yml

```yaml
services:
    logstash:
        image: docker.elastic.co/logstash/logstash:8.12.0
        container_name: logstash
        volumes:
            - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf

    router:
        build: ./router
        container_name: logstash-router
        cap_add:
            - NET_ADMIN
        sysctls:
            net.ipv4.ip_forward: "1"
        ports:
            - "5044:5044"
        depends_on:
            - logstash

    filebeat:
        image: docker.elastic.co/beats/filebeat:8.12.0
        container_name: filebeat
        user: root
        ports:
            - "5066:5066"
        volumes:
            - ./filebeat.yml:/usr/share/filebeat/filebeat.yml
            - ./logs:/logs
        depends_on:
            - router
```

1. Logstash 설정

파일명: logstash.conf

```conf
input {
  beats {
    port => 5044
  }
}

output {
  stdout {
    codec => rubydebug
  }
}
```

1. Filebeat 설정 (stalled 상황 유도용)

파일명: filebeat.yml

```yaml
filebeat.inputs:
 - type: log
 paths:
  - /logs/*.log

output.logstash:
 hosts: [ "router:5044" ]
 pipelining: 2
 ttl: 0
 timeout: 0s

logging.level: info
logging.selectors: [ "output" ]

queue.mem:
 events: 4096
 flush.min_events: 1
 flush.timeout: 1s

http.enabled: true
http.host: 0.0.0.0
http.port: 5066
```

1. 로그 생성기

테스트 로그 생성

```bash
mkdir -p logs

while true; do
  echo "$(date) test log" >> logs/test.log
  sleep 1
done
```

1. idle-timeout / silent drop 재연

방법 A: iptables 기반 silent drop (권장)

1. 정상 동작 확인

```bash
docker-compose up
```

- Logstash stdout에 이벤트가 지속적으로 출력되는지 확인

1. TCP 연결 상태 확인

ss -tanp | grep 5044

Filebeat -> Logstash 연결이 ESTABLISHED 상태인지 확인

1. 중간에서 패킷 drop 시작

```bash
iptables -A OUTPUT -p tcp --dport 5044 -j DROP
```

중요 사항
• FIN / RST 전송 없음
• TCP 세션은 커널 기준 ESTABLISHED 유지
• ACK 미수신 상태 유도

1. 관찰 포인트
   Filebeat
   • 명시적 network error 로그 없음
   • backoff / timeout 로그 반복 가능
   • 이벤트 전송 중단

Logstash
• stdout 이벤트 수신 중단

1. queue 증가 확인
   • Filebeat 내부 queue 지속 증가
   • acked_events 증가 없음

방법 B: tc(netem) 기반 blackhole (선택)

```bash
tc qdisc add dev eth0 root netem loss 100%
```

• 패킷 100% 유실
• ACK 미수신 상태 유도

방법 C: 일시 단절 후 자동 복구 실패 시나리오 (권장)

목적
• 네트워크 장비 패치/재시작처럼 짧은 기간 통신이 끊겼다가 복구되는 상황 재현
• 복구 이후에도 Filebeat가 자동으로 재연결하지 않는 상태 입증

1. 정상 동작 확인

```bash
docker-compose up
```

1. NAT 기반 중간장비(라우터)에서 일시 드롭 추가 (20초)

```bash
docker exec logstash-router iptables -I FORWARD -p tcp --dport 5044 -j DROP
docker exec logstash-router iptables -I FORWARD -p tcp --sport 5044 -j DROP
sleep 20
docker exec logstash-router iptables -D FORWARD -p tcp --dport 5044 -j DROP
docker exec logstash-router iptables -D FORWARD -p tcp --sport 5044 -j DROP
```

중요 사항
• FIN / RST 전송 없음
• 짧은 기간 동안 패킷만 드롭되어 half-open 상태 유도

1. 복구 이후에도 기존 세션만 blackhole 유지

```bash
# 세션 상태를 제거해 middlebox가 idle-timeout으로 상태를 잊은 상황을 모사
docker exec logstash-router conntrack -D -p tcp --dport 5044 || true
docker exec logstash-router conntrack -D -p tcp --sport 5044 || true

# 기존 세션 패킷(NEW, non-SYN)을 drop하여 half-open 고착 유도
docker exec logstash-router iptables -I FORWARD -p tcp --dport 5044 -m conntrack --ctstate NEW -m tcp ! --syn -j DROP
docker exec logstash-router iptables -I FORWARD -p tcp --sport 5044 -m conntrack --ctstate NEW -m tcp ! --syn -j DROP
```

1. 복구 이후 관찰 포인트
   Filebeat
   • 이벤트 전송이 재개되지 않음
   • acked_events 증가 없음
   • queue depth 증가 지속

Logstash
• stdout 이벤트 수신 재개되지 않음

참고
• router 컨테이너는 NAT 기반으로 동작하여 TCP 종단이 아님
• 실제 네트워크 장비 패치/재부팅으로 인한 단절과 유사한 조건 재현 가능

⸻

1. 실험 로그 저장 위치

• 모든 실험 로그/metrics는 `evidence/` 아래에 타임스탬프 기반 파일로 저장
• 예시 파일명
• `halfopen-nat-YYYYMMDD-HHMMSS-*.log`
• `halfopen-nat-YYYYMMDD-HHMMSS-metrics-*.json`
• `halfopen-nat-YYYYMMDD-HHMMSS-drop-window.txt`

실험 근거로 활용하는 주요 파일
• Filebeat 로그: `evidence/*filebeat-*.log`
• Logstash 로그: `evidence/*logstash-*.log`
• Metrics 스냅샷: `evidence/*metrics-*.json`
• 단절 타임라인: `evidence/*drop-window.txt`

⸻

1. Linux 호스트/VM에서 재현 (iptables/nft)

목적
• Docker 내부가 아니라 호스트/VM 레벨에서 중간장비 idle-timeout 상황을 더 현실적으로 재현
• 일시 단절 → conntrack 상태 소거 → old-flow 패킷 drop으로 half-open 고착 유도

사전 조건
• Linux 호스트/VM
• Docker Compose 실행 중 (Filebeat/Logstash 구성은 그대로 사용)
• root 권한 필요

iptables 버전 (권장)

1. 일시 단절 (20초)

```bash
iptables -I DOCKER-USER -p tcp --dport 5044 -j DROP
sleep 20
iptables -D DOCKER-USER -p tcp --dport 5044 -j DROP
```

1. conntrack 상태 소거

```bash
conntrack -D -p tcp --dport 5044 || true
conntrack -D -p tcp --sport 5044 || true
```

1. old-flow 패킷 drop (half-open 고착)

```bash
iptables -I DOCKER-USER -p tcp --dport 5044 -m conntrack --ctstate NEW -m tcp ! --syn -j DROP
iptables -I DOCKER-USER -p tcp --sport 5044 -m conntrack --ctstate NEW -m tcp ! --syn -j DROP
```

1. 관찰 포인트
   • Filebeat acked 증가 멈춤, queue depth 증가
   • Logstash stdout 수신 중단
   • TCP 상태는 ESTABLISHED 유지 가능

2. 정리

```bash
iptables -D DOCKER-USER -p tcp --dport 5044 -m conntrack --ctstate NEW -m tcp ! --syn -j DROP
iptables -D DOCKER-USER -p tcp --sport 5044 -m conntrack --ctstate NEW -m tcp ! --syn -j DROP
```

nftables 버전

1. 테이블/체인 준비

```bash
nft add table inet filter
nft add chain inet filter forward { type filter hook forward priority 0 \; }
```

1. 일시 단절 (20초)

```bash
nft add rule inet filter forward tcp dport 5044 drop
sleep 20
nft delete rule inet filter forward tcp dport 5044 drop
```

1. conntrack 상태 소거

```bash
conntrack -D -p tcp --dport 5044 || true
conntrack -D -p tcp --sport 5044 || true
```

1. old-flow 패킷 drop (half-open 고착)

```bash
nft add rule inet filter forward tcp dport 5044 ct state new tcp flags != syn drop
nft add rule inet filter forward tcp sport 5044 ct state new tcp flags != syn drop
```

1. 정리

```bash
nft delete rule inet filter forward tcp dport 5044 ct state new tcp flags != syn drop
nft delete rule inet filter forward tcp sport 5044 ct state new tcp flags != syn drop
```

⸻

1. 실패 입증을 위한 증거 수집 체크리스트

목표
• 단절 시점 전/중/후의 상태를 빠르게 비교할 수 있도록 최소 증거 세트 확보
• “일시 단절 이후 자동 복구 실패”를 문서로 남길 수 있도록 스냅샷 수집

권장 증거 항목 (최소)
• Filebeat 로그: output 전송 중단/재시도 흔적
• Logstash stdout: 이벤트 수신 중단 시점
• TCP 상태: `ss -tanp | grep 5044` 출력 스냅샷
• Filebeat metrics: acked_events / queue depth 변화

선택 증거 항목 (있으면 좋음)
• Filebeat 내부 큐 증가 추이 (metrics 스냅샷 주기적으로 저장)
• 단절 시점/복구 시점 타임라인

증거 수집 방식 예시

1. Filebeat/Logstash 로그 저장을 위한 호스트 볼륨 마운트

docker-compose.yml에 다음을 추가해 컨테이너 로그를 호스트로 수집

```yaml
services:
    logstash:
        volumes:
            - ./evidence/logstash:/usr/share/logstash/logs
    filebeat:
        volumes:
            - ./evidence/filebeat:/usr/share/filebeat/logs
```

1. 단절 시점 기록

```bash
date
iptables -I DOCKER-USER -p tcp --dport 5044 -j DROP
sleep 20
iptables -D DOCKER-USER -p tcp --dport 5044 -j DROP
date
```

1. TCP 상태 스냅샷

```bash
ss -tanp | grep 5044
```

1. Logstash stdout 확인

```bash
docker-compose logs -f logstash
```

1. Filebeat 로그 확인

```bash
docker-compose logs -f filebeat
```

1. 재연 성공 판단 기준

성공 조건
• Filebeat TCP 상태
• ESTABLISHED 유지
• Filebeat 내부 상태
• acked_events 증가 없음
• queue depth 지속 증가
• Filebeat 재시작 전까지 자동 복구되지 않음

실패 조건
• Filebeat가 스스로 reconnect 성공
• FIN / RST 발생으로 명시적 에러 처리됨

1. stalled connection 개선안 검증 포인트

stalled 판단 기준 예시
• 최근 N분 동안
• acked_events == 0
• queue depth 증가
• output client state: connected

기대 동작

1. detection-only 모드
   • WARN 로그 출력
   • metric 노출

```text
libbeat.output.stalled = 1
```

1. opt-in reconnect 모드
   • stalled 조건 충족 시
   • 해당 output client socket close
   • 다음 flush 시 신규 TCP 연결 생성
   • Logstash 이벤트 수신 재개

1) 환경 초기화

```bash
iptables -F
tc qdisc del dev eth0 root || true
docker-compose down
```

1. 참고 사항
   • 본 재연은 LB / NAT / FW idle-timeout 환경을 로컬에서 모사하기 위함임
   • 실제 클라우드 환경(AWS ELB/NLB, GCP LB, 사내 FW)에서 동일 패턴 다수 보고됨
   • 본 문서 기반으로 다음 작업 가능
   • libbeat stalled detection 로직 구현
   • metric 추가
   • opt-in reconnect 옵션 검증
