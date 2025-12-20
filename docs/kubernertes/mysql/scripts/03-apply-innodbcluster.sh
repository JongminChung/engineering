#!/usr/bin/env bash
set -euo pipefail

# InnoDBCluster는 Operator가 MySQL InnoDB Cluster의 라이프사이클(설치/유지/백업 등)을 관리하도록 하는 핵심 CR입니다.
# https://dev.mysql.com/doc/mysql-operator/en/

: "${NAMESPACE:=mysql}"
: "${CLUSTER_NAME:=mycluster}"
: "${MANIFEST:=manifests/innodbcluster.yaml}"

if [[ ! -f "$MANIFEST" ]]; then
  echo "ERROR: manifest not found: $MANIFEST"
  exit 1
fi

echo "[1/3] apply: $MANIFEST"
kubectl apply -f "$MANIFEST"

echo "[2/3] wait for InnoDBCluster resource to appear"
kubectl -n "$NAMESPACE" get innodbcluster "$CLUSTER_NAME" >/dev/null 2>&1 || {
  echo "WARN: InnoDBCluster/$CLUSTER_NAME not visible yet (CRD/operator not ready?)"
}

echo "[3/3] show current objects"
kubectl -n "$NAMESPACE" get innodbcluster "$CLUSTER_NAME" -o wide || true
kubectl -n "$NAMESPACE" get pods -o wide
