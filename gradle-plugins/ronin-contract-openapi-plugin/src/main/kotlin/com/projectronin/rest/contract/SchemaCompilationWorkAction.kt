package com.projectronin.rest.contract

import com.projectronin.openapi.shared.OpenApiKotlinConsolidatorParameters
import com.projectronin.openapi.shared.consolidateSpec
import org.gradle.workers.WorkAction

abstract class SchemaCompilationWorkAction : WorkAction<OpenApiKotlinConsolidatorParameters> {
    override fun execute() {
        consolidateSpec(parameters)
    }
}
