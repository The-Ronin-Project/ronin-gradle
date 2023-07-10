package com.projectronin.json.contract

import com.networknt.schema.SpecVersion
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class JsonContractPluginTest {
    private fun getProject(): Project {
        val project = ProjectBuilder.builder().withName("simple-test-project").build()
        project.plugins.apply("com.projectronin.json.contract")
        return project
    }

    @Test
    fun `registers extension`() {
        val project = getProject()

        val extension = project.extensions.findByName(JsonContractExtension.NAME) as JsonContractExtension
        assertThat(extension).isNotNull
        assertThat(extension.schemaSourceDir.get().asFile.absolutePath).isEqualTo("${project.rootDir.absolutePath}/src/main/resources/schemas")
        assertThat(extension.exampleSourceDir.get().asFile.absolutePath).isEqualTo("${project.rootDir.absolutePath}/src/test/resources/examples")
        assertThat(extension.specVersion.get()).isEqualTo(SpecVersion.VersionFlag.V201909)
        assertThat(extension.ignoredValidationKeywords.get()).isEmpty()
        assertThat(extension.packageName.get()).isEqualTo("com.projectronin.json.simpletestproject")
    }

    @Test
    fun `registers testContracts task`() {
        val project = getProject()

        val testTask = project.tasks.findByName("testContracts")
        assertThat(testTask).isNotNull
    }

    @Test
    fun `associates testContracts task with check`() {
        val project = getProject()

        val checkTask = project.tasks.findByName("check")
        val testTask = project.tasks.findByName("testContracts")
        assertThat(checkTask).isNotNull()
        assertThat(checkTask?.dependsOn?.find { it is TaskProvider<*> && it.get() == testTask }).isNotNull
    }

    @Test
    fun `registers generateContractDocs task`() {
        val project = getProject()

        val docsTask = project.tasks.findByName("generateContractDocs")
        assertThat(docsTask).isNotNull()
    }
}
