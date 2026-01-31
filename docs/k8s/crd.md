# CRD (Custom Resource Definition)

## 1. 무엇인가?

Kubernetes에 새로운 리소스 타입(API 오브젝트) 을 사용자 정의로 추가하는 메커니즘.

Pod, Service, Deployment 같은 기본 리소스처럼
`kubectl get xxx, kubectl apply -f`로 다룰 수 있는 나만의 리소스 종류를 만드는 방법.

한 줄로 요약하면, **CRD = Kubernetes API를 확장하는 공식적인 방법**

## 2. 목적

고수준 도메인 개념을 하나의 리소스로 표현하고 컨트롤러가 그 의미를 구현하도록 만듭니다.

- 고수준 도메인 개념: `이 앱은 외부 노출 + TLS + WAF`, `이 데이터베이스는 자동 백업 + 복제 정책이 필요함`

## 3. 구조: "정의"와 "인스턴스"의 분리

### 3.1 CRD(Definition)

- “이런 종류의 리소스가 있다”를 API 서버에 등록
- 스키마(OpenAPI), 버전(v1alpha1/v1beta1/v1) 포함

```yaml
apiVersion: apiextensions.k8s.io/v1
kind: CustomResourceDefinition
metadata:
  name: widgets.example.com
spec:
  group: example.com
  names:
    kind: Widget
    plural: widgets
  scope: Namespaced
  versions:
    - name: v1
      served: true
      storage: true
      schema:
        openAPIV3Schema:
          type: object
          properties:
            spec:
              type: object
              properties:
                size:
                  type: string
```

→ 이 시점부터 K8s는 Widget 이라는 리소스를 알게 됨

### 3.2 CR(Instance)

CRD로 정의된 실제 객체, Pod/Service처럼 생성/삭제/조회 가능

```yaml
apiVersion: example.com/v1
kind: Widget
metadata:
  name: my-widget
spec:
  size: large
```

→ 하지만 이 자체로는 아무 일도 일어나지 않음

### 3.3 Controller

CRD는 “명세(계약)”일 뿐이고, 실제 동작은 Controller가 구현

특정 CR을 감시(watch)하다가 CR의 spec을 읽고 실제 K8s 리소스(Pod, Service, ConfigMap 등)를 생성/수정 및 현제 상태를 `status`에 반영

#### 흐름 요약

```text
CR 생성
  ↓
API Server 저장
  ↓
Controller가 감지
  ↓
원하는 상태(spec)를 실제 리소스로 구현
  ↓
결과를 status에 기록
```
