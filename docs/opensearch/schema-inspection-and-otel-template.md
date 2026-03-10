# OpenSearch 인덱스 스키마 확인과 OTel 템플릿 검증

이 문서는 OpenSearch에서 Log, Traces, Metrics 인덱스가
실제로 어떤 스키마를 사용 중인지 확인하는 방법과,
Data Prepper의 OTel 기본 템플릿을 채택할 때 무엇을 봐야 하는지 정리한다.

관련 문서: [Index Template 운영과 스키마 변경 관리](./index-template-schema-management.md)

## 1. 먼저 구분할 것

OpenSearch에서 "이 인덱스가 어떤 스키마를 쓰는가"는 보통 아래 3가지를 구분해야 한다.

| 확인 대상                | 의미                                                   | 확인 API 또는 위치                                             |
| ------------------------ | ------------------------------------------------------ | -------------------------------------------------------------- |
| 실제 매핑                | 지금 존재하는 concrete index가 실제로 가진 필드 타입   | `GET <index>/_mapping`                                         |
| 미래 템플릿              | 다음 rollover 또는 다음 신규 인덱스에 적용될 설정/매핑 | `GET _index_template`, `GET _template`, `POST _simulate_index` |
| Data Prepper 내장 템플릿 | Data Prepper가 index type에 따라 읽어오는 기본 파일    | sink 설정, Data Prepper 코드, 리소스 JSON                      |

핵심은 `template file`과 `실제 인덱스 매핑`이 항상 같지 않다는 점이다.

1. 기존 인덱스는 이미 생성된 뒤라 템플릿 변경이 자동 반영되지 않는다.
2. `template_type`이 `v1`인지 `index-template`인지에 따라 확인해야 할 API가 달라진다.
3. `management_disabled`면 Data Prepper는 템플릿을 만들지 않으므로 클러스터 상태만 봐야 한다.

## 2. 가장 빠른 확인 순서

### 2.1 대상이 alias인지 data stream인지 concrete index인지 확인

예를 들어 로그가 `logs-otel-v1`로 들어간다고 가정하면 먼저 이름의 정체를 본다.

```bash
GET _resolve/index/logs-otel-v1
GET _data_stream/logs-otel-v1
```

여기서 확인할 것은 아래다.

1. `logs-otel-v1`가 data stream 이름인지
2. alias라면 어떤 concrete index들을 가리키는지
3. 현재 write index 또는 write backing index가 무엇인지

시계열 데이터는 alias나 data stream 뒤에 backing index가 여러 개 있을 수 있으므로,
이 단계 없이 바로 `_mapping`만 보면 해석이 모호해진다.

### 2.2 현재 write index의 실제 매핑 확인

가장 중요한 값은 현재 write index 또는 현재 write backing index의 매핑이다.

```bash
GET .ds-logs-otel-v1-2026.03.10-000001/_mapping
GET .ds-logs-otel-v1-2026.03.10-000001/_settings?flat_settings=true&include_defaults=false
```

설정에서 특히 볼 항목은 아래다.

1. `index.number_of_shards`
2. `index.number_of_replicas`
3. `index.opendistro.index_state_management.policy_id`
4. `index.opendistro.index_state_management.rollover_alias`

매핑에서 특히 볼 항목은 아래다.

1. `@timestamp`, `time`, `startTime`, `endTime` 같은 시간 필드 타입
2. `traceId`, `spanId`, `serviceName`, `name` 같은 검색 축 필드 타입
3. `resource.attributes.*`, `attributes.*`, `instrumentationScope.attributes.*`에 대한 동적 템플릿
4. `nested` 여부가 중요한 `events`, `links`, `buckets` 같은 필드

### 2.3 여러 backing index 사이에 필드 타입 드리프트가 있는지 확인

시계열 데이터는 rollover 시점에 따라 backing index마다 스키마가 다를 수 있다.
그래서 최신 index 하나만 보는 것보다 `field_caps`로 충돌 여부를 같이 보는 편이 안전하다.

```bash
GET logs-otel-v1*/_field_caps?fields=@timestamp,traceId,spanId,body,resource.attributes.*,attributes.*
```

아래와 같은 상황이면 스키마가 섞인 상태다.

1. 같은 필드가 index마다 `keyword`와 `text`로 갈려 있음
2. 어떤 index에는 있고 어떤 index에는 없음
3. 특정 필드가 `nested`와 `object`로 혼재함

## 3. 다음 rollover에 어떤 템플릿이 적용될지 확인

이 단계는 "앞으로 생성될 인덱스가 어떤 스키마를 쓸지" 보는 용도다.

### 3.1 composable index template를 쓰는 경우

```bash
GET _index_template/logs-otel-v1*
GET _component_template/*
POST _index_template/_simulate_index/logs-otel-v1-000123
```

`_simulate_index` 결과로 아래를 확인한다.

1. 어떤 템플릿이 매칭되는지
2. 최종 `settings`, `mappings`, `aliases`가 무엇인지
3. `component template`가 합쳐진 결과가 기대와 같은지

### 3.2 legacy `_template`를 쓰는 경우

Data Prepper는 기본적으로 `template_type: v1`을 사용한다.
즉, 별도 설정이 없다면 `_index_template`가 아니라 legacy `_template`를 보는 쪽이 먼저다.

```bash
GET _template/logs-otel-v1*
GET _template/metrics-otel-v1*
GET _template/otel-v1-apm-span*
```

Data Prepper README에도 `template_type` 기본값이 `v1`이라고 적혀 있고,
코드에서도 기본값은 `TemplateType.V1`이다.

참고:

- [Data Prepper README](../../../data-prepper/data-prepper-plugins/opensearch/README.md)
- [IndexConfiguration.java](../../../data-prepper/data-prepper-plugins/opensearch/src/main/java/org/opensearch/dataprepper/plugins/sink/opensearch/index/IndexConfiguration.java)

## 4. Data Prepper가 어떤 내장 템플릿 파일을 고르는지

Data Prepper는 `index_type`에 따라 다른 기본 템플릿 파일을 읽는다.

| index_type                  | 기본 alias/pattern                        | 기본 템플릿 파일                                                                                                                                                          |
| --------------------------- | ----------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `log-analytics`             | `logs-otel-v1` / `logs-otel-v1-*`         | [`logs-otel-v1-index-template.json`](../../../data-prepper/data-prepper-plugins/opensearch/src/main/resources/logs-otel-v1-index-template.json)                           |
| `log-analytics-plain`       | `logs-otel-v1` / `logs-otel-v1-*`         | [`logs-otel-v1-index-standard-template.json`](../../../data-prepper/data-prepper-plugins/opensearch/src/main/resources/logs-otel-v1-index-standard-template.json)         |
| `metric-analytics`          | `metrics-otel-v1` / `metrics-otel-v1-*`   | [`metrics-otel-v1-index-template.json`](../../../data-prepper/data-prepper-plugins/opensearch/src/main/resources/metrics-otel-v1-index-template.json)                     |
| `metric-analytics-plain`    | `metrics-otel-v1` / `metrics-otel-v1-*`   | [`metrics-otel-v1-index-standard-template.json`](../../../data-prepper/data-prepper-plugins/opensearch/src/main/resources/metrics-otel-v1-index-standard-template.json)   |
| `trace-analytics-raw`       | `otel-v1-apm-span` / `otel-v1-apm-span-*` | [`otel-v1-apm-span-index-template.json`](../../../data-prepper/data-prepper-plugins/opensearch/src/main/resources/otel-v1-apm-span-index-template.json)                   |
| `trace-analytics-plain-raw` | `otel-v1-apm-span` / `otel-v1-apm-span-*` | [`otel-v1-apm-span-index-standard-template.json`](../../../data-prepper/data-prepper-plugins/opensearch/src/main/resources/otel-v1-apm-span-index-standard-template.json) |

이 연결은 아래 코드에서 확인할 수 있다.

- [IndexType.java](../../../data-prepper/data-prepper-plugins/opensearch/src/main/java/org/opensearch/dataprepper/plugins/sink/opensearch/index/IndexType.java)
- [IndexConstants.java](../../../data-prepper/data-prepper-plugins/opensearch/src/main/java/org/opensearch/dataprepper/plugins/sink/opensearch/index/IndexConstants.java)
- [IndexConfiguration.java](../../../data-prepper/data-prepper-plugins/opensearch/src/main/java/org/opensearch/dataprepper/plugins/sink/opensearch/index/IndexConfiguration.java)

중요한 점은 `plain`과 기본 타입이 서로 다른 템플릿 파일을 읽더라도,
같은 alias와 같은 index pattern을 공유한다는 것이다.

즉 아래 상황이 생길 수 있다.

1. 예전에 `log-analytics`로 만든 index는 기존 템플릿 스키마를 가짐
2. 나중에 `log-analytics-plain`으로 바꾸면 다음 rollover index만 새 템플릿을 가짐
3. 결과적으로 `logs-otel-v1*` 안에 서로 다른 매핑이 공존할 수 있음

## 5. Log, Traces, Metrics는 모두 시계열이지만 스키마는 분리해야 한다

세 데이터는 모두 시간 축으로 쌓이지만, 한 템플릿으로 합치면 안 된다.

| 데이터 종류 | 핵심 시간 필드                       | 자주 보는 필드                                     | 주의할 타입                                             |
| ----------- | ------------------------------------ | -------------------------------------------------- | ------------------------------------------------------- |
| Logs        | `@timestamp`, `time`, `observedTime` | `severity`, `body`, `traceId`, `spanId`            | `body`는 `text`, attribute 계열은 동적 매핑 제어 필요   |
| Traces      | `@timestamp`, `startTime`, `endTime` | `traceId`, `spanId`, `parentSpanId`, `serviceName` | `events`, `links`는 `nested` 여부가 중요                |
| Metrics     | `@timestamp`, `time`, `startTime`    | `name`, `unit`, `aggregationTemporality`, `value`  | histogram/exponential histogram 구조와 수치 타입이 중요 |

따라서 권장 방향은 아래다.

1. Logs, Traces, Metrics는 index family를 분리한다.
2. 각 family 안에서만 rollover와 lifecycle을 관리한다.
3. 공통 규칙은 운영 문서와 템플릿 관리 방식으로 통일하고, 매핑은 signal별로 둔다.

## 6. `logs-otel-v1-index-standard-template.json`를 쓰려는 경우 확인할 것

질문하신 파일은 로그용 OTel 표준 템플릿 후보로 볼 수 있다.
다만 이 파일을 바로 채택하기 전에 아래를 먼저 확인해야 한다.

### 6.1 지금 클러스터가 legacy template를 쓰는지 composable template를 쓰는지

기본값은 `v1`이라서 `_template`를 쓰고 있을 가능성이 높다.
만약 `_index_template`만 보고 있으면 "템플릿이 없다"고 오해할 수 있다.

### 6.2 기존 `logs-otel-v1*`에 이미 다른 스키마가 섞여 있는지

`log-analytics`와 `log-analytics-plain`은 같은 alias/pattern을 공유한다.
기존 인덱스가 있다면 새 템플릿으로 바꿔도 과거 index는 그대로다.

이때는 아래 2가지 중 하나를 고르는 것이 안전하다.

1. 기존 namespace를 유지하고, rollover 이후부터만 새 스키마를 허용한다
2. `logs-otel-v2-*`처럼 새 namespace로 잘라서 이행한다

운영 안정성 관점에서는 2번이 더 명확하다.

### 6.3 템플릿 파일 내용 자체를 그대로 신뢰해도 되는지

로컬 리포지토리의 [`logs-otel-v1-index-standard-template.json`](../../../data-prepper/data-prepper-plugins/opensearch/src/main/resources/logs-otel-v1-index-standard-template.json) 파일을 보면
`observedTime` 선언이 legacy 템플릿과 비교했을 때 추가 검증이 필요한 형태로 보인다.

이 판단은 파일 내용 기준 추정이다.
즉, 운영 반영 전에는 아래 순서로 검증하는 편이 안전하다.

1. 테스트 클러스터에 템플릿 적용
2. 샘플 로그 색인
3. `GET <test-index>/_mapping` 확인
4. 실제 검색과 집계 쿼리 실행

### 6.4 Metrics는 naming convention도 같이 확인

이 리포지토리에는 `metric-analytics-plain`이 여전히 `metrics-otel-v1` alias를 사용해
Observability 화면 기대 패턴과 맞지 않을 수 있다는 메모가 있다.

참고:
[issue.md](../../../data-prepper/data-prepper-plugins/opensearch/src/main/java/org/opensearch/dataprepper/plugins/sink/opensearch/index/issue.md)

즉, Metrics는 매핑뿐 아니라 index naming까지 같이 확인해야 한다.

## 7. 실무 권장 절차

새 OTel 템플릿을 도입하거나 기존 템플릿을 교체할 때는 아래 순서를 권장한다.

1. Data Prepper sink 설정에서 `index_type`, `template_type`, `template_file`, `management_disabled`를 확인한다.
2. OpenSearch에서 alias, data stream, concrete index 관계를 먼저 확인한다.
3. 현재 write index와 대표적인 과거 index 몇 개의 `_mapping`과 `_field_caps`를 본다.
4. 현재 클러스터에 있는 template가 legacy인지 composable인지 확인한다.
5. 새 템플릿을 쓸 경우 `_simulate_index` 또는 테스트 index 생성으로 먼저 검증한다.
6. 기존 index와 혼합 운영이 싫다면 새 alias/pattern으로 cutover한다.
7. producer 배포는 항상 템플릿 반영 뒤에 한다.

## 8. 참고

- [OpenSearch Get mappings API](https://docs.opensearch.org/latest/api-reference/index-apis/get-mapping/)
- [OpenSearch Get settings API](https://docs.opensearch.org/latest/api-reference/index-apis/get-settings/)
- [OpenSearch Get index template API](https://docs.opensearch.org/latest/api-reference/index-apis/get-index-template/)
- [OpenSearch Get component template API](https://docs.opensearch.org/latest/api-reference/index-apis/get-component-template/)
- [OpenSearch Simulate index template API](https://docs.opensearch.org/latest/api-reference/index-apis/simulate-index-template/)
- [OpenSearch Resolve index API](https://docs.opensearch.org/latest/api-reference/index-apis/resolve-index/)
- [OpenSearch Get data streams API](https://docs.opensearch.org/latest/api-reference/index-apis/get-data-stream/)
- [OpenSearch Field capabilities API](https://docs.opensearch.org/latest/api-reference/search-apis/field-capabilities/)
