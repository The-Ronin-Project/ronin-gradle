package com.projectronin.buildconventions

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.michaelbull.retry.policy.binaryExponentialBackoff
import com.github.michaelbull.retry.policy.limitAttempts
import com.github.michaelbull.retry.policy.plus
import com.github.michaelbull.retry.retry
import com.projectronin.gradle.test.AbstractFunctionalTest
import com.projectronin.roninbuildconventionsroot.PluginIdentifiers
import kotlinx.coroutines.runBlocking
import okhttp3.Credentials
import okhttp3.Request
import okhttp3.internal.EMPTY_REQUEST
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.File
import java.time.Duration
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

    @Test
    fun `should build a single-module project`() {
        val m2RepositoryDir = projectDir.resolve(".m2/repository")

        val groupId = "com.projectronin.services.gradle"

        copyPluginToLocalRepo(m2RepositoryDir, groupId, "ronin-build-conventions-spring-service", "com.projectronin.buildconventions.spring-service", projectVersion)
        copyPluginToLocalRepo(m2RepositoryDir, groupId, "ronin-build-conventions-kotlin", "com.projectronin.buildconventions.kotlin-jvm", projectVersion)
        copyJarToLocalRepository(m2RepositoryDir, groupId, rootDirectory.resolve("shared-libraries/gradle-helpers"), "gradle-helpers", projectVersion)
        copyJarToLocalRepository(m2RepositoryDir, groupId, rootDirectory.resolve("shared-libraries/database-test-helpers"), "database-test-helpers", projectVersion)

        val result = setupAndExecuteTestProject(
            listOf("build", "--stacktrace", "-Dmaven.repo.local=$m2RepositoryDir"),
            projectSetup = ProjectSetup(
                extraSettingsFileText = null,
                otherPluginsToApply = listOf(
                    """id("com.projectronin.buildconventions.spring-service") version "$projectVersion"""",
                    """id("com.projectronin.buildconventions.kotlin-jvm") version "$projectVersion""""
                ),
                extraBuildFileText = """
                    group = "com.example"
                    dependencies {
                        implementation("org.springframework.boot:spring-boot-starter")
                        implementation("org.jetbrains.kotlin:kotlin-reflect")
                        testImplementation("org.springframework.boot:spring-boot-starter-test")
                    }
                """.trimIndent()
            )
        ) {
            copyResourceDir("projects/single", projectDir)
        }

        assertThat(result.output).contains("BUILD SUCCESSFUL")

        val reportResult = runProjectBuild(
            listOf("jacocoTestReport", "--stacktrace", "-Dmaven.repo.local=$m2RepositoryDir")
        )

        assertThat(reportResult.output).contains("BUILD SUCCESSFUL")

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
            assertThat(getAttribute("missed")).isEqualTo("13")
            assertThat(getAttribute("covered")).isEqualTo("3")
        }

        val sonarResult = runProjectBuild(
            listOf("sonar", "--stacktrace", "-Dmaven.repo.local=$m2RepositoryDir"),
            env = mapOf("SONAR_HOST_URL" to "http://localhost:33983"),
            fail = true
        )

        assertThat(sonarResult.output).contains("> Task :test\n> Task :jacocoTestReport")
        assertThat(sonarResult.output).contains("Unable to execute SonarScanner analysis")
    }

    @Test
    fun `should build with sonar`() {
        val container = GenericContainer(DockerImageName.parse("sonarqube:9-community"))
            .withEnv("SONAR_ES_BOOTSTRAP_CHECKS_DISABLE", "true")
            .withExposedPorts(9000)
            .waitingFor(Wait.forLogMessage(".*SonarQube is operational.*", 1))
            .withStartupTimeout(Duration.ofMinutes(5))
        kotlin.runCatching { container.start() }
            .onFailure { e ->
                println(container.logs)
                throw e
            }
        try {
            val creds = Credentials.basic("admin", "admin")
            val token = httpClient.newCall(
                Request.Builder()
                    .post(EMPTY_REQUEST)
                    .url("http://localhost:${container.getMappedPort(9000)}/api/user_tokens/generate?name=test")
                    .header("Authorization", creds)
                    .build()
            )
                .execute().use { response ->
                    ObjectMapper().readTree(response.body!!.string())["token"].textValue()
                }

            val result = setupAndExecuteTestProject(
                listOf("build", "sonar", "--stacktrace", "-Dsonar.login=$token", "-Dsonar.host.url=http://localhost:${container.getMappedPort(9000)}")
            ) {
                copyResourceDir("projects/demo", projectDir)
            }

            assertThat(result.output).contains("BUILD SUCCESSFUL")

            assertThat(projectDir.resolve("build/reports/jacoco/testCodeCoverageReport/html/index.html")).exists()
            assertThat(projectDir.resolve("build/reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml")).exists()

            runBlocking {
                retry(
                    limitAttempts(5) +
                        binaryExponentialBackoff(base = 1000, max = 10000)
                ) {
                    httpClient.newCall(
                        Request.Builder()
                            .get()
                            .url("http://localhost:${container.getMappedPort(9000)}/api/components/show?component=change-project-name-here")
                            .header("Authorization", creds)
                            .build()
                    )
                        .execute().use { response ->
                            assertThat(ObjectMapper().readTree(response.body!!.string())["component"]["version"].textValue()).isEqualTo("1.0.0-SNAPSHOT")
                        }
                }
            }
        } finally {
            container.stop()
        }
    }

    @Test
    fun `should build with sonar and service version`() {
        val container = GenericContainer(DockerImageName.parse("sonarqube:9-community"))
            .withEnv("SONAR_ES_BOOTSTRAP_CHECKS_DISABLE", "true")
            .withExposedPorts(9000)
            .waitingFor(Wait.forLogMessage(".*SonarQube is operational.*", 1))
            .withStartupTimeout(Duration.ofMinutes(5))
        kotlin.runCatching { container.start() }
            .onFailure { e ->
                println(container.logs)
                throw e
            }
        try {
            val creds = Credentials.basic("admin", "admin")
            val token = httpClient.newCall(
                Request.Builder()
                    .post(EMPTY_REQUEST)
                    .url("http://localhost:${container.getMappedPort(9000)}/api/user_tokens/generate?name=test")
                    .header("Authorization", creds)
                    .build()
            )
                .execute().use { response ->
                    ObjectMapper().readTree(response.body!!.string())["token"].textValue()
                }

            val result = setupAndExecuteTestProject(
                listOf("build", "sonar", "--stacktrace", "-Dsonar.login=$token", "-Dsonar.host.url=http://localhost:${container.getMappedPort(9000)}", "-Pservice-version=7.3.9"),
                projectSetup = ProjectSetup(prependedBuildFileText = null)
            ) {
                copyResourceDir("projects/demo", projectDir)
            }

            assertThat(result.output).contains("BUILD SUCCESSFUL")

            assertThat(projectDir.resolve("build/reports/jacoco/testCodeCoverageReport/html/index.html")).exists()
            assertThat(projectDir.resolve("build/reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml")).exists()

            runBlocking {
                retry(
                    limitAttempts(5) +
                        binaryExponentialBackoff(base = 1000, max = 10000)
                ) {
                    httpClient.newCall(
                        Request.Builder()
                            .get()
                            .url("http://localhost:${container.getMappedPort(9000)}/api/components/show?component=change-project-name-here")
                            .header("Authorization", creds)
                            .build()
                    )
                        .execute().use { response ->
                            assertThat(ObjectMapper().readTree(response.body!!.string())["component"]["version"].textValue()).isEqualTo("7.3.9")
                        }
                }
            }
        } finally {
            container.stop()
        }
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
    override fun defaultAdditionalBuildFileText(): String? = null

    override fun defaultPrependedBuildFileText(): String = """
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
