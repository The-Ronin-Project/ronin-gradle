package com.projectronin.versioning

import com.projectronin.gradle.helpers.applyPlugin
import com.projectronin.roninbuildconventionsversioning.DependencyHelper
import org.gradle.api.Plugin
import org.gradle.api.Project
import pl.allegro.tech.build.axion.release.domain.VersionConfig

class VersioningPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target
            .applyPlugin(DependencyHelper.Plugins.axionRelease.id)

        val computedVersion: String = when (val serviceVersion: String? = target.properties.getOrDefault("service-version", System.getenv("SERVICE_VERSION"))?.toString()?.takeIf { it.isNotBlank() }) {
            null -> {
                val scmVersion = target.extensions.getByType(VersionConfig::class.java)

                scmVersion.apply {
                    tag.apply {
                        initialVersion { _, _ -> "1.0.0" }
                        prefix.set("")
                    }
                    versionCreator { versionFromTag, position ->
                        val branchName = System.getenv("REF_NAME")?.ifBlank { null } ?: position.branch
                        val supportedHeads = setOf("master", "main")
                        // this code ensures that we get a labeled version for anything that's not master, main, or version/v<NUMBER> or v<NUMBER>.<NUMBER>.<NUMBER>,
                        // but that we get a PLAIN version for  master, main, or version/v<NUMBER> or v<NUMBER>.<NUMBER>.<NUMBER>
                        // The jiraBranchRegex tries to identify a ticket project-<NUMBER> format and uses that as the label
                        if (!supportedHeads.contains(branchName) && !branchName.matches("^version/v\\d+$".toRegex()) && !branchName.matches("^v\\d+\\.\\d+\\.\\d+$".toRegex())) {
                            val jiraBranchRegex = Regex("(?:.*/)?(\\w+)-(\\d+)(?:-(.+))?")
                            val match = jiraBranchRegex.matchEntire(branchName)
                            val branchExtension = match?.let {
                                val (jiraProject, ticketNumber, _) = it.destructured
                                "$jiraProject$ticketNumber"
                            } ?: branchName

                            "$versionFromTag-$branchExtension"
                        } else {
                            versionFromTag
                        }
                    }
                }

                scmVersion.version
            }

            else -> serviceVersion
        }

        target.version = computedVersion

        target.subprojects.forEach { subProject ->
            subProject.version = computedVersion
        }
    }
}
