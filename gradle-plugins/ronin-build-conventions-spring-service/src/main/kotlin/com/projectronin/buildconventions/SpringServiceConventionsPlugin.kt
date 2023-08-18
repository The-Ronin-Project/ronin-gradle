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
                        .setMatch("v*.*.*")
                        .setTarget("HEAD")
                        .call()
                    val (lastTag, commitDistance) = when (val match = "(v.*)-([0-9]+)-g.?[0-9a-fA-F]{3,}".toRegex().find(description)) {
                        null -> Pair(null, null)
                        else -> {
                            val (a, b) = match.destructured
                            Pair(a, b)
                        }
                    }
                    // language=json
                    outputFile.asFile.writeText(
                        """
                        {
                          "version": "${t.project.version}",
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
