package com.projectronin.buildconventions

import com.projectronin.gradle.test.AbstractFunctionalTest
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.Test
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper

class PublishingReposOnlyConventionsFunctionalTest : AbstractFunctionalTest() {

    private val jsonMapper = ObjectMapper()

    @Test
    fun `should publish locally`() {
        val result = testLocalPublish(
            listOf("publishToMavenLocal", "--stacktrace"),
            listOf(
                ArtifactVerification(
                    artifactId = "hello",
                    groupId = defaultGroupId(),
                    version = "1.0.0"
                )
            ),
            projectSetup = ProjectSetup(
                extraBuildFileText = """
                    version = "1.0.0"

                    dependencies {
                        api("com.fasterxml.jackson.core:jackson-annotations:2.15.2")
                        implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
                    }
                    
                """.trimIndent(),
                otherPluginsToApply = listOf("org.jetbrains.kotlin.jvm"),
                projectName = "hello"
            )
        ) {
            copyResourceDir("tests/simple-kotlin-project", projectDir)
        }
        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(projectDir.resolve("build/classes/kotlin/main/com/projectronin/sample/Hello.class")).exists()
    }

    @Test
    fun `remote publish succeeds`() {
        testRemotePublish(
            listOf("publish", "--stacktrace"),
            listOf(
                ArtifactVerification(
                    artifactId = "hello",
                    groupId = defaultGroupId(),
                    version = "1.0.0"
                )
            ),
            projectSetup = ProjectSetup(
                extraBuildFileText = """
                    version = "1.0.0"

                    dependencies {
                        api("com.fasterxml.jackson.core:jackson-annotations:2.15.2")
                        implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
                    }
                    
                """.trimIndent(),
                otherPluginsToApply = listOf("org.jetbrains.kotlin.jvm"),
                projectName = "hello"
            )
        ) {
            copyResourceDir("tests/simple-kotlin-project", projectDir)
        }
    }

    @Test
    fun `remote snapshot publish succeeds`() {
        testRemotePublish(
            listOf("tasks", "publish", "--stacktrace"),
            listOf(
                ArtifactVerification(
                    artifactId = "hello",
                    groupId = defaultGroupId(),
                    version = "1.0.0-FIDDLESTICKS-SNAPSHOT"
                )
            ),
            projectSetup = ProjectSetup(
                prependedBuildFileText = """
                    version = "1.0.0-FIDDLESTICKS-SNAPSHOT"
                """.trimIndent(),
                extraBuildFileText = """
                    dependencies {
                        api("com.fasterxml.jackson.core:jackson-annotations:2.15.2")
                        implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
                    }
                """.trimIndent(),
                otherPluginsToApply = listOf("org.jetbrains.kotlin.jvm"),
                projectName = "hello"
            )
        ) {
            copyResourceDir("tests/simple-kotlin-project", projectDir)
        }
    }

    override fun defaultPluginId(): String = "com.projectronin.buildconventions.publishingreposonly"

    override fun defaultAdditionalBuildFileText(): String? = null

    override fun defaultExtraStuffToDo(git: Git) {
        // nothing to do
    }

    override fun defaultGroupId(): String {
        return "com.projectronin.example"
    }
}
