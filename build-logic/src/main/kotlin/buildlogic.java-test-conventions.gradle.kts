@file:Suppress("UnstableApiUsage")

plugins {
    java
    jacoco
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

        val integrationTest by registering(JvmTestSuite::class) {
            useJUnitJupiter(rootProjectLibs.findVersion("junit").get().requiredVersion)

            dependencies {
                implementation(project())
            }

            targets.all {
                testTask.configure {
                    systemProperty("spring.profiles.active", "integrationTest")
                }
            }
        }
    }
}

configurations {
    named<Configuration>("integrationTestImplementation").get().extendsFrom(configurations.testImplementation.get())
    named<Configuration>("integrationTestRuntimeOnly").configure {
        extendsFrom(configurations.testRuntimeOnly.get())
    }
    testImplementation.get().extendsFrom(compileOnly.get())
}

tasks.withType<Test>().configureEach {
    systemProperty("junit.jupiter.execution.parallel.enabled", true)
    systemProperty("junit.jupiter.execution.parallel.mode.default", "same_thread")
    systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
    systemProperty("junit.jupiter.execution.parallel.config.strategy", "dynamic")
}

plugins.withId("java") {
    val sourceSets = extensions.getByType<SourceSetContainer>()
    val mainSourceSet = sourceSets.named("main").get()

    val jacocoDir = layout.buildDirectory.dir("jacoco")
    val jacocoExt = extensions.getByType<JacocoPluginExtension>()

    fun execFileForName(testTaskName: String): Provider<RegularFile> {
        val execName = when (testTaskName) {
            "test" -> "jacocoTest.exec"
            "integrationTest" -> "jacocoIntegrationTest.exec"
            else -> "$testTaskName.exec"
        }
        return jacocoDir.map { it.file(execName) }
    }

    configurations.maybeCreate("jacocoCli")
    dependencies {
        add("jacocoCli", "org.jacoco:org.jacoco.cli:${jacocoExt.toolVersion}")
    }

    tasks.withType<Test>().configureEach {
        configure<JacocoTaskExtension> {
            isEnabled = true
            destinationFile = execFileForName(name).get().asFile
        }
    }

    val unitTestTask = tasks.named<Test>("test")

    val integrationTestTask = tasks.named<Test>("integrationTest")

    val jacocoMergeTask = tasks.register<JavaExec>("jacocoMergeTestExec") {
        dependsOn(unitTestTask, integrationTestTask)
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        val testExec = execFileForName("test")
        val integrationExec = execFileForName("integrationTest")
        val mergedExec = jacocoDir.map { it.file("jacocoMerged.exec") }

        classpath = configurations.getByName("jacocoCli")
        mainClass.set("org.jacoco.cli.internal.Main")

        inputs.files(testExec, integrationExec)
        outputs.file(mergedExec)

        onlyIf { testExec.get().asFile.exists() || integrationExec.get().asFile.exists() }

        doFirst {
            val execFiles = listOf(testExec.get().asFile, integrationExec.get().asFile)
                .filter { it.exists() }

            if (execFiles.isEmpty()) {
                logger.lifecycle("Jacoco merge skipped: no exec files found.")
                return@doFirst
            }

            val dest = mergedExec.get().asFile
            args = listOf("merge") + execFiles.map { it.absolutePath } + listOf("--destfile", dest.absolutePath)
        }
    }

    val jacocoTestReport = tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(unitTestTask)
        group = LifecycleBasePlugin.VERIFICATION_GROUP

        executionData(execFileForName("test"))
        sourceSets(mainSourceSet)

        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    tasks.named<Test>("test") {
        finalizedBy(jacocoTestReport)
    }

    val jacocoIntegrationTestReport = tasks.register<JacocoReport>("jacocoIntegrationTestReport") {
        dependsOn(integrationTestTask)
        group = LifecycleBasePlugin.VERIFICATION_GROUP

        executionData(execFileForName("integrationTest"))
        sourceSets(mainSourceSet)

        reports {
            xml.required.set(true)
            html.required.set(true)
            xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/integrationTest/jacocoTestReport.xml"))
            html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/integrationTest/html"))
        }
    }

    integrationTestTask.configure {
        finalizedBy(jacocoIntegrationTestReport)
    }

    tasks.register<JacocoReport>("jacocoMergedCoverageReport") {
        dependsOn(jacocoMergeTask)
        group = LifecycleBasePlugin.VERIFICATION_GROUP

        val mergedExec = jacocoDir.map { it.file("jacocoMerged.exec") }
        onlyIf { mergedExec.get().asFile.exists() }

        executionData(mergedExec)
        sourceSets(mainSourceSet)

        reports {
            xml.required.set(true)
            html.required.set(true)
            xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/merged/jacocoMergedCoverage.xml"))
            html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/merged/html"))
        }
    }
}
