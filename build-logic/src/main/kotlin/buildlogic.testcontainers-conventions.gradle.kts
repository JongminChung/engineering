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
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")

    testImplementation(platform(libs.findLibrary("testcontainers-bom").get()))

    testImplementation(libs.findBundle("testcontainers").get())

    // Spring Boot Test
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

// 테스트 설정
tasks.test {
    useJUnitPlatform()

    // TestContainers 최적 설정
    systemProperty("testcontainers.reuse.enable", "true")
    systemProperty("testcontainers.image.substitutor", "org.testcontainers.utility.ImageSubstitutor")

    maxParallelForks = 1 // testcontainers 1번만 기동을 위함 (JVM 1번)

    // 테스트 로깅
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }

    // 메모리 설정
    minHeapSize = "512m"
    maxHeapSize = "2g"

    // JUnit 병렬 실행
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "same_thread") // same_thread
    systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")

    systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
    systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "8")
}

// 테스트 리포트 설정
tasks.withType<Test> {
    reports {
        html.required.set(true)
        junitXml.required.set(true)
    }
}
