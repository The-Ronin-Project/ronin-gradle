package com.projectronin.json.contract.task

import com.networknt.schema.SpecVersion
import com.projectronin.json.contract.EventContractExtension
import org.apache.commons.io.FileUtils
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

class TestTaskTest {
    private lateinit var extension: EventContractExtension

    private fun getTask(testDirectory: String): TestTask {
        val defaultProject = ProjectBuilder.builder().build()

        val projectDir = File("${defaultProject.projectDir}/src/test/resources/test/$testDirectory")
        val newProjectDir = projectDir
        newProjectDir.mkdirs()

        val testResourceDirectory = File(javaClass.classLoader.getResource("test/$testDirectory")!!.file)
        FileUtils.copyDirectory(testResourceDirectory, newProjectDir)

        val project =
            ProjectBuilder.builder()
                .withProjectDir(projectDir)
                .build()
        project.plugins.apply("com.projectronin.json.contract")
        extension = project.extensions.getByType(EventContractExtension::class.java).apply {
            exampleSourceDir.set(projectDir.resolve("v1/examples"))
            schemaSourceDir.set(projectDir.resolve("v1"))
        }
        return project.tasks.getByName("testEvents") as TestTask
    }

    @Test
    fun `no version directories found`() {
        val task = getTask("no-versions")
        val exception = assertThrows<IllegalStateException> { task.testSchema() }
        assertEquals("No schema files found in v1", exception.message)
    }

    @Test
    fun `no schema files found for version`() {
        val task = getTask("no-schema")
        val exception = assertThrows<IllegalStateException> { task.testSchema() }
        assertEquals("No schema files found in v1", exception.message)
    }

    @Test
    fun `single schema file has no examples`() {
        val task = getTask("no-examples")
        val schemas = task.testSchema()
        assertEquals(listOf("person-v1.schema.json"), schemas)
    }

    @Test
    fun `single schema file has single example file fail`() {
        val task = getTask("single-example-fail")
        val exception = assertThrows<GradleException> { task.testSchema() }
        assertEquals("Test failures occurred", exception.message)
    }

    @Test
    fun `single schema file has multiple example files fail`() {
        val task = getTask("multiple-examples-fail")
        val exception = assertThrows<GradleException> { task.testSchema() }
        assertEquals("Test failures occurred", exception.message)
    }

    @Test
    fun `single schema file passes all examples`() {
        val task = getTask("single-schema-passes")
        val schemas = task.testSchema()
        assertEquals(listOf("person-v1.schema.json"), schemas)
    }

    @Test
    fun `schema file with local reference for schema with id can pass`() {
        val task = getTask("references-pass")
        val schemas = task.testSchema()
        assertEquals(listOf("person-list-v1.schema.json"), schemas)
    }

    @Test
    fun `schema file with local reference for schema with id can fail`() {
        val task = getTask("references-fail")
        val exception = assertThrows<GradleException> { task.testSchema() }
        assertEquals("Test failures occurred", exception.message)
    }

    @Test
    fun `schema file with local reference containing another local reference can pass`() {
        val task = getTask("reference-to-reference")
        val schemas = task.testSchema()
        assertEquals(listOf("person-list-v1.schema.json"), schemas)
    }

    @Test
    fun `ignored keywords supported for V4`() {
        val task = getTask("ignored-keyword")
        extension.specVersion.set(SpecVersion.VersionFlag.V4)
        extension.ignoredValidationKeywords.set(listOf("unknownKeyword", "\$id"))

        val schemas = task.testSchema()
        assertEquals(listOf("person-v1.schema.json"), schemas)
    }

    @Test
    fun `ignored keywords supported for V6`() {
        val task = getTask("ignored-keyword")
        extension.specVersion.set(SpecVersion.VersionFlag.V6)
        extension.ignoredValidationKeywords.set(listOf("unknownKeyword"))

        val schemas = task.testSchema()
        assertEquals(listOf("person-v1.schema.json"), schemas)
    }

    @Test
    fun `ignored keywords supported for V7`() {
        val task = getTask("ignored-keyword")
        extension.specVersion.set(SpecVersion.VersionFlag.V7)
        extension.ignoredValidationKeywords.set(listOf("unknownKeyword"))

        val schemas = task.testSchema()
        assertEquals(listOf("person-v1.schema.json"), schemas)
    }

    @Test
    fun `ignored keywords supported for V201909`() {
        val task = getTask("ignored-keyword")
        extension.specVersion.set(SpecVersion.VersionFlag.V201909)
        extension.ignoredValidationKeywords.set(listOf("unknownKeyword"))

        val schemas = task.testSchema()
        assertEquals(listOf("person-v1.schema.json"), schemas)
    }

    @Test
    fun `ignored keywords supported for V202012`() {
        val task = getTask("ignored-keyword")
        extension.specVersion.set(SpecVersion.VersionFlag.V202012)
        extension.ignoredValidationKeywords.set(listOf("unknownKeyword"))

        val schemas = task.testSchema()
        assertEquals(listOf("person-v1.schema.json"), schemas)
    }

    @Test
    fun `multiple schema files all pass`() {
        val task = getTask("multiple-schemas-pass")
        val schemas = task.testSchema()
        assertEquals(listOf("medication-v1.schema.json", "person-v1.schema.json"), schemas)
    }

    @Test
    fun `multiple schema files some pass, some fail`() {
        val task = getTask("multiple-schemas-mixed")
        val exception = assertThrows<GradleException> { task.testSchema() }
        assertEquals("Test failures occurred", exception.message)
    }

    @Test
    fun `multiple schema files all fail`() {
        val task = getTask("multiple-schemas-fail")
        val exception = assertThrows<GradleException> { task.testSchema() }
        assertEquals("Test failures occurred", exception.message)
    }
}
