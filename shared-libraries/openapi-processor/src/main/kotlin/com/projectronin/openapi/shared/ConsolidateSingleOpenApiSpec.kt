package com.projectronin.openapi.shared

import com.projectronin.openapi.shared.util.WriterFactory
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.core.models.ParseOptions

/**
 * Given parameters, reads an openapi specification in and consolidates all its references and outputs a single
 * file containing the entire specification.  Does not work properly for version 3.1 specifications.
 */
fun consolidateSpec(parameters: OpenApiKotlinConsolidatorParameters) {
    if (!parameters.sourceSpecFile.isPresent && !parameters.sourceSpecUri.isPresent) {
        throw IllegalStateException("You must specify sourceSpecFile or sourceSpecUri")
    } else if (parameters.sourceSpecFile.isPresent && parameters.sourceSpecUri.isPresent) {
        throw IllegalStateException("You cannot specify both sourceSpecFile or sourceSpecUri")
    }
    val openApiSpec: OpenAPI = OpenAPIParser().readLocation(
        parameters.sourceSpecFile.orNull?.asFile?.absolutePath?.toString() ?: parameters.sourceSpecUri.get().toString(),
        null,
        ParseOptions().apply {
            @Suppress("UsePropertyAccessSyntax")
            setResolve(true)
        }
    ).openAPI
    parameters.versionOverride.orNull?.let { version ->
        openApiSpec.info.version = version
    }
    val outputDir = parameters.consolidatedSpecOutputDirectory.get().asFile
    if (!outputDir.exists()) {
        outputDir.mkdirs()
    }
    val specificationName = parameters.specificationName.get()
    Json.mapper().writer(WriterFactory.jsonPrettyPrinter()).writeValue(
        outputDir.resolve("$specificationName.json"),
        openApiSpec
    )
    WriterFactory.yamlWriter().writeValue(
        outputDir.resolve("$specificationName.yaml"),
        openApiSpec
    )
}
