package com.projectronin.openapi

import com.projectronin.gradle.helpers.addTaskThatDependsOnThisByName
import com.projectronin.gradle.helpers.implementationDependency
import com.projectronin.openapi.shared.OpenApiKotlinConsolidatorParameters
import com.projectronin.openapi.shared.OpenApiKotlinGeneratorParameters
import com.projectronin.openapi.shared.SupplementalConfiguration
import com.projectronin.openapi.shared.consolidateSpec
import com.projectronin.openapi.shared.createWorkQueueWithDependencies
import com.projectronin.openapi.shared.generateSources
import com.projectronin.roninopenapiplugin.DependencyHelper
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.net.URI
import java.net.URL
import java.util.UUID
import javax.inject.Inject

interface OpenApiKotlinGeneratorInputSpec {
    @get:InputFile
    @get:Optional
    val inputFile: RegularFileProperty

    @get:Input
    @get:Optional
    val inputDependency: Property<String>

    @get:Input
    @get:Optional
    val inputUrl: Property<URL>

    @get:Input
    val packageName: Property<String>

    @get:Input
    @get:Optional
    val finalResourcePath: Property<String>
}

interface OpenApiKotlinGeneratorExtension {
    val generateClient: Property<Boolean>
    val generateModel: Property<Boolean>
    val generateController: Property<Boolean>
    val schemas: ListProperty<OpenApiKotlinGeneratorInputSpec>
    val outputDir: DirectoryProperty
    val resourcesOutputDirectory: DirectoryProperty
    val controllerOptions: SetProperty<String>
    val modelOptions: SetProperty<String>
    val clientOptions: SetProperty<String>
    val supplementalConfiguration: Property<SupplementalConfiguration>
}

interface RoninOpenApiPluginWorkParameters : OpenApiKotlinConsolidatorParameters, OpenApiKotlinGeneratorParameters

abstract class OpenApiKotlinGeneratorTask : DefaultTask() {

    @get:Input
    abstract val generateClient: Property<Boolean>

    @get:Input
    abstract val generateModel: Property<Boolean>

    @get:Input
    abstract val generateController: Property<Boolean>

    @get:Input
    abstract val controllerOptions: SetProperty<String>

    @get:Input
    abstract val modelOptions: SetProperty<String>

    @get:Input
    abstract val clientOptions: SetProperty<String>

    @get:Nested
    abstract val schemas: ListProperty<OpenApiKotlinGeneratorInputSpec>

    @get:Nested
    @get:Optional
    abstract val supplementalConfiguration: Property<SupplementalConfiguration>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val resourcesOutputDirectory: DirectoryProperty

    @Inject
    abstract fun getWorkerExecutor(): WorkerExecutor?

    init {
        description = "Generates kotlin model, controller, and client classes from OpenAPI specifications"
        group = BasePlugin.BUILD_GROUP
    }

    @TaskAction
    fun generateOpenApi() {
        with(outputDir.get().asFile) {
            if (!exists()) {
                mkdirs()
            }
        }

        val workQueue: WorkQueue = getWorkerExecutor()!!.createWorkQueueWithDependencies(project)
        submitWorkQueueTasks(workQueue)
        workQueue.await()
    }

    private fun submitWorkQueueTasks(workQueue: WorkQueue) {
        schemas.get().forEach { input ->
            val inputUri: URI = if (input.inputFile.isPresent) {
                input.inputFile.get().asFile.toURI()
            } else if (input.inputUrl.isPresent) {
                input.inputUrl.get().toURI()
            } else if (input.inputDependency.isPresent) {
                val dependencySpec = "${input.inputDependency.get().replace("@.*$".toRegex(), "")}@json"
                val configName = dependencySpec.split(":")[1].replace("[^A-Za-z]".toRegex(), "_")
                project.configurations.maybeCreate(configName)
                project.dependencies.add(configName, dependencySpec)
                val dependencyFile = project.configurations.getByName(configName).resolve().first()
                downloadFileIfConfigured(input, dependencyFile)
                dependencyFile.toURI()
            } else {
                throw IllegalArgumentException("Must specify one of inputFile, inputUrl, or inputDependency")
            }

            workQueue.submit(
                GenerateSingleOpenApiSpec::class.java
            ) { parameters ->
                parameters.sourceSpecUri.set(inputUri)
                parameters.consolidatedSpecOutputDirectory.set(resourcesOutputDirectory.get())
                parameters.specificationName.set(inputUri.toString().toIdentifier())
                parameters.consolidatedSpecInputFile.set(resourcesOutputDirectory.get().asFile.resolve("${inputUri.toString().toIdentifier()}.yaml"))
                parameters.packageName.set(input.packageName.get())
                parameters.generateClient.set(generateClient.getOrElse(false))
                parameters.generateModel.set(generateModel.getOrElse(true))
                parameters.generateController.set(generateController.getOrElse(true))
                parameters.generatedSourcesOutputDir.set(outputDir)
                parameters.controllerOptions.set(controllerOptions)
                parameters.modelOptions.set(modelOptions)
                parameters.clientOptions.set(clientOptions)
                parameters.supplementalConfiguration.set(supplementalConfiguration)
            }
        }
    }

    private fun downloadFileIfConfigured(input: OpenApiKotlinGeneratorInputSpec, dependencyFile: File) {
        if (input.finalResourcePath.isPresent && resourcesOutputDirectory.isPresent) {
            val finalResourcePath = if (input.finalResourcePath.get().startsWith("META-INF/resources")) {
                input.finalResourcePath.get()
            } else {
                "META-INF/resources/${input.finalResourcePath.get().replace("^/".toRegex(), "")}"
            }
            if (resourcesOutputDirectory.get().asFile.exists()) {
                resourcesOutputDirectory.get().asFile.deleteRecursively()
            }
            resourcesOutputDirectory.get().asFile.mkdirs()
            dependencyFile.copyTo(resourcesOutputDirectory.get().file(finalResourcePath).asFile, true)
        }
    }
}

abstract class GenerateSingleOpenApiSpec : WorkAction<RoninOpenApiPluginWorkParameters> {
    override fun execute() {
        consolidateSpec(parameters)
        generateSources(parameters)
    }
}

class OpenApiKotlinGenerator : Plugin<Project> {
    override fun apply(project: Project) {
        val ex = project.extensions.create("generateOpenApiCode", OpenApiKotlinGeneratorExtension::class.java).apply {
            generateClient.convention(false)
            generateModel.convention(true)
            generateController.convention(true)
            outputDir.convention(project.layout.buildDirectory.dir("generated/openapi-kotlin-generator/kotlin"))
            resourcesOutputDirectory.convention(project.layout.buildDirectory.dir("generated/openapi-kotlin-generator/resources"))
            controllerOptions.convention(emptySet())
            modelOptions.convention(emptySet())
            clientOptions.convention(emptySet())
            supplementalConfiguration.convention(SupplementalConfiguration())
        }

        project.implementationDependency(DependencyHelper.jakarta)

        (project.properties["sourceSets"] as SourceSetContainer?)?.getByName("main")?.java?.srcDir(ex.outputDir)
        (project.properties["sourceSets"] as SourceSetContainer?)?.getByName("main")?.resources?.srcDir(ex.resourcesOutputDirectory)

        val generatorTask = project.tasks.register(
            "generateOpenApiCode",
            OpenApiKotlinGeneratorTask::class.java
        ) {
            it.group = BasePlugin.BUILD_GROUP
            it.generateClient.set(ex.generateClient)
            it.generateModel.set(ex.generateModel)
            it.generateController.set(ex.generateController)
            it.schemas.set(ex.schemas)
            it.outputDir.set(ex.outputDir)
            it.resourcesOutputDirectory.set(ex.resourcesOutputDirectory)
            it.clientOptions.set(ex.clientOptions)
            it.modelOptions.set(ex.modelOptions)
            it.controllerOptions.set(ex.controllerOptions)
            it.supplementalConfiguration.set(ex.supplementalConfiguration)
        }.get()
        generatorTask.addTaskThatDependsOnThisByName("compileKotlin")
        generatorTask.addTaskThatDependsOnThisByName("processResources")
        generatorTask.addTaskThatDependsOnThisByName("runKtlintCheckOverMainSourceSet")
        generatorTask.addTaskThatDependsOnThisByName("sourcesJar")
        generatorTask.addTaskThatDependsOnThisByName("kaptGenerateStubsKotlin")
    }
}

private fun String.toIdentifier(): String {
    return UUID.nameUUIDFromBytes(toByteArray()).toString()
}
