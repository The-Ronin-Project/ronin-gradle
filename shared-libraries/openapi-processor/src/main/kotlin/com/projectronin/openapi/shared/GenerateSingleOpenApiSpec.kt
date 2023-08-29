package com.projectronin.openapi.shared

import com.cjbooms.fabrikt.cli.ClientCodeGenOptionType
import com.cjbooms.fabrikt.cli.CodeGenerationType
import com.cjbooms.fabrikt.cli.CodeGenerator
import com.cjbooms.fabrikt.cli.ControllerCodeGenOptionType
import com.cjbooms.fabrikt.cli.ControllerCodeGenTargetType
import com.cjbooms.fabrikt.cli.ModelCodeGenOptionType
import com.cjbooms.fabrikt.cli.ValidationLibrary
import com.cjbooms.fabrikt.configurations.Packages
import com.cjbooms.fabrikt.generators.MutableSettings
import com.cjbooms.fabrikt.model.SourceApi
import java.nio.file.Path

/**
 * Given parameters, uses fabrikt to generate kotlin files from an openapi specifications.  Will probably not work correctly for
 * version 3.1 specifications.
 */
fun generateSources(parameters: OpenApiKotlinGeneratorParameters) {
    val apiContent = parameters.consolidatedSpecInputFile.get().asFile.readText()

    val codeGenTypes: Set<CodeGenerationType> = setOfNotNull(
        if (parameters.generateClient.getOrElse(false)) CodeGenerationType.CLIENT else null,
        if (parameters.generateModel.getOrElse(true)) CodeGenerationType.HTTP_MODELS else null,
        if (parameters.generateController.getOrElse(true)) CodeGenerationType.CONTROLLERS else null
    )

    MutableSettings.updateSettings(
        genTypes = codeGenTypes,
        controllerOptions = parameters.controllerOptions.get().map { ControllerCodeGenOptionType.valueOf(it) }.toSet(),
        controllerTarget = ControllerCodeGenTargetType.SPRING,
        modelOptions = parameters.modelOptions.get().map { ModelCodeGenOptionType.valueOf(it) }.toSet(),
        clientOptions = parameters.clientOptions.get().map { ClientCodeGenOptionType.valueOf(it) }.toSet(),
        validationLibrary = ValidationLibrary.JAKARTA_VALIDATION
    )

    val supplementalConfiguration = parameters.supplementalConfiguration.get()
    val packages = Packages(parameters.packageName.get())
    val sourceApi = SourceApi.create(apiContent, emptyList())
    val generator = CodeGenerator(packages, sourceApi, Path.of(""), Path.of(""))
    generator.generate().forEach { it.writeFileTo(parameters.generatedSourcesOutputDir.get().asFile) }

    // Brute-force various corrections or expanded feature support
    parameters.generatedSourcesOutputDir.get().asFile.walk()
        .forEach { file ->
            if (file.name.endsWith(".kt")) {
                file.writeText(
                    file.readText()
                        // the generator hasn't been updated with the javax -> jakarta switch
                        // this should really be fixed, but there's a bug in there somewhere
                        .replace("javax.", "jakarta.")
                        // the generator does not support Spring Reactive targets
                        .let {
                            if (supplementalConfiguration.controllerReactiveTypes && file.name.endsWith("Controller.kt")) {
                                it.replace(
                                    "import org.springframework.stereotype.Controller",
                                    "import org.reactivestreams.Publisher\n" +
                                            "import org.springframework.stereotype.Controller"
                                )
                                    /* There's an unsurprising difference in the return type requirements between
                                     * Flux and Mono which necessitates some additional configuration here. As API
                                     * interactions are likely to be exchange-based for the foreseeable future, the
                                     * streaming variant is the opt-in feature. */
                                    .replace(
                                        Regex(": ResponseEntity<List<([^>]*)>>"),
                                        if (supplementalConfiguration.controllerReactiveStreamTypes) {
                                            ": Publisher<$1>"
                                        } else {
                                            ": Publisher<List<$1>>"
                                        }
                                    )
                                    // The order of these replaces is important
                                    .replace(Regex(": ResponseEntity<([^>]*)>"), ": Publisher<$1>")
                            } else {
                                it
                            }
                        }
                )
            }
        }
}
