package com.projectronin.json.contract

import com.networknt.schema.SpecVersion
import com.projectronin.gradle.helpers.DEFAULT_PUBLICATION_NAME
import com.projectronin.gradle.helpers.JAVA_COMPONENT_NAME
import com.projectronin.gradle.helpers.applyPlugin
import com.projectronin.gradle.helpers.compileOnlyDependency
import com.projectronin.gradle.helpers.registerMavenRepository
import com.projectronin.json.contract.task.DocumentationTask
import com.projectronin.json.contract.task.TestTask
import com.projectronin.roninbuildconventionsversioning.PluginIdentifiers
import com.projectronin.ronincontractjsonplugin.DependencyHelper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.Directory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import org.jsonschema2pojo.gradle.JsonSchemaExtension
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/**
 * The JsonContractPlugin provides access to a set of tasks capable of validating and generating documentation for schemas.
 */
class JsonContractPlugin : Plugin<Project> {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(JsonContractPlugin::class.java)

        object Locations {
            private const val defaultSourceDir = "src/main/resources/schemas"
            fun schemaSourceDir(project: Project): Directory = project.layout.projectDirectory.dir(defaultSourceDir)
            private const val defaultExamplesDir = "src/test/resources/examples"
            fun exampleSourceDir(project: Project): Directory = project.layout.projectDirectory.dir(defaultExamplesDir)
            fun defaultPackageName(project: Project): String = "com.projectronin.json.${project.name.lowercase().replace("[^a-z]|contract|messaging".toRegex(), "")}"
            private fun versionInfix(project: Project): String = "v${project.version.toString().replace("^([0-9]+)\\..+".toRegex(), "$1")}"
            fun fullPackageName(settings: JsonContractExtension, project: Project): String = "${settings.packageName.get()}.${versionInfix(project)}"
            fun artifactId(project: Project): String = "${project.name}-${versionInfix(project)}"

            fun tarDir(project: Project): File = File("${project.buildDir}/tar")

            const val archiveExtension = "tar.gz"
            const val archiveClassifier = "schemas"
            fun archiveFileName(project: Project): String = "${project.name}-$archiveClassifier.$archiveExtension"
            fun dependencyDir(settings: JsonContractExtension): File = settings.schemaSourceDir.get().asFile.resolve(".dependencies")
        }

        object ContractTasks {
            const val testContracts = "testContracts"
            const val generateContractDocs = "generateContractDocs"
            const val createSchemaTar = "createSchemaTar"
            const val downloadSchemaDependencies = "downloadSchemaDependencies"
        }

        object ExternalTasks {
            const val check = "check"
            const val clean = "clean"
            const val jar = "jar"
            const val assemble = "assemble"
            const val generateJsonSchema2Pojo = "generateJsonSchema2Pojo"
            const val localPublishTask = "publishMavenPublicationToMavenLocal"

            const val remoteSnapshotPublishTask = "publishMavenPublicationToArtifactorySnapshotsRepository"
            const val remoteReleasePublishTask = "publishMavenPublicationToArtifactoryReleasesRepository"
        }

        object DependentPlugins {
            const val base: String = "base"
            const val java: String = "java"
            const val mavenPublish: String = "maven-publish"
        }

        object Extensions {
            const val axionRelease = "scmVersion"
            const val jsonSchema2Pojo = "jsonSchema2Pojo"
        }

        object DependencyScopes {
            const val compileOnly = "compileOnly"
            const val schemaDependency = "schemaDependency"
        }
    }

    override fun apply(project: Project) {
        project
            .applyPlugin(DependentPlugins.base)
            .applyPlugin(DependentPlugins.java)
            .applyPlugin(DependentPlugins.mavenPublish)
            .applyPlugin(DependencyHelper.Plugins.jsonschema2pojo.id)
            .applyPlugin(PluginIdentifiers.buildconventionsVersioning)

        project.compileOnlyDependency(DependencyHelper.jacksonAnnotations)

        project.configurations.create(DependencyScopes.schemaDependency)

        val extension = project.extensions.create(JsonContractExtension.NAME, JsonContractExtension::class.java).apply {
            specVersion.convention(SpecVersion.VersionFlag.V201909)
            ignoredValidationKeywords.convention(emptyList())
            schemaSourceDir.convention(Locations.schemaSourceDir(project))
            exampleSourceDir.convention(Locations.exampleSourceDir(project))
            packageName.convention(Locations.defaultPackageName(project))
        }

        (project.extensions.findByType(JsonSchemaExtension::class.java) ?: project.extensions.create(Extensions.jsonSchema2Pojo, JsonSchemaExtension::class.java)).apply {
            sourceFiles = listOf(extension.schemaSourceDir.asFile.get())
            targetPackage = Locations.fullPackageName(extension, project)
        }

        val testTask = project.tasks.register(ContractTasks.testContracts, TestTask::class.java)
        project.tasks.getByName(ExternalTasks.check) {
            it.dependsOn(testTask)
        }

        project.tasks.register(ContractTasks.generateContractDocs, DocumentationTask::class.java)

        val tarDir = Locations.tarDir(project)

        project.tasks.register(ContractTasks.createSchemaTar, Tar::class.java) { task ->
            task.group = BasePlugin.BUILD_GROUP
            task.dependsOn(ExternalTasks.jar)
            task.compression = Compression.GZIP
            task.archiveExtension.set(Locations.archiveExtension)
            task.archiveFileName.set(Locations.archiveFileName(project))
            task.destinationDirectory.set(tarDir)
            task.from(project.fileTree(extension.schemaSourceDir))
        }

        project.tasks.findByName(ExternalTasks.assemble)?.dependsOn(ContractTasks.createSchemaTar)

        registerPublications(
            project,
            tarDir.resolve(Locations.archiveFileName(project))
        )
        registerDownloadTask(extension, project)
    }

    private fun registerDownloadTask(
        settings: JsonContractExtension,
        project: Project
    ) {
        val outputDir = Locations.dependencyDir(settings)
        project.tasks.register(ContractTasks.downloadSchemaDependencies) { task ->
            task.group = "Build Setup"
            task.doLast {
                project.configurations.findByName(DependencyScopes.schemaDependency)?.run {
                    dependencies
                        .forEach { dependency ->
                            downloadAndExtractDependency(dependency, outputDir, project)
                        }
                }
            }
        }
        project.tasks.getByName(ContractTasks.testContracts).dependsOn(ContractTasks.downloadSchemaDependencies)
        project.tasks.getByName(ContractTasks.createSchemaTar).dependsOn(ContractTasks.downloadSchemaDependencies)
        project.tasks.getByName(ExternalTasks.generateJsonSchema2Pojo).dependsOn(ContractTasks.downloadSchemaDependencies)

        project.tasks.getByName(ExternalTasks.clean) {
            val clean = it as Delete
            clean.delete += listOf(
                outputDir.relativeTo(project.rootDir)
            )
        }
    }

    private fun Configuration.downloadAndExtractDependency(dependency: Dependency, outputDir: File, project: Project) {
        when (val dependencyFile = files(dependency).firstOrNull()) {
            null -> logger.warn("No dependency files for $dependency")
            else ->
                if (dependencyFile.name.endsWith("-${Locations.archiveClassifier}.${Locations.archiveExtension}")) {
                    val dependencyArtifactId = dependency.name
                    val outputFile = outputDir.resolve(dependencyArtifactId)
                    if (outputFile.exists()) {
                        outputFile.deleteRecursively()
                    }
                    outputFile.mkdirs()
                    project.copy {
                        it.from(project.tarTree(dependencyFile))
                        it.into(outputFile)
                    }
                }
        }
    }

    private fun registerPublications(
        project: Project,
        tarFileLocation: File
    ) {
        project.registerMavenRepository().apply {
            publications { publications ->
                publications.register(DEFAULT_PUBLICATION_NAME, MavenPublication::class.java) { mp ->
                    mp.groupId = project.group.toString()
                    mp.artifactId = Locations.artifactId(project)
                    mp.version = project.version.toString()
                    mp.from(project.components.getByName(JAVA_COMPONENT_NAME))
                    mp.artifact(tarFileLocation.absolutePath) { ma ->
                        ma.extension = Locations.archiveExtension
                        ma.classifier = Locations.archiveClassifier
                    }
                }
            }
        }
        project.tasks.findByName(ExternalTasks.localPublishTask)?.dependsOn(ContractTasks.createSchemaTar)
        project.tasks.findByName(ExternalTasks.remoteSnapshotPublishTask)?.dependsOn(ContractTasks.createSchemaTar)
        project.tasks.findByName(ExternalTasks.remoteReleasePublishTask)?.dependsOn(ContractTasks.createSchemaTar)
    }
}
