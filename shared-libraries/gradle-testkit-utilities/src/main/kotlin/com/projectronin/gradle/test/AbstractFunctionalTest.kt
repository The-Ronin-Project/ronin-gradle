package com.projectronin.gradle.test

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.io.File
import java.net.URL
import java.util.Properties
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * An abstract base class for functional tests.  Handles the complexities of project setup and execution, directory location, resource copying, etc.
 */
@Suppress("MemberVisibilityCanBePrivate")
@ExcludeAsIfGenerated
abstract class AbstractFunctionalTest {

    protected val httpClient = OkHttpClient.Builder()
        .connectTimeout(60L, TimeUnit.SECONDS)
        .readTimeout(60L, TimeUnit.SECONDS)
        .build()

    /**
     * The root directory of the generated project
     */
    @field:TempDir
    private lateinit var tempFolder: File

    protected val logger = KotlinLogging.logger {}

    protected val projectDir
        get() = tempFolder

    protected val buildFile
        get() = this.projectDir.resolve("build.gradle.kts")

    protected val settingsFile
        get() = this.projectDir.resolve("settings.gradle.kts")

    /**
     * These are written for all the gradle plugin projects from the main build file.  The file is just used to locate the right directories on the file system.
     */
    private val functionalTestProperties: Map<String, String> = run {
        val props = Properties()
        props.load(javaClass.classLoader.getResourceAsStream("functional-test.properties"))
        props.stringPropertyNames().associateWith { props.getProperty(it) }
    }

    /**
     * The project directory of the actual plugin project
     */
    protected val pluginProjectDirectory: File = File(functionalTestProperties["directory.project"]!!)

    /**
     * The "build" directory of the actual plugin project
     */
    @Suppress("unused")
    protected val pluginBuildDirectory: File = File(functionalTestProperties["directory.build"]!!)

    /**
     * The TEST resources output directory of the main plugin project.
     */
    protected val pluginResourcesDirectory: File = File(functionalTestProperties["directory.resources"]!!)

    /**
     * The root of the whole project
     */
    protected val rootDirectory: File = File(functionalTestProperties["directory.root"]!!)

    /**
     * The current version of the project
     */
    protected val projectVersion: String = functionalTestProperties["project.version"]!!

    /**
     * The name of the generated project.  Not particularly important, but is used for things like created or published artifacts, so
     * might be useful to change or utilize in your tests.
     */
    protected open fun defaultProjectName(): String = "change-project-name-here"

    /**
     * The id of the plugin you are testing.  Probably should set it to PluginIdentifiers.<your plugin id>
     */
    protected abstract fun defaultPluginId(): String

    /**
     * Can be blank, but can also be set to anything you want to be added to all the generated build files
     */
    protected abstract fun defaultAdditionalBuildFileText(): String?

    protected abstract fun defaultExtraStuffToDo(git: Git)

    protected open fun defaultExtraSettingsFileText(): String? = null

    protected open fun defaultExtraBuildFileText(): String? = null

    protected open fun defaultPrependedBuildFileText(): String? = null

    protected open fun defaultGroupId(): String = "com.projectronin.contract.json"

    /**
     * Setup and execute the project.  At a minimum, pass the build arguments you want.
     */
    protected fun setupAndExecuteTestProject(
        buildArguments: List<String>,
        fail: Boolean = false,
        printFileTree: Boolean = false,
        projectSetup: ProjectSetup = ProjectSetup(),
        extraStuffToDo: (Git) -> Unit = { defaultExtraStuffToDo(it) }
    ): BuildResult {
        val git = Git.init().setDirectory(this.projectDir).setInitialBranch("main").call()
        File(this.projectDir, ".gitignore").writeText(
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
        git.add().addFilepattern("*").call()
        git.commit().setMessage("Initial Commit").call()

        settingsFile.writeText(projectSetup.settingsText)
        projectSetup.extraSettingsFileText?.let { text ->
            settingsFile.appendText("\n$text\n")
        }

        val buildFileText = StringBuilder()

        fun appendText(text: String) {
            buildFileText.append(text)
            if (!text.endsWith("\n")) {
                buildFileText.append("\n")
            }
        }

        projectSetup.prependedBuildFileText?.let { appendText(it) }

        defaultAdditionalBuildFileText()?.let { appendText(it) }

        val pluginsToApply = listOf(projectSetup.pluginId) + projectSetup.otherPluginsToApply
        appendText(
            """
            plugins {
                ${pluginsToApply.joinToString("\n                ") { pluginName -> if (pluginName.contains("[(`]".toRegex())) pluginName else "id(\"$pluginName\")" }}
            }
            """.trimIndent()
        )
        projectSetup.extraBuildFileText?.let { appendText(it) }

        if (!buildFileText.endsWith("\n")) {
            buildFileText.append("\n")
        }

        buildFile.writeText(buildFileText.toString())

        extraStuffToDo(git)

        logger.warn { "=".repeat(80) }
        logger.warn { buildFile.readText() }
        logger.warn { "=".repeat(80) }
        logger.warn { settingsFile.readText() }
        logger.warn { "=".repeat(80) }

        // Run the build
        return runProjectBuild(buildArguments, fail, projectSetup.env, printFileTree = printFileTree)
    }

    protected fun runProjectBuild(buildArguments: List<String>, fail: Boolean = false, env: Map<String, String> = emptyMap(), printFileTree: Boolean = false): BuildResult {
        try {
            val runner = GradleRunner.create().withCoverage(pluginProjectDirectory, this.projectDir, groupId = defaultGroupId())
            runner.forwardOutput()
            runner.withEnvironment(
                System.getenv() + if (env.containsKey("REF_NAME")) {
                    env
                } else {
                    env + mapOf("REF_NAME" to "")
                }
            )
            runner.withPluginClasspath()
            runner.withArguments(buildArguments)
            runner.withProjectDir(this.projectDir)
            return if (fail) {
                runner.buildAndFail()
            } else {
                runner.build()
            }
        } finally {
            if (printFileTree) {
                this.projectDir.walk().forEach { file ->
                    logger.warn { file }
                }
            }
        }
    }

    protected fun testLocalPublish(
        buildArguments: List<String>,
        verifications: List<ArtifactVerification>,
        fail: Boolean = false,
        printFileTree: Boolean = false,
        projectSetup: ProjectSetup = ProjectSetup(),
        extraStuffToDo: (Git) -> Unit = { defaultExtraStuffToDo(it) }
    ): BuildResult {
        return testLocalPublish(buildArguments, { verifications }, fail, printFileTree, projectSetup, extraStuffToDo)
    }

    protected fun testLocalPublish(
        buildArguments: List<String>,
        verifications: (File) -> List<ArtifactVerification>,
        fail: Boolean = false,
        printFileTree: Boolean = false,
        projectSetup: ProjectSetup = ProjectSetup(),
        extraStuffToDo: (Git) -> Unit = { defaultExtraStuffToDo(it) }
    ): BuildResult {
        val m2RepositoryDir = projectDir.resolve(".m2/repository")
        m2RepositoryDir.mkdirs()
        val result = setupAndExecuteTestProject(buildArguments + listOf("-Dmaven.repo.local=$m2RepositoryDir"), fail, printFileTree, projectSetup) {
            m2RepositoryDir.mkdirs()
            extraStuffToDo(it)
        }
        verifications(m2RepositoryDir).forEach { verifyLocalRepositoryArtifact(m2RepositoryDir, it) }
        return result
    }

    protected fun verifyLocalRepositoryArtifact(
        repositoryDir: File,
        verification: ArtifactVerification
    ) {
        assertThat(repositoryDir.resolve(verification.artifactPath())).exists()
    }

    protected fun testRemotePublish(
        buildArguments: List<String>,
        verifications: List<ArtifactVerification>,
        fail: Boolean = false,
        printFileTree: Boolean = false,
        projectSetup: ProjectSetup = ProjectSetup(),
        somethingToExecuteWhileContainerIsRunning: (Int, String) -> Unit = { _, _ -> },
        extraStuffToDo: (Git) -> Unit = { defaultExtraStuffToDo(it) }
    ): BuildResult {
        return testRemotePublish(buildArguments, { verifications }, fail, printFileTree, projectSetup, somethingToExecuteWhileContainerIsRunning, extraStuffToDo)
    }

    protected fun testRemotePublish(
        buildArguments: List<String>,
        verifications: (File) -> List<ArtifactVerification>,
        fail: Boolean = false,
        printFileTree: Boolean = false,
        projectSetup: ProjectSetup = ProjectSetup(),
        somethingToExecuteWhileContainerIsRunning: (Int, String) -> Unit = { _, _ -> },
        extraStuffToDo: (Git) -> Unit = { defaultExtraStuffToDo(it) }
    ): BuildResult {
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

        return try {
            val m2RepositoryDir = projectDir.resolve(".m2/repository")
            val result = setupAndExecuteTestProject(
                buildArguments + listOf(
                    "-Pnexus-user=admin",
                    "-Pnexus-password=$secret",
                    "-Pnexus-snapshot-repo=http://localhost:$containerPort/snapshots",
                    "-Pnexus-release-repo=http://localhost:$containerPort/releases/",
                    "-Pnexus-insecure=true",
                    "-Dmaven.repo.local=$m2RepositoryDir"
                ),
                fail,
                printFileTree,
                projectSetup
            ) {
                extraStuffToDo(it)
                m2RepositoryDir.mkdirs()
            }

            verifications(m2RepositoryDir).forEach { verifyRemoteRepositoryArtifact(containerPort, it) }

            somethingToExecuteWhileContainerIsRunning(containerPort, secret)

            result
        } finally {
            container.stop()
        }
    }

    protected fun verifyRemoteRepositoryArtifact(
        containerPort: Int,
        verification: ArtifactVerification
    ) {
        val actualVersion: String = run {
            if (verification.isSnapshot) {
                val tree = ObjectMapper().readTree(URL("http://localhost:$containerPort/api/maven/details/snapshots/${verification.directoryPath()}"))
                tree["files"].find { jn -> jn["name"].textValue().endsWith(".pom") }!!["name"].textValue().replace("""${verification.artifactId}-(.+)\.pom""".toRegex(), "$1")
            } else {
                verification.version
            }
        }
        val url = "http://localhost:$containerPort/${if (verification.isSnapshot) "snapshots" else "releases"}/${verification.artifactPath(actualVersion)}"
        httpClient.newCall(
            Request.Builder()
                .head()
                .url(url)
                .build()
        )
            .execute().use { response ->
                assertThat(response.code).withFailMessage { "Could not find $url" }.isEqualTo(200)
            }
    }

    /**
     * Resolves the input directory, which is expected to be relative to the src/test/resources of your plugin project, to the output file location.  The filter can be used
     * to delete anything you don't want there.
     */
    protected fun copyResourceDir(relativeSourceDir: String, toDir: File, exclude: (file: File) -> Boolean = { false }) {
        pluginResourcesDirectory.resolve(relativeSourceDir).copyRecursively(toDir)
        toDir.walk(FileWalkDirection.BOTTOM_UP)
            .filter(exclude)
            .forEach { it.deleteRecursively() }
    }

    /**
     * Creates a dependency.  This is ... really specific to tar.gz files used for schema dependencies.
     */
    @Suppress("SameParameterValue")
    protected fun createDependency(relativeSourceDir: String, repoRoot: File, groupId: String, artifactId: String, version: String) {
        val baseDirectory = pluginResourcesDirectory.resolve(relativeSourceDir)

        val newTempDirectory = this.projectDir.resolve(".tmp/dependency-${UUID.randomUUID()}")
        newTempDirectory.mkdirs()

        val repoSubDirectory = repoRoot.resolve("${groupId.replace(".", "/")}/$artifactId/$version")
        repoSubDirectory.mkdirs()

        baseDirectory.copyRecursively(newTempDirectory)
        listOf(
            "tar",
            "czvf",
            repoSubDirectory.resolve("$artifactId-$version-schemas.tar.gz").absolutePath,
            "."
        ).runCommand(newTempDirectory)
        repoSubDirectory.resolve("$artifactId-$version.pom").writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              <modelVersion>4.0.0</modelVersion>
              <groupId>$groupId</groupId>
              <artifactId>$artifactId</artifactId>
              <version>$version</version>
              <packaging>pom</packaging>
            </project>
            """.trimIndent()
        )
        newTempDirectory.deleteRecursively()
    }

    protected fun putFileIntoLocalRepository(sourceFile: File, repoRoot: File, relativeDestFile: String) {
        val destFile = repoRoot.resolve(relativeDestFile)
        with(destFile.parentFile) {
            if (!exists()) {
                mkdirs()
            }
        }
        sourceFile.copyTo(destFile, true)
    }

    protected fun copyPluginToLocalRepo(
        m2RepositoryDir: File,
        groupId: String,
        projectName: String,
        pluginId: String,
        version: String
    ) {
        val serviceRoot = pluginProjectDirectory.parentFile.resolve("$projectName")
        val jarDestRoot = "${groupId.replace(".", "/")}/$projectName/$version"
        val pluginDestRoot = "${pluginId.replace(".", "/")}/$pluginId.gradle.plugin/$version"

        putFileIntoLocalRepository(
            serviceRoot.resolve("build/libs/$projectName-$version.jar"),
            m2RepositoryDir,
            "$jarDestRoot/$projectName-$version.jar"
        )
        putFileIntoLocalRepository(
            serviceRoot.resolve("build/publications/pluginMaven/module.json"),
            m2RepositoryDir,
            "$jarDestRoot/$projectName-$version.module"
        )
        putFileIntoLocalRepository(
            serviceRoot.resolve("build/publications/pluginMaven/pom-default.xml"),
            m2RepositoryDir,
            "$jarDestRoot/$projectName-$version.pom"
        )
        val segs = pluginId.split(".").last().split("-")
        val pluginPrefix = segs.first().lowercase() + segs.subList(1, segs.size).joinToString("") { seg -> seg[0].uppercase() + seg.substring(1).lowercase() }
        putFileIntoLocalRepository(
            serviceRoot.resolve("build/publications/${pluginPrefix}PluginPluginMarkerMaven/pom-default.xml"),
            m2RepositoryDir,
            "$pluginDestRoot/$pluginId.gradle.plugin-$version.pom"
        )
    }

    protected fun copyJarToLocalRepository(
        m2RepositoryDir: File,
        groupId: String,
        projectDir: File,
        projectName: String,
        version: String
    ) {
        val jarDestRoot = "${groupId.replace(".", "/")}/$projectName/$version"

        putFileIntoLocalRepository(
            projectDir.resolve("build/libs/$projectName-$version.jar"),
            m2RepositoryDir,
            "$jarDestRoot/$projectName-$version.jar"
        )
        putFileIntoLocalRepository(
            projectDir.resolve("build/publications/Maven/module.json"),
            m2RepositoryDir,
            "$jarDestRoot/$projectName-$version.module"
        )
        putFileIntoLocalRepository(
            projectDir.resolve("build/publications/Maven/pom-default.xml"),
            m2RepositoryDir,
            "$jarDestRoot/$projectName-$version.pom"
        )
    }

    /**
     * A parameter object for project setup.
     */
    inner class ProjectSetup(
        val pluginId: String = defaultPluginId(),
        val projectName: String = defaultProjectName(),
        val settingsText: String = """
            rootProject.name = "$projectName"

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
        val prependedBuildFileText: String? = defaultPrependedBuildFileText(),
        val extraBuildFileText: String? = defaultExtraBuildFileText(),
        val extraSettingsFileText: String? = defaultExtraSettingsFileText(),
        val otherPluginsToApply: List<String> = emptyList(),
        val env: Map<String, String> = emptyMap()
    )

    inner class ArtifactVerification(
        val artifactId: String,
        val groupId: String,
        val version: String,
        val extension: String = "jar",
        val classifier: String? = null
    ) {
        val isSnapshot: Boolean = version.contains("SNAPSHOT")
        val groupPath: String = groupId.replace(".", "/")
        fun artifactFileName(actualVersion: String = version): String = "$artifactId-${actualVersion}${classifier?.let { "-$it" } ?: ""}.$extension"
        fun artifactPath(actualVersion: String = version): String = "${directoryPath()}/${artifactFileName(actualVersion)}"
        fun directoryPath(): String = "$groupPath/$artifactId/$version"
    }
}
