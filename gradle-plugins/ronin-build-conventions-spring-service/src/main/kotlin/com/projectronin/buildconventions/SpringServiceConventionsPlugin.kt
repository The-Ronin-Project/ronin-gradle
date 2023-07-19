package com.projectronin.buildconventions

import com.projectronin.gradle.helpers.applyPlugin
import com.projectronin.roninbuildconventionskotlin.PluginIdentifiers
import com.projectronin.roninbuildconventionsspringservice.DependencyHelper
import org.gradle.api.Plugin
import org.gradle.api.Project
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
    }
}
