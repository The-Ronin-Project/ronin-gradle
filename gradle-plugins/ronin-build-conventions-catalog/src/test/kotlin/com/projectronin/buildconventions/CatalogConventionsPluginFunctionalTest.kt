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
            projectDir.resolve("catalog/build.gradle.kts").appendText(
                """
                    roninCatalog {
                       bundleNameMap.set(mapOf("a-bundle" to listOf("jackson-annotations", "ronin-common-subproject-03")))
                    }
                """.trimIndent()
            )
        }
        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(projectDir.resolve("catalog/build/version-catalog/libs.versions.toml")).exists()
        val toml = projectDir.resolve("catalog/build/version-catalog/libs.versions.toml").readText()

        assertThat(toml).contains("""ronin-common = "1.0.0-SNAPSHOT"""")
        assertThat(toml).contains("""jackson = "2.15.2"""")
        assertThat(toml).contains("""kotlin = "1.8.22"""")

        assertThat(toml).contains("""ronin-common-middle = {group = "com.projectronin.plugins", name = "middle", version.ref = "ronin-common" }""")
        assertThat(toml).contains("""ronin-common-subproject-01 = {group = "com.projectronin.plugins", name = "subproject-01", version.ref = "ronin-common" }""")
        assertThat(toml).contains("""ronin-common-subproject-03 = {group = "com.projectronin.plugins", name = "subproject-03", version.ref = "ronin-common" }""")
        assertThat(toml).contains("""jackson-annotations = {group = "com.fasterxml.jackson.core", name = "jackson-annotations", version.ref = "jackson" }""")

        assertThat(toml).contains(
            """[bundles]
            |a-bundle = ["jackson-annotations", "ronin-common-subproject-03"]
            """.trimMargin()
        )

        assertThat(toml).contains("""ronin-common-test-hello-jim = {id = "com.projectronin.test.hello-jim", version.ref = "ronin-common" }""")
        assertThat(toml).contains("""ronin-common-test-hello-world = {id = "com.projectronin.test.hello-world", version.ref = "ronin-common" }""")
        assertThat(toml).contains("""spring-kotlin-core = {id = "org.jetbrains.kotlin.plugin.spring", version.ref = "kotlin" }""")
    }

    @Test
    fun `should build and publish a catalog with config`() {
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
            projectDir.resolve("catalog/build.gradle.kts").appendText(
                """
                    roninCatalog {
                       prefix.set("ronin")
                       includeCatalogFile.set(false)
                       pluginNameMap.set(mapOf("com.projectronin.test.hello-jim" to "hi"))
                       libraryNameMap.set(mapOf(":empty-middle:subproject-01" to "project1"))
                       bundleNameMap.set(mapOf("a-bundle" to listOf("project1", "ronin-subproject-03")))
                    }
                """.trimIndent()
            )
        }
        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(projectDir.resolve("catalog/build/version-catalog/libs.versions.toml")).exists()
        val toml = projectDir.resolve("catalog/build/version-catalog/libs.versions.toml").readText()

        assertThat(toml).contains("""ronin = "1.0.0-SNAPSHOT"""")
        assertThat(toml).doesNotContain("""jackson = "2.15.2"""")
        assertThat(toml).doesNotContain("""kotlin = "1.8.22"""")

        assertThat(toml).contains("""ronin-middle = {group = "com.projectronin.plugins", name = "middle", version.ref = "ronin" }""")
        assertThat(toml).contains("""project1 = {group = "com.projectronin.plugins", name = "subproject-01", version.ref = "ronin" }""")
        assertThat(toml).contains("""ronin-subproject-03 = {group = "com.projectronin.plugins", name = "subproject-03", version.ref = "ronin" }""")
        assertThat(toml).doesNotContain("""jackson-annotations = {group = "com.fasterxml.jackson.core", name = "jackson-annotations", version.ref = "jackson" }""")

        assertThat(toml).contains(
            """[bundles]
            |a-bundle = ["project1", "ronin-subproject-03"]
            """.trimMargin()
        )

        assertThat(toml).contains("""hi = {id = "com.projectronin.test.hello-jim", version.ref = "ronin" }""")
        assertThat(toml).contains("""ronin-test-hello-world = {id = "com.projectronin.test.hello-world", version.ref = "ronin" }""")
        assertThat(toml).doesNotContain("""spring-kotlin-core = {id = "org.jetbrains.kotlin.plugin.spring", version.ref = "kotlin" }""")
    }

    @Test
    fun `should build and publish a catalog with config and no prefix`() {
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
            projectDir.resolve("catalog/build.gradle.kts").appendText(
                """
                    roninCatalog {
                       includePrefix.set(false)
                       prefix.set("ronin")
                       includeCatalogFile.set(false)
                       pluginNameMap.set(mapOf("com.projectronin.test.hello-jim" to "hi"))
                       libraryNameMap.set(mapOf(":empty-middle:subproject-01" to "project1"))
                    }
                """.trimIndent()
            )
        }
        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(projectDir.resolve("catalog/build/version-catalog/libs.versions.toml")).exists()
        val toml = projectDir.resolve("catalog/build/version-catalog/libs.versions.toml").readText()

        assertThat(toml).contains("""ronin = "1.0.0-SNAPSHOT"""")
        assertThat(toml).doesNotContain("""jackson = "2.15.2"""")
        assertThat(toml).doesNotContain("""kotlin = "1.8.22"""")

        assertThat(toml).contains("""middle = {group = "com.projectronin.plugins", name = "middle", version.ref = "ronin" }""")
        assertThat(toml).contains("""project1 = {group = "com.projectronin.plugins", name = "subproject-01", version.ref = "ronin" }""")
        assertThat(toml).contains("""subproject-03 = {group = "com.projectronin.plugins", name = "subproject-03", version.ref = "ronin" }""")
        assertThat(toml).doesNotContain("""jackson-annotations = {group = "com.fasterxml.jackson.core", name = "jackson-annotations", version.ref = "jackson" }""")

        assertThat(toml).contains("""hi = {id = "com.projectronin.test.hello-jim", version.ref = "ronin" }""")
        assertThat(toml).contains("""test-hello-world = {id = "com.projectronin.test.hello-world", version.ref = "ronin" }""")
        assertThat(toml).doesNotContain("""spring-kotlin-core = {id = "org.jetbrains.kotlin.plugin.spring", version.ref = "kotlin" }""")
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
