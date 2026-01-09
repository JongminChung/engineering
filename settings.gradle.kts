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
    "study:coding-test",
    "study:cloud",
    "study:grpc",
)

include(
    "distributed-lock:core",
    "distributed-lock:provider-redis",
    "distributed-lock:provider-jdbc",
    "distributed-lock:spring",
    "distributed-lock:spring-boot-autoconfigure",
    "distributed-lock:spring-boot-starter",
    "distributed-lock:spring-boot-starter-redis",
    "distributed-lock:spring-boot-starter-jdbc",
    "distributed-lock:dependencies",
    "distributed-lock:test",
    "distributed-lock:redis-integration-test",
    "distributed-lock:jdbc-integration-test"
)

buildCache {
    local {
        directory = file("${rootDir}/.gradle/build-cache")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()
    }
}
