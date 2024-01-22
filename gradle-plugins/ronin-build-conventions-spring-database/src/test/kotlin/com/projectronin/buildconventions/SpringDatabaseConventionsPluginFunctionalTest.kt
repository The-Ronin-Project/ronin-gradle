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
        val extraLibDir = rootDirectory.resolve("shared-libraries/database-test-helpers/build/libs")
        val jarFile = extraLibDir.listFiles()!!.find { it.name.endsWith("$projectVersion.jar") }!!
        val repoJar = m2RepositoryDir.resolve("com/projectronin/services/gradle/database-test-helpers/$projectVersion/database-test-helpers-$projectVersion.jar")
        jarFile.copyTo(repoJar)
        rootDirectory.resolve("shared-libraries/database-test-helpers/build/publications/Maven/pom-default.xml").copyTo(
            m2RepositoryDir.resolve("com/projectronin/services/gradle/database-test-helpers/$projectVersion/database-test-helpers-$projectVersion.pom")
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
