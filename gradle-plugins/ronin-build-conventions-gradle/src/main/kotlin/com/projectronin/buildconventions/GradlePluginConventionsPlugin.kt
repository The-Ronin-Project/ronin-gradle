package com.projectronin.buildconventions

import com.projectronin.gradle.helpers.BaseGradlePluginIdentifiers
import com.projectronin.gradle.helpers.applyPlugin
import com.projectronin.gradle.helpers.registerMavenRepository
import com.projectronin.roninbuildconventionskotlin.PluginIdentifiers
import org.gradle.api.Plugin
import org.gradle.api.Project

class GradlePluginConventionsPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target
            .applyPlugin(PluginIdentifiers.buildconventionsKotlinJvm)
            .applyPlugin(BaseGradlePluginIdentifiers.javaGradlePlugin)
            .applyPlugin(BaseGradlePluginIdentifiers.mavenPublish)

        target.registerMavenRepository(false)
    }
}
