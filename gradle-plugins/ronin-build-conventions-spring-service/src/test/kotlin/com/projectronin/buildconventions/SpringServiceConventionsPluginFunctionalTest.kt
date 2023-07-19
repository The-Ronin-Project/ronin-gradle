package com.projectronin.buildconventions

import com.projectronin.gradle.test.AbstractFunctionalTest
import com.projectronin.roninbuildconventionsspringservice.PluginIdentifiers
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test

class SpringServiceConventionsPluginFunctionalTest : AbstractFunctionalTest() {

    @Test
    fun `should build a spring service project`() {
        val result = setupAndExecuteTestProject(
            listOf("build", "--stacktrace")
        ) {
            copyResourceDir("projects/demo", projectDir)
        }

        assertThat(result.tasks.first { it.path == ":bootJar" }.outcome).isEqualTo(TaskOutcome.SUCCESS)

        assertThat(projectDir.resolve("build/libs/change-project-name-here-1.0.0-SNAPSHOT-plain.jar")).exists()
        assertThat(projectDir.resolve("build/libs/change-project-name-here-1.0.0-SNAPSHOT.jar")).exists()
    }

    override fun defaultPluginId(): String = PluginIdentifiers.buildconventionsSpringService

    override fun defaultAdditionalBuildFileText(): String = """
        group = "com.example"
        version = "1.0.0-SNAPSHOT"
        dependencies {
            implementation("org.springframework.boot:spring-boot-starter")
            implementation("org.jetbrains.kotlin:kotlin-reflect")
            testImplementation("org.springframework.boot:spring-boot-starter-test")
        }
    """.trimIndent()

    override fun defaultExtraStuffToDo(git: Git) {
        // do nothing
    }
}
