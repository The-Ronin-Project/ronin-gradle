package com.projectronin.openapi

import com.projectronin.gradle.test.AbstractFunctionalTest
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.Test
import java.util.zip.ZipFile

class OpenApiKotlinGeneratorFunctionalTest : AbstractFunctionalTest() {

    @Test
    fun `can run task`() {
        val result = setupTestProject(
            listOf("generateOpenApiCode", "--stacktrace")
        )

        // Verify the result
        with(result.output) {
            assertThat(this).contains("Task :generateOpenApiCode")
            assertThat(this).contains("BUILD SUCCESSFUL")
            assertThat(this).contains("1 actionable task: 1 executed")
        }
    }

    @Test
    fun `can actually generate code`() {
        val result = setupTestProject(
            listOf("generateOpenApiCode", "--stacktrace")
        ) {
            settingsFile.appendText("\n include(\"app\")\n")
            copyResourceDir("generation-test/placeholder", tempFolder)
        }

        with(tempFolder.resolve("app/build/generated/openapi-kotlin-generator/kotlin/com/examples/externalmodels/api/v1/models/PolymorphicEnumDiscriminator.kt").readText()) {
            assertThat(this).contains("package com.examples.externalmodels.api.v1.models")
            assertThat(this).contains("JsonSubTypes.Type")
            assertThat(this).contains("data class ConcreteImplOne")
            assertThat(this).contains("data class ConcreteImplTwo")
            assertThat(this).contains("class ConcreteImplThree")
        }
        with(tempFolder.resolve("app/build/generated/openapi-kotlin-generator/kotlin/com/examples/externalmodels/api/v1/models/Wrapper.kt").readText()) {
            assertThat(this).contains("val polymorph: PolymorphicEnumDiscriminator")
        }
        with(tempFolder.resolve("app/build/generated/openapi-kotlin-generator/kotlin/com/examples/externalmodels/api/v1/models/EnumDiscriminator.kt").readText()) {
            assertThat(this).contains("OBJ_ONE_ONLY(\"obj_one_only\")")
        }
        with(tempFolder.resolve("app/build/generated/openapi-kotlin-generator/kotlin/com/examples/externalmodels/api/v1/controllers/FooController.kt").readText()) {
            assertThat(this).contains("fun getFoo(): ResponseEntity<Wrapper>")
        }

        // Verify the result
        with(result.output) {
            assertThat(this).contains("BUILD SUCCESSFUL")
        }
    }

    @Test
    fun `can generate code using a dependency`() {
        val result = setupTestProject(
            listOf("assemble", "--stacktrace")
        ) {
            settingsFile.appendText("\n include(\"app\")\n")
            copyResourceDir("dependency-generation-test/placeholder", tempFolder)
        }

        assertThat(tempFolder.resolve("app/build/generated/openapi-kotlin-generator/resources/META-INF/resources/v1/questionnaire.json")).exists()
        assertThat(tempFolder.resolve("app/build/generated/openapi-kotlin-generator/kotlin/com/projectronin/services/questionnaire/api/v1/models/AbstractQuestionGroup.kt")).exists()
        assertThat(tempFolder.resolve("app/build/resources/main/META-INF/resources/v1/questionnaire.json")).exists()

        val entry = ZipFile(tempFolder.resolve("app/build/libs/app.jar")).getEntry("META-INF/resources/v1/questionnaire.json")
        assertThat(entry).isNotNull()
        assertThat(entry.compressedSize).isGreaterThan(1000L)

        // Verify the result
        with(result.output) {
            assertThat(this).contains("BUILD SUCCESSFUL")
        }
    }

    override val someTestResourcesPath: String = "generation-test/placeholder"

    override fun defaultPluginId(): String = "com.projectronin.openapi"

    override fun defaultAdditionalBuildFileText(): String = ""

    override fun defaultExtraStuffToDo(git: Git) {
    }
}
