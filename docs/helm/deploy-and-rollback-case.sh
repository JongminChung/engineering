#!/bin/bash

# [에러 발생 시 즉시 중단, 정의되지 않은 변수 사용 시 에러, 파이프 에러 체크]
# - `-e`: 에러 나면 즉시 종료
# - `-u`: 선언 안 된 변수 사용 시 에러
# - `-o pipefail`: 파이프 중간 실패도 실패로 처리
set -euo pipefail


# [Helm Deployment Script]
# - `upgrade --install`: 최초 배포와 재배포를 동일한 커맨드로 처리하여 멱등성 유지
# - `--wait`: 모든 Pod와 Service 등 리소스가 Ready 상태가 될 때까지 대기
# - `--rollback-on-failure`: 배포 실패 시 자동으로 이전 버전으로 Rollback 수행
# - `--timeout`: 롤아웃이 완료될 때까지의 최대 대기 시간 설정

CLUSTER_NAME="${CLUSTER_NAME:-infra-verify-cluster}"
NAMESPACE="${NAMESPACE:-verify}"
TIMEOUT="${TIMEOUT:-900s}"

MYSQL_RELEASE="${MYSQL_RELEASE:-mysql}"
REDIS_RELEASE="${REDIS_RELEASE:-redis}"
KAFKA_RELEASE="${KAFKA_RELEASE:-kafka}"
KAFKA_HOSTNAME="${KAFKA_HOSTNAME:-$KAFKA_RELEASE}"
APP_NAME="${APP_NAME:-infra-checker}"

# Helm Chart
HELM_REPO_NAME="${HELM_REPO_NAME:-bitnami}"
HELM_REPO_URL="${HELM_REPO_URL:-https://charts.bitnami.com/bitnami}"
REDIS_CHART_NAME="${REDIS_CHART_NAME:-bitnami/redis}"

cleanup() {
  echo "[CLEANUP] kind cluster delete"
  kind delete cluster --name "$CLUSTER_NAME" >/dev/null 2>&1 || true
}
trap cleanup EXIT # BASH 스크립트가 어떤 이유로든 종료될 때 반드시 cleanup 함수를 실행하도록 등록

echo "[1/8] kind cluster create"
kind create cluster --name "$CLUSTER_NAME"

echo "[2/8] namespace create"
kubectl get ns "$NAMESPACE" >/dev/null 2>&1 || kubectl create ns "$NAMESPACE"

echo "[3/8] helm repo setup"
helm repo add "$HELM_REPO_NAME" "$HELM_REPO_URL" >/dev/null 2>&1 || true
helm repo update >/dev/null

echo "[4/8] mysql deploy (official image)"
MYSQL_ROOT_PASSWORD="$(
  set +o pipefail
  LC_ALL=C tr -dc 'a-z0-9' </dev/urandom | head -c 16
)"
cat <<EOF | kubectl -n "$NAMESPACE" apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${MYSQL_RELEASE}
  labels:
    app.kubernetes.io/instance: ${MYSQL_RELEASE}
    app.kubernetes.io/name: mysql
spec:
  replicas: 1
  selector:
    matchLabels:
      app.kubernetes.io/instance: ${MYSQL_RELEASE}
      app.kubernetes.io/name: mysql
  template:
    metadata:
      labels:
        app.kubernetes.io/instance: ${MYSQL_RELEASE}
        app.kubernetes.io/name: mysql
    spec:
      containers:
        - name: mysql
          image: mysql:8.0
          env:
            - name: MYSQL_ROOT_PASSWORD
              value: "${MYSQL_ROOT_PASSWORD}"
          ports:
            - containerPort: 3306
          readinessProbe:
            exec:
              command:
                - sh
                - -c
                - mysqladmin ping -uroot -p"$MYSQL_ROOT_PASSWORD"
            initialDelaySeconds: 10
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 12
---
apiVersion: v1
kind: Service
metadata:
  name: ${MYSQL_RELEASE}
  labels:
    app.kubernetes.io/instance: ${MYSQL_RELEASE}
    app.kubernetes.io/name: mysql
spec:
  selector:
    app.kubernetes.io/instance: ${MYSQL_RELEASE}
    app.kubernetes.io/name: mysql
  ports:
    - name: mysql
      port: 3306
      targetPort: 3306
EOF
kubectl -n "$NAMESPACE" rollout status deployment/"$MYSQL_RELEASE" --timeout="$TIMEOUT"

echo "[5/8] helm deploy redis"
helm upgrade --install "$REDIS_RELEASE" "$REDIS_CHART_NAME" \
  -n "$NAMESPACE" \
  --wait \
  --rollback-on-failure \
  --timeout "$TIMEOUT" \
  --set architecture=standalone \
  --set auth.enabled=false \
  --set master.persistence.enabled=false

echo "[6/8] kafka deploy (apache image)"
cat <<EOF | kubectl -n "$NAMESPACE" apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${KAFKA_RELEASE}
  labels:
    app.kubernetes.io/instance: ${KAFKA_RELEASE}
    app.kubernetes.io/name: kafka
spec:
  replicas: 1
  selector:
    matchLabels:
      app.kubernetes.io/instance: ${KAFKA_RELEASE}
      app.kubernetes.io/name: kafka
  template:
    metadata:
      labels:
        app.kubernetes.io/instance: ${KAFKA_RELEASE}
        app.kubernetes.io/name: kafka
    spec:
      containers:
        - name: kafka
          image: apache/kafka:3.7.0
          env:
            - name: KAFKA_HOSTNAME
              value: "${KAFKA_RELEASE}"
          ports:
            - containerPort: 9092
          command:
            - bash
            - -c
            - |
              set -e
              CONFIG=/opt/kafka/config/kraft/server.properties
              sed -i \
                -e "s|controller.quorum.voters=.*|controller.quorum.voters=1@127.0.0.1:9093|" \
                -e "s|advertised.listeners=.*|advertised.listeners=PLAINTEXT://\${KAFKA_HOSTNAME}:9092|" \
                -e "s|log.dirs=.*|log.dirs=/var/lib/kafka/data|" \
                "\$CONFIG"
              if [ ! -f /var/lib/kafka/data/meta.properties ]; then
                KAFKA_CLUSTER_ID="\$(/opt/kafka/bin/kafka-storage.sh random-uuid)"
                /opt/kafka/bin/kafka-storage.sh format -t "\$KAFKA_CLUSTER_ID" -c "\$CONFIG"
              fi
              exec /opt/kafka/bin/kafka-server-start.sh "\$CONFIG"
          readinessProbe:
            exec:
              command:
                - bash
                - -c
                - 'echo > /dev/tcp/127.0.0.1/9092'
            initialDelaySeconds: 10
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 12
          volumeMounts:
            - name: kafka-data
              mountPath: /var/lib/kafka/data
      volumes:
        - name: kafka-data
          emptyDir: {}
---
apiVersion: v1
kind: Service
metadata:
  name: ${KAFKA_RELEASE}
  labels:
    app.kubernetes.io/instance: ${KAFKA_RELEASE}
    app.kubernetes.io/name: kafka
spec:
  selector:
    app.kubernetes.io/instance: ${KAFKA_RELEASE}
    app.kubernetes.io/name: kafka
  ports:
    - name: kafka
      port: 9092
      targetPort: 9092
EOF
kubectl -n "$NAMESPACE" rollout status deployment/"$KAFKA_RELEASE" --timeout="$TIMEOUT"

MYSQL_SVC="$(kubectl -n "$NAMESPACE" get svc -l "app.kubernetes.io/instance=$MYSQL_RELEASE" -o jsonpath='{range .items[?(@.spec.ports[0].port==3306)]}{.metadata.name}{"\n"}{end}' | head -n 1)"
REDIS_SVC="$(kubectl -n "$NAMESPACE" get svc -l "app.kubernetes.io/instance=$REDIS_RELEASE" -o jsonpath='{range .items[?(@.spec.ports[0].port==6379)]}{.metadata.name}{"\n"}{end}' | head -n 1)"
KAFKA_SVC="$(kubectl -n "$NAMESPACE" get svc -l "app.kubernetes.io/instance=$KAFKA_RELEASE" -o jsonpath='{range .items[?(@.spec.ports[0].port==9092)]}{.metadata.name}{"\n"}{end}' | head -n 1)"

echo "[7/8] app deploy (mysql/redis/kafka 의존 체크 포함)"
cat <<EOF | kubectl -n "$NAMESPACE" apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${APP_NAME}
  labels:
    app.kubernetes.io/name: ${APP_NAME}
spec:
  replicas: 1
  selector:
    matchLabels:
      app.kubernetes.io/name: ${APP_NAME}
  template:
    metadata:
      labels:
        app.kubernetes.io/name: ${APP_NAME}
    spec:
      containers:
        - name: app
          image: busybox:1.36
          command:
            - sh
            - -c
            - |
              echo "[app] mysql/redis/kafka 준비 대기중..."
              for i in \$(seq 1 120); do
                nc -z -w 2 "\$MYSQL_HOST" "\$MYSQL_PORT" \
                  && nc -z -w 2 "\$REDIS_HOST" "\$REDIS_PORT" \
                  && nc -z -w 2 "\$KAFKA_HOST" "\$KAFKA_PORT" \
                  && echo "[app] 의존성 준비 완료" \
                  && exec sleep 3600
                sleep 2
              done
              echo "[app] 의존성 준비 실패"
              exit 1
          env:
            - name: MYSQL_HOST
              value: "${MYSQL_SVC}"
            - name: MYSQL_PORT
              value: "3306"
            - name: REDIS_HOST
              value: "${REDIS_SVC}"
            - name: REDIS_PORT
              value: "6379"
            - name: KAFKA_HOST
              value: "${KAFKA_SVC}"
            - name: KAFKA_PORT
              value: "9092"
          readinessProbe:
            exec:
              command:
                - sh
                - -c
                - >
                  nc -z -w 2 "\$MYSQL_HOST" "\$MYSQL_PORT" &&
                  nc -z -w 2 "\$REDIS_HOST" "\$REDIS_PORT" &&
                  nc -z -w 2 "\$KAFKA_HOST" "\$KAFKA_PORT"
            initialDelaySeconds: 3
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 12
EOF

echo "[8/8] readiness gate: app Ready condition must be true"
kubectl -n "$NAMESPACE" wait \
  --for=condition=Ready pod \
  -l "app.kubernetes.io/name=$APP_NAME" \
  --timeout="$TIMEOUT"

APP_POD_NAME="$(kubectl -n "$NAMESPACE" get pod -l "app.kubernetes.io/name=$APP_NAME" -o jsonpath='{.items[0].metadata.name}')"
kubectl -n "$NAMESPACE" describe pod "$APP_POD_NAME" | sed -n '1,200p'

echo "OK: kind + helm 배포 리허설 + mysql/redis/kafka 의존성 체크 통과"
