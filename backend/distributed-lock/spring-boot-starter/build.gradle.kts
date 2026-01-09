plugins {
    id("buildlogic.java-library-conventions")
}

description = "Distributed lock Spring Boot starter"

dependencies {
    api(
        platform(
            "org.springframework.boot:spring-boot-dependencies:${libs.versions.springBoot.get()}",
        ),
    )

    api(project(":backend:distributed-lock:spring-boot-autoconfigure"))
    api(project(":backend:distributed-lock:provider-redis"))

    api("org.springframework.boot:spring-boot-starter")

    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation(libs.redisson)
}
