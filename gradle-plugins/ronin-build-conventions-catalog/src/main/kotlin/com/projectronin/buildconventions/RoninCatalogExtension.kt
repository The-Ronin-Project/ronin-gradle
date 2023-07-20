package com.projectronin.buildconventions

import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

interface RoninCatalogExtension {

    val includePrefix: Property<Boolean>

    val prefix: Property<String>

    val pluginNameMap: MapProperty<String, String>

    val libraryNameMap: MapProperty<String, String>

    val includeCatalogFile: Property<Boolean>

    val catalogFileToInclude: Property<String>
}
