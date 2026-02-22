import org.jetbrains.gradle.ext.settings
import org.jetbrains.gradle.ext.taskTriggers

plugins {
    java
    id("buildlogic.formatter-conventions")
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.3"
}

tasks.register("pnpmInstall", Exec::class.java) {
    group = "build"
    description = "Install dependencies using pnpm"

    inputs.file("package.json")
    inputs.file("pnpm-lock.yaml")
    commandLine = listOf("bash", "-lc", "pnpm install --frozen-lockfile")
    outputs.dir("node_modules")
}

idea.project.settings {
    taskTriggers {
//        afterSync(tasks.getByName("pnpmInstall"))
    }
}
