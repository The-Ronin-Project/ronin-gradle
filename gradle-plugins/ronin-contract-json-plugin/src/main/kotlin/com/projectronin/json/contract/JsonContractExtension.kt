package com.projectronin.json.contract

import com.networknt.schema.SpecVersion.VersionFlag
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Extension defining the configuration for the [JsonContractPlugin]
 */
interface JsonContractExtension {
    companion object {
        const val NAME = "contracts"
    }

    /**
     * The JSON Schema version against which this contract should be evaluated. Defaults to V201909.
     */
    val specVersion: Property<VersionFlag>

    /**
     * List of keywords that should be ignored while validating the json contracts. This may help ensure that validation
     * errors or warnings are not produced for items that may be necessary in the schema for generation or other processing.
     */
    val ignoredValidationKeywords: ListProperty<String>

    /**
     * The package name for generated classes.  Note that this will be suffixed with the major version number.
     */
    val packageName: Property<String>

    val schemaSourceDir: DirectoryProperty

    val exampleSourceDir: DirectoryProperty

    /**
     * An _optional_ version override in semver format: Major.Minor.Patch.  Will be suffixed with the real project version,
     * so be careful with this.  It's meant only for maintaining a contract _inside_ your service and keeping a vX and vY contract side-by-side.
     *
     * For instance, if you put your contract inside your service repo, you could do this:
     * ```
     *    project root dir, at tag 2.1.7
     *       contract-v1
     *          with versionOverride set to 1.0.3
     *       contract-current
     *       service
     *          depends on both contract-v1 and contract-current
     * ```
     * This setup will produce two sets of contract artifacts, 2.1.7 and 1.0.3-2.1.7.
     */
    val versionOverride: Property<String>
}

internal fun Project.jsonContractExtension(): JsonContractExtension = extensions.getByName(JsonContractExtension.NAME) as JsonContractExtension
