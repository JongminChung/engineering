import com.google.protobuf.gradle.id

plugins {
    id("buildlogic.spring-boot-conventions")
    id("com.google.protobuf") version "0.9.5"
}

description = "API communication patterns (REST & gRPC) study module"

extra["springGrpcVersion"] = "1.0.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("io.grpc:grpc-services")
    implementation(
        "org.springframework.grpc:spring-grpc-server-web-spring-boot-starter",
    )

    testImplementation("org.springframework.grpc:spring-grpc-test")

    testImplementation(
        "org.springframework.boot:spring-boot-starter-webmvc-test",
    )
    testImplementation(
        "org.springframework.boot:spring-boot-starter-security-test",
    )
    testImplementation(
        "org.springframework.boot:spring-boot-starter-validation-test",
    )
}

dependencyManagement {
    imports {
        mavenBom(
            "org.springframework.grpc:spring-grpc-dependencies:${property(
                "springGrpcVersion",
            )}",
        )
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc") {
                    option("@generated=omit")
                }
            }
        }
    }
}
