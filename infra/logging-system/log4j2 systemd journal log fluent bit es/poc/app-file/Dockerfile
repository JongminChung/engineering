FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        systemd \
        systemd-sysv \
        ca-certificates \
        curl \
        openjdk-17-jdk-headless \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /opt/log4j2-app/lib /opt/log4j2-app/conf /opt/log4j2-app/src /opt/log4j2-app/classes /var/log/log4j2-app

RUN curl -s -L -o /opt/log4j2-app/lib/log4j-api.jar https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-api/2.22.1/log4j-api-2.22.1.jar \
    && curl -s -L -o /opt/log4j2-app/lib/log4j-core.jar https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-core/2.22.1/log4j-core-2.22.1.jar

COPY src/Main.java /opt/log4j2-app/src/Main.java
COPY conf/log4j2.xml /opt/log4j2-app/conf/log4j2.xml
COPY log4j2-app.service /etc/systemd/system/log4j2-app.service

RUN javac -d /opt/log4j2-app/classes -cp "/opt/log4j2-app/lib/*" /opt/log4j2-app/src/Main.java \
    && ln -s /etc/systemd/system/log4j2-app.service /etc/systemd/system/multi-user.target.wants/log4j2-app.service

STOPSIGNAL SIGRTMIN+3

CMD ["/sbin/init"]
