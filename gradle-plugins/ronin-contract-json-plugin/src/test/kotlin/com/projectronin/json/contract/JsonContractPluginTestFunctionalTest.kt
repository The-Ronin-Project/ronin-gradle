package com.projectronin.json.contract

import com.fasterxml.jackson.databind.ObjectMapper
import com.projectronin.gradle.test.AbstractFunctionalTest
import okhttp3.OkHttpClient
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.gradle.internal.impldep.org.eclipse.jgit.api.Git
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.io.File
import java.net.URL
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.io.path.moveTo

/**
 * A simple functional test for the 'com.projectronin.rest.contract.support' plugin.
 */
class JsonContractPluginTestFunctionalTest : AbstractFunctionalTest() {

    private val jsonMapper = ObjectMapper()

    override val someTestResourcesPath: String = "dependency/person/person.schema.json"

    @Test
    fun `lists all the correct tasks`() {
        val result = setupTestProject(listOf("tasks", "--stacktrace"))
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
        val result = setupTestProject(listOf("check", "--stacktrace"))
        assertThat(result.output).contains("medication-v1.schema.json PASSED")
        assertThat(result.output).contains("person-v1.schema.json PASSED")
    }

    @Test
    fun `check fails`() {
        val result = setupTestProject(listOf("check", "--stacktrace"), fail = true) { git ->
            setupUsingResource(git, "test/multiple-schemas-mixed/v1/person-v1.schema.json")
        }
        assertThat(result.output).contains("medication-v1.schema.json PASSED")
        assertThat(result.output).contains("person-v1.schema.json FAILED")
    }

    @Test
    fun `single check succeeds`() {
        val result = setupTestProject(listOf("check", "--stacktrace")) { git ->
            setupUsingResource(git, "test/references-pass/v1/person-list-v1.schema.json")
        }
        assertThat(result.output).contains("person-list-v1.schema.json PASSED")
    }

    @Test
    fun `single check fails`() {
        val result = setupTestProject(listOf("check", "--stacktrace"), fail = true) { git ->
            setupUsingResource(git, "test/references-fail/v1/person-list-v1.schema.json")
        }
        assertThat(result.output).contains("person-list-v1.schema.json FAILED")
    }

    @Test
    fun `build works`() {
        setupTestProject(listOf("build", "assemble", "--stacktrace"))
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
            "dependency/person/person.schema.json",
            m2RepositoryDir,
            "com.projectronin.contract.json",
            "person-v1",
            "1.3.7"
        )

        setupTestProject(
            listOf("build", "assemble", "--stacktrace", "-Dmaven.repo.local=$m2RepositoryDir"),
            extraBuildFileText = """
                dependencies {
                    schemaDependency("com.projectronin.contract.json:person-v1:1.3.7:schemas@tar.gz")
                }
            """.trimIndent()
        ) { git ->
            setupUsingResource(git, "test/dependencies-pass/v1/person-list-v1.schema.json")
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
    fun `local publish succeeds`() {
        val m2RepositoryDir = projectDir.resolve(".m2/repository")
        setupTestProject(
            listOf("publishToMavenLocal", "-Dmaven.repo.local=$m2RepositoryDir")
        ) {
            defaultExtraStuffToDo(it)
            m2RepositoryDir.mkdirs()
        }

        assertThat(m2RepositoryDir).exists()

        assertThat(m2RepositoryDir.resolve("com/projectronin/contract/json/change-project-name-here-v1/1.0.0-SNAPSHOT/change-project-name-here-v1-1.0.0-SNAPSHOT.jar").exists()).isTrue()
        assertThat(m2RepositoryDir.resolve("com/projectronin/contract/json/change-project-name-here-v1/1.0.0-SNAPSHOT/change-project-name-here-v1-1.0.0-SNAPSHOT-schemas.tar.gz").exists()).isTrue()
    }

    @Test
    fun `remote publish succeeds`() {
        val secret = UUID.randomUUID().toString()
        val container = GenericContainer(DockerImageName.parse("dzikoysk/reposilite:3.4.0"))
            .withEnv("REPOSILITE_OPTS", "--token admin:$secret")
            .withExposedPorts(8080)
            .waitingFor(Wait.forLogMessage(".*Uptime:.*", 1))
        kotlin.runCatching { container.start() }
            .onFailure { e ->
                println(container.getLogs())
                throw e
            }

        val containerPort = container.getMappedPort(8080)

        val httpClient = OkHttpClient.Builder()
            .connectTimeout(60L, TimeUnit.SECONDS)
            .readTimeout(60L, TimeUnit.SECONDS)
            .build()

        try {
            val m2RepositoryDir = projectDir.resolve(".m2/repository")
            setupTestProject(
                listOf(
                    "publish",
                    "--stacktrace",
                    "-Pnexus-user=admin",
                    "-Pnexus-password=$secret",
                    "-Pnexus-snapshot-repo=http://localhost:$containerPort/snapshots",
                    "-Pnexus-release-repo=http://localhost:$containerPort/releases/",
                    "-Pnexus-insecure=true"
                ),
                prependedBuildFileText = """
                    System.setProperty("maven.repo.local", "$m2RepositoryDir")
                """.trimIndent()
            ) {
                defaultExtraStuffToDo(it)
                m2RepositoryDir.mkdirs()
            }

            fun verifyFile(
                isSnapshot: Boolean,
                version: String,
                extension: String,
                artifact: String = "change-project-name-here-v1",
                classifier: String? = null,
                packageDir: String = "com/projectronin/contract/json",
                expectedCode: Int = 200,
                realVersion: String = version
            ) {
                httpClient.newCall(
                    Request.Builder()
                        .head()
                        .url("http://localhost:$containerPort/${if (isSnapshot) "snapshots" else "releases"}/$packageDir/$artifact/$version/$artifact-$realVersion${classifier?.let { "-$it" } ?: ""}.$extension")
                        .build()
                )
                    .execute().use { response ->
                        assertThat(response.code).isEqualTo(expectedCode)
                    }
            }

            val tree = jsonMapper.readTree(URL("http://localhost:$containerPort/api/maven/details/snapshots/com/projectronin/contract/json/change-project-name-here-v1/1.0.0-SNAPSHOT"))
            val actualSnapshotVersion = tree["files"].find { jn -> jn["name"].textValue().endsWith(".pom") }!!["name"].textValue().replace("""change-project-name-here-v1-(.+)\.pom""".toRegex(), "$1")

            verifyFile(true, "1.0.0-SNAPSHOT", "jar", realVersion = actualSnapshotVersion)
            verifyFile(true, "1.0.0-SNAPSHOT", "tar.gz", realVersion = actualSnapshotVersion, classifier = "schemas")
        } finally {
            container.stop()
        }
    }

    @Nested
    @DisplayName("Version tests")
    inner class VersionTests {

        @Test
        fun `initial version works`() {
            val result = setupTestProject(listOf("currentVersion", "--stacktrace"))
            assertThat(result.output).contains("Project version: 1.0.0-SNAPSHOT\n")
        }

        @Test
        fun `branch works`() {
            val result = setupTestProject(listOf("currentVersion", "--stacktrace")) { git ->
                git.checkout().setCreateBranch(true).setName("DASH-3096-something").call()
            }
            assertThat(result.output).contains("Project version: 1.0.0-DASH3096-SNAPSHOT\n")
        }

        @Test
        fun `branch works with ticket and no suffix`() {
            val result = setupTestProject(listOf("currentVersion", "--stacktrace")) { git ->
                git.checkout().setCreateBranch(true).setName("DASH-3096").call()
            }
            assertThat(result.output).contains("Project version: 1.0.0-DASH3096-SNAPSHOT\n")
        }

        @Test
        fun `feature branch works`() {
            val result = setupTestProject(listOf("currentVersion", "--stacktrace")) { git ->
                git.checkout().setCreateBranch(true).setName("feature/DASH-3096-something").call()
            }
            assertThat(result.output).contains("Project version: 1.0.0-DASH3096-SNAPSHOT\n")
        }

        @Test
        fun `feature branch with ticket and no suffix works`() {
            val result = setupTestProject(listOf("currentVersion", "--stacktrace")) { git ->
                git.checkout().setCreateBranch(true).setName("feature/DASH-3096").call()
            }
            assertThat(result.output).contains("Project version: 1.0.0-DASH3096-SNAPSHOT\n")
        }

        @Test
        fun `some other feature branch works`() {
            val result = setupTestProject(listOf("currentVersion", "--stacktrace")) { git ->
                git.checkout().setCreateBranch(true).setName("feature/did-something-important").call()
            }
            assertThat(result.output).contains("Project version: 1.0.0-feature-did-something-important-SNAPSHOT\n")
        }

        @Test
        fun `version branch works`() {
            val result = setupTestProject(listOf("currentVersion", "--stacktrace")) { git ->
                git.checkout().setCreateBranch(true).setName("version/v1").call()
            }
            assertThat(result.output).contains("Project version: 1.0.0-SNAPSHOT\n")
        }

        @Test
        fun `tag works`() {
            val result = setupTestProject(listOf("currentVersion", "--stacktrace")) { git ->
                git.tag().setName("v1.0.0").call()
            }
            assertThat(result.output).contains("Project version: 1.0.0\n")
        }

        @Test
        fun `tag works with ref name`() {
            val result = setupTestProject(listOf("currentVersion", "--stacktrace"), env = mapOf("REF_NAME" to "v1.0.0")) { git ->
                git.tag().setName("v1.0.0").call()
            }
            assertThat(result.output).contains("Project version: 1.0.0\n")
        }

        @Test
        fun `next version works`() {
            val result = setupTestProject(listOf("currentVersion", "--stacktrace")) { git ->
                git.tag().setName("v1.1.0-alpha").call()
            }
            assertThat(result.output).contains("Project version: 1.1.0-SNAPSHOT\n")
        }
    }

    override fun defaultPluginId(): String = "com.projectronin.json.contract"

    override fun defaultAdditionalBuildFileText(): String = ""

    override fun defaultExtraStuffToDo(git: Git) {
        setupUsingResource(git, "test/multiple-schemas-pass/v1/person-v1.schema.json")
    }

    private fun setupUsingResource(git: Git, resourceName: String) {
        val schemasDir = File(tempFolder, "src/main/resources/schemas")
        val examplesSourceDir = File(tempFolder, "src/main/resources/schemas/examples")
        val examplesDir = File(tempFolder, "src/test/resources/examples")
        examplesDir.mkdirs()
        copyResourceDir(resourceName, schemasDir)
        examplesSourceDir.toPath().moveTo(examplesDir.toPath(), StandardCopyOption.REPLACE_EXISTING)
        git.add().addFilepattern("*").call()
        git.commit().setMessage("Adding schemas").call()
    }
}
