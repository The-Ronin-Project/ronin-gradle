package com.projectronin.versioning

import com.projectronin.gradle.helpers.roninProjectVersion
import org.gradle.api.Plugin
import org.gradle.api.Project

internal const val QUIET_PROPERTY = "release.quiet"

class VersioningPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val roninProjectVersion = target.roninProjectVersion
        val computedVersion = roninProjectVersion.serviceVersion ?: roninProjectVersion.tagBasedVersion

        target.version = computedVersion

        if (!target.hasProperty(QUIET_PROPERTY)) {
            target.logger.quiet("Setting project version to '$computedVersion'")
        }

        target.subprojects.forEach { subProject ->
            subProject.version = computedVersion
        }

        target.tasks.register("currentVersion", CurrentVersionTask::class.java)
    }
}
