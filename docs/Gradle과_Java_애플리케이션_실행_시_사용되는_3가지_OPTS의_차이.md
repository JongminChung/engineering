# Insight

## Gradle과 Java 애플리케이션 실행 시 사용되는 3가지 OPTS의 차이

### DEFAULT_JVM_OPTS

- **목적**: 애플리케이션이 항상 필요로 하는 기본 JVM 설정
- **설정 위치**: `build.gradle.kts`의 application 블록
- **특징**:
    - 모든 환경에서 동일하게 적용
    - 개발자가 명시적으로 설정한 필수 옵션
- **예시**:
    - 메모리 옵션
    - GC 설정
    - 인코딩 등등

```kotlin
application {
    applicationDefaultJvmArgs = listOf(
        "-Dotel.service.name=gcloud-nc",
        "-Dotel.exporter.otlp.protocol=grpc",
    )
}
```

### JAVA_OPTS

- **목적**: 배포 환경별로 달라지는 JVM 옵션, 개발팀이 제어할 수 있음
- **설정 위치**: 환경변수 또는 실행 스크립트
- **특징**:
    - 환경에 따라 동적으로 설정
    - 인프라/배포 관련 설정
- **예시**:
    - '-javaagent:/usr/local/gcloud/nc/lib/opentelemetry-javaagent.jar' : path가 동적으로 바뀜

```bash
export JAVA_OPTS="-javaagent:/path/to/opentelemetry-javaagent.jar"
```

### NC_OPTS(애플리케이션 커스텀 옵션)

- **목적**: 엔드 유저, 즉 운영팀이 설정하고 개발팀이 제어할 수 없는 자바 설정들
- **설정 위치**: 환경변수
- **특징**:
    - 환경에 따라 동적으로 설정
    - 애플리케이션 특화된 설정
- **예시**:
    - 'Dotel.exporter.otlp.endpoint=http://172.30.2.7:4317'

## 구체적인 배포 예시

### 1. Java 순수 에이전트 (systemd service)

```ini
# /etc/systemd/system/nc-service.service
[Unit]
Description=NC Service
After=network.target

[Service]
Type=simple
User=ncuser
WorkingDirectory=/usr/local/gcloud/nc

# JAVA_OPTS - JVM 레벨 설정 (바이트코드 조작, JVM 동작)
Environment="JAVA_OPTS=-javaagent:/usr/local/gcloud/nc/lib/opentelemetry-javaagent.jar -XX:+UseG1GC -Xmx2g"

# NC_OPTS - 애플리케이션 레벨 설정 (시스템 프로퍼티)
Environment="NC_OPTS=-Dotel.exporter.otlp.endpoint=http://172.30.2.7:4317 -Dlog4j2.configurationFile=classpath:log4j2.xml,file:///usr/local/gcloud/nc/conf/log4j2.xml -Dspring.profiles.active=prod -Dapp.config.path=/usr/local/gcloud/nc/conf"

ExecStart=/usr/bin/java $JAVA_OPTS $NC_OPTS -jar /usr/local/gcloud/nc/app.jar

[Install]
WantedBy=multi-user.target
```

### 2. Docker + Spring Boot 배포

#### Dockerfile

```dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# OpenTelemetry Agent 복사
COPY --from=ghcr.io/open-telemetry/opentelemetry-java-instrumentation/agent:latest \
     /javaagent.jar /opt/opentelemetry-javaagent.jar

# 애플리케이션 JAR 복사
COPY build/libs/app.jar /app/app.jar

# 외부 설정 파일을 위한 디렉토리
RUN mkdir -p /app/config

# ENTRYPOINT에서 JAVA_OPTS와 NC_OPTS를 받을 수 있도록 설정
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS $NC_OPTS -jar /app/app.jar"]
```

#### docker-compose.yml (로컬 개발)

```yaml
version: "3.8"

services:
    app:
        build: .
        ports:
            - "8080:8080"
            - "5005:5005" # 디버그 포트
        environment:
            # JAVA_OPTS - JVM 레벨
            JAVA_OPTS: >-
                -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
                -XX:+UseContainerSupport
                -XX:MaxRAMPercentage=75.0

            # NC_OPTS - 애플리케이션 레벨
            NC_OPTS: >-
                -Dspring.profiles.active=local
                -Dlogging.level.root=DEBUG
                -Dapp.config.path=/app/config
        volumes:
            - ./config:/app/config
```

#### Kubernetes Deployment (운영 환경)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
    name: spring-boot-app
spec:
    replicas: 3
    template:
        spec:
            containers:
                - name: app
                  image: myregistry/spring-boot-app:latest
                  ports:
                      - containerPort: 8080
                  env:
                      # JAVA_OPTS - JVM 레벨 (인프라 관심사)
                      - name: JAVA_OPTS
                        value: >-
                            -javaagent:/opt/opentelemetry-javaagent.jar
                            -XX:+UseContainerSupport
                            -XX:MaxRAMPercentage=75.0
                            -XX:+HeapDumpOnOutOfMemoryError
                            -XX:HeapDumpPath=/tmp/heapdump.hprof

                      # NC_OPTS - 애플리케이션 레벨 (애플리케이션 설정)
                      - name: NC_OPTS
                        value: >-
                            -Dotel.exporter.otlp.endpoint=http://otel-collector.observability.svc.cluster.local:4317
                            -Dotel.service.name=spring-boot-app
                            -Dspring.profiles.active=prod
                            -Dlogging.level.root=INFO
                            -Dapp.config.path=/app/config

                  volumeMounts:
                      - name: config-volume
                        mountPath: /app/config

                  resources:
                      requests:
                          memory: "512Mi"
                          cpu: "500m"
                      limits:
                          memory: "2Gi"
                          cpu: "2000m"

            volumes:
                - name: config-volume
                  configMap:
                      name: app-config
```

#### helm values.yaml (운영 환경 - 더 유연한 방식)

```yaml
# values-prod.yaml
image:
    repository: myregistry/spring-boot-app
    tag: "1.0.0"

env:
    # JAVA_OPTS - JVM 레벨
    javaOpts:
        enabled: true
        value: |
            -javaagent:/opt/opentelemetry-javaagent.jar
            -XX:+UseContainerSupport
            -XX:MaxRAMPercentage=75.0
            -XX:+HeapDumpOnOutOfMemoryError

    # NC_OPTS - 애플리케이션 레벨
    ncOpts:
        enabled: true
        value: |
            -Dotel.exporter.otlp.endpoint=http://otel-collector.observability.svc.cluster.local:4317
            -Dotel.service.name=spring-boot-app
            -Dspring.profiles.active=prod
            -Dlogging.level.root=INFO

resources:
    limits:
        memory: 2Gi
        cpu: 2000m
    requests:
        memory: 512Mi
        cpu: 500m
```

### 핵심 정리

| 환경                | JAVA_OPTS                               | NC_OPTS                                                     |
| ------------------- | --------------------------------------- | ----------------------------------------------------------- |
| **Systemd Service** | `-javaagent`, JVM 메모리/GC 설정        | `-D` 시스템 프로퍼티 (엔드포인트, 설정 파일 경로, 프로파일) |
| **Docker 로컬**     | 디버거 설정, 컨테이너 메모리 설정       | 로컬 프로파일, 디버그 로그 레벨                             |
| **K8s 운영**        | JavaAgent, 힙덤프 설정, 컨테이너 메모리 | OTLP 엔드포인트, 운영 프로파일, 설정 경로                   |
