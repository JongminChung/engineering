import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    id("buildlogic.spring-boot-conventions")
    id("buildlogic.kafka-study-conventions")
    id("buildlogic.postgresql-study-conventions")
}

val otelAgent by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}
val otelExtension by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

extra["springModulithVersion"] = "2.0.1"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    // This library has JavaTimeModule
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation("org.springframework.modulith:spring-modulith-starter-core")

    // H2 database for testing
    runtimeOnly("com.h2database:h2")

    // OpenTelemetry Java Agent and Extension
    otelAgent(libs.opentelemetry.javaagent)

    otelExtension(project(path = ":backend:otel-extension", configuration = "extensionJarElements"))

    testImplementation(
        "org.springframework.boot:spring-boot-starter-webmvc-test",
    )
    testImplementation(
        "org.springframework.boot:spring-boot-starter-security-test",
    )
    testImplementation(
        "org.springframework.boot:spring-boot-starter-validation-test",
    )
    testImplementation(
        "org.springframework.boot:spring-boot-starter-data-jpa-test",
    )

    testImplementation(
        "org.springframework.modulith:spring-modulith-starter-test",
    )
}

dependencyManagement {
    imports {
        mavenBom(
            "org.springframework.modulith:spring-modulith-bom:${property(
                "springModulithVersion",
            )}",
        )
    }
}

tasks.named<BootRun>("bootRun") {
    dependsOn(otelAgent, otelExtension)

    doFirst {
        val agentJar = otelAgent.singleFile
        val extensionJar = otelExtension.singleFile

        jvmArgs(
            "-javaagent:${agentJar.absolutePath}",
            "-Dotel.javaagent.extensions=${extensionJar.absolutePath}",
        )
    }

    environment("OTEL_SERVICE_NAME", "cloud-service")
    environment("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4317")
    environment("OTEL_EXPORTER_OTLP_PROTOCOL", "grpc")
}

/**
 * distributions {
 *   val main by getting {
 *       contents {
 *           from(otelAgent) {
 *               into("lib")
 *           }
 *       }
 *   }
 *}
 */
