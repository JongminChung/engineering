/*
 * Kafka 학습용 컨벤션 플러그인
 * - Kafka Client 및 Spring Kafka 의존성
 * - TestContainers를 이용한 통합 테스트 환경
 * - JSON 직렬화 및 로깅 설정
 */

plugins {
    id("buildlogic.java-common-conventions")
    `java-library`
}

// Kafka 관련 버전 관리
val kafkaVersion = "3.6.1"
val springKafkaVersion = "3.1.1"
val testcontainersVersion = "1.19.3"
val jacksonVersion = "2.16.0"
val awaitilityVersion = "4.2.0"

dependencies {
    // Kafka Client
    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")

    // Spring Kafka (Optional)
    implementation("org.springframework.kafka:spring-kafka:$springKafkaVersion")

    // JSON 직렬화
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    runtimeOnly("ch.qos.logback:logback-classic:1.4.14")

    // TestContainers
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.testcontainers:kafka:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")

    // Test Utilities
    testImplementation("org.awaitility:awaitility:$awaitilityVersion")
}

// 테스트 설정
tasks.test {
    useJUnitPlatform()

    // TestContainers를 위한 시스템 속성
    systemProperty("testcontainers.reuse.enable", "false")

    // 테스트 로그 출력
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// Docker Compose 태스크 정의
tasks.register("dockerComposeUp", Exec::class) {
    group = "kafka"
    description = "Start Kafka cluster with Docker Compose"
    workingDir = projectDir
    commandLine("docker-compose", "-f", "docker-compose.kafka.yml", "up", "-d")
}

tasks.register("dockerComposeDown", Exec::class) {
    group = "kafka"
    description = "Stop Kafka cluster"
    workingDir = projectDir
    commandLine("docker-compose", "-f", "docker-compose.kafka.yml", "down")
}

tasks.register("dockerComposeDownVolumes", Exec::class) {
    group = "kafka"
    description = "Stop Kafka cluster and remove volumes"
    workingDir = projectDir
    commandLine("docker-compose", "-f", "docker-compose.kafka.yml", "down", "-v")
}

tasks.register("dockerComposeLogs", Exec::class) {
    group = "kafka"
    description = "Show Kafka logs"
    workingDir = projectDir
    commandLine("docker-compose", "-f", "docker-compose.kafka.yml", "logs", "-f")
}

tasks.register<Exec>("kafkaUI") {
    group = "kafka"
    description = "Open Kafka UI in browser"
    doFirst {
        val os = System.getProperty("os.name").lowercase()
        val cmd = when {
            os.contains("mac") -> listOf("open", "http://localhost:8080")
            os.contains("nix") || os.contains("nux") -> listOf("xdg-open", "http://localhost:8080")
            os.contains("win") -> listOf("cmd", "/c", "start", "http://localhost:8080")
            else -> throw GradleException("Unsupported OS: $os")
        }
        commandLine = cmd
    }
}
