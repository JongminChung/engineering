plugins {
    id("buildlogic.testcontainers-conventions")
}

dependencies {
    // PostgreSQL Driver
    runtimeOnly(rootProjectLibs.findLibrary("postgresql").get())

    testImplementation(rootProjectLibs.findLibrary("testcontainers-postgresql").get())

    // Flyway
    testImplementation("org.springframework.boot:spring-boot-starter-flyway")
    testImplementation("org.flywaydb:flyway-database-postgresql")
}
