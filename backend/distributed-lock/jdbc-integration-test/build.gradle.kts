plugins {
    id("buildlogic.java-library-conventions")
}

description = "Distributed lock JDBC integration tests"

dependencies {
    testImplementation(
        project(":backend:distributed-lock:spring-boot-starter-jdbc"),
    )
    testImplementation(project(":backend:distributed-lock:test"))

    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.postgresql)
    testImplementation("org.springframework.boot:spring-boot-test")
}
