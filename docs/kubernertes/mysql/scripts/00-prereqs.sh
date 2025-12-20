#!/usr/bin/env bash
set -euo pipefail

: "${NAMESPACE:=mysql}"
: "${CLUSTER_NAME:=mycluster}"
: "${STORAGE_CLASS:=REPLACE_ME_STORAGECLASS}"

command -v kubectl >/dev/null 2>&1 || { echo "kubectl not found"; exit 1; }
command -v helm >/dev/null 2>&1 || { echo "helm not found"; exit 1; }

echo "[1/4] kubectl context"
kubectl config current-context

echo "[2/4] cluster connectivity"
kubectl get nodes -o wide

echo "[3/4] storageclass exists? ($STORAGE_CLASS)"
kubectl get storageclass "$STORAGE_CLASS" >/dev/null 2>&1 || {
  echo "ERROR: StorageClass '$STORAGE_CLASS' not found. Set STORAGE_CLASS properly."
  kubectl get storageclass
  exit 1
}

echo "[4/4] ok"
