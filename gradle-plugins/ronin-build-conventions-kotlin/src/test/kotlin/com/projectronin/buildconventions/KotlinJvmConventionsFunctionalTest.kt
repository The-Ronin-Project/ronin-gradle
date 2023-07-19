package com.projectronin.buildconventions

import com.projectronin.gradle.test.AbstractFunctionalTest
import com.projectronin.roninbuildconventionskotlin.DependencyHelper
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.URIish
import org.junit.jupiter.api.Test
import kotlin.io.path.moveTo

class KotlinJvmConventionsFunctionalTest : AbstractFunctionalTest() {

    @Test
    fun testSimpleWorkingProject() {
        val result = setupAndExecuteTestProject(
            listOf("build", "--stacktrace")
        ) {
            copyResourceDir("tests/simple-kotlin-project", projectDir)
        }
        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(projectDir.resolve("build/classes/kotlin/main/com/projectronin/sample/Hello.class")).exists()
        assertThat(projectDir.resolve("build/classes/kotlin/test/com/projectronin/sample/HelloTest.class")).exists()
        assertThat(projectDir.resolve("build/reports/tests/test/index.html")).exists()
        assertThat(projectDir.resolve("build/jacoco/test.exec")).exists()
    }

    @Test
    fun testSimpleFailingProject() {
        val result = setupAndExecuteTestProject(
            listOf("build", "--stacktrace"),
            fail = true
        ) {
            copyResourceDir("tests/wont-compile-kotlin-project", projectDir)
        }
        assertThat(result.output).contains("BUILD FAILED")
        assertThat(result.output).contains("Unresolved reference: doesnotexist")
    }

    @Test
    fun testTestFailsProject() {
        val result = setupAndExecuteTestProject(
            listOf("build", "--stacktrace"),
            fail = true
        ) {
            copyResourceDir("tests/test-fails-kotlin-project", projectDir)
        }
        assertThat(result.output).contains("BUILD FAILED")
        assertThat(result.output).matches("(?ms).*HelloTest.*testHello.*FAILED.*")
        assertThat(result.output).matches("(?ms).*expected.*\"Hello world!\".*")
        assertThat(result.output).matches("(?ms).*but was.*\"Hello world\".*")
        assertThat(result.output).matches("(?ms).*at.*com.projectronin.sample.HelloTest.testHello.*")
    }

    @Test
    fun testKtlintFailsProject() {
        val result = setupAndExecuteTestProject(
            listOf("build", "--stacktrace"),
            fail = true
        ) {
            copyResourceDir("tests/ktlint-fails-kotlin-project", projectDir)
        }
        assertThat(result.output).contains("BUILD FAILED")
        assertThat(result.output).contains("KtLint found code style violations")
    }

    @Test
    fun testDokka() {
        val result = setupAndExecuteTestProject(
            listOf("dokkaHtml", "--stacktrace")
        ) {
            copyResourceDir("tests/simple-kotlin-project", projectDir)
        }
        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(projectDir.resolve("build/dokka/html/index.html")).exists()
        assertThat(projectDir.resolve("build/dokka/html/change-project-name-here/com.projectronin.sample/-hello/index.html")).exists()
    }

    @Test
    fun testDokkaWithRemote() {
        var commit: String = ""
        val result = setupAndExecuteTestProject(
            listOf("dokkaHtml", "--stacktrace")
        ) { git ->
            git.remoteAdd().setName("origin").setUri(URIish("git@github.com:projectronin/ronin-gradle.git")).call()
            commit = git.repository.exactRef("HEAD").objectId.name
            copyResourceDir("tests/simple-kotlin-project", projectDir)
        }
        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(projectDir.resolve("build/dokka/html/index.html")).exists()
        assertThat(projectDir.resolve("build/dokka/html/change-project-name-here/com.projectronin.sample/-hello/index.html")).exists()
        assertThat(projectDir.resolve("build/dokka/html/change-project-name-here/com.projectronin.sample/-hello/index.html").readText()).contains(
            "https://github.com/projectronin/ronin-gradle/blob/$commit/src/main/kotlin/com/projectronin/sample/Hello.kt#L3"
        )
    }

    @Test
    fun testDokkaSubprojectWithRemote() {
        var commit: String = ""
        val result = setupAndExecuteTestProject(
            listOf("dokkaHtmlMultiModule", "--stacktrace"),
            printFileTree = true,
            projectSetup = ProjectSetup(
                extraSettingsFileText = "\ninclude(\":sub\")\n"
            )
        ) { git ->
            git.remoteAdd().setName("origin").setUri(URIish("git@github.com:projectronin/ronin-gradle.git")).call()
            commit = git.repository.exactRef("HEAD").objectId.name
            copyResourceDir("tests/simple-kotlin-project", projectDir.resolve("sub"))
            projectDir.resolve("build.gradle.kts").toPath().moveTo(projectDir.resolve("sub/build.gradle.kts").toPath())
            projectDir.resolve("build.gradle.kts").writeText(
                """
                plugins {
                   id("${DependencyHelper.Plugins.dokka.id}")
                }
                """.trimIndent()
            )
        }
        assertThat(result.output).contains("BUILD SUCCESSFUL")
        assertThat(result.output).contains("> Task :sub:dokkaHtmlPartial")
        assertThat(result.output).contains("> Task :dokkaHtmlMultiModule")
        assertThat(projectDir.resolve("sub/build/dokka/htmlPartial/index.html")).exists()
        assertThat(projectDir.resolve("sub/build/dokka/htmlPartial/com.projectronin.sample/-hello/say-hello.html")).exists()
        assertThat(projectDir.resolve("build/dokka/htmlMultiModule/sub/index.html")).exists()
        assertThat(projectDir.resolve("build/dokka/htmlMultiModule/sub/com.projectronin.sample/-hello/index.html")).exists()
        assertThat(projectDir.resolve("build/dokka/htmlMultiModule/sub/com.projectronin.sample/-hello/index.html").readText()).contains(
            "https://github.com/projectronin/ronin-gradle/blob/$commit/sub/src/main/kotlin/com/projectronin/sample/Hello.kt#L3"
        )
    }

    override fun defaultPluginId(): String = "com.projectronin.buildconventions.kotlin-jvm"

    override fun defaultAdditionalBuildFileText(): String? = null

    override fun defaultExtraStuffToDo(git: Git) {
        // nothing to do
    }
}
