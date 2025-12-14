pluginManagement {
    includeBuild("build-logic")
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
