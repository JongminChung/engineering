#!/usr/bin/env bash
set -euo pipefail

# InnoDBCluster의 Service는 MySQL Router로 연결되고, Router가 PRIMARY/SECONDARY에 따라 라우팅합니다.
# 여기서는 애플리케이션이 **“딱 6446(Primary 고정) / 6450(RW-splitting)”**만 보게 하는 커스텀 Service를 만듭니다(운영 계약).

: "${NAMESPACE:=mysql}"
: "${ROUTER_SVC_NAME:=mycluster-router}"
: "${MANIFEST:=manifests/router-service.yaml}"

if [[ ! -f "$MANIFEST" ]]; then
  echo "ERROR: manifest not found: $MANIFEST"
  exit 1
fi

echo "[1/2] apply: $MANIFEST"
kubectl apply -f "$MANIFEST"

echo "[2/2] verify service: $ROUTER_SVC_NAME"
kubectl -n "$NAMESPACE" get svc "$ROUTER_SVC_NAME" -o wide
kubectl -n "$NAMESPACE" describe svc "$ROUTER_SVC_NAME" | sed -n '1,140p'
