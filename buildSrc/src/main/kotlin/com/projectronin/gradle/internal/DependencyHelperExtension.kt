package com.projectronin.gradle.internal

import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.plugin.use.PluginDependency

interface DependencyHelperExtension {
    val helperDependencies: MapProperty<String, Provider<out ExternalModuleDependency>>
    val helperPlugins: MapProperty<String, Provider<out PluginDependency>>
}
