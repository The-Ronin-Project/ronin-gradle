package com.projectronin.json.contract.task

import com.fasterxml.jackson.databind.json.JsonMapper
import com.networknt.schema.JsonMetaSchema
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.NonValidationKeyword
import com.networknt.schema.SpecVersion
import com.networknt.schema.ValidationMessage
import com.networknt.schema.uri.URIFactory
import com.projectronin.json.contract.EventContractExtension
import com.projectronin.json.contract.eventContractExtension
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

/**
 * Executes Schema Validation tests against the present schema and examples.
 */
open class TestTask : BaseJsonContractTask() {
    private val mapper = JsonMapper()

    /**
     * Performs the schema test action. If successful, a List of all schema files inspected will be returned. If any schema fails,
     * the failure details will be logged as errors and a [GradleException] will be thrown.
     */
    @TaskAction
    fun testSchema(): List<String> {
        val config = project.eventContractExtension()

        val allSchemaFiles = mutableListOf<File>()
        val directory = config.schemaSourceDir.get().asFile

        val errorsByExampleByVersion = run {
            logger.lifecycle("Testing schemas for ${directory.name}")

            val schemaFiles = directory.listFiles { f -> f.name.endsWith(".schema.json") }?.toList() ?: emptyList()
            allSchemaFiles.addAll(schemaFiles)
            val errorsByExample = when (schemaFiles.size) {
                0 -> throw IllegalStateException("No schema files found in ${directory.name}")
                1 -> testSingleSchema(schemaFiles.first())
                else -> testMultipleSchema(schemaFiles)
            }

            logger.lifecycle("")

            errorsByExample.ifEmpty {
                null
            }
        }

        if (errorsByExampleByVersion.isNullOrEmpty()) {
            return allSchemaFiles.map { it.name }
        }

        errorsByExampleByVersion.forEach { (example, errors) ->
            errors.forEach { error ->
                logger.error("$example: $error")
            }
            logger.error("")
        }

        throw GradleException("Test failures occurred")
    }

    /**
     * Tests the schema [file] from the version [directory], returning any [ValidationMessage]s associated to their example file.
     * This will run the schema against all examples present in the [directory].
     */
    private fun testSingleSchema(file: File): Map<String, Set<ValidationMessage>> {
        val schema = getSchema(file)

        val examples = getExamples(project.eventContractExtension().exampleSourceDir.get().asFile)
        if (examples.isEmpty()) {
            logger.lifecycle("${file.name} NO TESTS")
            return emptyMap()
        }

        val results = testExamples(schema, examples)
        logger.lifecycle("${file.name} ${if (results.isEmpty()) "PASSED" else "FAILED"}")
        return results
    }

    /**
     * Tests all of the supplied schema [files] from the version [directory], returning any [ValidationMessage]s associated to their example file.
     * This will run each schema against the examples associated to it in the [directory]. The examples are determined by the schema's core name, which is the value prior to the "-vN.schema.json"
     */
    private fun testMultipleSchema(files: List<File>): Map<String, Set<ValidationMessage>> {
        return files.sortedBy { it.name }.flatMap { file ->
            val schema = getSchema(file)
            val primarySchemaName = Regex("(.+).schema.json").matchEntire(file.name)!!.destructured.component1().replace("-v\\d+".toRegex(), "")

            val examples = getExamples(project.eventContractExtension().exampleSourceDir.get().asFile) { it.name.startsWith(primarySchemaName) }
            val results = testExamples(schema, examples)
            if (results.isEmpty()) {
                logger.lifecycle("${file.name} ${if (examples.isEmpty()) "NO TESTS" else "PASSED"}")
                emptySet()
            } else {
                logger.lifecycle("${file.name} FAILED")
                results.entries.map { it.toPair() }
            }
        }.toMap()
    }

    /**
     * Tests the supplied [examples] against the [schema], returning any [ValidationMessage]s associated to their example.
     */
    private fun testExamples(schema: JsonSchema, examples: List<File>): Map<String, Set<ValidationMessage>> {
        return examples.mapNotNull { example ->
            val node = mapper.readTree(example)
            val errors = schema.validate(node)
            if (errors.isEmpty()) {
                null
            } else {
                example.name to errors
            }
        }.toMap()
    }

    /**
     * Retrieves the [JsonSchema] built from the [schema].
     */
    private fun getSchema(schema: File): JsonSchema {
        val schemaUri = schema.toURI()

        val schemaNode = mapper.readTree(schema)
        val schemaId = schemaNode["\$id"]?.asText()?.removeSuffix(schema.name)

        // This is needed in order to hijack the local references for our $id based schemas. Without this, it will attempt
        // to reach out to a remote server hosting the schema, which is not currently our intention, but could be configured in future versions.
        val uriFactory = object : URIFactory {
            override fun create(uri: String): URI {
                return try {
                    URI.create(uri)
                } catch (e: IllegalArgumentException) {
                    throw IllegalArgumentException("Unable to create URI.", e)
                }
            }

            override fun create(baseURI: URI, segment: String): URI {
                val start = if (schemaId != null && baseURI.toString().startsWith(schemaId)) {
                    getLocalUri(schemaUri, schemaId, baseURI)
                } else {
                    baseURI
                }

                val uri = try {
                    URL(start.toURL(), segment).toURI()
                } catch (e: MalformedURLException) {
                    throw java.lang.IllegalArgumentException("Unable to create URI.", e)
                } catch (e: URISyntaxException) {
                    throw java.lang.IllegalArgumentException("Unable to create URI.", e)
                }
                return uri
            }
        }

        val config = project.eventContractExtension()
        val baseFactory = getBaseJsonSchemaFactory(config)
        val factory = JsonSchemaFactory.builder(baseFactory)
            .uriFactory(uriFactory, "http", "https")
            .build()
        return factory.getSchema(schemaUri)
    }

    /**
     * Creates the base [JsonSchemaFactory] based off the supplied [config].
     */
    private fun getBaseJsonSchemaFactory(config: EventContractExtension): JsonSchemaFactory {
        return if (config.ignoredValidationKeywords.get().isEmpty()) {
            JsonSchemaFactory.getInstance(config.specVersion.get())
        } else {
            val baseMetaSchema = when (config.specVersion.get()) {
                SpecVersion.VersionFlag.V4 -> JsonMetaSchema.getV4()
                SpecVersion.VersionFlag.V6 -> JsonMetaSchema.getV6()
                SpecVersion.VersionFlag.V7 -> JsonMetaSchema.getV7()
                SpecVersion.VersionFlag.V201909 -> JsonMetaSchema.getV201909()
                SpecVersion.VersionFlag.V202012 -> JsonMetaSchema.getV202012()
                else -> throw IllegalStateException("SpecVersion ${config.specVersion} is not currently supported.")
            }

            val keywords = config.ignoredValidationKeywords.get().map { NonValidationKeyword(it) }.toList()
            val metaSchema = JsonMetaSchema.builder(baseMetaSchema.uri, baseMetaSchema).addKeywords(keywords).build()
            JsonSchemaFactory.Builder().defaultMetaSchemaURI(metaSchema.uri).addMetaSchema(metaSchema).build()
        }
    }

    /**
     * Determines the local URI based off the initial [schemaURI], the base [schemaId], and the current [baseURI] being sought out.
     */
    private fun getLocalUri(schemaURI: URI, schemaId: String, baseURI: URI): URI {
        val schemaUriString = schemaURI.toString()
        val baseSchemaURI = schemaUriString.substring(0, schemaUriString.lastIndexOf("/"))

        val relative = baseURI.toString().removePrefix(schemaId)
        return URI("$baseSchemaURI/$relative")
    }

    /**
     * Retrieves the examples from the [directory] and applies a [filter] to the returned files. If no [filter] is provided, all files in the [directory] will be returned.
     */
    private fun getExamples(directory: File, filter: (File) -> Boolean = { true }): List<File> =
        directory.listFiles(filter)?.toList()
            ?: emptyList()
}
