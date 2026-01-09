#!/bin/sh


# runner-register.sh /Users/jongminchung/gitlab-runner/config.toml

IMAGE="gitlab/gitlab-runner:latest"
CONFIG_FILE=$1

if [ -z "$CONFIG_FILE" ]; then
  echo "Usage: $0 <CONFIG_FILE_PATH>"
  exit 1
fi

docker run --rm -it \
  -v "$CONFIG_FILE:/etc/gitlab-runner/config.toml" \
  "$IMAGE" \
  register

echo "✔ GitLab Runner register successfully"
