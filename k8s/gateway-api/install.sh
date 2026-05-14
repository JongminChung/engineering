#!/bin/bash

set -e

echo "=== Envoy Gateway + Helm 설치 스크립트 ==="
echo ""

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. Gateway API CRDs 설치
echo -e "${YELLOW}[1/4] Gateway API CRDs 설치 중...${NC}"
kubectl apply -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.0.0/standard-install.yaml
echo -e "${GREEN}✓ Gateway API CRDs 설치 완료${NC}"
echo ""

# 2. Envoy Gateway 설치
echo -e "${YELLOW}[2/4] Envoy Gateway 설치 중...${NC}"
helm install eg oci://docker.io/envoyproxy/gateway-helm --version v1.0.0 -n envoy-gateway-system --create-namespace
echo -e "${GREEN}✓ Envoy Gateway 설치 완료${NC}"
echo ""

# 3. Envoy Gateway 준비 대기
echo -e "${YELLOW}[3/4] Envoy Gateway 준비 대기 중...${NC}"
kubectl wait --timeout=5m -n envoy-gateway-system deployment/envoy-gateway --for=condition=Available
echo -e "${GREEN}✓ Envoy Gateway 준비 완료${NC}"
echo ""

# 4. Demo 애플리케이션 설치
echo -e "${YELLOW}[4/4] Demo 애플리케이션 설치 중...${NC}"
helm install demo-gateway . -f values-local.yaml
echo -e "${GREEN}✓ Demo 애플리케이션 설치 완료${NC}"
echo ""

# 상태 확인
echo "=== 설치 상태 확인 ==="
echo ""
echo "Gateway:"
kubectl get gateway
echo ""
echo "HTTPRoute:"
kubectl get httproute
echo ""
echo "Pods:"
kubectl get pods
echo ""

echo -e "${GREEN}설치가 완료되었습니다!${NC}"
echo ""
echo "다음 명령어로 접속 가능합니다:"
echo "  1. /etc/hosts 설정: echo '127.0.0.1 demo.local' | sudo tee -a /etc/hosts"
echo "  2. 포트포워딩: kubectl port-forward -n envoy-gateway-system service/envoy-gateway-envoy-gateway 8080:80"
echo "  3. 접속 테스트: curl http://demo.local:8080"
