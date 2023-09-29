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

        target.extensions.create("roninCatalog", RoninCatalogExtension::class.java).apply {
            includePrefix.convention(true)
            prefix.convention(target.rootProject.name)
            pluginNameMap.convention(emptyMap())
            libraryNameMap.convention(emptyMap())
            includeCatalogFile.convention(true)
            catalogFileToInclude.convention("gradle/libs.versions.toml")
        }

        target.afterEvaluate {
            target.extensions.getByType(CatalogPluginExtension::class.java).apply {
                versionCatalog { builder ->
                    builder.apply {
                        val roninCatalog = target.extensions.getByType(RoninCatalogExtension::class.java)

                        if (roninCatalog.includeCatalogFile.get()) {
                            if (target.rootProject.projectDir.resolve(roninCatalog.catalogFileToInclude.get()).exists()) {
                                from(target.files("${target.rootProject.projectDir}/${roninCatalog.catalogFileToInclude.get()}"))
                            } else {
                                target.logger.warn("Could not find file to include: ${target.rootProject.projectDir}/${roninCatalog.catalogFileToInclude.get()}")
                            }
                        }

                        // This whole mess tries to supplement the TOML file by adding _this project's_ version to it dynamically,
                        // and by recursing the project structure and declaring libraries for each module.
                        val sanitizedRootName = if (roninCatalog.includePrefix.get()) {
                            "${roninCatalog.prefix.get().sanitizeName()}-"
                        } else {
                            ""
                        }
                        val versionRef = roninCatalog.prefix.get().sanitizeName()
                        version(versionRef, target.version.toString())

                        val gradlePluginSubprojects =
                            meaningfulSubProjects.filter {
                                it.extensions.findByName("gradlePlugin") != null
                            }
                        val librarySubprojects = meaningfulSubProjects - gradlePluginSubprojects

                        gradlePluginSubprojects
                            .forEach { extractPlugins(sanitizedRootName, versionRef, it, roninCatalog) }
                        librarySubprojects
                            .forEach {
                                val name = roninCatalog.libraryNameMap.get()[it.path] ?: "$sanitizedRootName${it.name.sanitizeName()}"
                                library(name, it.group.toString(), it.name).versionRef(versionRef)
                            }

                        roninCatalog.bundleNameMap.getOrElse(emptyMap()).forEach { (bundleName, bundleEntries) ->
                            bundle(bundleName, bundleEntries)
                        }
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

    private fun VersionCatalogBuilder.extractPlugins(sanitizedRootName: String, versionRef: String, currentProject: Project, roninCatalog: RoninCatalogExtension) {
        (currentProject.extensions.findByType(GradlePluginDevelopmentExtension::class.java)?.plugins?.toList() ?: emptyList())
            .forEach { plugin ->
                val pluginId = plugin.id
                val catalogName = roninCatalog.pluginNameMap.get()[pluginId] ?: "$sanitizedRootName${pluginId.replace("com.projectronin.", "").sanitizeName()}"
                plugin(catalogName, pluginId).versionRef(versionRef)
            }
    }

    private fun String.sanitizeName(): String = replace("[^a-zA-Z0-9]+".toRegex(), "-")
}
