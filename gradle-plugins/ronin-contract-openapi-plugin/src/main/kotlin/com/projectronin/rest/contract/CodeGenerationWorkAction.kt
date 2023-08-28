package com.projectronin.rest.contract

import com.projectronin.openapi.shared.OpenApiKotlinGeneratorParameters
import com.projectronin.openapi.shared.generateSources
import org.gradle.workers.WorkAction

abstract class CodeGenerationWorkAction : WorkAction<OpenApiKotlinGeneratorParameters> {
    override fun execute() {
        generateSources(parameters)
    }
}
