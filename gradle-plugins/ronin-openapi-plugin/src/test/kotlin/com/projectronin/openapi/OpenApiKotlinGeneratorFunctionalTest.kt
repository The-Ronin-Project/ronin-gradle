package com.projectronin.openapi

import com.projectronin.gradle.test.AbstractFunctionalTest
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.Test
import java.util.zip.ZipFile

class OpenApiKotlinGeneratorFunctionalTest : AbstractFunctionalTest() {

    @Test
    fun `can actually generate code`() {
        val result = setupAndExecuteTestProject(
            listOf("generateOpenApiCode", "--stacktrace")
        ) {
            settingsFile.appendText("\n include(\"app\")\n")
            buildFile.writeText("")
            copyResourceDir("generation-test", projectDir)
        }

        with(projectDir.resolve("app/build/generated/openapi-kotlin-generator/kotlin/com/examples/externalmodels/api/v1/models/PolymorphicEnumDiscriminator.kt").readText()) {
            assertThat(this).contains("package com.examples.externalmodels.api.v1.models")
            assertThat(this).contains("JsonSubTypes.Type")
            assertThat(this).contains("data class ConcreteImplOne")
            assertThat(this).contains("data class ConcreteImplTwo")
            assertThat(this).contains("class ConcreteImplThree")
        }
        with(projectDir.resolve("app/build/generated/openapi-kotlin-generator/kotlin/com/examples/externalmodels/api/v1/models/Wrapper.kt").readText()) {
            assertThat(this).contains("val polymorph: PolymorphicEnumDiscriminator")
        }
        with(projectDir.resolve("app/build/generated/openapi-kotlin-generator/kotlin/com/examples/externalmodels/api/v1/models/EnumDiscriminator.kt").readText()) {
            assertThat(this).contains("OBJ_ONE_ONLY(\"obj_one_only\")")
        }
        with(projectDir.resolve("app/build/generated/openapi-kotlin-generator/kotlin/com/examples/externalmodels/api/v1/controllers/FooController.kt").readText()) {
            assertThat(this).contains("fun getFoo(): ResponseEntity<Wrapper>")
        }

        // Verify the result
        with(result.output) {
            assertThat(this).contains("BUILD SUCCESSFUL")
        }
    }

    @Test
    fun `can generate code using a dependency`() {
        val result = setupAndExecuteTestProject(
            listOf("assemble", "--stacktrace")
        ) {
            settingsFile.appendText("\n include(\"app\")\n")
            buildFile.writeText("")
            copyResourceDir("dependency-generation-test", projectDir)
        }

        assertThat(projectDir.resolve("app/build/generated/openapi-kotlin-generator/resources/META-INF/resources/v1/questionnaire.json")).exists()
        assertThat(projectDir.resolve("app/build/generated/openapi-kotlin-generator/kotlin/com/projectronin/services/questionnaire/api/v1/models/AbstractQuestionGroup.kt")).exists()
        assertThat(projectDir.resolve("app/build/resources/main/META-INF/resources/v1/questionnaire.json")).exists()

        with(projectDir.resolve("app/build/generated/openapi-kotlin-generator/kotlin/com/projectronin/services/questionnaire/api/v1/controllers/SummaryController.kt").readText()) {
            assertThat(this).contains("suspend fun getSummary(")
        }

        val entry = ZipFile(projectDir.resolve("app/build/libs/app.jar")).getEntry("META-INF/resources/v1/questionnaire.json")
        assertThat(entry).isNotNull()
        assertThat(entry.compressedSize).isGreaterThan(1000L)

        // Verify the result
        with(result.output) {
            assertThat(this).contains("BUILD SUCCESSFUL")
        }
    }

    @Test
    fun `can generate webflux-friendly controller interfaces`() {
        val result = setupAndExecuteTestProject(
            listOf("generateOpenApiCode", "--stacktrace")
        ) {
            settingsFile.appendText("\n include(\"app\")\n")
            buildFile.writeText("")
            copyResourceDir("webflux-test", projectDir)
        }

        assertThat(result.output).contains("BUILD SUCCESSFUL")

        with(projectDir.resolve("app/build/generated/openapi-kotlin-generator/kotlin/com/examples/externalmodels/api/v1/controllers/FooController.kt").readText()) {
            assertThat(this).contains("import org.reactivestreams.Publisher")
            assertThat(this).contains("fun getFoo(): Publisher<Wrapper>")
        }
    }

    override fun defaultPluginId(): String = "com.projectronin.openapi"

    override fun defaultAdditionalBuildFileText(): String? = null

    override fun defaultExtraStuffToDo(git: Git) {
    }
}
