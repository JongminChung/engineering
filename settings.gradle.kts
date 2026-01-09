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
    "backend:cloud",
    "backend:grpc",
)

include(
    "backend:distributed-lock:core",
    "backend:distributed-lock:provider-redis",
    "backend:distributed-lock:provider-jdbc",
    "backend:distributed-lock:spring",
    "backend:distributed-lock:spring-boot-autoconfigure",
    "backend:distributed-lock:spring-boot-starter",
    "backend:distributed-lock:spring-boot-starter-redis",
    "backend:distributed-lock:spring-boot-starter-jdbc",
    "backend:distributed-lock:dependencies",
    "backend:distributed-lock:test",
    "backend:distributed-lock:redis-integration-test",
    "backend:distributed-lock:jdbc-integration-test"
)

include(
    "coding-test"
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
