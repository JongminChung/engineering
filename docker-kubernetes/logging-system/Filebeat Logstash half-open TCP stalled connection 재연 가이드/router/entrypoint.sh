#!/bin/sh
set -e

logstash_ip=$(getent hosts logstash | awk '{print $1}')
if [ -z "$logstash_ip" ]; then
  echo "logstash IP not found" >&2
  exit 1
fi

iptables -P FORWARD ACCEPT
iptables -t nat -A PREROUTING -p tcp --dport 5044 -j DNAT --to-destination ${logstash_ip}:5044
iptables -t nat -A POSTROUTING -p tcp -d ${logstash_ip} --dport 5044 -j MASQUERADE
iptables -A FORWARD -p tcp -d ${logstash_ip} --dport 5044 -j ACCEPT
iptables -A FORWARD -p tcp -s ${logstash_ip} --sport 5044 -j ACCEPT

exec tail -f /dev/null
