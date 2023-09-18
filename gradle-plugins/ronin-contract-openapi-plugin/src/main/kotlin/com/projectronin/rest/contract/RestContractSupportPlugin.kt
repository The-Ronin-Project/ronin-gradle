package com.projectronin.rest.contract

import com.github.gradle.node.npm.task.NpxTask
import com.projectronin.gradle.helpers.BaseGradlePluginIdentifiers
import com.projectronin.gradle.helpers.DEFAULT_PUBLICATION_NAME
import com.projectronin.gradle.helpers.JAVA_COMPONENT_NAME
import com.projectronin.gradle.helpers.addTaskThatDependsOnThisByName
import com.projectronin.gradle.helpers.applyPlugin
import com.projectronin.gradle.helpers.compileOnlyDependency
import com.projectronin.gradle.helpers.platformDependency
import com.projectronin.gradle.helpers.registerMavenRepository
import com.projectronin.openapi.shared.OpenApiKotlinConsolidatorParameters
import com.projectronin.openapi.shared.consolidateSpec
import com.projectronin.ronincontractopenapiplugin.DependencyHelper
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import com.projectronin.roninbuildconventionskotlin.PluginIdentifiers as KotlinPluginIdentifiers
import com.projectronin.roninbuildconventionsversioning.PluginIdentifiers as VersioningPluginIdentifiers

/**
 * A simple 'hello world' plugin.
 */
@Suppress("ObjectLiteralToLambda")
class RestContractSupportPlugin : Plugin<Project> {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(RestContractSupportPlugin::class.java)
        private const val LINT_TASK_NAME: String = "lintApi"
        private const val DOWNLOAD_TASK_NAME: String = "downloadApiDependencies"
        private const val API_DEPENDENCY_CONFIG_NAME: String = "openapi"
        const val COMPILE_TASK_NAME: String = "compileApi"
        private const val DOCS_TASK_NAME: String = "generateApiDocumentation"
        private const val CLEAN_TASK_NAME: String = "cleanApiOutput"
        private const val GENERATE_TASK_NAME: String = "generateOpenApiCode"
        private const val TAR_TASK_NAME: String = "tarApi"
        private const val ARCHIVE_EXTENSION = "tar.gz"
        private const val JSON_EXTENSION = "json"
        private const val YAML_EXTENSION = "yaml"
        private const val LOCAL_PUBLISH_TASK_NAME = "publishMavenPublicationToMavenLocal"
        private const val REMOTE_SNAPSHOT_PUBLISH_TASK_NAME = "publishMavenPublicationToArtifactorySnapshotsRepository"
        private const val REMOTE_RELEASE_PUBLISH_TASK_NAME = "publishMavenPublicationToArtifactoryReleasesRepository"
        private const val EXTENSION_NAME = "restContractSupport"
    }

    override fun apply(project: Project) {
        project
            .applyPlugin(DependencyHelper.Plugins.node.id)
            .applyPlugin(KotlinPluginIdentifiers.buildconventionsKotlinJvm)
            .applyPlugin(VersioningPluginIdentifiers.buildconventionsVersioning)
            .applyPlugin(BaseGradlePluginIdentifiers.mavenPublish)
            .applyPlugin(BaseGradlePluginIdentifiers.javaLibrary)

        project.platformDependency(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, DependencyHelper.springBom)
        project.compileOnlyDependency(DependencyHelper.jakarta)
        project.compileOnlyDependency(DependencyHelper.springWeb)
        project.compileOnlyDependency(DependencyHelper.springContext)
        project.compileOnlyDependency(DependencyHelper.jacksonAnnotations)

        val extension = project.extensions.create(EXTENSION_NAME, RestContractSupportExtension::class.java).apply {
            disableLinting.convention(false)
            inputFile.convention(project.layout.projectDirectory.file("src/main/openapi/${project.name}.json"))
            packageName.convention(defaultPackageName(project))
            generateClient.convention(false)
            generateModel.convention(true)
            generateController.convention(true)
            generatedSourcesOutputDir.convention(project.layout.buildDirectory.dir("generated/sources/openapi"))
            controllerOptions.convention(emptySet())
            modelOptions.convention(emptySet())
            clientOptions.convention(emptySet())
        }

        project.configurations.maybeCreate(API_DEPENDENCY_CONFIG_NAME)

        val tasks = project.tasks

        registerCleanTask(tasks, extension)
        registerLintTask(tasks, extension, project)
        registerDownloadTask(tasks, extension, project)
        registerCompileTask(tasks, extension, project)
        registerDocsTask(tasks, project)
        registerTarTask(tasks, project)
        registerCodeGenerationTask(tasks, extension, project)

        project.afterEvaluate { registerPublications(project) }
    }

    private fun registerLintTask(tasks: TaskContainer, extension: RestContractSupportExtension, project: Project) {
        tasks.register(LINT_TASK_NAME, NpxTask::class.java) { task ->
            task.group = LifecycleBasePlugin.VERIFICATION_GROUP
            task.command.set("@stoplight/spectral-cli@~6.6.0")
            task.onlyIf { !extension.disableLinting.get() }
            task.dependsOn(DOWNLOAD_TASK_NAME)
            task.doFirst(object : Action<Task> {
                override fun execute(t: Task) {
                    if (!project.projectDir.resolve("spectral.yaml").exists()) {
                        (project.projectDir.resolve("spectral.yaml")).writeText(
                            """
                            extends: ["spectral:oas"]
                            
                            rules:
                              oas3-unused-component: info
                            """.trimIndent()
                        )
                    }
                }
            })
            task.args.set(
                project.provider {
                    listOf(
                        "lint",
                        "--fail-severity=warn",
                        "--ruleset=spectral.yaml",
                        extension.inputFile.get().asFile.absolutePath
                    )
                }
            )
            task.addTaskThatDependsOnThisByName("check")
        }
    }

    private fun registerTarTask(tasks: TaskContainer, project: Project) {
        tasks.register(TAR_TASK_NAME, Tar::class.java) { task ->
            task.group = BasePlugin.BUILD_GROUP
            task.dependsOn(DOCS_TASK_NAME, COMPILE_TASK_NAME)
            task.compression = Compression.GZIP
            task.archiveExtension.set(ARCHIVE_EXTENSION)
            task.archiveFileName.set(project.provider { "${project.name}.$ARCHIVE_EXTENSION" })
            task.destinationDirectory.set(project.layout.buildDirectory.dir("tar"))
            task.from(
                project.fileTree(extendedOutputDir(project)),
                project.fileTree(documentsOutputDir(project))
            )
            task.addTaskThatDependsOnThisByName("assemble")
        }
    }

    private fun registerDocsTask(tasks: TaskContainer, project: Project) {
        tasks.register(DOCS_TASK_NAME, GenerateTask::class.java) { task ->
            task.group = BasePlugin.BUILD_GROUP
            task.dependsOn(COMPILE_TASK_NAME)
            task.logging.captureStandardOutput(LogLevel.DEBUG)
            task.logging.captureStandardError(LogLevel.DEBUG)
            task.generatorName.set("html2")
            task.inputSpec.set(compiledJsonFile(project).map { it.asFile.absolutePath })
            task.outputDir.set(documentsOutputDir(project).map { it.asFile.absolutePath })
            task.doLast(object : Action<Task> {
                val docsOutputDir = documentsOutputDir(project).get().asFile
                override fun execute(t: Task) {
                    deleteIfExists(File(project.rootDir, "openapitools.json"))
                    deleteIfExists(docsOutputDir.resolve(".openapi-generator"))
                    deleteIfExists(docsOutputDir.resolve(".openapi-generator-ignore"))
                    deleteIfExists(docsOutputDir.resolve("README.md"))
                }
            })
        }
    }

    private fun registerCompileTask(tasks: TaskContainer, extension: RestContractSupportExtension, project: Project) {
        tasks.register(COMPILE_TASK_NAME) { task ->
            task.group = BasePlugin.BUILD_GROUP
            task.dependsOn(DOWNLOAD_TASK_NAME)
            (project.properties["sourceSets"] as SourceSetContainer?)!!.getByName("main").resources.srcDir(project.layout.buildDirectory.dir("generated/resources/openapi"))
            task.doLast {
                consolidateSpec(object : OpenApiKotlinConsolidatorParameters {
                    override val sourceSpecFile: RegularFileProperty = extension.inputFile
                    override val sourceSpecUri: Property<URI> = project.objects.property(URI::class.java)
                    override val consolidatedSpecOutputDirectory: DirectoryProperty = project.objects.directoryProperty().value(extendedOutputDir(project))
                    override val specificationName: Property<String> = project.objects.property(String::class.java).value("${project.name}-compiled")
                    override val versionOverride: Property<String> = project.objects.property(String::class.java).value(project.version.toString())
                })
            }
            task.addTaskThatDependsOnThisByName("processResources")
        }
    }

    private fun registerDownloadTask(tasks: TaskContainer, extension: RestContractSupportExtension, project: Project) {
        tasks.register(DOWNLOAD_TASK_NAME) { task ->
            task.group = "Build Setup"
            task.doLast {
                project.configurations.getByName(API_DEPENDENCY_CONFIG_NAME).run {
                    dependencies.forEach { dependency ->
                        val dependencyFile = files(dependency).first()
                        val dependencyArtifactId = dependency.name
                        val destinationDirectory = extension.inputFile.get().asFile.parentFile.resolve(".dependencies/$dependencyArtifactId")
                        if (destinationDirectory.exists()) {
                            destinationDirectory.deleteRecursively()
                        }
                        destinationDirectory.mkdirs()
                        if (dependencyFile.name.endsWith(".$ARCHIVE_EXTENSION")) {
                            project.copy {
                                it.from(project.tarTree(dependencyFile))
                                it.into(destinationDirectory)
                            }
                        } else if (dependencyFile.name.matches(""".*\.(zip|jar)""".toRegex())) {
                            project.copy {
                                it.from(project.zipTree(dependencyFile))
                                it.into(destinationDirectory)
                            }
                        } else {
                            val fileExtension = dependencyFile.name.replace(".*$dependencyArtifactId-${dependency.version}\\.".toRegex(), "")
                            dependencyFile.copyTo(File(destinationDirectory, "$dependencyArtifactId.$fileExtension"))
                        }
                    }
                }
            }
        }
    }

    private fun registerCleanTask(tasks: TaskContainer, extension: RestContractSupportExtension) {
        tasks.register(CLEAN_TASK_NAME) { task ->
            task.group = BasePlugin.BUILD_GROUP
            task.addTaskThatDependsOnThisByName("clean")
            task.doLast {
                deleteIfExists(extension.inputFile.get().asFile.parentFile.resolve(".dependencies"))
            }
        }
    }

    private fun registerCodeGenerationTask(tasks: TaskContainer, extension: RestContractSupportExtension, project: Project) {
        tasks.register(GENERATE_TASK_NAME, CodeGenerationTask::class.java) { task ->
            task.dependsOn(COMPILE_TASK_NAME)
            task.addTaskThatDependsOnThisByName("compileKotlin")
            task.addTaskThatDependsOnThisByName("processResources")
            task.addTaskThatDependsOnThisByName("runKtlintCheckOverMainSourceSet")
            task.addTaskThatDependsOnThisByName("sourcesJar")
            task.onlyIf {
                extension.generateClient.getOrElse(false) || extension.generateModel.getOrElse(true) || extension.generateController.getOrElse(true)
            }
            task.consolidatedSpecInputFile.set(compiledJsonFile(project))
            task.finalPackageName.set(fullPackageName(extension, project))
            (project.properties["sourceSets"] as SourceSetContainer?)?.getByName("main")?.java?.srcDir(extension.generatedSourcesOutputDir)
        }
    }

    private fun registerPublications(project: Project) {
        project.registerMavenRepository().apply {
            publications { publications ->
                publications.register(DEFAULT_PUBLICATION_NAME, MavenPublication::class.java) { mp ->
                    mp.groupId = project.group.toString()
                    mp.artifactId = artifactId(project)
                    mp.version = project.version.toString()
                    mp.from(project.components.getByName(JAVA_COMPONENT_NAME))
                    mp.artifact(project.buildDir.resolve("tar/${project.name}.$ARCHIVE_EXTENSION").absolutePath) { ma ->
                        ma.extension = ARCHIVE_EXTENSION
                    }
                    mp.artifact(compiledJsonFile(project)) { ma ->
                        ma.extension = JSON_EXTENSION
                    }
                    mp.artifact(compiledYamlFile(project)) { ma ->
                        ma.extension = YAML_EXTENSION
                    }
                }
            }
        }
        project.tasks.findByName(LOCAL_PUBLISH_TASK_NAME)?.dependsOn(TAR_TASK_NAME)
        project.tasks.findByName(REMOTE_SNAPSHOT_PUBLISH_TASK_NAME)?.dependsOn(TAR_TASK_NAME)
        project.tasks.findByName(REMOTE_RELEASE_PUBLISH_TASK_NAME)?.dependsOn(TAR_TASK_NAME)
    }

    private fun deleteIfExists(file: File) {
        if (file.exists()) {
            file.deleteRecursively()
        }
    }

    private fun defaultPackageName(project: Project): String = "com.projectronin.rest.${project.name.lowercase().replace("[^a-z]|contract|messaging|openapi|rest}".toRegex(), "")}"
    private fun versionInfix(project: Project): String = "v${project.version.toString().replace("^([0-9]+)\\..+".toRegex(), "$1")}"
    private fun fullPackageName(settings: RestContractSupportExtension, project: Project): Provider<String> = settings.packageName.map { configuredName -> "$configuredName.${versionInfix(project)}" }
    private fun artifactId(project: Project): String = "${project.name}-${versionInfix(project)}"
    private fun rootOutputDir(project: Project): Provider<Directory> = project.layout.buildDirectory.dir("generated/resources/openapi")
    private fun extendedOutputDir(project: Project): Provider<Directory> = rootOutputDir(project).map { it.dir("META-INF/resources/${versionInfix(project)}") }
    private fun compiledJsonFile(project: Project): Provider<RegularFile> = extendedOutputDir(project).map { it.file("${project.name}-compiled.json") }
    private fun compiledYamlFile(project: Project): Provider<RegularFile> = extendedOutputDir(project).map { it.file("${project.name}-compiled.yaml") }
    private fun documentsOutputDir(project: Project): Provider<Directory> = project.layout.buildDirectory.dir("openapidocs")
}
