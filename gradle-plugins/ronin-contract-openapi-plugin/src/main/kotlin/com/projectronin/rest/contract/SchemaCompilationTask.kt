package com.projectronin.rest.contract

import com.projectronin.openapi.shared.createWorkQueueWithDependencies
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

abstract class SchemaCompilationTask : DefaultTask() {
    @get:InputFile
    abstract val sourceSpecFile: RegularFileProperty

    @get:OutputDirectory
    abstract val consolidatedSpecOutputDirectory: DirectoryProperty

    @get:Input
    abstract val specificationName: Property<String>

    @get:Input
    abstract val versionOverride: Property<String>

    @Inject
    abstract fun getWorkerExecutor(): WorkerExecutor?

    init {
        description = "Generates kotlin model, controller, and client classes from OpenAPI specifications"
        group = BasePlugin.BUILD_GROUP

        (project.properties["sourceSets"] as SourceSetContainer?)!!.getByName("main").resources.srcDir(project.layout.buildDirectory.dir("generated/resources/openapi"))
    }

    @TaskAction
    fun generateOpenApi() {
        val workQueue: WorkQueue = getWorkerExecutor()!!.createWorkQueueWithDependencies(project)
        workQueue.submit(
            SchemaCompilationWorkAction::class.java
        ) { parameters ->
            parameters.sourceSpecFile.set(sourceSpecFile)
            parameters.consolidatedSpecOutputDirectory.set(consolidatedSpecOutputDirectory)
            parameters.specificationName.set(specificationName)
            parameters.versionOverride.set(versionOverride)
        }
        workQueue.await()
    }
}
