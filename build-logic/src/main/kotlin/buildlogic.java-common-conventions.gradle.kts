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
            useJUnitJupiter(rootProjectLibs.findVersion("junit").get().requiredVersion)

            dependencies {
                implementation(rootProjectLibs.findLibrary("assertj-core").get())
            }

            targets.all {
                testTask.configure {
                    systemProperty("spring.profiles.active", "test")
                }
            }
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(rootProjectLibs.findVersion("java").get().requiredVersion.toInt())
    }
}
