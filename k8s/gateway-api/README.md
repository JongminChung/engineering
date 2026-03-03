# Kubernetes Gateway API with Envoy Gateway + Helm

로컬 K8s 환경에서 Envoy Gateway API를 사용하는 Helm 차트입니다.

## 📁 프로젝트 구조

```
k8s-gateway-helm/
├── Chart.yaml                  # Helm 차트 메타데이터
├── values.yaml                 # 기본 설정 (Envoy Gateway)
├── values-local.yaml           # 로컬 환경 설정
├── values-prod.yaml            # 프로덕션 환경 설정
├── templates/
│   ├── deployment.yaml         # 애플리케이션 배포
│   ├── service.yaml            # 서비스
│   ├── gateway.yaml            # Gateway 리소스
│   └── httproute.yaml          # HTTPRoute 리소스
├── install.sh                  # 자동 설치 스크립트
├── uninstall.sh                # 제거 스크립트
├── test.sh                     # 테스트 스크립트
└── README.md
```

## 🚀 빠른 시작 (자동 설치)

### 사전 요구사항
- Kubernetes 클러스터 (Docker Desktop, Minikube, Kind 등)
- kubectl 설치
- Helm 3.x 설치

### 1분 안에 시작하기

```bash
# 1. 프로젝트 디렉토리로 이동
cd k8s-gateway-helm

# 2. 자동 설치 스크립트 실행
./install.sh

# 3. /etc/hosts 설정
echo "127.0.0.1 demo.local" | sudo tee -a /etc/hosts

# 4. 터미널을 새로 열어 포트포워딩 (백그라운드에서 실행)
kubectl port-forward -n envoy-gateway-system service/envoy-gateway-envoy-gateway 8080:80 &

# 5. 접속 테스트
curl http://demo.local:8080
```

## 📋 수동 설치 (단계별)

### 1. Gateway API CRD 설치
```bash
kubectl apply -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.0.0/standard-install.yaml
```

### 2. Envoy Gateway 설치
```bash
helm install eg oci://docker.io/envoyproxy/gateway-helm \
  --version v1.0.0 \
  -n envoy-gateway-system \
  --create-namespace
```

### 3. Envoy Gateway 준비 대기
```bash
kubectl wait --timeout=5m \
  -n envoy-gateway-system \
  deployment/envoy-gateway \
  --for=condition=Available
```

### 4. Demo 애플리케이션 설치

#### 로컬 환경
```bash
helm install demo-gateway . -f values-local.yaml
```

#### 프로덕션 환경
```bash
helm install demo-gateway . -f values-prod.yaml -n production --create-namespace
```

#### 커스텀 설정
```bash
helm install demo-gateway . \
  --set app.replicas=3 \
  --set routes[0].hostnames[0]=myapp.local
```

## 🧪 테스트

### 자동 테스트
```bash
./test.sh
```

### 수동 테스트

#### 1. /etc/hosts 설정
```bash
echo "127.0.0.1 demo.local api.demo.local" | sudo tee -a /etc/hosts
```

#### 2. 리소스 상태 확인
```bash
# Gateway 상태
kubectl get gateway demo-gateway

# HTTPRoute 상태
kubectl get httproute demo-route

# Pods 상태
kubectl get pods

# 상세 정보
kubectl describe gateway demo-gateway
kubectl describe httproute demo-route
```

#### 3. 포트포워딩
```bash
# Envoy Gateway 서비스로 포트포워딩
kubectl port-forward -n envoy-gateway-system \
  service/envoy-gateway-envoy-gateway 8080:80
```

#### 4. HTTP 요청 테스트
```bash
# 기본 GET 요청
curl http://demo.local:8080

# 헤더 포함
curl -H "Host: demo.local" http://localhost:8080

# 상세 정보 출력
curl -v http://demo.local:8080

# 다른 호스트명 테스트 (values-local.yaml에 정의된)
curl http://api.demo.local:8080
```

## ⚙️ 설정 커스터마이징

### values-local.yaml (로컬 개발)
```yaml
app:
  replicas: 1              # 로컬에서는 1개로 충분

gateway:
  gatewayClassName: envoy-gateway
  listeners:
    - name: http
      protocol: HTTP
      port: 80
      allowedRoutes:
        namespaces:
          from: All        # 모든 네임스페이스 허용

routes:
  - name: demo-route
    hostnames:
      - demo.local
      - api.demo.local
```

### values-prod.yaml (프로덕션)
```yaml
app:
  replicas: 3              # 고가용성

gateway:
  listeners:
    - name: http
      protocol: HTTP
      port: 80
      allowedRoutes:
        namespaces:
          from: Same       # 같은 네임스페이스만
    - name: https          # HTTPS 추가
      protocol: HTTPS
      port: 443
```

### 주요 설정 옵션

| 설정 경로 | 설명 | 기본값 |
|---------|------|--------|
| `app.name` | 애플리케이션 이름 | `demo-app` |
| `app.replicas` | Pod 복제 개수 | `2` |
| `app.image` | 컨테이너 이미지 | `nginx:alpine` |
| `gateway.gatewayClassName` | Gateway Controller | `envoy-gateway` |
| `gateway.listeners[].protocol` | 프로토콜 (HTTP/HTTPS) | `HTTP` |
| `gateway.listeners[].port` | 리스너 포트 | `80` |
| `routes[].hostnames[]` | 호스트명 목록 | `[demo.local]` |

## 🔍 트러블슈팅

### Gateway가 Programmed 상태가 아닐 때
```bash
# Gateway 상태 확인
kubectl describe gateway demo-gateway

# Envoy Gateway 로그 확인
kubectl logs -n envoy-gateway-system deployment/envoy-gateway
```

### HTTPRoute가 Accepted 상태가 아닐 때
```bash
# HTTPRoute 상태 확인
kubectl describe httproute demo-route

# Service가 존재하는지 확인
kubectl get svc demo-app
```

### 포트포워딩이 작동하지 않을 때
```bash
# Envoy Gateway Service 확인
kubectl get svc -n envoy-gateway-system

# 다른 포트로 시도
kubectl port-forward -n envoy-gateway-system \
  service/envoy-gateway-envoy-gateway 9090:80
```

### 502 Bad Gateway 에러
```bash
# 백엔드 Pod 상태 확인
kubectl get pods -l app=demo-app

# Pod 로그 확인
kubectl logs -l app=demo-app
```

## 🗑️ 제거

### 자동 제거
```bash
./uninstall.sh
```

### 수동 제거
```bash
# 1. Demo 애플리케이션 제거
helm uninstall demo-gateway

# 2. Envoy Gateway 제거
helm uninstall eg -n envoy-gateway-system
kubectl delete namespace envoy-gateway-system

# 3. Gateway API CRDs 제거 (선택)
kubectl delete -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.0.0/standard-install.yaml
```

## 📚 참고 문서

- [Kubernetes Gateway API](https://gateway-api.sigs.k8s.io/)
- [Envoy Gateway](https://gateway.envoyproxy.io/)
- [Helm Documentation](https://helm.sh/docs/)

## 🔧 고급 사용법

### 다중 환경 배포
```bash
# 개발 환경
helm install demo-dev . -f values-local.yaml -n dev --create-namespace

# 스테이징 환경
helm install demo-staging . -f values.yaml -n staging --create-namespace

# 프로덕션 환경
helm install demo-prod . -f values-prod.yaml -n prod --create-namespace
```

### Helm 업그레이드
```bash
# 설정 변경 후 업그레이드
helm upgrade demo-gateway . -f values-local.yaml

# dry-run으로 변경사항 미리보기
helm upgrade demo-gateway . -f values-local.yaml --dry-run
```

### Values 확인
```bash
# 적용될 최종 values 확인
helm get values demo-gateway

# 템플릿 렌더링 결과 확인
helm template demo-gateway . -f values-local.yaml
```
