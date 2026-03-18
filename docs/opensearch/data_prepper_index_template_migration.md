# Data Prepper Index Migration

```bash
# 인덱스 조회
GET _cat/indices?v&s=index

# 데이터 삭제
# DELETE ss4o_metrics-otel-*

# V1 인덱스 템플릿 조회
GET _template?filter_path=*.index_patterns

# V1 인덱스 템플릿 삭제
DELETE _template/otel-v1-apm-service-map-index-template

# Composable 인덱스 템플릿 리스트
GET _index_template?filter_path=*.name

# Composable 인덱스 템플릿 조회
GET _index_template/logs-otel-v1-index-template

# Composable 인덱스 템플릿 마이그레이션 생성
PUT /_index_template/logs-otel-v1-index-template-migration
{
  "index_patterns": ["logs-otel-v1-*"],
  "priority": 200,
  "version": 3,
  "template": {
    "settings": {
      "index": {
        "opendistro": {
          "index_state_management": {
            "rollover_alias": "logs-otel-v1"
          }
        }
      }
    },
    "mappings": {
      "_source": {
        "enabled": true
      },
      "dynamic_templates": [
        {
          "long_resource_attributes": {
            "path_match": "resource.attributes.*",
            "mapping": {
              "type": "long"
            },
            "match_mapping_type": "long"
          }
        },
        {
          "double_resource_attributes": {
            "path_match": "resource.attributes.*",
            "mapping": {
              "type": "double"
            },
            "match_mapping_type": "double"
          }
        },
        {
          "string_resource_attributes": {
            "path_match": "resource.attributes.*",
            "mapping": {
              "ignore_above": 256,
              "type": "keyword"
            },
            "match_mapping_type": "string"
          }
        },
        {
          "long_scope_attributes": {
            "path_match": "instrumentationScope.attributes.*",
            "mapping": {
              "type": "long"
            },
            "match_mapping_type": "long"
          }
        },
        {
          "double_scope_attributes": {
            "path_match": "instrumentationScope.attributes.*",
            "mapping": {
              "type": "double"
            },
            "match_mapping_type": "double"
          }
        },
        {
          "string_scope_attributes": {
            "path_match": "instrumentationScope.attributes.*",
            "mapping": {
              "ignore_above": 256,
              "type": "keyword"
            },
            "match_mapping_type": "string"
          }
        },
        {
          "long_attributes": {
            "path_match": "attributes.*",
            "mapping": {
              "type": "long"
            },
            "match_mapping_type": "long"
          }
        },
        {
          "double_attributes": {
            "path_match": "attributes.*",
            "mapping": {
              "type": "double"
            },
            "match_mapping_type": "double"
          }
        },
        {
          "string_attributes": {
            "path_match": "attributes.*",
            "mapping": {
              "ignore_above": 256,
              "type": "keyword"
            },
            "match_mapping_type": "string"
          }
        }
      ],
      "date_detection": false,
      "properties": {
        "severity": {
          "properties": {
            "number": {
              "type": "integer"
            },
            "text": {
              "ignore_above": 32,
              "type": "keyword"
            }
          }
        },
        "traceId": {
          "ignore_above": 32,
          "type": "keyword"
        },
        "spanId": {
          "ignore_above": 16,
          "type": "keyword"
        },
        "instrumentationScope": {
          "properties": {
            "name": {
              "ignore_above": 128,
              "type": "keyword"
            },
            "droppedAttributesCount": {
              "type": "integer"
            },
            "version": {
              "ignore_above": 64,
              "type": "keyword"
            },
            "schemaUrl": {
              "ignore_above": 256,
              "type": "keyword"
            }
          }
        },
        "@timestamp": {
          "type": "date_nanos"
        },
        "resource": {
          "properties": {
            "droppedAttributesCount": {
              "type": "integer"
            },
            "schemaUrl": {
              "ignore_above": 256,
              "type": "keyword"
            }
          }
        },
        "flags": {
          "type": "long"
        },
        "droppedAttributesCount": {
          "type": "integer"
        },
        "time": {
          "type": "date_nanos"
        },
        "body": {
          "type": "text"
        },
        "observedTime": {
          "type": "date_nanos"
        }
      }
    },
    "aliases": {}
  }
}

GET /_index_template/logs-otel-v1-index-template-migration

DELETE /_template/logs-otel-v1-index-template

# 어떠한 인덱스를 탈지 확인한다.
PUT /logs-otel-v1-000014
GET /logs-otel-v1-000014/_settings
GET /logs-otel-v1-000014/_mapping

# priority 및 vesrion 더 높게
PUT /_index_template/logs-otel-v1-index-template
{
  "index_patterns": [
    "logs-otel-v1-*"
  ],
  "priority": 300,
  "version": 3,
  "template": {
    "settings": {
      "index": {
        "opendistro": {
          "index_state_management": {
            "rollover_alias": "logs-otel-v1"
          }
        }
      }
    },
    "mappings": {
      "_source": {
        "enabled": true
      },
      "dynamic_templates": [
        {
          "long_resource_attributes": {
            "path_match": "resource.attributes.*",
            "mapping": {
              "type": "long"
            },
            "match_mapping_type": "long"
          }
        },
        {
          "double_resource_attributes": {
            "path_match": "resource.attributes.*",
            "mapping": {
              "type": "double"
            },
            "match_mapping_type": "double"
          }
        },
        {
          "string_resource_attributes": {
            "path_match": "resource.attributes.*",
            "mapping": {
              "ignore_above": 256,
              "type": "keyword"
            },
            "match_mapping_type": "string"
          }
        },
        {
          "long_scope_attributes": {
            "path_match": "instrumentationScope.attributes.*",
            "mapping": {
              "type": "long"
            },
            "match_mapping_type": "long"
          }
        },
        {
          "double_scope_attributes": {
            "path_match": "instrumentationScope.attributes.*",
            "mapping": {
              "type": "double"
            },
            "match_mapping_type": "double"
          }
        },
        {
          "string_scope_attributes": {
            "path_match": "instrumentationScope.attributes.*",
            "mapping": {
              "ignore_above": 256,
              "type": "keyword"
            },
            "match_mapping_type": "string"
          }
        },
        {
          "long_attributes": {
            "path_match": "attributes.*",
            "mapping": {
              "type": "long"
            },
            "match_mapping_type": "long"
          }
        },
        {
          "double_attributes": {
            "path_match": "attributes.*",
            "mapping": {
              "type": "double"
            },
            "match_mapping_type": "double"
          }
        },
        {
          "string_attributes": {
            "path_match": "attributes.*",
            "mapping": {
              "ignore_above": 256,
              "type": "keyword"
            },
            "match_mapping_type": "string"
          }
        }
      ],
      "date_detection": false,
      "properties": {
        "severity": {
          "properties": {
            "number": {
              "type": "integer"
            },
            "text": {
              "ignore_above": 32,
              "type": "keyword"
            }
          }
        },
        "traceId": {
          "ignore_above": 32,
          "type": "keyword"
        },
        "spanId": {
          "ignore_above": 16,
          "type": "keyword"
        },
        "instrumentationScope": {
          "properties": {
            "name": {
              "ignore_above": 128,
              "type": "keyword"
            },
            "droppedAttributesCount": {
              "type": "integer"
            },
            "version": {
              "ignore_above": 64,
              "type": "keyword"
            },
            "schemaUrl": {
              "ignore_above": 256,
              "type": "keyword"
            }
          }
        },
        "@timestamp": {
          "type": "date_nanos"
        },
        "resource": {
          "properties": {
            "droppedAttributesCount": {
              "type": "integer"
            },
            "schemaUrl": {
              "ignore_above": 256,
              "type": "keyword"
            }
          }
        },
        "flags": {
          "type": "long"
        },
        "droppedAttributesCount": {
          "type": "integer"
        },
        "time": {
          "type": "date_nanos"
        },
        "body": {
          "type": "text"
        },
        "observedTime": {
          "type": "date_nanos"
        }
      }
    },
    "aliases": {}
  }
}

DELETE /_index_template/logs-otel-v1-index-template-migration

POST /_index_template/_simulate_index/logs-otel-v1-000015
```
