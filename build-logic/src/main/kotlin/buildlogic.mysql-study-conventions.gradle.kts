plugins {
    id("buildlogic.testcontainers-conventions")
}

dependencies {
    // PostgreSQL Driver
    runtimeOnly(rootProjectLibs.findLibrary("mysql").get())

    testImplementation(rootProjectLibs.findLibrary("testcontainers-mysql").get())

    // Flyway
    testImplementation("org.springframework.boot:spring-boot-starter-flyway")
    testImplementation("org.flywaydb:flyway-mysql")
}
