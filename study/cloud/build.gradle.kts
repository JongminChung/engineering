
plugins {
    id("buildlogic.spring-boot-conventions")
    id("buildlogic.kafka-study-conventions")
    id("buildlogic.mysql-study-conventions")
}

description = "Modulith Cloud"

extra["springModulithVersion"] = "2.0.1"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    // This library has JavaTimeModule
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation("org.springframework.modulith:spring-modulith-starter-core")

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
