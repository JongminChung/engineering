#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_DIR="${ROOT_DIR}/full-stack-application"
SERVER_LOG="/tmp/smithy-server.log"
APP_LOG="/tmp/smithy-app.log"
SERVER_PID=""
APP_PID=""

cleanup() {
  if [[ -n "${APP_PID}" ]]; then
    kill "${APP_PID}" 2>/dev/null || true
  fi
  if [[ -n "${SERVER_PID}" ]]; then
    kill "${SERVER_PID}" 2>/dev/null || true
  fi
}

trap cleanup EXIT

if [[ ! -d "${APP_DIR}" ]]; then
  echo "full-stack-application 디렉터리가 없습니다: ${APP_DIR}"
  echo "먼저 'smithy init -t full-stack-application'을 실행하세요."
  exit 1
fi

cd "${APP_DIR}"

echo "==> smithy build + codegen postprocess"
make build-smithy

echo "==> build server SDK + server"
make build-server

echo "==> build client SDK"
make build-client

echo "==> build app"
make build-app

echo "==> start server"
node server/dist/index.js > "${SERVER_LOG}" 2>&1 &
SERVER_PID=$!
sleep 1

echo "==> verify /menu"
curl -s http://127.0.0.1:3001/menu > /dev/null

echo "==> verify client calls"
cd "${APP_DIR}/client"
node -e "const { CoffeeShop } = require('@com.example/coffee-shop-client'); \
const client = new CoffeeShop({ endpoint: { protocol: 'http', hostname: '127.0.0.1', port: 3001, path: '/' } }); \
client.getMenu().then(() => client.createOrder({ coffeeType: 'DRIP' })) \
.then(res => client.getOrder({ id: res.id })) \
.then(() => process.exit(0)) \
.catch(err => { console.error(err); process.exit(1); });"
cd "${APP_DIR}"

echo "==> start app"
cd "${APP_DIR}/app"
pnpm start > "${APP_LOG}" 2>&1 &
APP_PID=$!
sleep 2

echo "==> verify app"
curl -s http://127.0.0.1:3000 > /dev/null

echo "==> cleanup"
kill "${APP_PID}" "${SERVER_PID}"

echo "PoC 완료"
