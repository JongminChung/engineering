#!/bin/bash

set -e

echo "=== Envoy Gateway 테스트 스크립트 ==="
echo ""

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 1. /etc/hosts 확인
echo -e "${YELLOW}[1/4] /etc/hosts 설정 확인${NC}"
if grep -q "demo.local" /etc/hosts; then
    echo -e "${GREEN}✓ demo.local이 /etc/hosts에 설정되어 있습니다${NC}"
else
    echo -e "${RED}✗ demo.local이 /etc/hosts에 없습니다${NC}"
    echo "다음 명령어로 추가하세요:"
    echo "  echo '127.0.0.1 demo.local' | sudo tee -a /etc/hosts"
    exit 1
fi
echo ""

# 2. Gateway 상태 확인
echo -e "${YELLOW}[2/4] Gateway 상태 확인${NC}"
kubectl get gateway demo-gateway -o jsonpath='{.status.conditions[?(@.type=="Programmed")].status}' | grep -q "True" && \
    echo -e "${GREEN}✓ Gateway가 정상 작동 중입니다${NC}" || \
    echo -e "${RED}✗ Gateway가 준비되지 않았습니다${NC}"
echo ""

# 3. HTTPRoute 상태 확인
echo -e "${YELLOW}[3/4] HTTPRoute 상태 확인${NC}"
kubectl get httproute demo-route -o jsonpath='{.status.parents[0].conditions[?(@.type=="Accepted")].status}' | grep -q "True" && \
    echo -e "${GREEN}✓ HTTPRoute가 정상 설정되었습니다${NC}" || \
    echo -e "${RED}✗ HTTPRoute 설정에 문제가 있습니다${NC}"
echo ""

# 4. 포트포워딩 및 접속 테스트
echo -e "${YELLOW}[4/4] 접속 테스트${NC}"
echo "포트포워딩을 시작합니다..."
kubectl port-forward -n envoy-gateway-system service/envoy-gateway-envoy-gateway 8080:80 &
PF_PID=$!

# 포트포워딩이 준비될 때까지 대기
sleep 3

# HTTP 요청 테스트
echo ""
echo "테스트 요청 중..."
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://demo.local:8080)

if [ "$RESPONSE" = "200" ]; then
    echo -e "${GREEN}✓ 접속 테스트 성공! (HTTP $RESPONSE)${NC}"
    echo ""
    echo "실제 응답 내용:"
    curl -s http://demo.local:8080 | head -20
else
    echo -e "${RED}✗ 접속 테스트 실패 (HTTP $RESPONSE)${NC}"
fi

# 포트포워딩 종료
kill $PF_PID 2>/dev/null || true

echo ""
echo "=== 테스트 완료 ==="
echo ""
echo "수동으로 테스트하려면:"
echo "  1. 포트포워딩: kubectl port-forward -n envoy-gateway-system service/envoy-gateway-envoy-gateway 8080:80"
echo "  2. 접속: curl http://demo.local:8080"
