plugins {
    id("buildlogic.java-library-conventions")
}

description = "Distributed lock JDBC provider"

dependencies {
    api(project(":backend:distributed-lock:core"))

    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.postgresql)
}
