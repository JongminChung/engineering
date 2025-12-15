@file:Suppress("UnstableApiUsage")

plugins {
    java

    id("com.diffplug.spotless")
    id("io.github.jongminchung.spotless.convention")
}

repositories {
    mavenCentral()
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter(libs.findVersion("junit").get().requiredVersion)

            dependencies {
                implementation(libs.findLibrary("assertj-core").get())
            }

            // Ensure every Spring test task activates the "test" profile
            targets.all {
                testTask.configure {
                    systemProperty("spring.profiles.active", "test")
                }
            }
        }
    }
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.findVersion("java").get().requiredVersion.toInt())
    }
}
