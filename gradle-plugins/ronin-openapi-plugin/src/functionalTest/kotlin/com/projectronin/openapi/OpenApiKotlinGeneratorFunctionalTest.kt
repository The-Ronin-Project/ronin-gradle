package com.projectronin.openapi

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.zip.ZipFile

@Suppress("UsePropertyAccessSyntax")
class OpenApiKotlinGeneratorFunctionalTest {

    @TempDir
    lateinit var tempDir: Path

    private fun getProjectDir() = tempDir.toFile()
    private fun getBuildFile() = getProjectDir().resolve("build.gradle")
    private fun getSettingsFile() = getProjectDir().resolve("settings.gradle")

    @Test
    fun `can run task`() {
        // Setup the test build
        getSettingsFile().writeText("")
        getBuildFile().writeText(
            """
                plugins {
                    id('com.projectronin.openapi')
                }
                
                repositories {
                    mavenLocal()
                    mavenCentral()
                }
            """.trimIndent()
        )

        // Run the build
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("generateOpenApiCode", "--stacktrace")
        runner.withProjectDir(getProjectDir())
        val result = runner.build()

        // Verify the result
        with(result.output) {
            assertThat(this).contains("Task :generateOpenApiCode")
            assertThat(this).contains("BUILD SUCCESSFUL")
            assertThat(this).contains("1 actionable task: 1 executed")
        }
    }

    @Test
    fun `can actually generate code`() {
        val resource = javaClass.classLoader.getResource("generation-test/settings.gradle.kts")
        assertThat(resource).describedAs("Resource must not be null").isNotNull()
        val generationTestInputDirectory = File(resource!!.file).parentFile
        generationTestInputDirectory.copyRecursively(getProjectDir())

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("build", "--stacktrace")
        runner.withProjectDir(getProjectDir())
        val result = runner.build()

        with(tempDir.toFile().resolve("app/build/generated/openapi-kotlin-generator/kotlin/com/examples/externalmodels/api/v1/models/PolymorphicEnumDiscriminator.kt").readText()) {
            assertThat(this).contains("package com.examples.externalmodels.api.v1.models")
            assertThat(this).contains("JsonSubTypes.Type")
            assertThat(this).contains("data class ConcreteImplOne")
            assertThat(this).contains("data class ConcreteImplTwo")
            assertThat(this).contains("class ConcreteImplThree")
        }
        with(tempDir.toFile().resolve("app/build/generated/openapi-kotlin-generator/kotlin/com/examples/externalmodels/api/v1/models/Wrapper.kt").readText()) {
            assertThat(this).contains("val polymorph: PolymorphicEnumDiscriminator")
        }
        with(tempDir.toFile().resolve("app/build/generated/openapi-kotlin-generator/kotlin/com/examples/externalmodels/api/v1/models/EnumDiscriminator.kt").readText()) {
            assertThat(this).contains("OBJ_ONE_ONLY(\"obj_one_only\")")
        }
        with(tempDir.toFile().resolve("app/build/generated/openapi-kotlin-generator/kotlin/com/examples/externalmodels/api/v1/controllers/FooController.kt").readText()) {
            assertThat(this).contains("fun getFoo(): ResponseEntity<Wrapper>")
        }

        // Verify the result
        with(result.output) {
            assertThat(this).contains("BUILD SUCCESSFUL")
        }
    }

    @Test
    fun `can generate code using a dependency`() {
        val resource = javaClass.classLoader.getResource("dependency-generation-test/settings.gradle.kts")
        assertThat(resource).describedAs("Resource must not be null").isNotNull()
        val generationTestInputDirectory = File(resource!!.file).parentFile
        generationTestInputDirectory.copyRecursively(getProjectDir())

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("assemble", "--stacktrace")
        runner.withProjectDir(getProjectDir())
        val result = runner.build()

        assertThat(tempDir.toFile().resolve("app/build/generated/openapi-kotlin-generator/resources/META-INF/resources/v1/questionnaire.json")).exists()
        assertThat(tempDir.toFile().resolve("app/build/generated/openapi-kotlin-generator/kotlin/com/projectronin/services/questionnaire/api/v1/models/AbstractQuestionGroup.kt")).exists()
        assertThat(tempDir.toFile().resolve("app/build/resources/main/META-INF/resources/v1/questionnaire.json")).exists()

        val entry = ZipFile(tempDir.toFile().resolve("app/build/libs/app.jar")).getEntry("META-INF/resources/v1/questionnaire.json")
        assertThat(entry).isNotNull()
        assertThat(entry.compressedSize).isGreaterThan(1000L)

        // Verify the result
        with(result.output) {
            assertThat(this).contains("BUILD SUCCESSFUL")
        }
    }
}
