/*
 * OData 라이브러리 공통 컨벤션 플러그인
 * - Apache Olingo OData v4 의존성
 * - JSON 직렬화(Jackson), 로깅(SLF4J/Logback)
 * - 테스트(JUnit Jupiter) 기본 설정
 */

plugins {
    id("buildlogic.java-common-conventions")
    `java-library`
}

// 버전 관리
val olingoVersion = "4.9.0" // Apache Olingo v4 최신 안정판 기준
val jacksonVersion = "2.17.2"

dependencies {
    // OData (Apache Olingo v4)
    api("org.apache.olingo:odata-client-core:$olingoVersion")
    api("org.apache.olingo:odata-commons-core:$olingoVersion")

    // 선택: 서버 기능 확장 시 사용 (현재는 주석)
    // api("org.apache.olingo:odata-server-core:$olingoVersion")

    // JSON 직렬화
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.16")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.12")
}

// 테스트 설정 강화 (공통 java-common-conventions 를 기반으로 추가 조정 가능)
tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
