package com.projectronin.buildconventions

import com.projectronin.gradle.test.AbstractFunctionalTest
import com.projectronin.roninbuildconventionsroot.PluginIdentifiers
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.Test
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

class RootConventionsPluginFunctionalTest : AbstractFunctionalTest() {

    @Test
    fun `should build a multi-module project`() {
        val result = setupAndExecuteTestProject(
            listOf("build", "--stacktrace")
        ) {
            copyResourceDir("projects/demo", projectDir)
        }

        assertThat(result.output).contains("BUILD SUCCESSFUL")

        val reportResult = runProjectBuild(
            listOf("testCodeCoverageReport", "--stacktrace")
        )

        assertThat(reportResult.output).contains("BUILD SUCCESSFUL")

        verifySubProject(projectDir.resolve("empty-middle/subproject-01"), "subproject-01")
        verifySubProject(projectDir.resolve("middle"), "middle")
        verifySubProject(projectDir.resolve("middle/subproject-02"), "subproject-02")
        verifySubProject(projectDir.resolve("subproject-03"), "subproject-03")

        assertThat(projectDir.resolve("build/reports/jacoco/testCodeCoverageReport/html/index.html")).exists()
        assertThat(projectDir.resolve("build/reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml")).exists()

        val builderFactory = DocumentBuilderFactory.newInstance()
        val builder = builderFactory.newDocumentBuilder()
        val xmlDocument = builder.parse(
            ByteArrayInputStream(
                projectDir.resolve("build/reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml").readText()
                    .replace("<!DOCTYPE.*?>".toRegex(), "")
                    .toByteArray()
            )
        )
        val xPath = XPathFactory.newInstance().newXPath()
        with(xPath.compile("report/counter[@type='INSTRUCTION']").evaluate(xmlDocument, XPathConstants.NODE) as Element) {
            assertThat(getAttribute("missed")).isEqualTo("0")
            assertThat(getAttribute("covered")).isEqualTo("60")
        }
        with(xPath.compile("report/counter[@type='LINE']").evaluate(xmlDocument, XPathConstants.NODE) as Element) {
            assertThat(getAttribute("missed")).isEqualTo("0")
            assertThat(getAttribute("covered")).isEqualTo("4")
        }
        with(xPath.compile("report/counter[@type='METHOD']").evaluate(xmlDocument, XPathConstants.NODE) as Element) {
            assertThat(getAttribute("missed")).isEqualTo("0")
            assertThat(getAttribute("covered")).isEqualTo("8")
        }
        with(xPath.compile("report/counter[@type='CLASS']").evaluate(xmlDocument, XPathConstants.NODE) as Element) {
            assertThat(getAttribute("missed")).isEqualTo("0")
            assertThat(getAttribute("covered")).isEqualTo("4")
        }
    }

    @Test
    fun `see if we can fail the base ktlint`() {
        val result = setupAndExecuteTestProject(
            listOf("build", "--stacktrace"),
            projectSetup = ProjectSetup(
                prependedBuildFileText = "import java.util.UUID\n"
            ),
            fail = true
        ) {
            copyResourceDir("projects/demo", projectDir)
        }

        assertThat(result.output).contains("BUILD FAILED")
        assertThat(result.output).contains("build/reports/ktlint/ktlintKotlinScriptCheck/ktlintKotlinScriptCheck.txt")
    }

    @Test
    fun `should contain the releasehub tasks`() {
        val result = setupAndExecuteTestProject(
            listOf("tasks", "--stacktrace")
        ) {
            copyResourceDir("projects/demo", projectDir)
        }

        assertThat(result.output).contains("validateDependencies")
        assertThat(result.output).contains("listDependenciesToUpgrade")
        assertThat(result.output).contains("listDependencies")
        assertThat(result.output).contains("upgradeDependencies")
    }

    @Test
    fun `should generate documentation`() {
        val result = setupAndExecuteTestProject(
            listOf("dokkaHtmlMultiModule", "--stacktrace"),
            printFileTree = true
        ) {
            copyResourceDir("projects/demo", projectDir)
        }

        assertThat(result.output).contains("BUILD SUCCESSFUL")

        assertThat(projectDir.resolve("build/dokka/htmlMultiModule/middle/index.html")).exists()
        verifySubProjectDokka(projectDir.resolve("empty-middle/subproject-01"), "subproject01", "subproject-01")
        verifySubProjectDokka(projectDir.resolve("middle"), "middle", "middle")
        verifySubProjectDokka(projectDir.resolve("middle/subproject-02"), "subproject02", "subproject-02")
        verifySubProjectDokka(projectDir.resolve("subproject-03"), "subproject03", "subproject-03")
    }

    private fun verifySubProject(path: File, expectedArtifactId: String, expectedVersion: String? = null) {
        assertThat(path.resolve("build/libs/$expectedArtifactId${expectedVersion?.let { "-$it" } ?: ""}.jar")).exists()
        assertThat(path.resolve("build/jacoco/test.exec")).exists()
        assertThat(path.resolve("build/test-results")).exists()
        assertThat(path.resolve("build/reports/ktlint/ktlintKotlinScriptCheck")).exists()
        assertThat(path.resolve("build/reports/tests/test/index.html")).exists()
    }

    private fun verifySubProjectDokka(path: File, expectedPackage: String, expectedArtifactId: String) {
        assertThat(path.resolve("build/dokka/htmlPartial/com.example.$expectedPackage/-hello-world/index.html")).exists()
        assertThat(projectDir.resolve("build/dokka/htmlMultiModule/middle/$expectedArtifactId/com.example.$expectedPackage/-hello-world/index.html"))
    }

    override fun defaultPluginId(): String = PluginIdentifiers.buildconventionsRoot

    override fun defaultAdditionalBuildFileText(): String = """
        version = "1.0.0-SNAPSHOT"
    """.trimIndent()

    override fun defaultExtraStuffToDo(git: Git) {
        // do nothing
    }

    override fun defaultExtraSettingsFileText(): String = """
        include(":empty-middle:subproject-01")
        include(":middle")
        include(":middle:subproject-02")
        include(":subproject-03")
    """.trimIndent()
}
