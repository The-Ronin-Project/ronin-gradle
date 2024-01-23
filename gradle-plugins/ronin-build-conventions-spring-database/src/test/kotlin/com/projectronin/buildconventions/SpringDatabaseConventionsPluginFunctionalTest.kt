package com.projectronin.buildconventions

import com.projectronin.gradle.test.AbstractFunctionalTest
import com.projectronin.roninbuildconventionsspringdatabase.PluginIdentifiers
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.Test
import java.io.File

class SpringDatabaseConventionsPluginFunctionalTest : AbstractFunctionalTest() {

    @Test
    fun `should build a spring service project`() {
        val m2RepositoryDir = projectDir.resolve(".m2/repository")
        m2RepositoryDir.mkdirs()
        val result = setupAndExecuteTestProject(
            listOf("build", "--stacktrace", "-Dmaven.repo.local=$m2RepositoryDir"),
            printFileTree = true
        ) {
            copyResourceDir("projects/student-data-database", projectDir)
            copyDbHelperFile(m2RepositoryDir)
        }

        assertThat(result.output).contains("BUILD SUCCESSFUL")

        assertThat(projectDir.resolve("build/libs/change-project-name-here-1.0.0-SNAPSHOT.jar")).exists()
    }

    private fun copyDbHelperFile(m2RepositoryDir: File) {
        copyJarToLocalRepository(
            m2RepositoryDir = m2RepositoryDir,
            groupId = "com.projectronin.services.gradle",
            projectDir = rootDirectory.resolve("shared-libraries/database-test-helpers"),
            projectName = "database-test-helpers",
            version = projectVersion
        )
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
