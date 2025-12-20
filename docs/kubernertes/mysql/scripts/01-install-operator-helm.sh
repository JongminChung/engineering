#!/usr/bin/env bash
set -euo pipefail

# https://dev.mysql.com/doc/mysql-operator/en/mysql-operator-installation-helm.html

: "${OPERATOR_NAMESPACE:=mysql-operator}"

echo "[1/4] create operator namespace: $OPERATOR_NAMESPACE"
kubectl get ns "$OPERATOR_NAMESPACE" >/dev/null 2>&1 || kubectl create ns "$OPERATOR_NAMESPACE"

echo "[2/4] add helm repo (Artifact Hub 기준 mysql-operator 차트)"
helm repo add mysql-operator https://mysql.github.io/mysql-operator/ 2>/dev/null || true
helm repo update

echo "[3/4] install/upgrade operator"
helm upgrade --install mysql-operator mysql-operator/mysql-operator \
  -n "$OPERATOR_NAMESPACE" \
  --wait

echo "[4/4] verify"
kubectl -n "$OPERATOR_NAMESPACE" get deploy,pods
