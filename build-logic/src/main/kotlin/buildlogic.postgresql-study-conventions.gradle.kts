plugins {
    id("buildlogic.testcontainers-conventions")
}

dependencies {
    // PostgreSQL Driver
    runtimeOnly(libs.findLibrary("postgresql").get())

    // Testcontainers PostgreSQL Module
    testImplementation(libs.findLibrary("testcontainers-postgresql").get())

    // Flyway
    testImplementation("org.springframework.boot:spring-boot-starter-flyway")
}
