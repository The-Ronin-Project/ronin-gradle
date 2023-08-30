package com.projectronin.rest.contract

import com.projectronin.openapi.shared.SupplementalConfiguration
import com.projectronin.openapi.shared.createWorkQueueWithDependencies
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

abstract class CodeGenerationTask : DefaultTask() {
    @get:InputFile
    abstract val consolidatedSpecInputFile: RegularFileProperty

    @get:Input
    abstract val finalPackageName: Property<String>

    @Inject
    abstract fun getWorkerExecutor(): WorkerExecutor?

    init {
        description = "Generates kotlin model, controller, and client classes from OpenAPI specifications"
        group = BasePlugin.BUILD_GROUP
    }

    @TaskAction
    fun generateOpenApi() {
        val workQueue: WorkQueue = getWorkerExecutor()!!.createWorkQueueWithDependencies(project)
        val extension = project.extensions.getByType(RestContractSupportExtension::class.java)
        workQueue.submit(
            CodeGenerationWorkAction::class.java
        ) { parameters ->
            parameters.consolidatedSpecInputFile.set(consolidatedSpecInputFile)
            parameters.packageName.set(finalPackageName)
            parameters.generateClient.set(extension.generateClient.getOrElse(false))
            parameters.generateModel.set(extension.generateModel.getOrElse(true))
            parameters.generateController.set(extension.generateController.getOrElse(true))
            parameters.generatedSourcesOutputDir.set(extension.generatedSourcesOutputDir)
            parameters.controllerOptions.set(extension.controllerOptions)
            parameters.modelOptions.set(extension.modelOptions)
            parameters.clientOptions.set(extension.clientOptions)
            parameters.supplementalConfiguration.set(
                extension.supplementalConfiguration.getOrElse(SupplementalConfiguration())
            )
        }
        workQueue.await()
    }
}
