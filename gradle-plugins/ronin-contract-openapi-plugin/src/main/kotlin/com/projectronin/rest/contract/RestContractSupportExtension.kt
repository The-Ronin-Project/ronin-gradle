package com.projectronin.rest.contract

import com.projectronin.openapi.shared.SupplementalConfiguration
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
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
     * Defaults to FABRIKT.  But can switch to OPENAPI_GENERATOR, which will do quite different things.  Note that options
     * from controllerOptions/modelOptions/clientOptions are ignored, and clients aren't generaed
     */
    val generatorType: Property<GeneratorType>

    /**
     * For `generatorType==OPENAPI_GENERATOR`, any additional properties.  See docs [here](https://openapi-generator.tech/docs/generators/kotlin-spring/).
     *
     * Ones you might want are:
     * - `reactive=true`
     */
    val openApiGeneratorAdditionalProperties: MapProperty<String, Any>

    /**
     * Supplemental configuration is intended to bridge gaps where requirements fall outside the current feature
     * sets of the underlying libraries — *stability not guaranteed*.
     */
    val supplementalConfiguration: Property<SupplementalConfiguration>

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
