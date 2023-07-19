package com.projectronin.buildconventions

import com.projectronin.gradle.helpers.applyPlugin
import com.projectronin.gradle.helpers.implementationDependency
import com.projectronin.gradle.helpers.platformDependency
import com.projectronin.gradle.helpers.runtimeOnlyDependency
import com.projectronin.gradle.helpers.testImplementationDependency
import com.projectronin.roninbuildconventionskotlin.PluginIdentifiers
import com.projectronin.roninbuildconventionsspringdatabase.DependencyHelper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

class SpringDatabaseConventionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target
            .applyPlugin(PluginIdentifiers.buildconventionsKotlinJvm)
            .platformDependency(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, DependencyHelper.springBootBom)
            .implementationDependency(DependencyHelper.liquibaseCore)
            .runtimeOnlyDependency(DependencyHelper.mysqlConnector)
            .testImplementationDependency(DependencyHelper.springBootTest)
            .testImplementationDependency(DependencyHelper.testcontainersMySql)
    }
}
