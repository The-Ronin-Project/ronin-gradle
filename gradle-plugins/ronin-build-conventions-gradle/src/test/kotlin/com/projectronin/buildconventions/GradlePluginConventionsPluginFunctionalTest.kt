package com.projectronin.buildconventions

import com.projectronin.gradle.test.AbstractFunctionalTest
import com.projectronin.roninbuildconventionsgradle.PluginIdentifiers
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.Test

class GradlePluginConventionsPluginFunctionalTest : AbstractFunctionalTest() {

    @Test
    fun `should build and publish a plugin`() {
        val result = testLocalPublish(
            listOf("build", "publishToMavenLocal", "--stacktrace"),
            listOf(
                ArtifactVerification("com.projectronin.test.hello.gradle.plugin", "com.projectronin.test.hello", "1.0.0-SNAPSHOT", "pom"),
                ArtifactVerification("another-plugin", "com.projectronin.plugins", "1.0.0-SNAPSHOT", "jar"),
                ArtifactVerification("another-plugin", "com.projectronin.plugins", "1.0.0-SNAPSHOT", "jar", "sources"),
                ArtifactVerification("another-plugin", "com.projectronin.plugins", "1.0.0-SNAPSHOT", "jar", "javadoc")
            ),
            projectSetup = ProjectSetup(
                projectName = "another-plugin",
                prependedBuildFileText = """
                    version = "1.0.0-SNAPSHOT"
                """.trimIndent(),
                extraBuildFileText = """
                    gradlePlugin {
                        plugins {
                            create("hello") {
                                id = "com.projectronin.test.hello"
                                implementationClass = "com.projectronin.test.Hello"
                            }
                        }
                    }
                """.trimIndent()
            ),
            printFileTree = true
        ) {
            copyResourceDir("projects/another-plugin", projectDir)
        }
        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `remote publish succeeds`() {
        val result = testRemotePublish(
            listOf("publish", "--stacktrace"),
            listOf(
                ArtifactVerification("com.projectronin.test.hello.gradle.plugin", "com.projectronin.test.hello", "1.0.0", "pom"),
                ArtifactVerification("another-plugin", "com.projectronin.plugins", "1.0.0", "jar"),
                ArtifactVerification("another-plugin", "com.projectronin.plugins", "1.0.0", "jar", "sources"),
                ArtifactVerification("another-plugin", "com.projectronin.plugins", "1.0.0", "jar", "javadoc")
            ),
            projectSetup = ProjectSetup(
                projectName = "another-plugin",
                prependedBuildFileText = """
                    version = "1.0.0"
                """.trimIndent(),
                extraBuildFileText = """
                    gradlePlugin {
                        plugins {
                            create("hello") {
                                id = "com.projectronin.test.hello"
                                implementationClass = "com.projectronin.test.Hello"
                            }
                        }
                    }
                """.trimIndent()
            ),
            printFileTree = true
        ) {
            copyResourceDir("projects/another-plugin", projectDir)
        }
        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `remote snapshot publish succeeds`() {
        val result = testRemotePublish(
            listOf("publish", "--stacktrace"),
            listOf(
                ArtifactVerification("com.projectronin.test.hello.gradle.plugin", "com.projectronin.test.hello", "1.0.0-SNAPSHOT", "pom"),
                ArtifactVerification("another-plugin", "com.projectronin.plugins", "1.0.0-SNAPSHOT", "jar"),
                ArtifactVerification("another-plugin", "com.projectronin.plugins", "1.0.0-SNAPSHOT", "jar", "sources"),
                ArtifactVerification("another-plugin", "com.projectronin.plugins", "1.0.0-SNAPSHOT", "jar", "javadoc")
            ),
            projectSetup = ProjectSetup(
                projectName = "another-plugin",
                prependedBuildFileText = """
                    version = "1.0.0-SNAPSHOT"
                """.trimIndent(),
                extraBuildFileText = """
                    gradlePlugin {
                        plugins {
                            create("hello") {
                                id = "com.projectronin.test.hello"
                                implementationClass = "com.projectronin.test.Hello"
                            }
                        }
                    }
                """.trimIndent()
            ),
            printFileTree = true
        ) {
            copyResourceDir("projects/another-plugin", projectDir)
        }
        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    override fun defaultPluginId(): String = PluginIdentifiers.buildconventionsGradleplugin

    override fun defaultAdditionalBuildFileText(): String? = null

    override fun defaultExtraStuffToDo(git: Git) {
        // do nothing
    }

    override fun defaultGroupId(): String {
        return "com.projectronin.plugins"
    }

    override fun defaultExtraSettingsFileText(): String? = null
}
