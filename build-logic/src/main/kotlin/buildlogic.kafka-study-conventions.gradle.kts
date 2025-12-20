plugins {
    id("buildlogic.testcontainers-conventions")

    `java-library`
}

dependencies {
    implementation(rootProjectLibs.findLibrary("spring-kafka").get())

    testImplementation(rootProjectLibs.findLibrary("testcontainers-kafka").get())
    testImplementation(rootProjectLibs.findLibrary("awaitility").get())
}

// 테스트 설정
tasks.test {
    useJUnitPlatform()

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
