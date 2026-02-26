# Gateway API 네트워크 접근 - 클라우드 vs 로컬 차이

## 핵심 차이점

**Gateway API 자체는 똑같습니다.** 하지만 **Gateway Controller가 만드는 Service의 동작이 완전히 다릅니다.**

## 실제 동작 방식

### ☁️ 클라우드 환경 (AWS EKS, GCP GKE, Azure AKS)

#### 1단계: Gateway 리소스 생성
```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: Gateway
metadata:
  name: demo-gateway
spec:
  gatewayClassName: envoy-gateway
  listeners:
    - name: http
      protocol: HTTP
      port: 80
```

#### 2단계: Gateway Controller가 자동으로 수행
```bash
# Gateway Controller가 자동으로 LoadBalancer Service 생성
kubectl get svc -n envoy-gateway-system
NAME                          TYPE           EXTERNAL-IP      PORT(S)
envoy-gateway-envoy-gateway   LoadBalancer   34.123.45.67     80:31234/TCP
                                             ↑
                                    클라우드 LB가 자동 할당!
```

#### 3단계: 클라우드 프로바이더가 개입
- AWS: ELB/ALB/NLB 자동 생성 (실제 IP: 34.123.45.67)
- GCP: Cloud Load Balancer 자동 생성
- Azure: Azure Load Balancer 자동 생성

#### 결과
```bash
# 인터넷에서 바로 접근 가능!
curl http://34.123.45.67
# Internet → AWS LoadBalancer → K8s Service → Envoy Gateway → App
```

### 💻 로컬 환경 (Docker Desktop, Minikube, Kind)

#### 1단계: 동일한 Gateway 리소스 생성
```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: Gateway
metadata:
  name: demo-gateway
spec:
  gatewayClassName: envoy-gateway
  listeners:
    - name: http
      protocol: HTTP
      port: 80
```

#### 2단계: Gateway Controller가 Service 생성
```bash
# 하지만 로컬에서는...
kubectl get svc -n envoy-gateway-system
NAME                          TYPE        CLUSTER-IP      PORT(S)
envoy-gateway-envoy-gateway   ClusterIP   10.96.123.45    80/TCP
                                          ↑
                              클러스터 내부 IP만 있음!
```

#### 3단계: 클라우드 프로바이더가 없음!
- ❌ LoadBalancer를 만들어줄 클라우드 프로바이더 없음
- ❌ External IP 할당 불가
- ✅ ClusterIP만 생성됨 (기본값)

#### 결과
```bash
# 클러스터 외부에서 접근 불가!
curl http://10.96.123.45  # ❌ 실패 (클러스터 내부 IP)
curl http://localhost     # ❌ 실패 (연결 안됨)

# 클러스터 내부에서만 가능
kubectl run test --rm -it --image=nginx:alpine -- curl http://10.96.123.45  # ✅ 성공
```

## 왜 이런 차이가 발생하나?

### Service Type: LoadBalancer의 동작 차이

```yaml
# Gateway Controller가 만드는 Service (단순화)
apiVersion: v1
kind: Service
metadata:
  name: envoy-gateway-envoy-gateway
spec:
  type: LoadBalancer  # ← 여기가 핵심!
  ports:
    - port: 80
  selector:
    app: envoy-gateway
```

**클라우드:**
```bash
kubectl get svc
NAME       TYPE           EXTERNAL-IP
my-svc     LoadBalancer   34.123.45.67   ← 클라우드가 실제 LB 프로비저닝
```
- Cloud Controller Manager가 실제 LoadBalancer 생성
- 외부 IP 자동 할당
- 인터넷 트래픽 라우팅

**로컬:**
```bash
kubectl get svc
NAME       TYPE           EXTERNAL-IP
my-svc     LoadBalancer   <pending>      ← 영원히 pending...
# 또는
my-svc     ClusterIP      <none>         ← 아예 ClusterIP로 생성
```
- LoadBalancer를 만들어줄 컴포넌트 없음
- External IP 할당 불가
- Pending 상태로 남거나 ClusterIP로 fallback

## 해결 방법 비교

### 방법 1: Envoy Gateway 설치 시 NodePort 지정

```bash
# 로컬에서만 필요한 설정
helm install eg oci://docker.io/envoyproxy/gateway-helm \
  --set service.type=NodePort \
  --set service.ports[0].nodePort=30080
```

**결과:**
```bash
kubectl get svc -n envoy-gateway-system
NAME                          TYPE       PORT(S)
envoy-gateway-envoy-gateway   NodePort   80:30080/TCP
                                         ↑
                          노드의 30080 포트로 접근 가능!
```

**접근:**
```bash
curl http://localhost:30080      # ✅ 작동!
# localhost:30080 → K8s Node:30080 → Service → Envoy → App
```

### 방법 2: MetalLB 설치 (로컬 LoadBalancer 에뮬레이션)

```bash
# MetalLB 설치
kubectl apply -f https://raw.githubusercontent.com/metallb/metallb/v0.13.12/config/manifests/metallb-native.yaml

# IP 풀 설정
cat <<EOF | kubectl apply -f -
apiVersion: metallb.io/v1beta1
kind: IPAddressPool
metadata:
  name: local-pool
  namespace: metallb-system
spec:
  addresses:
  - 192.168.1.240-192.168.1.250
EOF
```

**이제 LoadBalancer가 작동:**
```bash
kubectl get svc -n envoy-gateway-system
NAME                          TYPE           EXTERNAL-IP
envoy-gateway-envoy-gateway   LoadBalancer   192.168.1.240  ← MetalLB가 할당!
```

### 방법 3: Docker Desktop의 LoadBalancer 지원

Docker Desktop은 특별히 LoadBalancer를 `localhost`로 매핑해줍니다:

```bash
kubectl get svc -n envoy-gateway-system
NAME                          TYPE           EXTERNAL-IP
envoy-gateway-envoy-gateway   LoadBalancer   localhost      ← Docker Desktop 자동 매핑
```

```bash
curl http://localhost:80  # ✅ 작동!
```

## 정리: Gateway API는 똑같지만...

| 구분 | 클라우드 | 로컬 |
|-----|---------|------|
| **Gateway API YAML** | 동일 ✅ | 동일 ✅ |
| **Gateway Controller** | 동일 ✅ | 동일 ✅ |
| **Service 생성** | LoadBalancer | ClusterIP (기본) |
| **Cloud Controller** | 있음 (AWS/GCP/Azure) | 없음 |
| **External IP** | 자동 할당 ✅ | 할당 안됨 ❌ |
| **외부 접근** | 바로 가능 ✅ | 추가 설정 필요 ⚠️ |

## 실습: 차이 확인하기

### 클라우드 (EKS 예시)
```bash
# 1. Gateway 생성
kubectl apply -f gateway.yaml

# 2. 1-2분 대기 후
kubectl get gateway demo-gateway
NAME           CLASS            ADDRESS          READY
demo-gateway   envoy-gateway    34.123.45.67     True
                                ↑
                        AWS LoadBalancer IP!

# 3. 바로 접근
curl http://34.123.45.67
```

### 로컬 (Docker Desktop)
```bash
# 1. 동일한 Gateway 생성
kubectl apply -f gateway.yaml

# 2. 확인
kubectl get gateway demo-gateway
NAME           CLASS            ADDRESS         READY
demo-gateway   envoy-gateway    10.96.123.45    True
                                ↑
                        클러스터 내부 IP만!

# 3. 외부 접근 불가
curl http://10.96.123.45  # ❌ 실패

# 4. NodePort나 port-forward 필요
kubectl port-forward -n envoy-gateway-system svc/envoy-gateway-envoy-gateway 8080:80
curl http://localhost:8080  # ✅ 성공
```

## 결론

**질문:** "클라우드든 로컬이든 똑같지 않아?"

**답변:**
- **Gateway API 스펙은 똑같습니다** ✅
- **하지만 인프라 레벨에서 완전히 다릅니다** ❌

**차이의 핵심:**
- **클라우드:** LoadBalancer Service → 클라우드가 실제 LB 생성 → External IP 할당
- **로컬:** LoadBalancer Service → 아무도 만들어주지 않음 → ClusterIP로 fallback

**로컬 해결책:**
1. NodePort 사용 (가장 간단)
2. MetalLB 설치 (LoadBalancer 에뮬레이션)
3. Docker Desktop 사용 (자동 지원)
4. port-forward (임시)
