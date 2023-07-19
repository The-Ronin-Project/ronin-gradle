package com.projectronin.buildconventions

import com.projectronin.gradle.test.AbstractFunctionalTest
import com.projectronin.roninbuildconventionscatalog.PluginIdentifiers
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.Test

class CatalogConventionsPluginFunctionalTest : AbstractFunctionalTest() {

    @Test
    fun `should build and publish a catalog`() {
        val result = testLocalPublish(
            listOf("build", "publishToMavenLocal", "--stacktrace"),
            listOf(
                ArtifactVerification("catalog", "com.projectronin.plugins", "1.0.0-SNAPSHOT", "toml"),
                ArtifactVerification("catalog", "com.projectronin.plugins", "1.0.0-SNAPSHOT", "module"),
                ArtifactVerification("catalog", "com.projectronin.plugins", "1.0.0-SNAPSHOT", "pom")
            ),
            projectSetup = ProjectSetup(
                projectName = "ronin-common"
            )
        ) {
            buildFile.writeText(
                """
                 version = "1.0.0-SNAPSHOT"
                 
                 subprojects.forEach { it.version = "1.0.0-SNAPSHOT" }
                """.trimIndent()
            )
            copyResourceDir("projects/demo", projectDir)
        }
        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(projectDir.resolve("catalog/build/version-catalog/libs.versions.toml")).exists()
        val toml = projectDir.resolve("catalog/build/version-catalog/libs.versions.toml").readText()

        assertThat(toml).contains("""ronin-common = "1.0.0-SNAPSHOT"""")
        assertThat(toml).contains("""jackson = "2.15.2"""")
        assertThat(toml).contains("""kotlin = "1.8.22"""")

        assertThat(toml).contains("""ronin-common-middle = {group = "com.projectronin.plugins", name = "middle", version.ref = "ronin-common" }""")
        assertThat(toml).contains("""ronin-common-subproject- = {group = "com.projectronin.plugins", name = "subproject-01", version.ref = "ronin-common" }""")
        assertThat(toml).contains("""jackson-annotations = {group = "com.fasterxml.jackson.core", name = "jackson-annotations", version.ref = "jackson" }""")

        assertThat(toml).contains("""ronin-common-test-hello-jim = {id = "com.projectronin.test.hello-jim", version.ref = "ronin-common" }""")
        assertThat(toml).contains("""ronin-common-test-hello-world = {id = "com.projectronin.test.hello-world", version.ref = "ronin-common" }""")
        assertThat(toml).contains("""spring-kotlin-core = {id = "org.jetbrains.kotlin.plugin.spring", version.ref = "kotlin" }""")
    }

    @Test
    fun `remote publish succeeds`() {
        testRemotePublish(
            listOf("publish", "--stacktrace"),
            listOf(
                ArtifactVerification("catalog", "com.projectronin.plugins", "1.0.0", "toml"),
                ArtifactVerification("catalog", "com.projectronin.plugins", "1.0.0", "module"),
                ArtifactVerification("catalog", "com.projectronin.plugins", "1.0.0", "pom")
            ),
            projectSetup = ProjectSetup(
                projectName = "ronin-common"
            )
        ) {
            buildFile.writeText(
                """
                 version = "1.0.0"
                 
                 subprojects.forEach { it.version = "1.0.0" }
                """.trimIndent()
            )
            copyResourceDir("projects/demo", projectDir)
        }
    }

    @Test
    fun `remote snapshot publish succeeds`() {
        testRemotePublish(
            listOf("publish", "--stacktrace"),
            listOf(
                ArtifactVerification("catalog", "com.projectronin.plugins", "1.0.0-SNAPSHOT", "toml"),
                ArtifactVerification("catalog", "com.projectronin.plugins", "1.0.0-SNAPSHOT", "module"),
                ArtifactVerification("catalog", "com.projectronin.plugins", "1.0.0-SNAPSHOT", "pom")
            ),
            projectSetup = ProjectSetup(
                projectName = "ronin-common"
            )
        ) {
            buildFile.writeText(
                """
                 version = "1.0.0-SNAPSHOT"
                 
                 subprojects.forEach { it.version = "1.0.0-SNAPSHOT" }
                """.trimIndent()
            )
            copyResourceDir("projects/demo", projectDir)
        }
    }

    override fun defaultPluginId(): String = PluginIdentifiers.buildconventionsCatalog

    override fun defaultAdditionalBuildFileText(): String? = null

    override fun defaultExtraStuffToDo(git: Git) {
        // do nothing
    }

    override fun defaultGroupId(): String {
        return "com.projectronin.plugins"
    }

    override fun defaultExtraSettingsFileText(): String = """
        include(":empty-middle:subproject-01")
        include(":middle")
        include(":middle:subproject-02")
        include(":subproject-03")
        include(":catalog")
    """.trimIndent()
}
