## 환경 구성 개요

- dns1: dns1 컨테이너를 '존재하지 않는 도메인 (NXDOMAIN)' 응답을 반환하는 DNS 서버로 설정
- dns2: 두 번째 DNS 서버, dnsmasq 기반으로 정상동작하는 DNS 서버
- client: busybox 컨테이너, 두 DNS 서버 IP를 DNS 서버로 지정하고 nslookup 실행해 테스트

## 실행 및 테스트 방법

```bash
# 1. 빌드 및 컨테이너 실행
docker-compose up --build -d

# 2. client 접속
docker exec -it client sh

# 3. DNS 테스트 예

nslookup google.com
# 결과: dns1 (172.18.0.2)에서 NXDOMAIN 응답 → 클라이언트는 두 번째 DNS서버로 넘어가지 않고 NXDOMAIN 결과 출력

nslookup nonexist.testdomain
# 결과: 역시 DNS1에서 NXDOMAIN 응답

# 4. 만약 dns1이 무응답인 상황 테스트 원한다면 dns1 서비스 중지 후 nslookup 다시 실행 시 dns2가 응답함
```

### 결과

- 모든 도메인에 빈 존(zone ".")으로 NXDOMAIN 응답 하도록 설정
- 두 번째 DNS서버는 dnsmasq로 정상 포워딩 DNS 역할 수행
- DNS 클라이언트는 NXDOMAIN 응답받으면 다른 DNS 서버로 넘어가지 않습니다.
- 실제 failover 테스트를 원하시면 dns1을 중지하거나 무응답 상태로 만들면 두 번째 DNS서버로 넘어갑니다.

**왜 두 번째 DNS 서버(backup)에 쿼리가 안 넘어가는가?**

- DNS 클라이언트는 NXDOMAIN (없는 도메인) 응답을 최종 응답으로 취급합니다.
- 첫 네임서버가 명확한 NXDOMAIN을 반환하면 두 번째 네임서버는 호출되지 않습니다.
- 반면 첫 서버가 응답하지 않거나 ERROR(예: SERVFAIL)를 리턴하면 클라이언트는 다음 서버로 요청을 보내 failover 합니다.

## 유의할 점

busybox:stable(Alpine 기반)는 docker-compose의 dns 옵션을 무시하고, 내부적으로 127.0.0.11(docker embedded DNS)을 계속 사용합니다.
command로 /etc/resolv.conf를 덮어써도, busybox/alpine의 init 프로세스가 컨테이너 시작 시점에 다시 127.0.0.11로 덮어씁니다.
