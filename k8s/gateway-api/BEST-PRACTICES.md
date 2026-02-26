# Gateway API 베스트 프랙티스 - 자체 구축 K8s (k3s, kubeadm)

## 환경별 권장 구성

### 1. 단일 서버 (홈랩, 개발 서버)

**추천: k3s + Traefik (기본 내장) ✅**

```bash
# k3s 설치 (Traefik 자동 설치됨)
curl -sfL https://get.k3s.io | sh -

# Gateway API CRDs 설치
kubectl apply -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.0.0/standard-install.yaml

# Traefik은 이미 설치되어 있고 GatewayClass도 자동 생성됨
kubectl get gatewayclass
NAME      CONTROLLER
traefik   traefik.io/gateway-controller
```

**Gateway 생성:**
```yaml
# gateway.yaml
apiVersion: gateway.networking.k8s.io/v1
kind: Gateway
metadata:
  name: main-gateway
spec:
  gatewayClassName: traefik
  listeners:
    - name: http
      protocol: HTTP
      port: 80
    - name: https
      protocol: HTTPS
      port: 443
      tls:
        certificateRefs:
          - kind: Secret
            name: tls-cert
```

**접근:**
```bash
# k3s는 기본적으로 HostPort 사용
curl http://your-server-ip
```

**장점:**
- 추가 설치 불필요 (Traefik 내장)
- 가볍고 빠름 (single binary)
- 80/443 포트 직접 바인딩
- 자동 TLS (Let's Encrypt)

---

### 2. 소규모 클러스터 (3-5 노드)

**추천: kubeadm + MetalLB + Envoy/Istio Gateway ✅**

#### 아키텍처
```
Internet
    ↓
방화벽/라우터 (포트포워딩)
    ↓
MetalLB (Virtual IP: 192.168.1.200)
    ↓
Gateway Controller (Envoy/Istio)
    ↓
Application Pods
```

#### 설치 순서

**1단계: Kubernetes 설치**
```bash
# 마스터 노드
kubeadm init --pod-network-cidr=10.244.0.0/16

# CNI 설치 (Calico)
kubectl apply -f https://raw.githubusercontent.com/projectcalico/calico/v3.26.1/manifests/calico.yaml

# 워커 노드 조인
kubeadm join ...
```

**2단계: MetalLB 설치**
```bash
# MetalLB 설치
kubectl apply -f https://raw.githubusercontent.com/metallb/metallb/v0.13.12/config/manifests/metallb-native.yaml

# IP 풀 설정 (네트워크 환경에 맞게 조정)
cat <<EOF | kubectl apply -f -
apiVersion: metallb.io/v1beta1
kind: IPAddressPool
metadata:
  name: production-pool
  namespace: metallb-system
spec:
  addresses:
  - 192.168.1.200-192.168.1.210  # 사용 가능한 IP 범위
---
apiVersion: metallb.io/v1beta1
kind: L2Advertisement
metadata:
  name: l2-advert
  namespace: metallb-system
spec:
  ipAddressPools:
  - production-pool
EOF
```

**3단계: Gateway API + Controller 설치**
```bash
# Gateway API CRDs
kubectl apply -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.0.0/standard-install.yaml

# Envoy Gateway 설치
helm install eg oci://docker.io/envoyproxy/gateway-helm \
  --version v1.0.0 \
  -n envoy-gateway-system \
  --create-namespace \
  --set service.type=LoadBalancer  # ← MetalLB가 IP 할당
```

**4단계: Gateway 생성**
```yaml
# production-gateway.yaml
apiVersion: gateway.networking.k8s.io/v1
kind: Gateway
metadata:
  name: production-gateway
  namespace: default
spec:
  gatewayClassName: envoy-gateway
  listeners:
    - name: http
      protocol: HTTP
      port: 80
      allowedRoutes:
        namespaces:
          from: All
    - name: https
      protocol: HTTPS
      port: 443
      tls:
        mode: Terminate
        certificateRefs:
          - kind: Secret
            name: wildcard-tls
      allowedRoutes:
        namespaces:
          from: All
```

**5단계: 확인**
```bash
kubectl get svc -n envoy-gateway-system
NAME                          TYPE           EXTERNAL-IP
envoy-gateway-envoy-gateway   LoadBalancer   192.168.1.200  # ✅ MetalLB 할당

curl http://192.168.1.200
```

---

### 3. 프로덕션 환경 (고가용성)

**추천: kubeadm + MetalLB + Istio Gateway + Cert-Manager ✅**

#### 아키텍처
```
┌─────────────────────────────────────────────┐
│              Internet                        │
└───────────────────┬─────────────────────────┘
                    │
┌───────────────────▼─────────────────────────┐
│   외부 L4 로드밸런서 (선택)                    │
│   (HAProxy, Nginx, 클라우드 LB)              │
└───────────────────┬─────────────────────────┘
                    │
┌───────────────────▼─────────────────────────┐
│   MetalLB (L2 또는 BGP 모드)                │
│   Virtual IP: 192.168.1.200                 │
└───────────────────┬─────────────────────────┘
                    │
┌───────────────────▼─────────────────────────┐
│   Istio Gateway (여러 노드에 분산)            │
│   - HA 구성 (replica: 3+)                   │
│   - PodDisruptionBudget 설정                │
└───────────────────┬─────────────────────────┘
                    │
        ┌───────────┴───────────┐
        ▼                       ▼
┌───────────────┐      ┌───────────────┐
│  Service Mesh │      │  Applications  │
│   (Istio)     │      │    (Pods)      │
└───────────────┘      └───────────────┘
```

#### 설치 스크립트

```bash
#!/bin/bash
set -e

echo "=== 프로덕션 Gateway API 구축 ==="

# 1. MetalLB 설치 (BGP 모드 - 고가용성)
kubectl apply -f https://raw.githubusercontent.com/metallb/metallb/v0.13.12/config/manifests/metallb-native.yaml

cat <<EOF | kubectl apply -f -
apiVersion: metallb.io/v1beta1
kind: IPAddressPool
metadata:
  name: production-pool
  namespace: metallb-system
spec:
  addresses:
  - 192.168.1.200-192.168.1.210
  autoAssign: true
---
apiVersion: metallb.io/v1beta1
kind: BGPAdvertisement
metadata:
  name: bgp-advert
  namespace: metallb-system
spec:
  ipAddressPools:
  - production-pool
EOF

# 2. Gateway API CRDs
kubectl apply -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.0.0/standard-install.yaml

# 3. Istio 설치 (Gateway Controller)
curl -L https://istio.io/downloadIstio | sh -
cd istio-*
export PATH=$PWD/bin:$PATH

# Istio 프로필 설치 (Gateway만)
istioctl install --set profile=minimal \
  --set values.gateways.istio-ingressgateway.autoscaleEnabled=true \
  --set values.gateways.istio-ingressgateway.autoscaleMin=3 \
  --set values.gateways.istio-ingressgateway.autoscaleMax=10 -y

# 4. Cert-Manager 설치 (자동 TLS)
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# 5. Let's Encrypt Issuer 설정
cat <<EOF | kubectl apply -f -
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@example.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: istio
EOF

echo "✅ 설치 완료!"
```

#### Gateway 설정 (프로덕션)

```yaml
# production-gateway.yaml
apiVersion: gateway.networking.k8s.io/v1
kind: Gateway
metadata:
  name: production-gateway
  namespace: istio-system
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  gatewayClassName: istio
  listeners:
    # HTTP (HTTPS로 리다이렉트)
    - name: http
      protocol: HTTP
      port: 80
      allowedRoutes:
        namespaces:
          from: All

    # HTTPS (자동 TLS)
    - name: https
      protocol: HTTPS
      port: 443
      hostname: "*.example.com"
      tls:
        mode: Terminate
        certificateRefs:
          - kind: Secret
            name: wildcard-example-com-tls
      allowedRoutes:
        namespaces:
          from: All
```

**고가용성 설정:**
```yaml
# gateway-ha.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: istio-ingressgateway
  namespace: istio-system
spec:
  replicas: 3  # 최소 3개
  strategy:
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0  # 무중단 배포
  template:
    spec:
      affinity:
        podAntiAffinity:  # 다른 노드에 분산
          requiredDuringSchedulingIgnoredDuringExecution:
          - labelSelector:
              matchLabels:
                app: istio-ingressgateway
            topologyKey: kubernetes.io/hostname
      containers:
      - name: istio-proxy
        resources:
          requests:
            cpu: 500m
            memory: 512Mi
          limits:
            cpu: 2000m
            memory: 2Gi
---
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: istio-ingressgateway
  namespace: istio-system
spec:
  minAvailable: 2  # 최소 2개는 항상 실행
  selector:
    matchLabels:
      app: istio-ingressgateway
```

---

## Gateway Controller 선택 가이드

| Controller | 장점 | 단점 | 추천 환경 |
|-----------|------|------|----------|
| **Traefik** | k3s 기본 내장, 간단, Let's Encrypt 자동 | Service Mesh 없음 | 단일 서버, 소규모 |
| **Envoy Gateway** | CNCF 프로젝트, 빠름, 경량 | 상대적으로 신규 | 중소규모, 단순 환경 |
| **Istio** | Service Mesh, 고급 기능, 성숙도 높음 | 복잡, 리소스 많이 사용 | 프로덕션, 대규모 |
| **Kong** | API Gateway 기능 풍부, 플러그인 | 상용 기능은 유료 | API 중심 아키텍처 |
| **Nginx Gateway** | 성능 우수, 안정적 | 상대적으로 신규 | 높은 트래픽 |

---

## 모니터링 & 관찰성

**Prometheus + Grafana 구성:**

```bash
# kube-prometheus-stack 설치
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install monitoring prometheus-community/kube-prometheus-stack \
  -n monitoring \
  --create-namespace

# Envoy Gateway 메트릭 수집 설정
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Service
metadata:
  name: envoy-gateway-metrics
  namespace: envoy-gateway-system
  labels:
    app: envoy-gateway
spec:
  ports:
  - name: metrics
    port: 19001
    targetPort: 19001
  selector:
    app: envoy-gateway
---
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: envoy-gateway
  namespace: envoy-gateway-system
spec:
  selector:
    matchLabels:
      app: envoy-gateway
  endpoints:
  - port: metrics
    interval: 30s
EOF
```

**Grafana 대시보드:**
- Envoy Gateway: https://grafana.com/grafana/dashboards/19653
- Istio: https://grafana.com/grafana/dashboards/7645

---

## 보안 베스트 프랙티스

### 1. TLS 필수

```yaml
# HTTP → HTTPS 리다이렉트
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: http-redirect
spec:
  parentRefs:
  - name: production-gateway
  hostnames:
  - "example.com"
  rules:
  - filters:
    - type: RequestRedirect
      requestRedirect:
        scheme: https
        statusCode: 301
```

### 2. Rate Limiting

```yaml
# Envoy Gateway Rate Limit
apiVersion: gateway.envoyproxy.io/v1alpha1
kind: BackendTrafficPolicy
metadata:
  name: rate-limit-policy
spec:
  targetRef:
    group: gateway.networking.k8s.io
    kind: Gateway
    name: production-gateway
  rateLimit:
    type: Global
    global:
      rules:
      - clientSelectors:
        - headers:
          - name: x-user-id
            type: Distinct
        limit:
          requests: 100
          unit: Minute
```

### 3. 네임스페이스 격리

```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: Gateway
metadata:
  name: team-gateway
  namespace: team-a
spec:
  gatewayClassName: envoy-gateway
  listeners:
    - name: http
      protocol: HTTP
      port: 80
      allowedRoutes:
        namespaces:
          from: Same  # 같은 네임스페이스만 허용
```

---

## 트러블슈팅 체크리스트

### Gateway가 IP를 받지 못할 때

```bash
# 1. MetalLB 상태 확인
kubectl get pods -n metallb-system
kubectl logs -n metallb-system -l app=metallb

# 2. IPAddressPool 확인
kubectl get ipaddresspool -n metallb-system -o yaml

# 3. Service 이벤트 확인
kubectl describe svc -n envoy-gateway-system envoy-gateway-envoy-gateway

# 4. ARP 테이블 확인 (L2 모드)
ip neigh | grep 192.168.1.200
```

### HTTPRoute가 작동하지 않을 때

```bash
# 1. Route 상태 확인
kubectl get httproute -A
kubectl describe httproute <name>

# 2. Gateway 연결 확인
kubectl get gateway <name> -o jsonpath='{.status}'

# 3. Gateway Controller 로그
kubectl logs -n envoy-gateway-system -l app=envoy-gateway

# 4. DNS 확인
nslookup your-domain.com
```

---

## 비용 최적화

### 리소스 요청/제한 설정

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: envoy-gateway
spec:
  template:
    spec:
      containers:
      - name: envoy
        resources:
          requests:
            cpu: 100m      # 최소 필요 리소스
            memory: 128Mi
          limits:
            cpu: 1000m     # 최대 사용 가능
            memory: 512Mi
```

### HPA (Horizontal Pod Autoscaler)

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: envoy-gateway-hpa
  namespace: envoy-gateway-system
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: envoy-gateway
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

---

## 요약: 환경별 최종 권장

### 개인 서버 / 홈랩
```bash
✅ k3s + Traefik (내장)
- 설치 명령어 하나로 완료
- 추가 구성 최소화
- Let's Encrypt 자동
```

### 소규모 스타트업 (3-10 노드)
```bash
✅ kubeadm + MetalLB + Envoy Gateway
- 간단하면서도 확장 가능
- CNCF 표준 스택
- 적은 리소스 사용
```

### 프로덕션 / 대규모 (10+ 노드)
```bash
✅ kubeadm + MetalLB + Istio + Cert-Manager
- 고가용성 보장
- Service Mesh 통합
- 고급 트래픽 관리
- 자동 TLS 관리
```

### 멀티 테넌트 환경
```bash
✅ kubeadm + MetalLB + 여러 Gateway Controller
- 팀별 독립 Gateway
- 네임스페이스 격리
- RBAC 세밀하게 설정
```
