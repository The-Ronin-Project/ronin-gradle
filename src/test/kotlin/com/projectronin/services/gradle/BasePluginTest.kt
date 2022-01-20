package com.projectronin.services.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BasePluginTest {
    private lateinit var project: Project

    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.projectronin.services.gradle.base")
    }

    @Test
    fun `includes interop base plugin`() {
        assertNotNull(project.plugins.findPlugin("com.projectronin.interop.gradle.base"))
    }

    @Test
    fun `includes interop junit plugin`() {
        assertNotNull(project.plugins.findPlugin("com.projectronin.interop.gradle.junit"))
    }

    @Test
    fun `includes interop jacoco plugin`() {
        assertNotNull(project.plugins.findPlugin("com.projectronin.interop.gradle.jacoco"))
    }

    @Test
    fun `includes interop publish`() {
        assertNotNull(project.plugins.findPlugin("com.projectronin.interop.gradle.publish"))
    }
}
