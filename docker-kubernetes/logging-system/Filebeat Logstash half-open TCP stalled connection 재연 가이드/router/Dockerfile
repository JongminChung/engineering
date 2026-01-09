FROM alpine:3.19
RUN apk add --no-cache iproute2 iptables conntrack-tools
COPY entrypoint.sh /router/entrypoint.sh
RUN chmod +x /router/entrypoint.sh
CMD ["/router/entrypoint.sh"]
