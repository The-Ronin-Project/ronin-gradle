package com.projectronin.rest.contract

import com.projectronin.openapi.shared.SupplementalConfiguration
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

interface RestContractSupportExtension {

    /**
     * Disable linting (default false, not recommended to modify)
     */
    val disableLinting: Property<Boolean>

    /**
     * The openapi file to read.  Defaults to src/main/openapi/${project-name}.json
     */
    val inputFile: RegularFileProperty

    /**
     * Package name.  Defaults to com.projectronin.rest.
     */
    val packageName: Property<String>

    /**
     * Should we generate a kotlin client for the API.  Default false.
     */
    val generateClient: Property<Boolean>

    /**
     * Should we generate a kotlin model for the API.  Default true
     */
    val generateModel: Property<Boolean>

    /**
     * Should we generate a kotlin/spring controller for the API.  Default true
     */
    val generateController: Property<Boolean>

    /**
     * The output directory to put the kotlin sources in.  Default "generated/sources/openapi"
     */
    val generatedSourcesOutputDir: DirectoryProperty

    /**
     * Optional controller options, each matching a member of com.cjbooms.fabrikt.cli.ControllerCodeGenOptionType.
     * See https://github.com/cjbooms/fabrikt/blob/10.0.0/src/main/kotlin/com/cjbooms/fabrikt/cli/CodeGenOptions.kt
     */
    val controllerOptions: SetProperty<String>

    /**
     * Optional model options, each matching a member of com.cjbooms.fabrikt.cli.ModelCodeGenOptionType.
     * See https://github.com/cjbooms/fabrikt/blob/10.0.0/src/main/kotlin/com/cjbooms/fabrikt/cli/CodeGenOptions.kt
     */
    val modelOptions: SetProperty<String>

    /**
     * Optional client options, each matching a member of com.cjbooms.fabrikt.cli.ClientCodeGenOptionType.
     * See https://github.com/cjbooms/fabrikt/blob/10.0.0/src/main/kotlin/com/cjbooms/fabrikt/cli/CodeGenOptions.kt
     */
    val clientOptions: SetProperty<String>

    /**
     * Supplemental configuration is intended to bridge gaps where requirements fall outside the current feature
     * sets of the underlying libraries — *stability not guaranteed*.
     */
    val supplementalConfiguration: Property<SupplementalConfiguration>
}
