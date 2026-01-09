#!/bin/sh

CONTAINER_NAME="gitlab-runner"
IMAGE="gitlab/gitlab-runner:latest"
CONFIG_FILE=$1
DOCKER_SOCK="/var/run/docker.sock"

if [ -z "$CONFIG_FILE" ]; then
  echo "Usage: $0 <CONFIG_FILE_PATH>"
  exit 1
fi

docker run -d \
  --name "$CONTAINER_NAME" \
  --restart always \
  -v "$CONFIG_FILE:/etc/gitlab-runner/config.toml" \
  -v "$DOCKER_SOCK:$DOCKER_SOCK" \
  "$IMAGE"

echo "✔ GitLab Runner started successfully"
