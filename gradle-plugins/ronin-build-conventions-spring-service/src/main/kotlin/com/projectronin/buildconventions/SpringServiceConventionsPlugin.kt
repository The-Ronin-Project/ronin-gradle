package com.projectronin.buildconventions

import com.projectronin.gradle.helpers.addTaskThatDependsOnThisByName
import com.projectronin.gradle.helpers.applyPlugin
import com.projectronin.roninbuildconventionskotlin.PluginIdentifiers
import com.projectronin.roninbuildconventionsspringservice.DependencyHelper
import org.eclipse.jgit.api.Git
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.SourceSetContainer
import org.springframework.boot.gradle.dsl.SpringBootExtension

class SpringServiceConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target
            .applyPlugin(PluginIdentifiers.buildconventionsKotlinJvm)
            .applyPlugin(DependencyHelper.Plugins.springBoot.id)
            .applyPlugin(DependencyHelper.Plugins.springDependencyManager.id)
            .applyPlugin(DependencyHelper.Plugins.springKotlinCore.id)
            .applyPlugin(DependencyHelper.Plugins.springKotlinJpa.id)
            .applyPlugin(DependencyHelper.Plugins.kapt.id)

        target.extensions.getByType(SpringBootExtension::class.java).apply {
            buildInfo()
        }

        val kapt = target.configurations.maybeCreate("kapt")
        target.dependencies.add(kapt.name, DependencyHelper.springAnnotationProcessor)

        val annotationProcessor = target.configurations.maybeCreate("annotationProcessor")
        target.dependencies.add(annotationProcessor.name, DependencyHelper.springAnnotationProcessor)

        target.task("generateServiceInfo") { t ->
            val outputDir = t.project.layout.buildDirectory.dir("generated/resources/service-info")
            t.outputs.dir(outputDir)

            val outputFile = outputDir.get().file("service-info.json")
            t.group = BasePlugin.BUILD_GROUP
            t.description = "Builds a json file containing some service information for actuator to use"

            (t.project.properties["sourceSets"] as SourceSetContainer?)!!.getByName("main").resources.srcDir(outputDir)
            t.addTaskThatDependsOnThisByName("processResources")
            t.addTaskThatDependsOnThisByName("sourcesJar")

            @Suppress("ObjectLiteralToLambda")
            t.doLast(object : Action<Task> {
                override fun execute(t: Task) {
                    val git = Git.open(target.rootProject.projectDir)
                    val ref = git.repository.exactRef("HEAD")
                    val reader = git.repository.newObjectReader()
                    val description = git.describe()
                        .setTags(true)
                        .setAlways(true)
                        .setMatch("[0-9]*.[0-9]*.[0-9]*")
                        .setTarget("HEAD")
                        .call()

                    // note that this won't produce the same version as SERVICE_VERSION would.  But the intent is that this is only
                    // informational AND that GHAs will set SERVICE_VERSION.
                    val descriptionPattern = """^([0-9]+)\.*([0-9]+)\.*([0-9]+)(-alpha)?(?:-([0-9]+)-g.?[0-9a-fA-F]{3,})?$""".toRegex()
                    val (lastTag: String?, commitDistance: Int?, tagBasedVersion: String?) = when (val match = descriptionPattern.find(description)) {
                        null -> Triple(null, null, null)

                        else -> {
                            val (major, minor, patch, suffix, commitDistance) = match.destructured
                            Triple(
                                "$major.$minor.$patch${suffix.takeIf { it.isNotBlank() } ?: ""}",
                                commitDistance.takeIf { it.isNotBlank() }?.toInt() ?: 0,
                                when (commitDistance) {
                                    "0", "" -> "$major.$minor.$patch"
                                    else -> if (suffix.isNotBlank()) {
                                        "$major.$minor.$patch-SNAPSHOT"
                                    } else {
                                        "$major.$minor.${patch.toInt() + 1}-SNAPSHOT"
                                    }
                                }
                            )
                        }
                    }

                    val version: String =
                        when (val serviceVersion: String? = target.properties.getOrDefault("service-version", System.getenv("SERVICE_VERSION"))?.toString()?.takeIf { it.isNotBlank() }) {
                            null -> tagBasedVersion ?: "1.0.0-SNAPSHOT"
                            else -> serviceVersion
                        }

                    // language=json
                    outputFile.asFile.writeText(
                        """
                        {
                          "version": "$version",
                          "lastTag": "${lastTag ?: "n/a"}",
                          "commitDistance": ${commitDistance ?: 0},
                          "gitHash": "${reader.abbreviate(ref.objectId).name()}",
                          "gitHashFull": "${ref.objectId.name}",
                          "branchName": "${System.getenv("REF_NAME")?.ifBlank { null } ?: git.repository.branch}",
                          "dirty": ${!git.status().call().isClean}
                        }
                        """.trimIndent()
                    )
                }
            })
        }
    }
}
