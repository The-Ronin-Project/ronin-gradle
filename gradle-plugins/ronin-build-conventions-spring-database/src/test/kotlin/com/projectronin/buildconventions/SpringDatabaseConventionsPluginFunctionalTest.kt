package com.projectronin.buildconventions

import com.projectronin.gradle.test.AbstractFunctionalTest
import com.projectronin.roninbuildconventionsspringdatabase.PluginIdentifiers
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.Test

class SpringDatabaseConventionsPluginFunctionalTest : AbstractFunctionalTest() {

    @Test
    fun `should build a spring service project`() {
        val result = setupAndExecuteTestProject(
            listOf("build", "--stacktrace"),
            printFileTree = true
        ) {
            copyResourceDir("projects/student-data-database", projectDir)
        }

        assertThat(result.output).contains("BUILD SUCCESSFUL")

        assertThat(projectDir.resolve("build/libs/change-project-name-here-1.0.0-SNAPSHOT.jar")).exists()
    }

    override fun defaultPluginId(): String = PluginIdentifiers.buildconventionsSpringDatabase

    override fun defaultAdditionalBuildFileText(): String = """
        group = "com.example"
        version = "1.0.0-SNAPSHOT"
    """.trimIndent()

    override fun defaultExtraStuffToDo(git: Git) {
        // do nothing
    }
}
