package com.projectronin.jsonschema

import org.jsonschema2pojo.GenerationConfig
import org.jsonschema2pojo.Jsonschema2Pojo
import org.jsonschema2pojo.gradle.GradleRuleLogger
import org.jsonschema2pojo.gradle.JsonSchemaExtension
import java.io.FileFilter
import java.net.URL

plugins {
    kotlin("jvm")
    java
}

dependencies {
    implementation("com.google.code.findbugs:jsr305:3.0.2")
}

/**
 * The logic and configuration "store" for the plugin tasks
 */
abstract class JsonSchemaImportTask : DefaultTask() {
    private val schemaBaseUrl = "https://repo.devops.projectronin.io/repository/ronin-raw/"

    @get:Input
    abstract val schema: Property<String>

    @get:Input
    abstract val namespace: Property<String>

    @get:Input
    abstract val version: Property<String>

    @get:Input
    abstract val tag: Property<String>

    @get:Input
    @get:Optional
    abstract val gitUrl: Property<String>

    @get:Input
    @get:Optional
    abstract val gitBranch: Property<String>

    private val rootDir = project.file(project.buildDir.path + "/jsonschema/$name")
    private val sourcesDir = project.file(rootDir.path + "/sources")
    private val gitRepoDir = project.file(rootDir.path + "/git")

    @get:OutputDirectory
    val generatedClasses: File = project.file(rootDir.path + "/generated")

    init {
        generatedClasses.mkdirs()
        project.tasks.compileJava {
            dependsOn(this@JsonSchemaImportTask)
        }
        project.tasks.compileKotlin {
            dependsOn(this@JsonSchemaImportTask)
        }

        project.sourceSets["main"].java.srcDir(generatedClasses)
    }

    private val config: GenerationConfig by lazy {
        JsonSchemaExtension().apply {
            sourceFiles = listOf(sourcesDir)
            targetDirectory = generatedClasses
            targetPackage = "com.projectronin.jsonschema.generated.${namespace.get()}.${schema.get()}.${version.get()}".replace("-", "")

            includeConstructors = true
            includeAllPropertiesConstructor = true
            includeGetters = false
            includeSetters = false
            includeHashcodeAndEquals = true
            includeToString = true
            includeAdditionalProperties = true
            dateType = "java.time.LocalDate"
            dateTimeType = "java.time.Instant"
            initializeCollections = true // true=collection initialized as empty collection
            includeJsr305Annotations = true
            fileFilter = FileFilter { file ->
                // Ignore the files that aren't part of the schema itself
                file.path.matches("^(.*)\\.schema\\.json\$".toRegex())
            }
        }
    }

    @TaskAction
    fun fetchSchema() {
        rootDir.mkdirs()

        if (gitUrl.isPresent && gitBranch.isPresent) {
            fetchGitSchema()
        } else {
            fetchNexusSchema()
        }

        Jsonschema2Pojo.generate(config, GradleRuleLogger(project.logger))
    }

    private fun fetchGitSchema() {
        gitRepoDir.deleteRecursively()
        project.exec {
            workingDir = rootDir
            commandLine("git", "clone", "--depth", "1", gitUrl.get(), "--branch", gitBranch.get(), "--single-branch", gitRepoDir.name)
        }

        project.copy {
            from(project.file(gitRepoDir.path + "/" + version.get()))
            into(sourcesDir)
        }
    }

    private fun fetchNexusSchema() {
        val artifactName = "${schema.get()}-${version.get()}-${tag.get()}.tar.gz"
        val artifactFile = project.file(rootDir.path + "/$artifactName")

        if (!artifactFile.exists()) {
            artifactFile.writeBytes(URL("$schemaBaseUrl${namespace.get()}/${schema.get()}/$artifactName").readBytes())
        }

        project.copy {
            from(project.tarTree(artifactFile))
            into(sourcesDir)
        }
    }
}

/**
 * The actual interface between the plugin and the consuming build.gradle file
 */
abstract class JsonSchemasExtension(private val project: Project) {
    fun schema(name: String, version: String, tag: String, namespace: String = "contract-event") {
        project.tasks.create<JsonSchemaImportTask>(formatName(name, version, namespace)) {
            this.namespace.set(namespace)
            this.schema.set(name)
            this.version.set(version)
            this.tag.set(tag)
        }
    }

    fun branchSchema(name: String, version: String, url: String, branch: String, namespace: String = "contact-event") {
        project.tasks.create<JsonSchemaImportTask>(formatName(name, version, namespace)) {
            this.namespace.set(namespace)
            this.schema.set(name)
            this.version.set(version)
            this.tag.set("git-branch:$branch")
            this.gitUrl.set(url)
            this.gitBranch.set(branch)
        }
    }

    private fun formatName(name: String, version: String, namespace: String): String {
        return Regex("-[a-zA-Z]").replace("generate-$namespace-$name-$version-Classes") {
            it.value.replace("-", "").uppercase()
        }
    }
}

extensions.create<JsonSchemasExtension>("jsonSchema", project)
