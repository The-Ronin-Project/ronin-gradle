package com.projectronin.versioning

import com.projectronin.gradle.test.AbstractFunctionalTest
import com.projectronin.roninbuildconventionsversioning.PluginIdentifiers
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.Test

class VersioningPluginFunctionalTest : AbstractFunctionalTest() {

    @Test
    fun `initial version works`() {
        val result = setupAndExecuteTestProject(listOf("currentVersion", "--stacktrace"))
        assertThat(result.output).contains("Project version: 1.0.0-SNAPSHOT\n")
    }

    @Test
    fun `branch works`() {
        val result = setupAndExecuteTestProject(listOf("currentVersion", "--stacktrace")) { git ->
            git.checkout().setCreateBranch(true).setName("DASH-3096-something").call()
        }
        assertThat(result.output).contains("Project version: 1.0.0-DASH3096-SNAPSHOT\n")
    }

    @Test
    fun `branch works with ticket and no suffix`() {
        val result = setupAndExecuteTestProject(listOf("currentVersion", "--stacktrace")) { git ->
            git.checkout().setCreateBranch(true).setName("DASH-3096").call()
        }
        assertThat(result.output).contains("Project version: 1.0.0-DASH3096-SNAPSHOT\n")
    }

    @Test
    fun `feature branch works`() {
        val result = setupAndExecuteTestProject(listOf("currentVersion", "--stacktrace")) { git ->
            git.checkout().setCreateBranch(true).setName("feature/DASH-3096-something").call()
        }
        assertThat(result.output).contains("Project version: 1.0.0-DASH3096-SNAPSHOT\n")
    }

    @Test
    fun `feature branch with ticket and no suffix works`() {
        val result = setupAndExecuteTestProject(listOf("currentVersion", "--stacktrace")) { git ->
            git.checkout().setCreateBranch(true).setName("feature/DASH-3096").call()
        }
        assertThat(result.output).contains("Project version: 1.0.0-DASH3096-SNAPSHOT\n")
    }

    @Test
    fun `some other feature branch works`() {
        val result = setupAndExecuteTestProject(listOf("currentVersion", "--stacktrace")) { git ->
            git.checkout().setCreateBranch(true).setName("feature/did-something-important").call()
        }
        assertThat(result.output).contains("Project version: 1.0.0-feature-did-something-important-SNAPSHOT\n")
    }

    @Test
    fun `version branch works`() {
        val result = setupAndExecuteTestProject(listOf("currentVersion", "--stacktrace")) { git ->
            git.checkout().setCreateBranch(true).setName("version/v1").call()
        }
        assertThat(result.output).contains("Project version: 1.0.0-SNAPSHOT\n")
    }

    @Test
    fun `tag works`() {
        val result = setupAndExecuteTestProject(listOf("currentVersion", "--stacktrace")) { git ->
            git.tag().setName("v1.0.0").call()
        }
        assertThat(result.output).contains("Project version: 1.0.0\n")
    }

    @Test
    fun `tag works with ref name`() {
        val result = setupAndExecuteTestProject(
            listOf("currentVersion", "--stacktrace"),
            projectSetup = ProjectSetup(
                env = mapOf("REF_NAME" to "v1.0.0")
            )
        ) { git ->
            git.tag().setName("v1.0.0").call()
        }
        assertThat(result.output).contains("Project version: 1.0.0\n")
    }

    @Test
    fun `next version works`() {
        val result = setupAndExecuteTestProject(listOf("currentVersion", "--stacktrace")) { git ->
            git.tag().setName("v1.1.0-alpha").call()
        }
        assertThat(result.output).contains("Project version: 1.1.0-SNAPSHOT\n")
    }

    override fun defaultPluginId(): String = PluginIdentifiers.buildconventionsVersioning

    override fun defaultAdditionalBuildFileText(): String? = null

    override fun defaultExtraStuffToDo(git: Git) {
        // do nothing
    }
}
