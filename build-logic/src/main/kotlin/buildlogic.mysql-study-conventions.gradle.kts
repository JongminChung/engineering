plugins {
    id("buildlogic.testcontainers-conventions")
}

dependencies {
    // PostgreSQL Driver
    runtimeOnly(rootProjectLibs.findLibrary("mysql").get())

    // Testcontainers PostgreSQL Module
    testImplementation(rootProjectLibs.findLibrary("testcontainers-kafka").get())

    // Flyway
    testImplementation("org.springframework.boot:spring-boot-starter-flyway")
    testImplementation("org.flywaydb:flyway-database-postgresql")
}
