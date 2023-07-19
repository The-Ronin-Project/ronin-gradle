package com.projectronin.buildconventions

import com.projectronin.gradle.helpers.BaseGradlePluginIdentifiers
import com.projectronin.gradle.helpers.applyPlugin
import com.projectronin.gradle.helpers.registerMavenRepository
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.dsl.VersionCatalogBuilder
import org.gradle.api.plugins.catalog.CatalogPluginExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension

class CatalogConventionsPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target
            .applyPlugin(BaseGradlePluginIdentifiers.base)
            .applyPlugin(BaseGradlePluginIdentifiers.versionCatalog)
            .applyPlugin(BaseGradlePluginIdentifiers.mavenPublish)

        val meaningfulSubProjects = target.rootProject.subprojects.filter {
            it.buildFile.exists() && it != target
        }

        meaningfulSubProjects.forEach { target.evaluationDependsOn(it.path) }

        target.extensions.getByType(CatalogPluginExtension::class.java).apply {
            versionCatalog { builder ->
                builder.apply {
                    if (target.rootProject.projectDir.resolve("gradle/libs.versions.toml").exists()) {
                        from(target.files("${target.rootProject.projectDir}/gradle/libs.versions.toml"))
                    }
                    // This whole mess tries to supplement the TOML file by adding _this project's_ version to it dynamically,
                    // and by recursing the project structure and declaring libraries for each module.
                    val sanitizedRootName = target.rootProject.name.sanitizeName()
                    version(sanitizedRootName, target.version.toString())

                    val gradlePluginSubprojects =
                        meaningfulSubProjects.filter {
                            it.extensions.findByName("gradlePlugin") != null
                        }
                    val librarySubprojects = meaningfulSubProjects - gradlePluginSubprojects

                    gradlePluginSubprojects
                        .forEach { extractPlugins(sanitizedRootName, it) }
                    librarySubprojects
                        .forEach {
                            library("$sanitizedRootName-${it.name.sanitizeName()}", it.group.toString(), it.name).versionRef(sanitizedRootName)
                        }
                }
            }
        }

        target.registerMavenRepository(false).apply {
            publications.apply {
                create("maven", MavenPublication::class.java).apply {
                    from(target.components.getByName("versionCatalog"))
                }
            }
        }
    }

    private fun VersionCatalogBuilder.extractPlugins(sanitizedRootName: String, currentProject: Project) {
        (currentProject.extensions.findByType(GradlePluginDevelopmentExtension::class.java)?.plugins?.toList() ?: emptyList())
            .forEach { plugin ->
                val pluginId = plugin.id
                val catalogName = pluginId.replace("com.projectronin.", "").sanitizeName()
                plugin("$sanitizedRootName-$catalogName", pluginId).versionRef(sanitizedRootName)
            }
    }

    private fun String.sanitizeName(): String = replace("[^a-zA-Z]+".toRegex(), "-")
}
