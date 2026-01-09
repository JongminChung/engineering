#!/bin/sh
set -eu

mkdir -p /central

while true; do
  journalctl --directory=/var/log/journal --no-pager -o export --since "1 minute ago" >> /central/journal-export.log 2>/central/journal-export.err || true
  sleep 5
done
