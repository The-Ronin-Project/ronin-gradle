package com.projectronin.buildconventions

import com.projectronin.gradle.helpers.addTaskThatDependsOnThisByName
import com.projectronin.gradle.helpers.applyPlugin
import com.projectronin.gradle.helpers.roninProjectVersion
import com.projectronin.gradle.helpers.runtimeOnlyDependency
import com.projectronin.gradle.helpers.testImplementationDependency
import com.projectronin.roninbuildconventionskotlin.PluginIdentifiers
import com.projectronin.roninbuildconventionsspringservice.DependencyHelper
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPlugin
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
            .testImplementationDependency("com.projectronin.services.gradle:database-test-helpers:${PluginIdentifiers.version}")

        target.extensions.getByType(SpringBootExtension::class.java).apply {
            buildInfo()
        }

        val kapt = target.configurations.maybeCreate("kapt")
        target.dependencies.add(kapt.name, DependencyHelper.springAnnotationProcessor)

        val annotationProcessor = target.configurations.maybeCreate("annotationProcessor")
        target.dependencies.add(annotationProcessor.name, DependencyHelper.springAnnotationProcessor)

        target.afterEvaluate {
            potentiallyAddOsXNettyResolver(target)
        }

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
                    val roninProjectVersion = target.roninProjectVersion

                    // language=json
                    outputFile.asFile.writeText(
                        """
                        {
                          "version": "${roninProjectVersion.serviceVersion ?: roninProjectVersion.tagBasedVersion}",
                          "lastTag": "${roninProjectVersion.lastTag ?: "n/a"}",
                          "commitDistance": ${roninProjectVersion.commitDistance ?: 0},
                          "gitHash": "${roninProjectVersion.gitHash}",
                          "gitHashFull": "${roninProjectVersion.gitHashFull}",
                          "branchName": "${roninProjectVersion.branchName}",
                          "dirty": ${roninProjectVersion.dirty}
                        }
                        """.trimIndent()
                    )
                }
            })
        }
    }

    internal fun potentiallyAddOsXNettyResolver(
        target: Project,
        osName: String = System.getProperty("os.name"),
        architecture: String = System.getProperty("os.arch").lowercase()
    ) {
        val isMacOS = osName.startsWith("Mac OS X")
        if (isMacOS && architecture == "aarch64") {
            val potentialWebfluxDependencies = setOf(
                "spring-boot-starter-webflux",
                "spring-boot-webflux",
                "product-spring-webflux-starter"
            )
            val isWebflux = target.configurations.getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME).dependencies.any { potentialWebfluxDependencies.contains(it.name) }
            if (isWebflux) {
                target.runtimeOnlyDependency("io.netty:netty-resolver-dns-native-macos::osx-aarch_64")
            }
        }
    }
}
