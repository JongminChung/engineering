FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        systemd \
    && rm -rf /var/lib/apt/lists/*

COPY export.sh /usr/local/bin/export.sh
RUN chmod +x /usr/local/bin/export.sh

CMD ["/usr/local/bin/export.sh"]
