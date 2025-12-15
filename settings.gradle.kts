@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("build-logic")

    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "engineering"

include(
    "odata:odata-core",
    "odata:odata-spring",
)

include(
    "study:infra",
    "study:coding-test",
    "study:api-communication"
)

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()
    }
}
