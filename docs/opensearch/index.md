# OpenSearch Index (OTLP 기준) 정리

이 문서는 OTLP 로그/트레이스 데이터를 OpenSearch에 저장할 때
필요한 인덱스 설계 기준을 정리한다.

`semconv` 키를 어떻게 정의할지는 별도 문서에서 다루고,
여기서는 정의된 키를 어떤 타입으로 저장/검색할지에 집중한다.

## 1. `text` vs `keyword`

기준은 단순하다. **검색 점수 기반 전문 검색이 필요하면 `text`**,
**정확 일치/필터/집계/정렬이면 `keyword`** 를 사용한다.

### `keyword`로 두는 필드(OTLP에서 기본값)

- 식별자: `trace_id`, `span_id`
- 리소스/환경: `resource.service.name`, `resource.service.namespace`, `resource.service.instance.id`
- 인프라: `resource.k8s.namespace.name`, `resource.k8s.pod.name`, `resource.host.name`, `resource.cloud.region`
- HTTP/RPC 라우팅 키: `http.request.method`, `url.scheme`, `server.address`, `rpc.system`
- 로그 분류: `severity_text`, `event.domain`, `event.name`

위 필드는 대시보드 필터, `terms` 집계, 정확 매칭이 주 용도라 `keyword`가 맞다.

### `text`가 필요한 필드

- `body`(로그 본문), `exception.message`, 사용자 입력 텍스트

운영에서는 아래처럼 멀티 필드가 안전하다.

```json
{
    "body": {
        "type": "text",
        "fields": {
            "raw": {
                "type": "keyword",
                "ignore_above": 1024
            }
        }
    }
}
```

### 숫자/시간은 문자열 금지

- `@timestamp`: `date`
- `severity_number`: `byte` 또는 `short`
- 지연/카운트 계열: `long`, `double`
- 성공/실패 플래그: `boolean`

숫자를 문자열로 두면 범위 조회와 정렬 성능이 크게 떨어진다.

## 2. Analyzer 설계 방법

Analyzer는 전역 1개로 끝내지 말고, **필드 단위로 목적 기반 설계**가 원칙이다.

1. 먼저 쿼리 유형을 고정한다.
    - 본문 검색: 형태소/토큰 검색
    - 운영 필터: 정확 일치

2. 기본 매핑을 분리한다.
    - 본문: `text` + analyzer
    - 태그/속성: `keyword`

3. 언어별 analyzer를 필요한 필드에만 붙인다.
    - 한국어 로그 본문이 많으면 `nori`
    - 영문 중심이면 `standard` + `lowercase` 기반

4. 검증은 샘플 쿼리로 한다.
    - 동일 검색어에 대해 결과 누락/과다 매칭을 비교
    - 운영자가 실제 쓰는 검색식으로 리그레션 테스트

## 3. 샤드/복제/라우팅과 fan-out 검색

### 색인 라우팅

- 기본값: `_id` 해시로 primary shard 결정
- 커스텀 `routing`을 주면 같은 라우팅 값은 같은 샤드로 감

### 검색 fan-out

검색 시 코디네이팅 노드는 대상 인덱스의 **각 샤드 ID마다 primary/replica 중 1개**에 요청한다.
즉 `primary=6`, `replica=1`이면 물리 샤드는 12개여도, 검색 fan-out 대상은 샤드 ID 기준 6개다.

- `routing` 없이 검색: 대상 인덱스의 모든 샤드 ID 조회
- `routing` 지정 검색: 해당 라우팅이 매핑되는 샤드만 조회

OTLP에서 `tenant.id` 또는 `service.name` 기준 조회가 많다면 라우팅 최적화 여지가 있다. 다만 특정 라우팅 값 쏠림(핫샤드) 위험을 함께 봐야 한다.

## 4. scoring 쿼리 vs filter 쿼리

- Query context (`must`, `should`의 `match` 등): 관련도 점수 계산
- Filter context (`filter`, `must_not`, `term`, `range`): 점수 없이 포함/제외만 판단

OTLP 운영 검색은 대부분 filter 중심이 맞다.

- 예: `service.name = api`, `severity_number >= 13`, `@timestamp` 최근 15분
- 정렬은 보통 `@timestamp desc`

전문 검색(로그 본문 유사도)이 필요한 화면에서만 scoring을 추가한다.

## 5. 인덱싱 성능 측정과 개선

### 무엇을 측정할지

- 처리량: docs/s, MB/s
- 지연: 색인 p95/p99, refresh 지연
- 실패: bulk error rate, write rejection
- 자원: CPU, heap, GC, segment merge 시간

### 최소 관측 API

```bash
GET _nodes/stats/indices,indexing,thread_pool,jvm,fs
GET _cat/thread_pool/write?v
GET otlp-logs-*/_stats/indexing,refresh,merge,segments,store
```

### 개선 우선순위

1. Bulk 크기/동시성 튜닝(작게 시작해 점진 증가)
2. `refresh_interval` 완화(예: `1s` -> `10s` 또는 `30s`)
3. 대량 백필 시 `number_of_replicas=0` 후 완료 뒤 원복
4. 동적 매핑 최소화(필드 폭증 방지)
5. 샤드 크기를 적정 범위(대략 20~50GB)로 유지하도록 롤오버

## 6. OTLP용 인덱스 템플릿 기준

핵심은 **고정 필드는 명시 매핑**, **가변 속성은 제한된 방식으로 수용**이다.

```json
PUT _index_template/otlp-logs-template
{
  "index_patterns": ["otlp-logs-*"],
  "template": {
    "settings": {
      "number_of_shards": 3,
      "number_of_replicas": 1,
      "refresh_interval": "10s"
    },
    "mappings": {
      "dynamic": false,
      "properties": {
        "@timestamp": { "type": "date" },
        "trace_id": { "type": "keyword", "ignore_above": 64 },
        "span_id": { "type": "keyword", "ignore_above": 32 },
        "severity_text": { "type": "keyword", "ignore_above": 64 },
        "severity_number": { "type": "byte" },
        "resource.service.name": { "type": "keyword", "ignore_above": 256 },
        "resource.service.namespace": { "type": "keyword", "ignore_above": 256 },
        "resource.service.instance.id": { "type": "keyword", "ignore_above": 256 },
        "body": {
          "type": "text",
          "fields": {
            "raw": { "type": "keyword", "ignore_above": 1024 }
          }
        },
        "attributes": { "type": "flattened" }
      }
    }
  }
}
```

운영 권장사항:

- 로그/트레이스/메트릭 인덱스는 분리(`otlp-logs-*`, `otlp-traces-*`, `otlp-metrics-*`)
- 템플릿 + ISM(보존/롤오버) 조합으로 수명주기 고정
- alias를 조회 진입점으로 사용해 무중단 재색인 경로 확보

## 7. 빠른 체크리스트

1. semconv 핵심 필드가 `keyword`/숫자/날짜로 고정됐는가
2. 로그 본문만 `text`로 열고 나머지는 필터 중심으로 설계했는가
3. fan-out 범위를 줄일 수 있게 인덱스/라우팅 전략이 있는가
4. bulk 실패율, write rejection, merge 시간을 모니터링하는가
5. 템플릿과 ISM이 코드/설정으로 관리되는가
