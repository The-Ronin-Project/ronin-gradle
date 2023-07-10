package com.projectronin.gradle.test

import mu.KotlinLogging
import org.gradle.internal.impldep.org.eclipse.jgit.api.Git
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.UUID

@ExcludeAsIfGenerated
abstract class AbstractFunctionalTest {

    @field:TempDir
    lateinit var tempFolder: File

    protected val logger = KotlinLogging.logger {}

    protected val projectDir
        get() = tempFolder
    protected val buildFile
        get() = projectDir.resolve("build.gradle.kts")

    protected val settingsFile
        get() = projectDir.resolve("settings.gradle.kts")

    protected abstract val someTestResourcesPath: String

    protected val pluginProjectDirectory: File
        get() = File(File(javaClass.classLoader.getResource(someTestResourcesPath)!!.file).parentFile.absolutePath.replace("/build.*".toRegex(), ""))

    protected open fun defaultProjectName(): String = "change-project-name-here"

    protected abstract fun defaultPluginId(): String

    protected abstract fun defaultAdditionalBuildFileText(): String

    protected abstract fun defaultExtraStuffToDo(git: Git)

    protected fun setupTestProject(
        buildArguments: List<String>,
        pluginId: String = defaultPluginId(),
        projectName: String = defaultProjectName(),
        settingsText: String = """
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
        prependedBuildFileText: String = "",
        extraBuildFileText: String? = null,
        fail: Boolean = false,
        printFileTree: Boolean = false,
        env: Map<String, String> = emptyMap(),
        extraStuffToDo: (Git) -> Unit = { defaultExtraStuffToDo(it) }
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
        git.add().addFilepattern("*").call()
        git.commit().setMessage("Initial Commit").call()

        settingsFile.writeText(settingsText)
        buildFile.writeText("$prependedBuildFileText\n")
        buildFile.appendText(defaultAdditionalBuildFileText())
        buildFile.appendText("\n\n")
        buildFile.appendText(
            """
            plugins {
                id("$pluginId")
            }
            """.trimIndent()
        )

        extraBuildFileText?.run {
            buildFile.appendText("\n$this")
        }

        extraStuffToDo(git)

        logger.info { "=".repeat(80) }
        logger.info { buildFile.readText() }
        logger.info { "=".repeat(80) }

        // Run the build
        return try {
            runProjectBuild(buildArguments, fail, env)
        } finally {
            if (printFileTree) {
                projectDir.walk().forEach { file ->
                    logger.info { file }
                }
            }
        }
    }

    protected fun runProjectBuild(buildArguments: List<String>, fail: Boolean, env: Map<String, String>): BuildResult {
        val runner = GradleRunner.create().withCoverage(pluginProjectDirectory, tempFolder)
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
        runner.withProjectDir(projectDir)
        return if (fail) {
            runner.buildAndFail()
        } else {
            runner.build()
        }
    }

    protected fun copyResourceDir(resourceName: String, toDir: File, exclude: (file: File) -> Boolean = { false }) {
        val classLoader = javaClass.classLoader
        val baseDirectory = File(classLoader.getResource(resourceName)!!.file).parentFile
        baseDirectory.copyRecursively(toDir)
        toDir.walk(FileWalkDirection.BOTTOM_UP)
            .filter(exclude)
            .forEach { it.deleteRecursively() }
    }

    protected fun createDependency(resourceName: String, repoRoot: File, groupId: String, artifactId: String, version: String) {
        val classLoader = javaClass.classLoader

        val baseDirectory = File(classLoader.getResource(resourceName)!!.file).parentFile

        val newTempDirectory = projectDir.resolve(".tmp/dependency-${UUID.randomUUID()}")
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
}
