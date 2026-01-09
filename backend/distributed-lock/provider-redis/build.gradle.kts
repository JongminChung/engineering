plugins {
    id("buildlogic.java-library-conventions")
}

description = "Distributed lock Redis provider"

dependencies {
    api(project(":backend:distributed-lock:core"))

    implementation(libs.redisson)

    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers.core)
}
