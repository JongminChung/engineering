plugins {
    `java-library`

    id("buildlogic.java-common-conventions")
}

dependencies {
    implementation(rootProjectLibs.findLibrary("slf4j-api").get())
    runtimeOnly(rootProjectLibs.findLibrary("logback-classic").get())

    compileOnly(rootProjectLibs.findLibrary("lombok").get())
    annotationProcessor(rootProjectLibs.findLibrary("lombok").get())
    testCompileOnly(rootProjectLibs.findLibrary("lombok").get())
    testAnnotationProcessor(rootProjectLibs.findLibrary("lombok").get())
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}
