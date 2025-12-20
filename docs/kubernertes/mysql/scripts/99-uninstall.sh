#!/usr/bin/env bash
set -euo pipefail

: "${NAMESPACE:=mysql}"
: "${OPERATOR_NAMESPACE:=mysql-operator}"
: "${CLUSTER_NAME:=mycluster}"
: "${ROUTER_SVC_NAME:=mycluster-router}"

echo "[1/4] delete app router service/pdb/cluster"
kubectl -n "$NAMESPACE" delete svc "$ROUTER_SVC_NAME" >/dev/null 2>&1 || true
kubectl -n "$NAMESPACE" delete pdb --all >/dev/null 2>&1 || true
kubectl -n "$NAMESPACE" delete innodbcluster "$CLUSTER_NAME" >/dev/null 2>&1 || true

echo "[2/4] delete namespace (WARNING: will delete PVCs depending on reclaim policy)"
kubectl delete ns "$NAMESPACE" || true

echo "[3/4] uninstall operator"
helm -n "$OPERATOR_NAMESPACE" uninstall mysql-operator || true

echo "[4/4] done"
