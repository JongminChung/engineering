plugins {
    id("buildlogic.odata-library-conventions")
}

description = "OData Spring integration: Web MVC converters and config"

dependencies {
    api(project(":odata:odata-core"))
    implementation("org.springframework:spring-webmvc:6.1.14")
}
