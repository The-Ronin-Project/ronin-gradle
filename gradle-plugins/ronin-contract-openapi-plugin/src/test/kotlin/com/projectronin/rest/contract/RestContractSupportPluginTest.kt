package com.projectronin.rest.contract

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

/**
 * A simple unit test for the 'com.projectronin.rest.contract.greeting' plugin.
 */
class RestContractSupportPluginTest {
    @Test
    fun `plugin registers task`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.projectronin.openapi.contract")

        // Verify the result
        assertThat(project.tasks.findByName("lintApi")).isNotNull()
    }
}
