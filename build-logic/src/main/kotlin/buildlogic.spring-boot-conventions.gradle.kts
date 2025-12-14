import gradle.kotlin.dsl.accessors._fbaadc48f4a28e86ca8ecab4e1793b69.annotationProcessor
import gradle.kotlin.dsl.accessors._fbaadc48f4a28e86ca8ecab4e1793b69.compileOnly
import gradle.kotlin.dsl.accessors._fbaadc48f4a28e86ca8ecab4e1793b69.implementation
import gradle.kotlin.dsl.accessors._fbaadc48f4a28e86ca8ecab4e1793b69.testAnnotationProcessor
import gradle.kotlin.dsl.accessors._fbaadc48f4a28e86ca8ecab4e1793b69.testCompileOnly
import gradle.kotlin.dsl.accessors._fbaadc48f4a28e86ca8ecab4e1793b69.testImplementation
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.invoke

plugins {
    id("buildlogic.java-common-conventions")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters (버전은 Spring Boot Plugin이 관리)
    implementation("org.springframework.boot:spring-boot-starter")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }

    compileOnly("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

