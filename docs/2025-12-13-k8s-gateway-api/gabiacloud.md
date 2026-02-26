# Gabiacloud

- [로드 밸런서 사용하기](https://customer.gabia.com/manual/cloud/23381/24281)
- [nginx.yml](./nginx.yml)

`nginx.service`는 `clusterIp`로 기본적으로 노출되는 것이 아니라, `NodePort`로 control-plane에 노출됨.


## TODO

- [ ] gabiacloud TLS 인증서를 어떻게 연결하는지

```mermaid
sequenceDiagram
    autonumber
    actor Client as External Client
    participant LB as nginx-service (Service: LoadBalancer, EXTERNAL-IP)
    participant Nginx as NGINX Pods (nginx-deployment)
    participant EGW as Envoy Gateway Proxy (Service: ClusterIP)
    participant GWAPI as Gateway API (Gateway/HTTPRoute/GRPCRoute)
    participant SVC as Backend Service (ClusterIP)
    participant POD as Backend Pods

    Client->>LB: 1) TCP 443 (or 80) to EXTERNAL-IP
    LB->>Nginx: 2) L4 forward to targetPort (NGINX Pod)
    Note over Nginx: TLS termination happens here\n(nginx has cert/key)

    Nginx->>EGW: 3) HTTP request proxied to Envoy Proxy Service (ClusterIP:80)
    Note over Nginx,EGW: Preserve Host header for HTTPRoute.hostnames\nAdd X-Forwarded-* headers

    EGW->>GWAPI: 4) Match Gateway listener + Route rules\n(host/path/headers or gRPC methods)
    GWAPI->>SVC: 5) Select backendRef (Service:port)
    SVC->>POD: 6) Kube-proxy routes to one Pod endpoint

    POD-->>SVC: 7) Response
    SVC-->>EGW: 8) Response
    EGW-->>Nginx: 9) Response (HTTP)
    Nginx-->>LB: 10) Response (HTTPS re-encrypted to client)
    LB-->>Client: 11) HTTPS response
```
