package com.projectronin.buildconventions

import com.projectronin.gradle.helpers.BaseGradlePluginIdentifiers
import com.projectronin.gradle.helpers.applyPlugin
import com.projectronin.roninbuildconventionskotlin.PluginIdentifiers
import org.gradle.api.Plugin
import org.gradle.api.Project

class KotlinLibraryConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.applyPlugin(PluginIdentifiers.buildconventionsKotlinJvm)
        target.applyPlugin(com.projectronin.roninbuildconventionspublishing.PluginIdentifiers.buildconventionsPublishing)
        target.applyPlugin(BaseGradlePluginIdentifiers.javaLibrary)
    }
}
