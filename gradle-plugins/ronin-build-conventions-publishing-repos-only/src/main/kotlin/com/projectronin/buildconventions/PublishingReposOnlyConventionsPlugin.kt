package com.projectronin.buildconventions

import com.projectronin.gradle.helpers.BaseGradlePluginIdentifiers
import com.projectronin.gradle.helpers.applyPlugin
import com.projectronin.gradle.helpers.registerMavenRepository
import org.gradle.api.Plugin
import org.gradle.api.Project

class PublishingReposOnlyConventionsPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            applyPlugin(BaseGradlePluginIdentifiers.java)
            applyPlugin(BaseGradlePluginIdentifiers.mavenPublish)
            registerMavenRepository(true)
        }
    }
}
