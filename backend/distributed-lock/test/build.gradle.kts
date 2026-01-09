plugins {
    id("buildlogic.java-library-conventions")
}

description = "Distributed lock test utilities"

dependencies {
    api(project(":backend:distributed-lock:core"))
    api(platform(libs.testcontainers.bom))

    api(libs.assertj.core)
    api(libs.testcontainers.core)
}
