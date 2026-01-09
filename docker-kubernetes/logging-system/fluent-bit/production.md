# 운영

**_Table of Contents_**

<!-- TOC -->

- [운영](#운영)
  - [backlog](#backlog)
    - [backolog 상한은 "시간 기준"으로 산정](#backolog-상한은-시간-기준으로-산정)
    - [모니터링](#모니터링)
  - [ES 복구 후 ingest가 느린 경우 베스트 프랙티스](#es-복구-후-ingest가-느린-경우-베스트-프랙티스)
  - [docker json-file rotate](#docker-json-file-rotate)

<!-- TOC -->

```text
컨테이너 stdout
  → Docker json-file (/var/lib/docker/containers/*.log)
    → Fluent Bit tail input
      → filesystem buffer (chunk)
        → ES
```

**ES 단절 발생 시**

- Fluent Bit은
  - ES 전송 실패 감지
  - **전송 못한 로그 chunk를 디스크(storage.path)에 저장**
  - retry 상태로 대기함
- 이 동안에도
  - 컨테이너 로그 파일은 계속 증가
  - Fluent Bit은 계속 tail 해서 새 chunk를 디스크에 적재

**ES 복구 시**

- Fluent Bit이
  - 디스크에 쌓여 있던 chunk부터 순서대로 재전송
  - 성공하면 chunk 삭제
- 별도 조치 필요 없음

## backlog

- **무한 backlog 금지**
- 로그는 버려도 되지만 **서버는 죽으면 안 됨**
- /fluent-bit/state는
  - 루트 디스크와 분리된 파티션 또는 볼륨 사용 권장
- 이유
  - 로그 backlogs로 OS 전체가 마비되는 상태 방지

### backolog 상한은 "시간 기준"으로 산정

허용 단절 시간 * 평균 로그 유입량 = 필요 디스크

예시

- 평균 로그 유입: 2MB/s
- 허용 ES 장애: 30분

```text
2MB * 60 * 30 = 3.6GB
```

→ storage.max_chunks_up을 이 용량을 넘기지 않는 선에서 설정

### 모니터링

- Fluent Bit storage 디렉터리 용량
- backlog chunk 개수
- 드롭 발생 로그

**모니터링이 없으면 유실이 언제 시작됐는지 모름**

---

## ES 복구 후 ingest가 느린 경우 베스트 프랙티스

**backlog 소진 속도 ≥ 로그 유입 속도 유지**

- 이게 안 되면 backlog는 영원히 안 줄어듦

**튜닝:**

**output workers**

```conf
[OUTPUT]
    Workers 2
```

- CPU/네트워크 여유 있으면 2~4부터 시작
- 무턱대고 키우면 ES에 역효과 날 수 있음

**flush 주기**

```conf
[SERVICE]
    Flush 5
```

- 너무 짧으면 ES에 잦은 요청
- 너무 길면 backlog 복구 지연
- 보통 5~10초가 현실적 시작점

**bulk 크기**

- ES output의 bulk size 옵션(환경에 따라 다름)
- 작은 bulk → 요청 수 폭증
- 큰 bulk → 메모리/timeout 리스크

## docker json-file rotate

계속 쌓이면 안되니 지속적으로 제거되게 처리해야함.

## kubernetes

Kubernetes 공식 문서에서도 노드 레벨 로깅 에이전트는 DaemonSet
권장임 ([공식 문서](https://kubernetes.io/docs/concepts/cluster-administration/logging/?utm_source=chatgpt.com#:~:text=Because%20the%20logging%20agent%20must%20run%20on%20every%20node%2C%20it%20is%20recommended%20to%20run%20the%20agent%20as%20a%20DaemonSet.))
