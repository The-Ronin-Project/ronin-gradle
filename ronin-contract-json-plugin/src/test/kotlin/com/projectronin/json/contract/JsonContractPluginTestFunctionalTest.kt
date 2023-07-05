package com.projectronin.json.contract

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.gradle.internal.impldep.org.eclipse.jgit.api.Git
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.io.File
import java.io.InputStream
import java.net.URL
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * A simple functional test for the 'com.projectronin.rest.contract.support' plugin.
 */
class JsonContractPluginTestFunctionalTest {

    @field:TempDir
    lateinit var tempFolder: File

    companion object {
        private val testCounter = AtomicInteger(0)
    }

    private val jsonMapper = ObjectMapper()
    private val yamlMapper = ObjectMapper(YAMLFactory())

    private val projectDir
        get() = tempFolder
    private val buildFile
        get() = projectDir.resolve("build.gradle.kts")
    private val settingsFile
        get() = projectDir.resolve("settings.gradle.kts")

    private fun InputStream.toFile(file: File) {
        use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
    }

    private fun GradleRunner.withJaCoCo(): GradleRunner {
        tempFolder.resolve("gradle.properties").writeText("\ngroup=com.projectronin.contract.event\n")
        return this
    }

    @Test
    fun `lists all the correct tasks`() {
        val result = setupTestProject(listOf("tasks", "--stacktrace"))
        listOf(
            "testEvents",
            "generateEventDocs",
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
        val result = setupTestProject(listOf("check", "--stacktrace"), sourceDirectory = "test/multiple-schemas-mixed/v1/person-v1.schema.json", fail = true)
        assertThat(result.output).contains("medication-v1.schema.json PASSED")
        assertThat(result.output).contains("person-v1.schema.json FAILED")
    }

    @Test
    fun `single check succeeds`() {
        val result = setupTestProject(listOf("check", "--stacktrace"), sourceDirectory = "test/references-pass/v1/person-list-v1.schema.json")
        assertThat(result.output).contains("person-list-v1.schema.json PASSED")
    }

    @Test
    fun `single check fails`() {
        val result = setupTestProject(listOf("check", "--stacktrace"), sourceDirectory = "test/references-fail/v1/person-list-v1.schema.json", fail = true)
        assertThat(result.output).contains("person-list-v1.schema.json FAILED")
    }

    @Test
    fun `build works`() {
        setupTestProject(listOf("build", "assemble", "--stacktrace"))
        assertThat(projectDir.resolve("build/generated-sources/js2p/com/projectronin/event/changeprojectnamehere/v1/MedicationV1Schema.java").exists()).isTrue()
        assertThat(projectDir.resolve("build/generated-sources/js2p/com/projectronin/event/changeprojectnamehere/v1/PersonV1Schema.java").exists()).isTrue()
        assertThat(projectDir.resolve("build/classes/java/main/com/projectronin/event/changeprojectnamehere/v1/PersonV1Schema.class").exists()).isTrue()
        assertThat(projectDir.resolve("build/classes/java/main/com/projectronin/event/changeprojectnamehere/v1/MedicationV1Schema.class").exists()).isTrue()
        assertThat(projectDir.resolve("build/libs/change-project-name-here-1.0.0-SNAPSHOT.jar").exists()).isTrue()
        assertThat(projectDir.resolve("build/tar/change-project-name-here-schemas.tar.gz").exists()).isTrue()
    }

    @Test
    fun `local publish succeeds`() {
        val m2RepositoryDir = projectDir + ".m2/repository"
        val hostRepositoryDir = projectDir + "host-repository"
        (hostRepositoryDir + "com/projectronin/rest/contract").mkdirs()
        (hostRepositoryDir + "com/projectronin/rest/contract/foo.txt").writeText("foo")
        (hostRepositoryDir + "com/projectronin/rest/contract/bar").mkdirs()
        (hostRepositoryDir + "com/projectronin/rest/contract/bar/bar.txt").writeText("bar")
        setupTestProject(
            listOf("publishToMavenLocal", "-Phost-repository=$hostRepositoryDir"),
            prependedBuildFileText = """
                System.setProperty("maven.repo.local", "$m2RepositoryDir")
            """.trimIndent()
        ) {
            m2RepositoryDir.mkdirs()
        }

        assertThat(m2RepositoryDir).exists()

        assertThat(m2RepositoryDir.resolve("com/projectronin/contract/event/change-project-name-here-v1/1.0.0-SNAPSHOT/change-project-name-here-v1-1.0.0-SNAPSHOT.jar").exists()).isTrue()
        assertThat(m2RepositoryDir.resolve("com/projectronin/contract/event/change-project-name-here-v1/1.0.0-SNAPSHOT/change-project-name-here-v1-1.0.0-SNAPSHOT-schemas.tar.gz").exists()).isTrue()
    }

    @Nested
    @DisplayName("Version tests")
    inner class VersionTests {

        @Test
        fun `initial version works`() {
            val result = setupTestProject(listOf("currentVersion", "--stacktrace"))
            assertThat(result.output).contains("1.0.0-SNAPSHOT")
        }

        @Test
        fun `branch works`() {
            val result = setupTestProject(listOf("currentVersion", "--stacktrace")) { git ->
                git.checkout().setCreateBranch(true).setName("DASH-3096-something").call()
            }
            assertThat(result.output).contains("1.0.0-DASH3096-SNAPSHOT")
        }

        @Test
        fun `feature branch works`() {
            val result = setupTestProject(listOf("currentVersion", "--stacktrace")) { git ->
                git.checkout().setCreateBranch(true).setName("feature/DASH-3096-something").call()
            }
            assertThat(result.output).contains("1.0.0-DASH3096-SNAPSHOT")
        }

        @Test
        fun `some other feature branch works`() {
            val result = setupTestProject(listOf("currentVersion", "--stacktrace")) { git ->
                git.checkout().setCreateBranch(true).setName("feature/did-something-important").call()
            }
            assertThat(result.output).contains("1.0.0-feature-did-something-important-SNAPSHOT")
        }

        @Test
        fun `version branch works`() {
            val result = setupTestProject(listOf("currentVersion", "--stacktrace")) { git ->
                git.checkout().setCreateBranch(true).setName("version/v1").call()
            }
            assertThat(result.output).contains("1.0.0-SNAPSHOT")
        }

        @Test
        fun `tag works`() {
            val result = setupTestProject(listOf("currentVersion", "--stacktrace")) { git ->
                git.tag().setName("v1.0.0").call()
            }
            assertThat(result.output).contains("1.0.0")
        }

        @Test
        fun `next version works`() {
            val result = setupTestProject(listOf("currentVersion", "--stacktrace")) { git ->
                git.tag().setName("v1.1.0-alpha").call()
            }
            assertThat(result.output).contains("1.1.0-SNAPSHOT")
        }
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
            val m2RepositoryDir = projectDir + ".m2/repository"
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
                m2RepositoryDir.mkdirs()
            }

            fun verifyFile(
                isSnapshot: Boolean,
                version: String,
                extension: String,
                artifact: String = "change-project-name-here-v1",
                classifier: String? = null,
                packageDir: String = "com/projectronin/contract/event",
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

            val tree = jsonMapper.readTree(URL("http://localhost:$containerPort/api/maven/details/snapshots/com/projectronin/contract/event/change-project-name-here-v1/1.0.0-SNAPSHOT"))
            val actualSnapshotVersion = tree["files"].find { jn -> jn["name"].textValue().endsWith(".pom") }!!["name"].textValue().replace("""change-project-name-here-v1-(.+)\.pom""".toRegex(), "$1")

            verifyFile(true, "1.0.0-SNAPSHOT", "jar", realVersion = actualSnapshotVersion)
            verifyFile(true, "1.0.0-SNAPSHOT", "tar.gz", realVersion = actualSnapshotVersion, classifier = "schemas")
        } finally {
            container.stop()
        }
    }

    private fun mapperForFile(file: File): ObjectMapper = if (file.name.endsWith(".json")) jsonMapper else yamlMapper

    private fun File.setVersion(version: String) {
        val tree = readTree()
        (tree["info"] as ObjectNode).set<ObjectNode>("version", TextNode(version))
        writeValue(tree)
    }

    private fun File.readFileVersion(): String {
        return this.readTree()["info"]["version"].asText()
    }

    private fun File.readTree(): ObjectNode {
        return mapperForFile(this).readTree(this) as ObjectNode
    }

    private fun File.writeValue(node: JsonNode) {
        mapperForFile(this).writeValue(this, node)
    }

    private fun setupTestProject(
        buildArguments: List<String>,
        settingsText: String = """
            rootProject.name = "change-project-name-here"

            pluginManagement {
                repositories {
                    maven {
                        url = uri("https://repo.devops.projectronin.io/repository/maven-public/")
                    }
                    mavenLocal()
                    gradlePluginPortal()
                }
            }

            dependencyResolutionManagement {
                repositories {
                    maven {
                        url = uri("https://repo.devops.projectronin.io/repository/maven-public/")
                    }
                    mavenLocal()
                    gradlePluginPortal()
                }
            }
        """.trimIndent(),
        prependedBuildFileText: String = "",
        extraBuildFileText: String? = null,
        fail: Boolean = false,
        sourceDirectory: String = "test/multiple-schemas-pass/v1/person-v1.schema.json",
        printFileTree: Boolean = false,
        env: Map<String, String> = emptyMap(),
        extraStuffToDo: (Git) -> Unit = {}
    ): BuildResult {
        val git = Git.init().setDirectory(projectDir).call()
        File(projectDir, ".gitignore").writeText(
            """
            docs/
            build/
            v*/version
            codecov/
            
            *.iml
            
            .gradle
            !gradle/wrapper/gradle-wrapper.jar
            .idea/
            .dependencies/
            """.trimIndent()
        )
        git.add().addFilepattern(".gitignore").call()
        git.commit().setMessage("Initial Commit").call()

        copyDirectory(sourceDirectory)
        settingsFile.writeText(settingsText)
        buildFile.writeText("$prependedBuildFileText\n")
        buildFile.appendText(
            """
            plugins {
                id("com.projectronin.json.contract")
            }
            """.trimIndent()
        )

        extraBuildFileText?.run {
            buildFile.appendText("\n$this")
        }

        extraStuffToDo(git)

        println("=".repeat(80))
        println(buildFile.readText())
        println("=".repeat(80))

        // Run the build
        return runProjectBuild(buildArguments, fail, env).also {
            if (printFileTree) {
                projectDir.walk().forEach { file ->
                    println(file)
                }
            }
        }
    }

    private fun runProjectBuild(buildArguments: List<String>, fail: Boolean, env: Map<String, String>): BuildResult {
        val runner = GradleRunner.create().withJaCoCo()
        runner.forwardOutput()
        runner.withEnvironment(
            if (env.containsKey("REF_NAME")) {
                env
            } else {
                env + mapOf("REF_NAME" to "")
            }
        )
        runner.withPluginClasspath()
        runner.withArguments(buildArguments)
        runner.withProjectDir(projectDir)
        runner.withDebug(System.getenv("DEBUG_RUNNER")?.lowercase() == "true")
        return if (fail) {
            runner.buildAndFail()
        } else {
            runner.build()
        }
    }

    private fun copyDirectory(resourceName: String) {
        val classLoader = javaClass.classLoader
        val baseDirectory = File(classLoader.getResource(resourceName)!!.file).parentFile
        baseDirectory.resolve("examples").copyRecursively(File(tempFolder, "src/test/resources/examples"))
        baseDirectory.copyRecursively(File(tempFolder, "src/main/resources/schemas"))
        File(tempFolder, "src/main/resources/schemas/examples").deleteRecursively()
    }

    private operator fun File.plus(other: String): File = this.resolve(other)
}
