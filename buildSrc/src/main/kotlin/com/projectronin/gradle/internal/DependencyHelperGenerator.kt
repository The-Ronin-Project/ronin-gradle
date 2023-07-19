package com.projectronin.gradle.internal

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.gradle.api.Project
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import java.io.File

object DependencyHelperGenerator {

    fun generateHelper(outDir: File, dependencyHelper: DependencyHelperExtension, targetProject: Project) {
        val packageName = "com.projectronin.${targetProject.name.replace("[^a-zA-Z]".toRegex(), "").lowercase()}"

        val pluginDependencies = dependencyHelper.helperDependencies.get()

        val pluginPlugins = dependencyHelper.helperPlugins.get()

        val simplePluginClassName = ClassName(packageName, "DependencyHelper", "SimplePlugin")

        val pluginConstructor = FunSpec.constructorBuilder()
            .addParameter("id", String::class)
            .addParameter("version", String::class)
            .build()

        val simplePluginClass = TypeSpec.classBuilder(simplePluginClassName)
            .primaryConstructor(pluginConstructor)
            .addProperty(
                PropertySpec.builder("id", String::class)
                    .initializer("id")
                    .build()
            )
            .addProperty(
                PropertySpec.builder("version", String::class)
                    .initializer("version")
                    .build()
            )
            .build()

        val plugins = TypeSpec.objectBuilder("Plugins")
            .run {
                pluginPlugins.entries.fold(this) { builder, entry ->
                    builder
                        .addProperty(
                            PropertySpec.builder(entry.key, simplePluginClassName)
                                .initializer("SimplePlugin(%S, %S)", entry.value.get().pluginId, entry.value.get().version)
                                .build()
                        )
                }
            }
            .build()

        val file = FileSpec.builder(packageName, "DependencyHelper")
            .addType(
                TypeSpec.objectBuilder("DependencyHelper")
                    .addType(simplePluginClass)
                    .addType(plugins)
                    .run {
                        pluginDependencies.entries.fold(this) { builder, entry ->
                            builder
                                .addProperty(
                                    PropertySpec.builder(entry.key, String::class)
                                        .initializer("%S", entry.value.get().toString())
                                        .build()
                                )
                        }
                    }
                    .build()
            )
            .build()

        file.writeTo(outDir)

        fun String.toPropertyName(): String = replace("com.projectronin.", "").lowercase().replace("[^a-z][a-z]".toRegex()) { it.value.last().uppercase() }

        val pluginList = ((targetProject.extensions.findByType(GradlePluginDevelopmentExtension::class.java)?.plugins?.map { plugin -> plugin.id.toPropertyName() to plugin.id } ?: emptyList()) +
            targetProject.fileTree(targetProject.projectDir.resolve("src/main/kotlin")).files
                .filter { it.name.contains(".gradle.kts") }
                .map { pluginFileName ->
                    val id = pluginFileName.path.replace("^.*src/main/kotlin/(.+)\\.gradle\\.kts$".toRegex(), "$1").replace("/", ".")
                    id.toPropertyName() to id
                })
            .toMap()
            .toList()

        if (pluginList.isNotEmpty()) {
            val pluginIdFile = FileSpec.builder(packageName, "PluginIdentifiers")
                .addType(
                    TypeSpec.objectBuilder("PluginIdentifiers")
                        .run {
                            pluginList
                                .fold(this) { builder, pluginPair ->
                                    builder
                                        .addProperty(
                                            PropertySpec.builder(pluginPair.first, String::class)
                                                .initializer("%S", pluginPair.second)
                                                .build()
                                        )
                                    builder
                                }
                        }
                        .build()
                )
                .build()

            pluginIdFile.writeTo(outDir)
        }
    }
}
