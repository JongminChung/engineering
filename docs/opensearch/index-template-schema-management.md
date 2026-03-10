# OpenSearch Index Template 운영과 스키마 변경 관리

이 문서는 OpenSearch에서 로그성 데이터, 특히 감사로그를 저장할 때
`index template`를 어떻게 관리하고, 이후 스키마 변경을 어떤 절차로 반영할지 정리한다.

## 1. 핵심 원칙

1. `index template`는 새로 생성되는 인덱스 또는 새 backing index에만 적용된다.
2. 기존 인덱스의 매핑은 자동으로 바뀌지 않는다.
3. 스키마 변경은 `추가 변경`과 `호환 불가 변경`으로 나눠서 관리한다.
4. 배포 순서는 항상 `template 먼저`, `현재 write index 반영`, `producer 나중`이 안전하다.

감사로그처럼 append-only 성격의 시계열 데이터는 `data stream` 기반 운영을 우선 고려한다.

## 2. 권장 관리 방식

| 대상      | 리소스                | 역할                                                       |
| --------- | --------------------- | ---------------------------------------------------------- |
| 공통 설정 | `_component_template` | shard, replica, refresh interval                           |
| 공통 매핑 | `_component_template` | 필드 타입, dynamic 정책                                    |
| 최종 조합 | `_index_template`     | `index_patterns`, `priority`, `data_stream`, `composed_of` |

운영 기준은 아래와 같다.

1. UI에서 직접 수정하지 말고 Git에서 JSON 또는 YAML로 관리한다.
2. `version`과 `_meta.schema_version`을 함께 올려 변경 이력을 남긴다.
3. `index template`는 적게 두고, 공통화는 `component template`로 해결한다.
4. 적용 전에는 `simulate index template`로 최종 매핑을 확인한다.
5. 템플릿 겹침이 생기지 않도록 `index_patterns`와 `priority`를 명확히 둔다.

## 3. 변경 유형별 대응

| 변경 유형      | 예시                            | 기존 인덱스 즉시 반영 | 재색인 필요성 | 권장 절차                                                    |
| -------------- | ------------------------------- | --------------------- | ------------- | ------------------------------------------------------------ |
| 필드 추가      | `request.id`, `source.ip` 추가  | 가능                  | 보통 없음     | template 수정 -> `PUT _mapping` -> producer 배포 -> rollover |
| 필드 옵션 추가 | 새 필드에 `keyword`/`text` 분리 | 가능                  | 보통 없음     | template 수정 -> `PUT _mapping` -> rollover                  |
| 필드 타입 변경 | `source.ip: keyword -> ip`      | 불가                  | 필요          | 새 인덱스/새 data stream 생성 -> reindex -> alias 전환       |
| 필드 이름 변경 | `actor.id -> user.id`           | 직접 변경 불가        | 필요 가능     | 일정 기간 dual write -> 새 스키마 정착 -> 필요 시 reindex    |
| 필드 삭제      | 더 이상 사용하지 않는 필드 제거 | 기존 문서에는 남음    | 선택          | 새 템플릿부터 제거, 과거 정리가 필요하면 reindex             |

핵심은 간단하다.

1. 새 필드를 추가하는 경우는 비교적 가볍다.
2. 기존 필드의 의미나 타입을 바꾸는 경우는 새 인덱스로 옮긴다고 생각하는 편이 안전하다.

## 4. 감사로그 예시

기본 감사로그를 `audit-logs` data stream으로 운영한다고 가정한다.

### 4.1 초기 스키마

```json
PUT _component_template/audit-mappings-v1
{
  "template": {
    "mappings": {
      "dynamic": "strict",
      "properties": {
        "@timestamp": { "type": "date" },
        "event_type": { "type": "keyword" },
        "action": { "type": "keyword" },
        "result": { "type": "keyword" },
        "actor": {
          "properties": {
            "id": { "type": "keyword" },
            "type": { "type": "keyword" }
          }
        },
        "resource": {
          "properties": {
            "id": { "type": "keyword" },
            "type": { "type": "keyword" }
          }
        }
      }
    }
  },
  "version": 1,
  "_meta": {
    "schema_version": "1.0.0"
  }
}
```

```json
PUT _index_template/audit-logs-template
{
  "index_patterns": ["audit-logs*"],
  "data_stream": {},
  "composed_of": ["audit-mappings-v1"],
  "priority": 200,
  "version": 1,
  "_meta": {
    "schema_version": "1.0.0"
  }
}
```

### 4.2 신규 감사 필드 추가

운영 중 아래 필드를 추가로 수집한다고 가정한다.

- `request.id`: 요청 추적 ID
- `source.ip`: 요청자 IP
- `reason`: 변경 사유

이 경우는 호환 가능한 변경이므로 아래 순서로 처리한다.

1. 새 버전의 매핑 컴포넌트를 만든다.
2. `index template`를 새 컴포넌트로 갱신한다.
3. 현재 write backing index에도 `PUT _mapping`을 적용한다.
4. producer 또는 수집기를 배포한다.
5. rollover 이후 새 backing index가 새 템플릿을 사용하도록 한다.

```json
PUT _component_template/audit-mappings-v2
{
  "template": {
    "mappings": {
      "dynamic": "strict",
      "properties": {
        "@timestamp": { "type": "date" },
        "event_type": { "type": "keyword" },
        "action": { "type": "keyword" },
        "result": { "type": "keyword" },
        "actor": {
          "properties": {
            "id": { "type": "keyword" },
            "type": { "type": "keyword" }
          }
        },
        "resource": {
          "properties": {
            "id": { "type": "keyword" },
            "type": { "type": "keyword" }
          }
        },
        "request": {
          "properties": {
            "id": { "type": "keyword" }
          }
        },
        "source": {
          "properties": {
            "ip": { "type": "ip" }
          }
        },
        "reason": {
          "type": "text",
          "fields": {
            "keyword": {
              "type": "keyword",
              "ignore_above": 256
            }
          }
        }
      }
    }
  },
  "version": 2,
  "_meta": {
    "schema_version": "1.1.0"
  }
}
```

```json
PUT _index_template/audit-logs-template
{
  "index_patterns": ["audit-logs*"],
  "data_stream": {},
  "composed_of": ["audit-mappings-v2"],
  "priority": 200,
  "version": 2,
  "_meta": {
    "schema_version": "1.1.0"
  }
}
```

```json
PUT /audit-logs/_mapping?write_index_only=true
{
  "properties": {
    "request": {
      "properties": {
        "id": { "type": "keyword" }
      }
    },
    "source": {
      "properties": {
        "ip": { "type": "ip" }
      }
    },
    "reason": {
      "type": "text",
      "fields": {
        "keyword": {
          "type": "keyword",
          "ignore_above": 256
        }
      }
    }
  }
}
```

```json
POST /_index_template/_simulate_index/audit-logs
```

```json
POST /audit-logs/_rollover
```

주의할 점은 다음과 같다.

1. 템플릿만 수정하면 현재 write backing index는 바뀌지 않는다.
2. `dynamic: strict`라면 새 필드를 먼저 보내는 순간 색인 실패가 날 수 있다.
3. 따라서 producer보다 템플릿과 현재 write index 반영이 먼저다.

## 5. 호환 불가 변경 예시

처음에 `source.ip`를 `keyword`로 저장했는데, 이후 IP 범위 검색을 위해 `ip` 타입으로 바꾸고 싶을 수 있다.

이 경우 기존 필드 타입을 그대로 바꾸는 것은 불가능하다고 보고 운영한다.

1. 새 매핑으로 `audit-logs-v2` data stream 또는 새 인덱스 패턴을 만든다.
2. 새 템플릿에 `source.ip: ip`를 정의한다.
3. 기존 데이터를 `reindex`한다.
4. 조회 alias 또는 애플리케이션의 조회 대상을 새 인덱스로 전환한다.

필드 이름 변경도 같은 부류로 보는 것이 안전하다.

## 6. 운영 체크리스트

1. 감사로그는 `dynamic: strict` 또는 제한된 동적 매핑을 기본값으로 둔다.
2. 변경 이력은 템플릿 이름, `version`, `_meta.schema_version`으로 함께 남긴다.
3. 필드 추가 시에는 `template 수정`과 `write index mapping 반영`을 같이 한다.
4. 필드 타입 변경 시에는 기존 인덱스를 고치려 하지 말고 새 인덱스로 이관한다.
5. 조회 진입점은 가능하면 alias 또는 data stream 이름으로 고정한다.
6. 템플릿, ISM, rollover 기준은 코드와 문서로 함께 관리한다.

## 7. 참고

- [OpenSearch Create or update index template](https://docs.opensearch.org/latest/api-reference/index-apis/create-index-template/)
- [OpenSearch Simulate index template](https://docs.opensearch.org/latest/api-reference/index-apis/simulate-index-template/)
- [OpenSearch Put mapping](https://docs.opensearch.org/latest/api-reference/index-apis/put-mapping/)
- [OpenSearch Aliases API](https://docs.opensearch.org/latest/api-reference/alias/aliases-api/)
- [OpenSearch Data streams](https://docs.opensearch.org/latest/im-plugin/data-streams/)
- [OpenSearch Reindex](https://docs.opensearch.org/latest/api-reference/document-apis/reindex/)
