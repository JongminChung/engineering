#!/usr/bin/env bash
set -euo pipefail

# 클러스터 내부에서 mysql 클라이언트를 띄워 6446/6450 접속만 확인합니다.
# (운영망에서는 NetworkPolicy로 접근을 제한하는 것이 일반적입니다.)

: "${NAMESPACE:=mysql}"
: "${ROUTER_SVC_NAME:=mycluster-router}"
: "${MYSQL_ROOT_PASSWORD:=REPLACE_ME_STRONG_PASSWORD}"

echo "[1/3] primary port(6446) test"
kubectl -n "$NAMESPACE" run mysql-client-primary --rm -it --restart=Never \
  --image=mysql:8.4 \
  --env="MYSQL_PWD=${MYSQL_ROOT_PASSWORD}" \
  --command -- bash -lc \
  "mysql -h ${ROUTER_SVC_NAME} -P 6446 -u root -e 'select @@hostname, @@read_only, now();'"

echo "[2/3] rw-split port(6450) test"
kubectl -n "$NAMESPACE" run mysql-client-split --rm -it --restart=Never \
  --image=mysql:8.4 \
  --env="MYSQL_PWD=${MYSQL_ROOT_PASSWORD}" \
  --command -- bash -lc \
  "mysql -h ${ROUTER_SVC_NAME} -P 6450 -u root -e 'select @@hostname, @@read_only, now();'"

echo "[3/3] done"
