plugins {
    id("buildlogic.java-library-conventions")
}

description = "Distributed lock Spring integration: AOP, SpEL, template"

dependencies {
    api(
        platform(
            "org.springframework.boot:spring-boot-dependencies:${libs.versions.springBoot.get()}",
        ),
    )
    api(project(":backend:distributed-lock:core"))

    implementation("org.aspectj:aspectjrt")
    implementation("org.aspectj:aspectjweaver")
    implementation("org.springframework:spring-aop")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-expression")

    testImplementation(project(":backend:distributed-lock:test"))
}
