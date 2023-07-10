package com.projectronin.json.contract

import com.networknt.schema.SpecVersion
import com.projectronin.json.contract.task.DocumentationTask
import com.projectronin.json.contract.task.TestTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.file.Directory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.internal.DefaultPublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import org.jsonschema2pojo.gradle.JsonSchemaExtension
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.allegro.tech.build.axion.release.domain.VersionConfig
import java.io.File
import java.net.URI

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
            fun versionInfix(project: Project): String = "v${project.version.toString().replace("^([0-9]+)\\..+".toRegex(), "$1")}"
            fun fullPackageName(settings: JsonContractExtension, project: Project): String = "${settings.packageName.get()}.${versionInfix(project)}"
            fun artifactId(project: Project): String = "${project.name}-${versionInfix(project)}"

            fun tarDir(project: Project): File = File("${project.buildDir}/tar")

            val archiveExtension = "tar.gz"
            val archiveClassifier = "schemas"
            fun archiveFileName(project: Project): String = "${project.name}-$archiveClassifier.$archiveExtension"
            fun dependencyDir(settings: JsonContractExtension): File = settings.schemaSourceDir.get().asFile.resolve(".dependencies")
        }

        object ContractTasks {
            val testContracts = "testContracts"
            val generateContractDocs = "generateContractDocs"
            val createSchemaTar = "createSchemaTar"
            val downloadSchemaDependencies = "downloadSchemaDependencies"
        }

        object ExternalTasks {
            val check = "check"
            val clean = "clean"
            val jar = "jar"
            val assemble = "assemble"
            val generateJsonSchema2Pojo = "generateJsonSchema2Pojo"
            val localPublishTask = "publishMavenPublicationToMavenLocal"
            val remotePublishTask = "publishMavenPublicationToNexusRepository"
        }

        object DependentPlugins {
            const val base: String = "base"
            const val java: String = "java"
            const val mavenPublish: String = "maven-publish"
            const val jsonSchema2Pojo: String = "org.jsonschema2pojo"
            const val axionRelease: String = "pl.allegro.tech.build.axion-release"
        }

        object Extensions {
            const val axionRelease = "scmVersion"
            const val jsonSchema2Pojo = "jsonSchema2Pojo"
        }

        object DependencyScopes {
            const val compileOnly = "compileOnly"
            const val schemaDependency = "schemaDependency"
        }

        object Dependencies {
            const val jacksonAnnotations = "com.fasterxml.jackson.core:jackson-annotations:2.15.2"
        }

        object ProjectProperties {
            val nexusRepoName = "nexus"
            fun nexusReleaseRepo(project: Project): String = project.properties.getOrDefault("nexus-release-repo", "https://repo.devops.projectronin.io/repository/maven-releases/").toString()
            fun nexusSnapshotRepo(project: Project): String = project.properties.getOrDefault("nexus-snapshot-repo", "https://repo.devops.projectronin.io/repository/maven-snapshots/").toString()
            fun nexusUsername(project: Project): String? = project.properties.getOrDefault("nexus-user", System.getenv("NEXUS_USER"))?.toString()
            fun nexusPassword(project: Project): String? = project.properties.getOrDefault("nexus-password", System.getenv("NEXUS_TOKEN"))?.toString()
            fun isNexusInsecure(project: Project): Boolean = project.properties.getOrDefault("nexus-insecure", "false").toString().toBoolean()
        }
    }

    private fun applyPlugin(project: Project, id: String) {
        if (!project.pluginManager.hasPlugin(id)) {
            project.pluginManager.apply(id)
        }
    }

    override fun apply(project: Project) {
        applyPlugin(project, DependentPlugins.base)
        applyPlugin(project, DependentPlugins.java)
        applyPlugin(project, DependentPlugins.mavenPublish)
        applyPlugin(project, DependentPlugins.jsonSchema2Pojo)
        applyPlugin(project, DependentPlugins.axionRelease)

        val versionExtension = (project.extensions.findByType(VersionConfig::class.java) ?: project.extensions.create(Extensions.axionRelease, VersionConfig::class.java))

        versionExtension.apply {
            tag.apply {
                initialVersion { _, _ -> "1.0.0" }
            }
            versionCreator { versionFromTag, position ->
                val branchName = System.getenv("REF_NAME")?.ifBlank { null } ?: position.branch
                val supportedHeads = setOf("master", "main")
                // this code ensures that we get a labeled version for anything that's not master, main, or version/v<NUMBER> or v<NUMBER>.<NUMBER>.<NUMBER>,
                // but that we get a PLAIN version for  master, main, or version/v<NUMBER> or v<NUMBER>.<NUMBER>.<NUMBER>,
                // The jiraBranchRegex tries to identify a ticket project-<NUMBER> format and uses that as the label
                if (!supportedHeads.contains(branchName) && !branchName.matches("^version/v\\d+$".toRegex()) && !branchName.matches("^v\\d+\\.\\d+\\.\\d+$".toRegex())) {
                    val jiraBranchRegex = Regex("(?:.*/)?(\\w+)-(\\d+)(?:-(.+))?")
                    val match = jiraBranchRegex.matchEntire(branchName)
                    val branchExtension = match?.let {
                        val (jiraProject, ticketNumber, _) = it.destructured
                        "$jiraProject$ticketNumber"
                    } ?: branchName

                    "$versionFromTag-$branchExtension"
                } else {
                    versionFromTag
                }
            }
        }

        project.version = versionExtension.version

        project.dependencies.apply {
            add(DependencyScopes.compileOnly, Dependencies.jacksonAnnotations)
        }

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
                    val destinationDirectory = outputFile
                    if (destinationDirectory.exists()) {
                        destinationDirectory.deleteRecursively()
                    }
                    destinationDirectory.mkdirs()
                    project.copy {
                        it.from(project.tarTree(dependencyFile))
                        it.into(destinationDirectory)
                    }
                }
        }
    }

    private fun registerPublications(
        project: Project,
        tarFileLocation: File
    ) {
        if (project.extensions.findByName(PublishingExtension.NAME) == null) {
            project.extensions.create(PublishingExtension::class.java, PublishingExtension.NAME, DefaultPublishingExtension::class.java)
        }
        (project.extensions.getByName(PublishingExtension.NAME) as PublishingExtension).run {
            if (repositories.find { r -> r is MavenArtifactRepository } == null) {
                project.logger.info("Adding maven repository")
                repositories { rh ->
                    rh.maven { mar ->
                        mar.name = ProjectProperties.nexusRepoName
                        mar.isAllowInsecureProtocol = ProjectProperties.isNexusInsecure(project)
                        mar.credentials { pc ->
                            pc.username = ProjectProperties.nexusUsername(project)
                            pc.password = ProjectProperties.nexusPassword(project)
                        }
                        mar.url = URI(
                            if (project.version.toString().contains("SNAPSHOT")) {
                                ProjectProperties.nexusSnapshotRepo(project)
                            } else {
                                ProjectProperties.nexusReleaseRepo(project)
                            }
                        )
                    }
                }
            }
            publications { publications ->
                publications.register("Maven", MavenPublication::class.java) { mp ->
                    mp.groupId = project.group.toString()
                    mp.artifactId = Locations.artifactId(project)
                    mp.version = project.version.toString()
                    mp.from(project.components.getByName("java"))
                    mp.artifact(tarFileLocation.absolutePath) { ma ->
                        ma.extension = Locations.archiveExtension
                        ma.classifier = Locations.archiveClassifier
                    }
                }
            }
        }
        project.tasks.findByName(ExternalTasks.localPublishTask)?.dependsOn(ContractTasks.createSchemaTar)
        project.tasks.findByName(ExternalTasks.remotePublishTask)?.dependsOn(ContractTasks.createSchemaTar)
    }
}
