package com.projectronin.openapi.shared

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkParameters
import java.net.URI

/**
 * Parameters necessary for consolidator function
 */
interface OpenApiKotlinConsolidatorParameters : WorkParameters {
    /**
     * Source specification file.  This or sourceSpecUri must be specified but not both
     */
    val sourceSpecFile: RegularFileProperty

    /**
     * Source specification url.  This or sourceSpecFile must be specified, but not both
     */
    val sourceSpecUri: Property<URI>

    /**
     * The directory to output the consolidated files to.
     */
    val consolidatedSpecOutputDirectory: DirectoryProperty

    /**
     * The base filename of the generated files.  Files will be `${consolidatedSpecOutputDirectory}/${specificationName}.[json|yaml]`
     */
    val specificationName: Property<String>

    /**
     * If specified, will change the output specification's info/version field.
     */
    val versionOverride: Property<String>
}
