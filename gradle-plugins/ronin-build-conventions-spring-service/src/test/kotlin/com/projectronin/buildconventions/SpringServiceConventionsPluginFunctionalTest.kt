package com.projectronin.buildconventions

import com.fasterxml.jackson.databind.ObjectMapper
import com.projectronin.gradle.test.AbstractFunctionalTest
import com.projectronin.roninbuildconventionsspringservice.PluginIdentifiers
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.util.jar.JarFile

class SpringServiceConventionsPluginFunctionalTest : AbstractFunctionalTest() {

    @Test
    fun `should build a spring service project`() {
        val result = setupAndExecuteTestProject(
            listOf("build", "--stacktrace")
        ) {
            it.tag().setName("1.0.0-alpha").call()
            copyResourceDir("projects/demo", projectDir)
            projectDir.resolve(".gitignore").appendText("\ngradle.properties\n")
            it.add().addFilepattern(".").call()
            it.commit()
                .setMessage("Added some stuff")
                .setAuthor("Dionne Wable", "Dionne.Wable@example.org")
                .setCommitter("Dionne Wable", "Dionne.Wable@example.org")
                .call()
        }

        assertThat(result.tasks.first { it.path == ":bootJar" }.outcome).isEqualTo(TaskOutcome.SUCCESS)

        val info = ObjectMapper().readTree(projectDir.resolve("build/generated/resources/service-info/service-info.json"))
        assertThat(info["version"]?.textValue()).isEqualTo("1.0.0-SNAPSHOT")
        assertThat(info["lastTag"]?.textValue()).isEqualTo("1.0.0-alpha")
        assertThat(info["commitDistance"]?.intValue()).isEqualTo(1)
        assertThat(info["gitHash"]?.textValue()).matches("^[a-f0-9]{3,}$")
        assertThat(info["gitHashFull"]?.textValue()).matches("^[a-f0-9]{40}$")
        assertThat(info["branchName"]?.textValue()).isEqualTo("main")
        assertThat(info["dirty"]?.booleanValue()).isFalse()

        assertThat(projectDir.resolve("build/libs/change-project-name-here.jar")).exists()
        assertThat(projectDir.resolve("build/libs/change-project-name-here.jar")).exists()
        assertThat(projectDir.resolve("build/generated/resources/service-info/service-info.json")).exists()

        val jar = JarFile(projectDir.resolve("build/libs/change-project-name-here.jar"))
        val entry = jar.entries().asSequence().find { it.name == "BOOT-INF/classes/service-info.json" }
        assertThat(entry).isNotNull
    }

    @Test
    fun `should create version info without a tag`() {
        setupAndExecuteTestProject(
            listOf("generateServiceInfo", "--stacktrace")
        ) {
            copyResourceDir("projects/demo", projectDir)
            projectDir.resolve(".gitignore").appendText("\ngradle.properties\n")
            it.add().addFilepattern(".").call()
            it.commit()
                .setMessage("Added some stuff")
                .setAuthor("Dionne Wable", "Dionne.Wable@example.org")
                .setCommitter("Dionne Wable", "Dionne.Wable@example.org")
                .call()
        }

        assertThat(projectDir.resolve("build/generated/resources/service-info/service-info.json")).exists()
        val info = ObjectMapper().readTree(projectDir.resolve("build/generated/resources/service-info/service-info.json"))
        assertThat(info["version"]?.textValue()).isEqualTo("1.0.0-SNAPSHOT")
        assertThat(info["lastTag"]?.textValue()).isEqualTo("n/a")
        assertThat(info["commitDistance"]?.intValue()).isEqualTo(0)
        assertThat(info["gitHash"]?.textValue()).matches("^[a-f0-9]{3,}$")
        assertThat(info["gitHashFull"]?.textValue()).matches("^[a-f0-9]{40}$")
        assertThat(info["branchName"]?.textValue()).isEqualTo("main")
        assertThat(info["dirty"]?.booleanValue()).isFalse()
    }

    @Test
    fun `should create version using direct version input`() {
        setupAndExecuteTestProject(
            listOf("generateServiceInfo", "--stacktrace", "-Pservice-version=7.32.1")
        ) {
            copyResourceDir("projects/demo", projectDir)
            projectDir.resolve(".gitignore").appendText("\ngradle.properties\n")
            it.add().addFilepattern(".").call()
            it.commit()
                .setMessage("Added some stuff")
                .setAuthor("Dionne Wable", "Dionne.Wable@example.org")
                .setCommitter("Dionne Wable", "Dionne.Wable@example.org")
                .call()
        }

        assertThat(projectDir.resolve("build/generated/resources/service-info/service-info.json")).exists()
        val info = ObjectMapper().readTree(projectDir.resolve("build/generated/resources/service-info/service-info.json"))
        assertThat(info["version"]?.textValue()).isEqualTo("7.32.1")
        assertThat(info["lastTag"]?.textValue()).isEqualTo("n/a")
        assertThat(info["commitDistance"]?.intValue()).isEqualTo(0)
        assertThat(info["gitHash"]?.textValue()).matches("^[a-f0-9]{3,}$")
        assertThat(info["gitHashFull"]?.textValue()).matches("^[a-f0-9]{40}$")
        assertThat(info["branchName"]?.textValue()).isEqualTo("main")
        assertThat(info["dirty"]?.booleanValue()).isFalse()
    }

    @Test
    fun `should create version info that's dirty`() {
        setupAndExecuteTestProject(
            listOf("generateServiceInfo", "--stacktrace")
        ) {
            it.tag().setName("3.1.7").call()
            copyResourceDir("projects/demo", projectDir)
            projectDir.resolve(".gitignore").appendText("\ngradle.properties\n")
            it.add().addFilepattern(".").call()
            it.commit()
                .setMessage("Added some stuff")
                .setAuthor("Dionne Wable", "Dionne.Wable@example.org")
                .setCommitter("Dionne Wable", "Dionne.Wable@example.org")
                .call()
            projectDir.resolve("anuntrackedfile.txt").writeText("Hi!  I'm untracked!")
        }

        assertThat(projectDir.resolve("build/generated/resources/service-info/service-info.json")).exists()
        val info = ObjectMapper().readTree(projectDir.resolve("build/generated/resources/service-info/service-info.json"))
        assertThat(info["version"]?.textValue()).isEqualTo("3.1.8-SNAPSHOT")
        assertThat(info["lastTag"]?.textValue()).isEqualTo("3.1.7")
        assertThat(info["commitDistance"]?.intValue()).isEqualTo(1)
        assertThat(info["gitHash"]?.textValue()).matches("^[a-f0-9]{3,}$")
        assertThat(info["gitHashFull"]?.textValue()).matches("^[a-f0-9]{40}$")
        assertThat(info["branchName"]?.textValue()).isEqualTo("main")
        assertThat(info["dirty"]?.booleanValue()).isTrue()
    }

    @Test
    fun `should create version info that is not a snapshot`() {
        setupAndExecuteTestProject(
            listOf("generateServiceInfo", "--stacktrace")
        ) {
            copyResourceDir("projects/demo", projectDir)
            projectDir.resolve(".gitignore").appendText("\ngradle.properties\n")
            it.add().addFilepattern(".").call()
            it.commit()
                .setMessage("Added some stuff")
                .setAuthor("Dionne Wable", "Dionne.Wable@example.org")
                .setCommitter("Dionne Wable", "Dionne.Wable@example.org")
                .call()
            it.tag().setName("3.1.7").call()
        }

        assertThat(projectDir.resolve("build/generated/resources/service-info/service-info.json")).exists()
        val info = ObjectMapper().readTree(projectDir.resolve("build/generated/resources/service-info/service-info.json"))
        assertThat(info["version"]?.textValue()).isEqualTo("3.1.7")
        assertThat(info["lastTag"]?.textValue()).isEqualTo("3.1.7")
        assertThat(info["commitDistance"]?.intValue()).isEqualTo(0)
        assertThat(info["gitHash"]?.textValue()).matches("^[a-f0-9]{3,}$")
        assertThat(info["gitHashFull"]?.textValue()).matches("^[a-f0-9]{40}$")
        assertThat(info["branchName"]?.textValue()).isEqualTo("main")
        assertThat(info["dirty"]?.booleanValue()).isFalse()
    }

    override fun defaultPluginId(): String = PluginIdentifiers.buildconventionsSpringService

    override fun defaultAdditionalBuildFileText(): String = """
        group = "com.example"
        dependencies {
            implementation("org.springframework.boot:spring-boot-starter")
            implementation("org.jetbrains.kotlin:kotlin-reflect")
            testImplementation("org.springframework.boot:spring-boot-starter-test")
        }
    """.trimIndent()

    override fun defaultExtraStuffToDo(git: Git) {
        // do nothing
    }
}
