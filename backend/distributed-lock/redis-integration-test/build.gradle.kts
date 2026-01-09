plugins {
    id("buildlogic.java-library-conventions")
}

description = "Distributed lock Redis integration tests"

dependencies {
    testImplementation(
        project(":backend:distributed-lock:spring-boot-starter-redis"),
    )
    testImplementation(project(":backend:distributed-lock:test"))

    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.redisson)
    testImplementation("org.springframework.boot:spring-boot-test")
}
