package com.projectronin.openapi

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class OpenApiKotlinGeneratorTest {

    @Test
    fun `plugin registers task`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder().build()

        project.plugins.apply("com.projectronin.openapi")

        // Verify the result
        assertThat(project.tasks.findByName("generateOpenApiCode")).isNotNull()
    }
}
