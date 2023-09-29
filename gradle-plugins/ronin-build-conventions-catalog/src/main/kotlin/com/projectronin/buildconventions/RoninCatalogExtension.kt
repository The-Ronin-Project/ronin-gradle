package com.projectronin.buildconventions

import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

interface RoninCatalogExtension {

    /**
     * Include a prefix before all the project aliases, or don't and just use the raw names
     */
    val includePrefix: Property<Boolean>

    /**
     * If a prefix is included, the default is the parent project name.  Change that by setting this property
     */
    val prefix: Property<String>

    /**
     * Change any plugin names that might be in the output.  The map key is the project path from the root,
     * and the value is the desired alias.  E.g.:
     * ```
     * libraryNameMap.set(mapOf(":empty-middle:subproject-01" to "project1"))
     * ```
     */
    val pluginNameMap: MapProperty<String, String>

    /**
     * Change any library names that might be in the output.  The map key is the project path from the root,
     * and the value is the desired alias.  E.g.:
     * ```
     * libraryNameMap.set(mapOf(":empty-middle:subproject-01" to "project1"))
     * ```
     */
    val libraryNameMap: MapProperty<String, String>

    /**
     * Each entry in this map outputs a bundle in the catalog.  E.g.:
     * ```
     * bundleNameMap.set(
     *     "spring-test" to listOf("spring-test-boot", "spring-test-security", "mockk", "springmockk")
     * )
     * ```
     */
    val bundleNameMap: MapProperty<String, List<String>>

    /**
     * Include all the entries from another catalog file, essentially creating a combined one.  Defaults to `true`
     */
    val includeCatalogFile: Property<Boolean>

    /**
     * What file to include.  Defaults to `libs.versions.toml`
     */
    val catalogFileToInclude: Property<String>
}
