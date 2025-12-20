#!/usr/bin/env bash
set -euo pipefail

: "${NAMESPACE:=mysql}"
: "${CLUSTER_NAME:=mycluster}"
: "${MYSQL_ROOT_PASSWORD:=REPLACE_ME_STRONG_PASSWORD}"

echo "[1/3] create namespace: $NAMESPACE"
kubectl get ns "$NAMESPACE" >/dev/null 2>&1 || kubectl create ns "$NAMESPACE"

echo "[2/3] create root secret: ${CLUSTER_NAME}-root"
kubectl -n "$NAMESPACE" delete secret "${CLUSTER_NAME}-root" >/dev/null 2>&1 || true
kubectl -n "$NAMESPACE" create secret generic "${CLUSTER_NAME}-root" \
  --from-literal=rootPassword="${MYSQL_ROOT_PASSWORD}"

echo "[3/3] done"
kubectl -n "$NAMESPACE" get secret "${CLUSTER_NAME}-root"
