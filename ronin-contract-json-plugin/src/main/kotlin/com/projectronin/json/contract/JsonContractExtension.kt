package com.projectronin.json.contract

import com.networknt.schema.SpecVersion.VersionFlag
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Extension defining the configuration for the [JsonContractPlugin]
 */
interface EventContractExtension {
    companion object {
        const val NAME = "events"
    }

    /**
     * The JSON Schema version against which this contract should be evaluated. Defaults to V201909.
     */
    val specVersion: Property<VersionFlag>

    /**
     * List of keywords that should be ignored while validating the event contracts. This may help ensure that validation
     * errors or warnings are not produced for items that may be necessary in the schema for generation or other processing.
     */
    val ignoredValidationKeywords: ListProperty<String>

    /**
     * The package name for generated classes.  Note that this will be suffixed with the major version number.
     */
    val packageName: Property<String>

    val schemaSourceDir: DirectoryProperty

    val exampleSourceDir: DirectoryProperty
}

internal fun Project.eventContractExtension(): EventContractExtension = extensions.getByName(EventContractExtension.NAME) as EventContractExtension
