package com.projectronin.openapi

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
import com.projectronin.gradle.helpers.addTaskThatDependsOnThisByName
import com.projectronin.gradle.helpers.implementationDependency
import com.projectronin.roninopenapiplugin.DependencyHelper
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.core.util.Yaml
import io.swagger.v3.parser.core.models.ParseOptions
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
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Path
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
    val controllerOptions: SetProperty<ControllerCodeGenOptionType>
    val modelOptions: SetProperty<ModelCodeGenOptionType>
    val clientOptions: SetProperty<ClientCodeGenOptionType>
}

interface OpenApiKotlinGeneratorParameters : WorkParameters {
    val inputUri: Property<URI>
    val packageName: Property<String>
    val generateClient: Property<Boolean>
    val generateModel: Property<Boolean>
    val generateController: Property<Boolean>
    val outputDir: DirectoryProperty
    val controllerOptions: SetProperty<ControllerCodeGenOptionType>
    val modelOptions: SetProperty<ModelCodeGenOptionType>
    val clientOptions: SetProperty<ClientCodeGenOptionType>
}

abstract class OpenApiKotlinGeneratorTask : DefaultTask() {

    @get:Input
    abstract val generateClient: Property<Boolean>

    @get:Input
    abstract val generateModel: Property<Boolean>

    @get:Input
    abstract val generateController: Property<Boolean>

    @get:Input
    abstract val controllerOptions: SetProperty<ControllerCodeGenOptionType>

    @get:Input
    abstract val modelOptions: SetProperty<ModelCodeGenOptionType>

    @get:Input
    abstract val clientOptions: SetProperty<ClientCodeGenOptionType>

    @get:Nested
    abstract val schemas: ListProperty<OpenApiKotlinGeneratorInputSpec>

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

        val workQueue: WorkQueue = createWorkQueueWithDependencies()
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
                parameters.inputUri.set(inputUri)
                parameters.packageName.set(input.packageName.get())
                parameters.generateClient.set(generateClient.getOrElse(false))
                parameters.generateModel.set(generateModel.getOrElse(true))
                parameters.generateController.set(generateController.getOrElse(true))
                parameters.outputDir.set(outputDir)
                parameters.clientOptions.set(clientOptions)
                parameters.modelOptions.set(modelOptions)
                parameters.controllerOptions.set(controllerOptions)
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

    private fun createWorkQueueWithDependencies(): WorkQueue {
        val fabriktConfigName = "fabrikt"
        project.configurations.maybeCreate(fabriktConfigName)
        project.dependencies.add(fabriktConfigName, DependencyHelper.fabrikt)
        project.dependencies.add(fabriktConfigName, DependencyHelper.swaggerParser)
        val fabriktDependencies = project.configurations.getByName(fabriktConfigName).resolve()

        val workQueue: WorkQueue = getWorkerExecutor()!!.classLoaderIsolation { workerSpec -> workerSpec.classpath.from(*fabriktDependencies.toTypedArray()) }
        return workQueue
    }
}

abstract class GenerateSingleOpenApiSpec : WorkAction<OpenApiKotlinGeneratorParameters> {
    override fun execute() {
        val params = parameters
        val outputPackageName = params.packageName
        val result = OpenAPIParser().readLocation(
            params.inputUri.get().toString(),
            null,
            ParseOptions().apply {
                setResolve(true)
            }
        )

        val apiContent = Yaml.mapper().writeValueAsString(result.openAPI)

        val codeGenTypes: Set<CodeGenerationType> = setOfNotNull(
            if (params.generateClient.getOrElse(false)) CodeGenerationType.CLIENT else null,
            if (params.generateModel.getOrElse(true)) CodeGenerationType.HTTP_MODELS else null,
            if (params.generateController.getOrElse(true)) CodeGenerationType.CONTROLLERS else null
        )

        MutableSettings.updateSettings(
            genTypes = codeGenTypes,
            controllerOptions = params.controllerOptions.get(),
            controllerTarget = ControllerCodeGenTargetType.SPRING,
            modelOptions = params.modelOptions.get(),
            clientOptions = params.clientOptions.get(),
            validationLibrary = ValidationLibrary.JAKARTA_VALIDATION
        )

        val packages = Packages(outputPackageName.get())
        val sourceApi = SourceApi.create(apiContent, emptyList())
        val generator = CodeGenerator(packages, sourceApi, Path.of(""), Path.of(""))
        generator.generate().forEach { it.writeFileTo(params.outputDir.get().asFile) }

        // because the generator hasn't been updated with the javax -> jakarta switch, brute-force that here
        // this should really be fixed, but there's a bug in there somewhere
        params.outputDir.get().asFile.walk()
            .forEach {
                if (it.name.endsWith(".kt")) {
                    it.writeText(
                        it.readText().replace("javax.", "jakarta.")
                    )
                }
            }
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
        }.get()
        generatorTask.addTaskThatDependsOnThisByName("compileKotlin")
        generatorTask.addTaskThatDependsOnThisByName("processResources")
        generatorTask.addTaskThatDependsOnThisByName("runKtlintCheckOverMainSourceSet")
        generatorTask.addTaskThatDependsOnThisByName("sourcesJar")
        generatorTask.addTaskThatDependsOnThisByName("kaptGenerateStubsKotlin")
    }
}
