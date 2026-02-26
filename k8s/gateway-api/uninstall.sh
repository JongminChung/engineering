#!/bin/bash

set -e

echo "=== Envoy Gateway + Helm 제거 스크립트 ==="
echo ""

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. Demo 애플리케이션 제거
echo -e "${YELLOW}[1/3] Demo 애플리케이션 제거 중...${NC}"
helm uninstall demo-gateway || echo "demo-gateway가 이미 제거되었거나 존재하지 않습니다."
echo -e "${GREEN}✓ Demo 애플리케이션 제거 완료${NC}"
echo ""

# 2. Envoy Gateway 제거
echo -e "${YELLOW}[2/3] Envoy Gateway 제거 중...${NC}"
helm uninstall eg -n envoy-gateway-system || echo "Envoy Gateway가 이미 제거되었거나 존재하지 않습니다."
kubectl delete namespace envoy-gateway-system || echo "Namespace가 이미 제거되었습니다."
echo -e "${GREEN}✓ Envoy Gateway 제거 완료${NC}"
echo ""

# 3. Gateway API CRDs 제거 (선택사항)
echo -e "${YELLOW}[3/3] Gateway API CRDs 제거 여부${NC}"
read -p "Gateway API CRDs를 제거하시겠습니까? (y/N): " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]
then
    kubectl delete -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.0.0/standard-install.yaml || echo "CRDs 제거 실패"
    echo -e "${GREEN}✓ Gateway API CRDs 제거 완료${NC}"
else
    echo "Gateway API CRDs는 유지됩니다."
fi
echo ""

echo -e "${GREEN}제거가 완료되었습니다!${NC}"
