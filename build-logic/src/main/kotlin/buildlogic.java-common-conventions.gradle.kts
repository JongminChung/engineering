@file:Suppress("UnstableApiUsage")

plugins {
    java
    id("com.diffplug.spotless")
    id("io.github.jongminchung.spotless.convention")
    id("buildlogic.java-test-conventions")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(rootProjectLibs.findLibrary("jspecify").get())
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(rootProjectLibs.findVersion("java").get().requiredVersion.toInt())
    }
}
