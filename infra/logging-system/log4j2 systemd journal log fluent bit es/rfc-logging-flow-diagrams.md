# RFC 첨부 A: 구성도/플로우 다이어그램

## 목적

- 3안의 데이터 흐름을 시각적으로 비교하기 위한 첨부 문서.
- 운영/설계 논의 시 공통 이해를 맞추는 용도.

## 안 1) log4j2 -> journald -> fluent-bit -> Proxy(파일 + ES) -> ES

```text
[App log4j2]
      |
      v
 [systemd-journald]
      |
      v
   [fluent-bit]
      |
      v
 [Proxy 서버]
   |        \
   |         \
   v          v
(파일 저장)  [ES 전송]
               |
               v
              [ES]
```

## 안 2) log4j2 -> 파일(app log) -> filebeat -> ES (+ 파일 중앙 스토리지)

```text
[App log4j2]
      |
      v
 [App log file]
      |
      v
   [filebeat]
      |
      v
      [ES]

[App log file] --> [중앙 스토리지 (NAS/S3 등)]
```

## 안 3) log4j2 -> journald -> fluent-bit -> ES + journald 파일 보관(중앙화)

```text
[App log4j2]
      |
      v
 [systemd-journald]
      |            \
      |             \
      v              v
 [fluent-bit]   [journald export/forward]
      |              |
      v              v
     [ES]     [중앙 스토리지]
```

## 공통 고려사항

- ECS 포맷(JSON) 유지 여부 확인.
- 전송 실패 시 재시도/버퍼링 정책 명확화.
- 파일 보관 기간 및 접근 권한(감사 대응) 정의.
