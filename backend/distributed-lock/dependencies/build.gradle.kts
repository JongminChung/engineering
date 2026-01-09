plugins {
    `java-platform`
}

description = "Distributed lock dependency platform (BOM)"

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        api(project(":backend:distributed-lock:core"))
        api(project(":backend:distributed-lock:spring"))
        api(project(":backend:distributed-lock:spring-boot-autoconfigure"))
        api(project(":backend:distributed-lock:spring-boot-starter"))
        api(project(":backend:distributed-lock:spring-boot-starter-redis"))
        api(project(":backend:distributed-lock:spring-boot-starter-jdbc"))
        api(project(":backend:distributed-lock:provider-redis"))
        api(project(":backend:distributed-lock:provider-jdbc"))
        api(project(":backend:distributed-lock:test"))
    }
}
