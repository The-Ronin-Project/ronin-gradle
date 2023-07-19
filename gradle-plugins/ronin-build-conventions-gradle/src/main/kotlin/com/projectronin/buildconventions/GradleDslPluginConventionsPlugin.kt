package com.projectronin.buildconventions

import com.projectronin.gradle.helpers.BaseGradlePluginIdentifiers
import com.projectronin.gradle.helpers.applyPlugin
import com.projectronin.gradle.helpers.registerMavenRepository
import com.projectronin.roninbuildconventionsgradle.DependencyHelper
import org.gradle.api.Plugin
import org.gradle.api.Project

class GradleDslPluginConventionsPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target
            .applyPlugin(DependencyHelper.Plugins.kotlinDsl.id)
            .applyPlugin(DependencyHelper.Plugins.ktlint.id)
            .applyPlugin(BaseGradlePluginIdentifiers.jacoco)
            .applyPlugin(BaseGradlePluginIdentifiers.java)
            .applyPlugin(BaseGradlePluginIdentifiers.mavenPublish)

        KotlinJvmConventionsPlugin.applyBasicKotlin(target)

        target.registerMavenRepository(false)
    }
}
