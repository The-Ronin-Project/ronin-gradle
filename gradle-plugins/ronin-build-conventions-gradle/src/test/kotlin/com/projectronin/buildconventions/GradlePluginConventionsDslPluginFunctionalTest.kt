package com.projectronin.buildconventions

import com.projectronin.gradle.test.AbstractFunctionalTest
import com.projectronin.roninbuildconventionsgradle.PluginIdentifiers
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.Test

class GradlePluginConventionsDslPluginFunctionalTest : AbstractFunctionalTest() {

    @Test
    fun `should build and publish a plugin`() {
        val result = testLocalPublish(
            listOf("build", "publishToMavenLocal", "--stacktrace"),
            listOf(
                ArtifactVerification("com.projectronin.test.hello-jim.gradle.plugin", "com.projectronin.test.hello-jim", "1.0.0-SNAPSHOT", "pom"),
                ArtifactVerification("com.projectronin.test.hello-world.gradle.plugin", "com.projectronin.test.hello-world", "1.0.0-SNAPSHOT", "pom"),
                ArtifactVerification("a-plugin", "com.projectronin.plugins", "1.0.0-SNAPSHOT", "jar"),
                ArtifactVerification("a-plugin", "com.projectronin.plugins", "1.0.0-SNAPSHOT", "jar", "sources"),
                ArtifactVerification("a-plugin", "com.projectronin.plugins", "1.0.0-SNAPSHOT", "jar", "javadoc")
            ),
            projectSetup = ProjectSetup(
                projectName = "a-plugin",
                prependedBuildFileText = """
                    version = "1.0.0-SNAPSHOT"
                """.trimIndent()
            )
        ) {
            copyResourceDir("projects/a-plugin", projectDir)
        }
        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `remote publish succeeds`() {
        val result = testRemotePublish(
            listOf("publish", "--stacktrace"),
            listOf(
                ArtifactVerification("com.projectronin.test.hello-jim.gradle.plugin", "com.projectronin.test.hello-jim", "1.0.0", "pom"),
                ArtifactVerification("com.projectronin.test.hello-world.gradle.plugin", "com.projectronin.test.hello-world", "1.0.0", "pom"),
                ArtifactVerification("a-plugin", "com.projectronin.plugins", "1.0.0", "jar"),
                ArtifactVerification("a-plugin", "com.projectronin.plugins", "1.0.0", "jar", "sources"),
                ArtifactVerification("a-plugin", "com.projectronin.plugins", "1.0.0", "jar", "javadoc")
            ),
            projectSetup = ProjectSetup(
                projectName = "a-plugin",
                prependedBuildFileText = """
                    version = "1.0.0"
                """.trimIndent()
            )
        ) {
            copyResourceDir("projects/a-plugin", projectDir)
        }
        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `remote snapshot publish succeeds`() {
        val result = testRemotePublish(
            listOf("publish", "--stacktrace"),
            listOf(
                ArtifactVerification("com.projectronin.test.hello-jim.gradle.plugin", "com.projectronin.test.hello-jim", "1.0.0-SNAPSHOT", "pom"),
                ArtifactVerification("com.projectronin.test.hello-world.gradle.plugin", "com.projectronin.test.hello-world", "1.0.0-SNAPSHOT", "pom"),
                ArtifactVerification("a-plugin", "com.projectronin.plugins", "1.0.0-SNAPSHOT", "jar"),
                ArtifactVerification("a-plugin", "com.projectronin.plugins", "1.0.0-SNAPSHOT", "jar", "sources"),
                ArtifactVerification("a-plugin", "com.projectronin.plugins", "1.0.0-SNAPSHOT", "jar", "javadoc")
            ),
            projectSetup = ProjectSetup(
                projectName = "a-plugin",
                prependedBuildFileText = """
                    version = "1.0.0-SNAPSHOT"
                """.trimIndent()
            )
        ) {
            copyResourceDir("projects/a-plugin", projectDir)
        }
        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    override fun defaultPluginId(): String = PluginIdentifiers.buildconventionsGradledslplugin

    override fun defaultAdditionalBuildFileText(): String? = null

    override fun defaultExtraStuffToDo(git: Git) {
        // do nothing
    }

    override fun defaultGroupId(): String {
        return "com.projectronin.plugins"
    }

    override fun defaultExtraSettingsFileText(): String? = null
}
