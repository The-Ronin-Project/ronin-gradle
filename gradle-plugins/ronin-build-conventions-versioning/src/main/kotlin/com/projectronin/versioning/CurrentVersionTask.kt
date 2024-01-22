package com.projectronin.versioning

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class CurrentVersionTask : DefaultTask() {
    @get:Input
    abstract val quiet: Property<Boolean>

    init {
        description = "Outputs the project's current version"
        group = "help"
        quiet.convention(project.provider { project.hasProperty(QUIET_PROPERTY) })
    }

    @TaskAction
    fun printCurrentVersion() {
        if (quiet.get()) {
            logger.quiet(project.version.toString())
        } else {
            logger.quiet("\nProject version: ${project.version}")
        }
    }
}
