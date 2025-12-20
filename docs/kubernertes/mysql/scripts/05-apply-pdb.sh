#!/usr/bin/env bash
set -euo pipefail

# 목적: 노드 드레인/계획 정비 시 DB Pod가 동시에 2개 내려가 quorum이 깨지는 것을 방지.

: "${NAMESPACE:=mysql}"
: "${MANIFEST:=manifests/pdb.yaml}"

if [[ ! -f "$MANIFEST" ]]; then
  echo "ERROR: manifest not found: $MANIFEST"
  exit 1
fi

echo "[1/2] apply: $MANIFEST"
kubectl apply -f "$MANIFEST"

echo "[2/2] verify pdb"
kubectl -n "$NAMESPACE" get pdb -o wide
