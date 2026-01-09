# 왜 filebeat를 사용하지 않았나

**_Table of Contents_**

<!-- TOC -->

- [왜 filebeat를 사용하지 않았나](#왜-filebeat를-사용하지-않았나)
  - [배경](#배경)
  - [“응답이 안 오면 왜 바로 끊지 않는가?”](#응답이-안-오면-왜-바로-끊지-않는가)
  - [결론 요약](#결론-요약)
  - [1. 흔한 오해](#1-흔한-오해)
  - [2. TCP가 연결 종료를 판단하는 경우 (딱 3가지)](#2-tcp가-연결-종료를-판단하는-경우-딱-3가지)
    - [1) FIN 수신](#1-fin-수신)
    - [2) RST 수신](#2-rst-수신)
    - [3) 커널 타임아웃 초과](#3-커널-타임아웃-초과)
  - [3. 중간 장비(LB/NAT/FW)의 실제 동작](#3-중간-장비lbnatfw의-실제-동작)
  - [4. half-open 세션이 만들어지는 과정 (타임라인)](#4-half-open-세션이-만들어지는-과정-타임라인)
    - [(1) 정상 상태](#1-정상-상태)
    - [(2) 중간 장비가 세션 삭제](#2-중간-장비가-세션-삭제)
    - [(3) Filebeat가 데이터 전송](#3-filebeat가-데이터-전송)
  - [5. ACK가 안 와도 왜 바로 끊지 않는가](#5-ack가-안-와도-왜-바로-끊지-않는가)
  - [6. 그래서 언제까지 살아있나?](#6-그래서-언제까지-살아있나)
  - [7. “서로 살아있다고 믿는다”는 표현의 의미](#7-서로-살아있다고-믿는다는-표현의-의미)
  - [8. 재시작하면 왜 즉시 해결되는가](#8-재시작하면-왜-즉시-해결되는가)
  - [9. 이 문제가 Filebeat에서 특히 체감되는 이유](#9-이-문제가-filebeat에서-특히-체감되는-이유)
  - [한 줄 요약](#한-줄-요약)

<!-- TOC -->

## 배경

HAProxy Session Data 수집 당시에 Filebeat와 Metricbeat를 활용해 데이터를 적재하는 시스템을 구축하였습니다.

데이터가 적재되지 않았던 이슈가 존재하였다.

- Processor(Logstash), DataStore(Elasticsearch)는 외부에 존재함. (중간 장비 존재함)
- <https://github.com/elastic/beats/issues/16335>
  - Filebeat는 long-lived connection을 기본으로 함
  - Filebeat는 전송 성공을 **ACK 수신**으로 판단함
  - LB/NAT가 idle timeout 이후 FIN/RST을 보내지 않고 세션만 제거하는 경우,
      Filebeat는 여전히 ESTABLISHED 상태로 간주함
      → 실제로 데이터가 안 가도 커널은 연결 살아있다고 판단함
- <https://github.com/elastic/beats/issues/661>

**Filebeat가 Logstash로 long-lived TCP 커넥션을 잡고 있고,**
로그 유입이 없으면 결국 그 연결은 idle 상태가 됨.
그 idle 기간이 LB/NAT/FW idle-timeout보다 길면,
중간 장비가 FIN/RST 없이 세션을 “조용히” 버릴 수 있고, 그게 half-open/정체로 이어질 수 있음.

**ERR_CONNECTION_CLOSED (FIN)**, **ERR_CONNECTION_RESET (RST)**

**중간 장비에서 세션을 닫아 버림.**

--

**Filebeat**

- 연결 + 세션 중심
- “이 연결에서 ACK를 받았는가?”
- ACK가 안 오면
  - 연결 유지
  - backoff
  - 같은 세션에 묶임

👉 **세션이 꼬이면 전체가 막힘**

**Fluent Bit**

- flush(전송 시도) 단위
- “이번 flush 시도는 성공했는가?”

- 실패하면
  - 이 chunk를 실패 처리
  - 다음 flush 주기에 다시 시도

👉 **연결에 집착하지 않음**

## “응답이 안 오면 왜 바로 끊지 않는가?”

## 결론 요약

**TCP는 “응답이 없다”는 이유만으로 연결을 끊지 않는다.**
연결이 종료되려면 반드시 **명시적인 신호(FIN/RST)** 또는 **아주 긴 커널 타임아웃**이 필요하다.

그래서

- TCP 상태는 `ESTABLISHED`
- 실제 데이터는 전달되지 않음 이라는 **half-open(blackhole) 세션**이 현실에서 자주 발생한다.

---

## 1. 흔한 오해

> “응답이 안 오면 TCP가 알아서 끊지 않나?”

❌ 아님

TCP는 다음을 **엄격히 구분**한다.

- **응답이 없음**
- **연결이 죽었음**

TCP 프로토콜 관점에서는
“응답이 없는 것”은 **아직 죽었다고 판단할 근거가 아님**이다.

---

## 2. TCP가 연결 종료를 판단하는 경우 (딱 3가지)

TCP는 아래 경우에만 연결이 종료된다.

### 1) FIN 수신

- 상대가 **정상 종료 의사**를 명시적으로 전달
- graceful close

### 2) RST 수신

- 상대가 **강제 종료 의사**를 명시적으로 전달
- abnormal close

### 3) 커널 타임아웃 초과

- 재전송 반복 실패
- keepalive probe 실패
- **기본값 기준 수 분 ~ 수 시간**
  - Linux TCP keepalive 기본값: **2시간**

👉 이 외의 경우에는 **연결 유지가 기본 정책**

---

## 3. 중간 장비(LB/NAT/FW)의 실제 동작

현실의 중간 장비는 다음과 같이 동작하는 경우가 매우 많다.

- idle timeout 초과
- 내부 세션 테이블에서 엔트리 삭제
- **FIN/RST 전송 안 함**
- 이후 패킷은 **그냥 DROP**

중요:

- 이는 **TCP 위반이 아님**
- 중간 장비는 TCP 종료를 중재할 의무가 없음

---

## 4. half-open 세션이 만들어지는 과정 (타임라인)

### (1) 정상 상태

```text
Filebeat <==== TCP ====> Logstash
```

### (2) 중간 장비가 세션 삭제

```text
Filebeat <==== TCP ====> LB/NAT (세션 삭제) X Logstash
```

- Filebeat 커널: 연결 살아있다고 판단 (`ESTABLISHED`)
- Logstash 커널: 연결 살아있다고 판단 (`ESTABLISHED`)
- 중간 장비: 이 세션을 더 이상 모름

👉 **양 끝은 서로 살아있다고 “믿는” 상태**

---

### (3) Filebeat가 데이터 전송

- Filebeat → `send()`
- 커널 send buffer에 정상적으로 적재
- 패킷은 중간 장비에서 DROP
- **RST 없음**
- **ICMP 없음**

👉 커널은 실패를 인지하지 못함

---

## 5. ACK가 안 와도 왜 바로 끊지 않는가

ACK가 안 오는 이유는 매우 다양할 수 있다.

- 상대 프로세스 busy
- GC pause
- 네트워크 지연
- 일시적 패킷 손실
- 혼잡 제어 상황

TCP는 신뢰성 프로토콜이기 때문에
이런 상황에서 **바로 연결을 끊지 않고 재전송을 선택**한다.

👉 “조금 기다린다”가 TCP의 기본 전략

---

## 6. 그래서 언제까지 살아있나?

- 재전송 한계 초과
- keepalive probe 실패
- 애플리케이션 레벨 timeout

문제:

- 이 시간은 **운영 체감 기준으로 너무 김**
- 그동안 애플리케이션은 멀쩡히 살아있음
- 데이터는 안 감

---

## 7. “서로 살아있다고 믿는다”는 표현의 의미

이건 감정적 표현이 아니라 **TCP 상태 머신 설명**이다.

- 양쪽 커널 상태: `ESTABLISHED`
- FIN/RST 미수신
- 타임아웃 미도달

👉 TCP 관점에서는 **정상 연결**

그래서
> “서로 살아있다고 믿는다”
> 라고 표현한다.

---

## 8. 재시작하면 왜 즉시 해결되는가

재시작은 다음을 의미한다.

- 기존 소켓 강제 `close()`
- 커널 TCP 상태 완전 초기화
- 새 3-way handshake 수행

👉 중간 장비에 **새 세션 생성**
👉 정상 경로로 통신 재개

그래서
> “재시작하니 바로 로그가 밀린다”
> 라는 현상이 발생한다.

---

## 9. 이 문제가 Filebeat에서 특히 체감되는 이유

- 장시간 연결 유지
- ACK 기반 전송 모델
- 보수적인 재연결 정책
- 중간 장비(LB/NAT/FW) 개입 빈번

👉 half-open 상태가 **오래 유지**
👉 운영자 입장에서는 “멈춘 것처럼” 보임

---

## 한 줄 요약

> **TCP는 응답이 없다고 연결을 끊지 않는다.
> FIN/RST 또는 매우 긴 타임아웃이 있어야 종료된다.
> 중간 장비가 세션을 조용히 버리면
> 양 끝은 `ESTABLISHED` 상태로 남을 수 있다.**

이게 **half-open(blackhole) 세션의 본질**이다.

## 기여 방향

### 1. 문제 요약

- Filebeat는 Logstash와 **long-lived TCP connection**을 유지함
- 중간 장비(LB / NAT / FW)가 **idle-timeout으로 FIN/RST 없이 세션을 삭제**하는 경우
  - Filebeat 커널 상태는 `ESTABLISHED`
  - ACK는 오지 않음
  - backoff / timeout만 반복
- 운영 체감상 **Filebeat 재시작 전까지 복구되지 않는 상황**이 발생함
- 최신 Filebeat 공식 설정에는
  - half-open 감지
  - stalled connection 판단
  - 자동 reconnect
      를 직접 해결하는 옵션이 없음

### 2. 기존 설정의 한계

#### `output.logstash.ttl`

- 오래된 커넥션을 주기적으로 재수립하는 옵션
- sticky connection / 로드밸런서 불균형 완화 목적
- **middlebox idle-timeout이 더 짧아지면 동일 문제 재발**
- `pipelining` 사용 시 적용 불가

#### `timeout`, `backoff`

- 네트워크 에러 이후 재시도 동작 제어용
- **FIN/RST 없는 silent drop / half-open 상태를 선제적으로 감지하지는 못함**

---

### 3. 기여 아이디어 (현실적으로 수용 가능)

#### 3.1 Stalled connection 감지 (1차, 가장 유력)

> 기본 동작 변경 없음 / 운영 가시성 개선 중심

##### 감지 조건 예시

- N분 동안 `acked_events == 0`
- 같은 기간 `queue depth` 증가
- output connection은 alive 상태

##### 판단

- “stalled connection” 상태로 명시적 분류

##### 동작

- WARN 로그 출력
- metric 노출 (예: `libbeat.output.stalled = 1`)
- ❌ 기본 reconnect 없음

##### 장점

- backward compatibility 유지
- 오탐 리스크 낮음
- 운영자가 문제를 명확히 인지 가능
- maintainer 수용 가능성 높음

---

#### 3.2 opt-in reconnect 옵션 (2차 단계)

> 기본 OFF 유지

```yaml
output.logstash:
    reconnect_on_stall: true
    stall_timeout: 2m
```

**동작**

- stalled 조건 충족 시
- 해당 output client 소켓 close
- 다음 flush에서 새 연결 생성

**주의사항**

- queue 증가 조건 포함 필수 (idle과 구분)
- 기본 OFF 유지가 핵심

---

### 4. 코드

- **libbeat 영역**
  - publisher/pipeline
    - ACK 진행 감시
    - stall 판단 로직

  - publisher/queue
    - queue depth 정보 제공

- outputs/logstash
  - opt-in reconnect 시 client close 트리거

> Filebeat 단일 문제가 아니라
> Beats 공통 output 구조의 개선 포인트
