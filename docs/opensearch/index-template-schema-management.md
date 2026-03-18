# OpenSearch Index Template

OpenSearch의 Legacy v1 템플릿 API(`/_template/...`)는 [deprecated](https://docs.opensearch.org/latest/api-reference/index-apis/put-template-legacy)이고,
composable index template(`/_index_template/...`)와 component template 사용이 권장된다.

## 순서

### 1. 기존 Legacy template 확인

```bash
GET /_template
```
