# Kubernetes

_**Table of Contents**_

<!-- TOC -->
- [Kubernetes](#kubernetes)
  - [CRD (Custom Resource Definition)](#crd-custom-resource-definition)
    - [CRD란 무엇인가?](#crd란-무엇인가)
<!-- TOC -->

## CRD (Custom Resource Definition)

K8s는 처음에 아주 제한된 객체 집합만 다룸.

- Pod
- Service
- Deployment
- ConfigMap
- Secret

모두 "컨테이너를 실행⋅노출⋅스케일링" 하는데 초점이 있었음.

**현실 세계의 요구**

실제로 원했던 것은:

- 데이터베이스 클러스터
- 메시지 큐
- 스토리지 시스템
- 클라우드 리소스

하지만 K8s의 기본 객체로는 다음을 표현할 수 없음.

```text
“PostgreSQL 클러스터 3노드, Primary/Replica,
자동 failover, 백업, 업그레이드 전략”
```

이를 기존 리소스로 표현하려면:

- ConfigMap + StatefulSet + Job + Script + 운영자 (수동 작업)
- 자동화는 외부 스크립트/툴에 흩어짐
- 상태 추적 불가
- 선언적 관리 불가

Kubernetes의 핵심 강점(선언적 상태 관리)를 활용할 수 없었음.

**Pod 처럼, 도메인 객체 자체를 K8s가 이해하게 만들 수 없음?**

예:

```yaml
kind: PostgreSQLCluster
spec:
  instances: 3
  failover:
    automatic: true
```

이 질문에 대한 답이 **CRD**임.

### CRD란 무엇인가?

CRD는 K8s API를 확장해서 사용자가 '자기 도메인 리소스 타입'을 정의할 수 있게 하는 메커니즘.

CustomResourceDefinition가 게시되면 사용자는 다른 쿠버네티스 리소스와 마찬가지로 JSON 또는 YAML 매니페스트를
API 서버에 게시해 사용자 정의 리소스 인스턴스를 만들 수 있음.

사용자 정의 오브젝트 인스턴스를 추가하는 것 이외에 CustomResourceDefinition를 통해 어떤 작업을 수행하도록 하려면 컨트롤러도 배포해야 함.
