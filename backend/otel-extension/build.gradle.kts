plugins {
    id("buildlogic.java-library-conventions")
    alias(libs.plugins.shadow)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

val shadowElements by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

val extensionJarElements by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

dependencies {
    compileOnly(libs.opentelemetry.sdk)
    compileOnly(libs.opentelemetry.sdk.autoconfigure.spi)
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("all")

    // Include only our extension classes and META-INF/services
    // Exclude OpenTelemetry SDK classes (they're provided by the agent)
    dependencies {
        exclude(dependency("io.opentelemetry:.*"))
    }
}

tasks.named("build") {
    dependsOn("shadowJar")
}

artifacts {
    add(shadowElements.name, tasks.named("shadowJar"))
    add(extensionJarElements.name, tasks.named("jar"))
}
