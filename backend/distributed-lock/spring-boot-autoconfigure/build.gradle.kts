plugins {
    id("buildlogic.java-library-conventions")
}

description = "Distributed lock Spring Boot autoconfiguration"

dependencies {
    api(
        platform(
            "org.springframework.boot:spring-boot-dependencies:${libs.versions.springBoot.get()}",
        ),
    )
    api(platform(libs.testcontainers.bom))

    api(project(":backend:distributed-lock:spring"))

    compileOnly(project(":backend:distributed-lock:provider-redis"))
    compileOnly(project(":backend:distributed-lock:provider-jdbc"))
    compileOnly(libs.redisson)

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor(
        platform(
            "org.springframework.boot:spring-boot-dependencies:${libs.versions.springBoot.get()}",
        ),
    )
    annotationProcessor(
        "org.springframework.boot:spring-boot-configuration-processor",
    )

    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation(project(":backend:distributed-lock:provider-redis"))
    testImplementation(project(":backend:distributed-lock:provider-jdbc"))
    testImplementation(libs.redisson)
}
