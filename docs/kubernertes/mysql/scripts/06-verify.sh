#!/usr/bin/env bash
set -euo pipefail

: "${NAMESPACE:=mysql}"
: "${CLUSTER_NAME:=mycluster}"
: "${ROUTER_SVC_NAME:=mycluster-router}"

echo "[1/6] innodbcluster"
kubectl -n "$NAMESPACE" get innodbcluster "$CLUSTER_NAME" -o wide || true

echo "[2/6] pods"
kubectl -n "$NAMESPACE" get pods -o wide

echo "[3/6] router service ports"
kubectl -n "$NAMESPACE" get svc "$ROUTER_SVC_NAME" -o jsonpath='{range .spec.ports[*]}{.name}={.port}{"\n"}{end}'

echo "[4/6] endpoints"
kubectl -n "$NAMESPACE" get endpoints "$ROUTER_SVC_NAME" -o wide || true

echo "[5/6] show router pod labels (selector 검증용)"
kubectl -n "$NAMESPACE" get pods --show-labels | grep -E "router|mysqlrouter|${CLUSTER_NAME}" || true

echo "[6/6] done"
