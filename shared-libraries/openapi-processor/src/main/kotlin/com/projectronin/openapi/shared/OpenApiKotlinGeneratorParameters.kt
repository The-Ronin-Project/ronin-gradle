package com.projectronin.openapi.shared

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkParameters

/**
 * Parameters for generating kotlin code from a consolidated specification.
 */
interface OpenApiKotlinGeneratorParameters : WorkParameters {
    /**
     * Input location
     */
    val consolidatedSpecInputFile: RegularFileProperty

    /**
     * Package name to output
     */
    val packageName: Property<String>

    /**
     * Should we generate a client
     */
    val generateClient: Property<Boolean>

    /**
     * Should we generate models (isn't that the point?)
     */
    val generateModel: Property<Boolean>

    /**
     * Should we generate spring controllers
     */
    val generateController: Property<Boolean>

    /**
     * The output directory to write the generated sources to
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
}
