package com.projectronin.json.contract

import com.fasterxml.jackson.databind.ObjectMapper
import com.projectronin.gradle.test.AbstractFunctionalTest
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.StandardCopyOption
import kotlin.io.path.moveTo

/**
 * A simple functional test for the 'com.projectronin.rest.contract.support' plugin.
 */
class JsonContractPluginTestFunctionalTest : AbstractFunctionalTest() {

    private val jsonMapper = ObjectMapper()

    @Test
    fun `lists all the correct tasks`() {
        val result = setupAndExecuteTestProject(listOf("tasks", "--stacktrace"))
        listOf(
            "testContracts",
            "generateContractDocs",
            "createSchemaTar",
            "downloadSchemaDependencies",
            "generateJsonSchema2Pojo",
            "currentVersion",
            "createRelease",
            "markNextVersion",
            "pushRelease",
            "release",
            "verifyRelease"
        ).forEach { taskName ->
            assertThat(result.output).contains(taskName)
        }
    }

    @Test
    fun `check succeeds`() {
        val result = setupAndExecuteTestProject(listOf("check", "--stacktrace"))
        assertThat(result.output).contains("medication-v1.schema.json PASSED")
        assertThat(result.output).contains("person-v1.schema.json PASSED")
    }

    @Test
    fun `check fails`() {
        val result = setupAndExecuteTestProject(listOf("check", "--stacktrace"), fail = true) { git ->
            setupUsingResource(git, "test/multiple-schemas-mixed/v1")
        }
        assertThat(result.output).contains("medication-v1.schema.json PASSED")
        assertThat(result.output).contains("person-v1.schema.json FAILED")
    }

    @Test
    fun `single check succeeds`() {
        val result = setupAndExecuteTestProject(listOf("check", "--stacktrace")) { git ->
            setupUsingResource(git, "test/references-pass/v1")
        }
        assertThat(result.output).contains("person-list-v1.schema.json PASSED")
    }

    @Test
    fun `single check fails`() {
        val result = setupAndExecuteTestProject(listOf("check", "--stacktrace"), fail = true) { git ->
            setupUsingResource(git, "test/references-fail/v1")
        }
        assertThat(result.output).contains("person-list-v1.schema.json FAILED")
    }

    @Test
    fun `build works`() {
        setupAndExecuteTestProject(listOf("build", "assemble", "--stacktrace"))
        assertThat(projectDir.resolve("build/generated-sources/js2p/com/projectronin/json/changeprojectnamehere/v1/MedicationV1Schema.java").exists()).isTrue()
        assertThat(projectDir.resolve("build/generated-sources/js2p/com/projectronin/json/changeprojectnamehere/v1/PersonV1Schema.java").exists()).isTrue()
        assertThat(projectDir.resolve("build/classes/java/main/com/projectronin/json/changeprojectnamehere/v1/PersonV1Schema.class").exists()).isTrue()
        assertThat(projectDir.resolve("build/classes/java/main/com/projectronin/json/changeprojectnamehere/v1/MedicationV1Schema.class").exists()).isTrue()
        assertThat(projectDir.resolve("build/libs/change-project-name-here-1.0.0-SNAPSHOT.jar").exists()).isTrue()
        assertThat(projectDir.resolve("build/tar/change-project-name-here-schemas.tar.gz").exists()).isTrue()
    }

    @Test
    fun `build with dependency works`() {
        val m2RepositoryDir = projectDir.resolve(".m2/repository")

        createDependency(
            "dependency/person",
            m2RepositoryDir,
            "com.projectronin.contract.json",
            "person-v1",
            "1.3.7"
        )

        setupAndExecuteTestProject(
            listOf("build", "assemble", "--stacktrace", "-Dmaven.repo.local=$m2RepositoryDir"),
            projectSetup = ProjectSetup(
                extraBuildFileText = """
                    dependencies {
                        schemaDependency("com.projectronin.contract.json:person-v1:1.3.7:schemas@tar.gz")
                    }
                """.trimIndent()
            )
        ) { git ->
            setupUsingResource(git, "test/dependencies-pass/v1")
            m2RepositoryDir.mkdirs()
        }

        assertThat(projectDir.resolve("build/generated-sources/js2p/com/projectronin/json/changeprojectnamehere/v1/PersonListV1Schema.java").exists()).isTrue()
        assertThat(projectDir.resolve("build/generated-sources/js2p/com/projectronin/json/changeprojectnamehere/v1/_dependencies/person_v1/PersonSchema.java").exists()).isTrue()
        assertThat(projectDir.resolve("build/classes/java/main/com/projectronin/json/changeprojectnamehere/v1/PersonListV1Schema.class").exists()).isTrue()
        assertThat(projectDir.resolve("build/classes/java/main/com/projectronin/json/changeprojectnamehere/v1/_dependencies/person_v1/PersonSchema.class").exists()).isTrue()
        assertThat(projectDir.resolve("build/libs/change-project-name-here-1.0.0-SNAPSHOT.jar").exists()).isTrue()
        assertThat(projectDir.resolve("build/tar/change-project-name-here-schemas.tar.gz").exists()).isTrue()
    }

    @Test
    fun `should publish locally`() {
        val result = testLocalPublish(
            listOf("publishToMavenLocal", "--stacktrace"),
            listOf(
                ArtifactVerification(
                    artifactId = "change-project-name-here-v2",
                    groupId = defaultGroupId(),
                    version = "2.7.4"
                ),
                ArtifactVerification(
                    artifactId = "change-project-name-here-v2",
                    groupId = defaultGroupId(),
                    version = "2.7.4",
                    extension = "tar.gz",
                    classifier = "schemas"
                )
            ),
            projectSetup = ProjectSetup()
        ) {
            defaultExtraStuffToDo(it)
            it.tag().setName("v2.7.4").call()
        }
        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `remote publish succeeds`() {
        val result = testRemotePublish(
            listOf("publish", "--stacktrace"),
            listOf(
                ArtifactVerification(
                    artifactId = "change-project-name-here-v2",
                    groupId = defaultGroupId(),
                    version = "2.7.4"
                ),
                ArtifactVerification(
                    artifactId = "change-project-name-here-v2",
                    groupId = defaultGroupId(),
                    version = "2.7.4",
                    extension = "tar.gz",
                    classifier = "schemas"
                )
            ),
            projectSetup = ProjectSetup()
        ) {
            defaultExtraStuffToDo(it)
            it.tag().setName("v2.7.4").call()
        }
        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `remote snapshot publish succeeds`() {
        val result = testRemotePublish(
            listOf("publish", "--stacktrace"),
            listOf(
                ArtifactVerification(
                    artifactId = "change-project-name-here-v1",
                    groupId = defaultGroupId(),
                    version = "1.3.7-SNAPSHOT"
                ),
                ArtifactVerification(
                    artifactId = "change-project-name-here-v1",
                    groupId = defaultGroupId(),
                    version = "1.3.7-SNAPSHOT",
                    extension = "tar.gz",
                    classifier = "schemas"
                )
            ),
            projectSetup = ProjectSetup()
        ) {
            defaultExtraStuffToDo(it)
            it.tag().setName("v1.3.7-alpha").call()
        }
        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `initial version works`() {
        val result = setupAndExecuteTestProject(listOf("currentVersion", "--stacktrace"))
        assertThat(result.output).contains("Project version: 1.0.0-SNAPSHOT\n")
    }

    override fun defaultPluginId(): String = "com.projectronin.json.contract"

    override fun defaultAdditionalBuildFileText(): String? = null

    override fun defaultExtraStuffToDo(git: Git) {
        setupUsingResource(git, "test/multiple-schemas-pass/v1")
    }

    private fun setupUsingResource(git: Git, resourceName: String) {
        val schemasDir = File(projectDir, "src/main/resources/schemas")
        val examplesSourceDir = File(projectDir, "src/main/resources/schemas/examples")
        val examplesDir = File(projectDir, "src/test/resources/examples")
        examplesDir.mkdirs()
        copyResourceDir(resourceName, schemasDir)
        examplesSourceDir.toPath().moveTo(examplesDir.toPath(), StandardCopyOption.REPLACE_EXISTING)
        git.add().addFilepattern("*").call()
        git.commit().setMessage("Adding schemas").call()
    }
}
