package com.projectronin.rest.contract

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.projectronin.gradle.test.AbstractFunctionalTest
import com.projectronin.gradle.test.getArchiveEntries
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID
import java.util.jar.JarFile

/**
 * A simple functional test for the 'com.projectronin.rest.contract.support' plugin.
 */
class RestContractSupportPluginFunctionalTest : AbstractFunctionalTest() {

    private val jsonMapper = ObjectMapper()

    private val yamlMapper = ObjectMapper(YAMLFactory())

    @Test
    fun `building the API works`() {
        basicBuildTest("1.4.7", "v1")
    }

    @Test
    fun `building the API works with a different package name`() {
        basicBuildTest(
            "1.4.7",
            "v1",
            packageName = "com.foo.questionnaire"
        ) {
            defaultExtraStuffToDo(it)
            projectDir.resolve("build.gradle.kts").appendText(
                """
                    restContractSupport {
                        packageName.set("com.foo.questionnaire")
                    }
                    
                """.trimIndent()
            )
        }
    }

    @Test
    fun `building the API works for v2`() {
        basicBuildTest(
            "2.0.0-SNAPSHOT",
            "v2"
        ) { git ->
            git.tag().setName("v2.0.0-alpha").call()
            copyBaseResources("v2")
            writeSpectralConfig()
            projectDir.resolve("build.gradle.kts").appendText(
                """
                    restContractSupport {
                        inputFile.set(layout.projectDirectory.file("src/main/openapi/questionnaire.yml"))
                    }
                    
                """.trimIndent()
            )
            commit(git)
        }
    }

    @Test
    fun `fails if the project name doesn't match`() {
        val result = setupAndExecuteTestProject(
            listOf("build"),
            projectSetup = ProjectSetup(
                extraSettingsFileText = """
                    rootProject.name = "foo"
                """.trimIndent()
            ),
            fail = true
        )

        assertThat(result.output.contains("Unable to read location.*foo.json".toRegex()))
    }

    @Test
    fun `linting fails and prints problems`() {
        val result = setupAndExecuteTestProject(
            listOf("check"),
            fail = true
        ) {
            copyBaseResources()
            writeSpectralConfig()
            val v1Tree = (projectDir.resolve("src/main/openapi/questionnaire.json")).readTree()
            v1Tree.remove("tags")
            (projectDir.resolve("src/main/openapi/questionnaire.json")).writeValue(v1Tree)
            commit(it)
        }

        assertThat(result.output).contains("operation-tag-defined")
    }

    @Test
    fun `linting does not fail if we disable linting`() {
        val result = setupAndExecuteTestProject(
            listOf("check"),
            projectSetup = ProjectSetup(
                extraBuildFileText = """
                    
                    restContractSupport {
                        disableLinting.set(true)
                    }
                    
                """.trimIndent() + defaultExtraBuildFileText()
            )
        ) {
            copyBaseResources()
            writeSpectralConfig()
            val v1Tree = (projectDir.resolve("src/main/openapi/questionnaire.json")).readTree()
            v1Tree.remove("tags")
            (projectDir.resolve("src/main/openapi/questionnaire.json")).writeValue(v1Tree)
            commit(it)
        }

        assertThat(result.output).contains("Task :lintApi SKIPPED")
    }

    // cleanTaskName = "cleanApiOutput",
    @Test
    fun `clean output works`() {
        setupAndExecuteTestProject(
            listOf("clean")
        ) {
            copyBaseResources()
            writeSpectralConfig()
            copyResourceDir("test-apis/v1", projectDir.resolve("src/main/resources/openapi/.dependencies"))

            assertThat(projectDir.resolve("src/main/resources/openapi/.dependencies")).exists()

            commit(it)
        }

        assertThat(projectDir.resolve("src/main/openapi/.dependencies")).doesNotExist()
    }

    @Test
    fun `works using openapi-generator`() {
        basicBuildTest(
            "1.4.7",
            "v1",
            packageName = "com.foo.questionnaire",
            modelPackageSuffix = "model",
            controllerPackageSuffix = "api",
            expectedModelFiles = arrayOf(
                "AbstractQuestionGroup.kt",
                "AbstractQuestionnaire.kt",
                "AbstractQuestionnaireAssignment.kt",
                "AbstractResponse.kt",
                "Action.kt",
                "ActionType.kt",
                "AlertTier.kt",
                "Answer.kt",
                "AnswerDefinition.kt",
                "AnswerDefinitionIdentifier.kt",
                "AnswerSubmission.kt",
                "AnswerSummary.kt",
                "AssignmentRequestContext.kt",
                "CreatePatientRequestBody.kt",
                "EndQuestionGroupAction.kt",
                "ErrorResponse.kt",
                "ErrorResponseAllOf.kt",
                "ErrorResponseAllOfError.kt",
                "FreeTextAnswer.kt",
                "FreeTextAnswerAllOf.kt",
                "JumpToQuestionAction.kt",
                "JumpToQuestionActionAllOf.kt",
                "MultipleChoiceAnswer.kt",
                "MultipleChoiceAnswerAllOf.kt",
                "NextQuestionAction.kt",
                "NumericRangeAnswer.kt",
                "NumericRangeAnswerAllOf.kt",
                "PatientTelecom.kt",
                "PeriodDetail.kt",
                "Question.kt",
                "QuestionGroup.kt",
                "QuestionGroupAllOf.kt",
                "QuestionGroupIdentifier.kt",
                "QuestionGroupState.kt",
                "QuestionGroupStateAllOf.kt",
                "QuestionGroupSummary.kt",
                "QuestionGroupSummaryDetail.kt",
                "QuestionGroupSummaryDetailAllOf.kt",
                "QuestionIdentifier.kt",
                "QuestionInputType.kt",
                "Questionnaire.kt",
                "QuestionnaireAllOf.kt",
                "QuestionnaireAssignment.kt",
                "QuestionnaireAssignmentAllOf.kt",
                "QuestionnaireAssignmentResponse.kt",
                "QuestionnaireAssignmentResponseAllOf.kt",
                "QuestionnaireAssignmentState.kt",
                "QuestionnaireAssignmentStateAllOf.kt",
                "QuestionnaireAssignmentStateResponse.kt",
                "QuestionnaireAssignmentStateResponseAllOf.kt",
                "QuestionnaireState.kt",
                "QuestionnaireStateAllOf.kt",
                "QuestionnaireSummary.kt",
                "QuestionnaireSummaryResponse.kt",
                "QuestionnaireSummaryResponseAllOf.kt",
                "QuestionState.kt",
                "RequiredTags.kt",
                "ResponseType.kt",
                "SingleChoiceAnswer.kt",
                "SingleChoiceAnswerAllOf.kt",
                "SummaryPeriodType.kt"
            ),
            expectedModelClasses = arrayOf(
                "AbstractQuestionGroup.class",
                "AbstractQuestionnaire.class",
                "AbstractQuestionnaireAssignment.class",
                "AbstractResponse.class",
                "Action.class",
                "ActionType.class",
                "AlertTier.class",
                "Answer.class",
                "AnswerDefinition.class",
                "AnswerDefinitionIdentifier.class",
                "AnswerSubmission.class",
                "AnswerSummary.class",
                "AssignmentRequestContext.class",
                "CreatePatientRequestBody.class",
                "EndQuestionGroupAction.class",
                "ErrorResponse.class",
                "ErrorResponseAllOf.class",
                "ErrorResponseAllOfError.class",
                "FreeTextAnswer.class",
                "FreeTextAnswerAllOf.class",
                "JumpToQuestionAction.class",
                "JumpToQuestionActionAllOf.class",
                "MultipleChoiceAnswer.class",
                "MultipleChoiceAnswerAllOf.class",
                "NextQuestionAction.class",
                "NumericRangeAnswer.class",
                "NumericRangeAnswerAllOf.class",
                "PatientTelecom.class",
                "PeriodDetail.class",
                "Question.class",
                "QuestionGroup.class",
                "QuestionGroupAllOf.class",
                "QuestionGroupIdentifier.class",
                "QuestionGroupState.class",
                "QuestionGroupStateAllOf.class",
                "QuestionGroupSummary.class",
                "QuestionGroupSummaryDetail.class",
                "QuestionGroupSummaryDetailAllOf.class",
                "QuestionIdentifier.class",
                "QuestionInputType.class",
                "Questionnaire.class",
                "QuestionnaireAllOf.class",
                "QuestionnaireAssignment.class",
                "QuestionnaireAssignmentAllOf.class",
                "QuestionnaireAssignmentResponse.class",
                "QuestionnaireAssignmentResponseAllOf.class",
                "QuestionnaireAssignmentState.class",
                "QuestionnaireAssignmentStateAllOf.class",
                "QuestionnaireAssignmentStateResponse.class",
                "QuestionnaireAssignmentStateResponseAllOf.class",
                "QuestionnaireState.class",
                "QuestionnaireStateAllOf.class",
                "QuestionnaireSummary.class",
                "QuestionnaireSummaryResponse.class",
                "QuestionnaireSummaryResponseAllOf.class",
                "QuestionState.class",
                "RequiredTags.class",
                "RequiredTags\$Operator.class",
                "ResponseType.class",
                "SingleChoiceAnswer.class",
                "SingleChoiceAnswerAllOf.class",
                "SummaryPeriodType.class"
            ),
            expectedControllerFiles = arrayOf("QuestionnaireApi.kt", "SummaryApi.kt"),
            expectedControllerClasses = arrayOf("QuestionnaireApi.class", "SummaryApi.class"),
            extraVerifiers = {
                val fileText = projectDir.resolve("build/generated/sources/openapi/com/foo/questionnaire/v1/api/QuestionnaireApi.kt").readText()
                assertThat(fileText.contains("suspend fun"))
                assertThat(fileText.contains("import org.springframework.stereotype.Controller"))
                assertThat(fileText.contains("@Controller\ninterface "))
            }
        ) {
            defaultExtraStuffToDo(it)
            projectDir.resolve("build.gradle.kts").appendText(
                """
                restContractSupport {
                    packageName.set("com.foo.questionnaire")
                    generatorType.set(com.projectronin.rest.contract.GeneratorType.OPENAPI_GENERATOR)
                    openApiGeneratorAdditionalProperties.put("reactive", true)
                }
                
                """.trimIndent()
            )
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

    override fun defaultPluginId(): String = "com.projectronin.openapi.contract"

    override fun defaultAdditionalBuildFileText(): String? = null

    override fun defaultExtraStuffToDo(git: Git) {
        copyBaseResources()
        writeSpectralConfig()
        commit(git)
        git.tag().setName("v1.4.7").call()
    }

    private fun commit(git: Git) {
        git.add().addFilepattern("*").call()
        git.commit().setMessage("Adding necessary resources").call()
    }

    private fun writeSpectralConfig() {
        (projectDir.resolve("spectral.yaml")).writeText(
            """
            extends: ["spectral:oas"]
            
            rules:
              oas3-unused-component: info
            """.trimIndent()
        )
    }

    private fun copyBaseResources(versionToInclude: String = "v1") {
        copyResourceDir("test-apis/$versionToInclude", projectDir.resolve("src/main/openapi"))
    }

    private fun basicBuildTest(
        semver: String,
        shortVersion: String,
        packageName: String = "com.projectronin.rest.questionnaire",
        printFileTree: Boolean = false,
        expectedModelFiles: Array<String> = expectedModelFiles(),
        expectedControllerFiles: Array<String> = expectedControllerFiles(),
        expectedModelClasses: Array<String> = expectedModelClasses(),
        expectedControllerClasses: Array<String> = expectedControllerClasses(),
        modelPackageSuffix: String = "models",
        controllerPackageSuffix: String = "controllers",
        extraVerifiers: () -> Unit = {},
        extraStuffToDo: (Git) -> Unit = { defaultExtraStuffToDo(it) }
    ) {
        runCatching {
            val result = testRemotePublish(
                listOf("build", "publish", "--stacktrace"),
                verifications = listOf(
                    ArtifactVerification("questionnaire-$shortVersion", "com.projectronin.rest.contract", semver, "json"),
                    ArtifactVerification("questionnaire-$shortVersion", "com.projectronin.rest.contract", semver, "yaml"),
                    ArtifactVerification("questionnaire-$shortVersion", "com.projectronin.rest.contract", semver, "tar.gz"),
                    ArtifactVerification("questionnaire-$shortVersion", "com.projectronin.rest.contract", semver, "jar")
                ),
                extraStuffToDo = extraStuffToDo,
                printFileTree = printFileTree
            )

            val packageDir = packageName.replace(".", "/")

            assertThat(result.output).contains("No results with a severity of 'warn' or higher found!")

            assertThat(projectDir.resolve("build/generated/resources/openapi/static/v3/api-docs/questionnaire/$shortVersion.json")).exists()
            assertThat(projectDir.resolve("build/generated/resources/openapi/static/v3/api-docs/questionnaire/$shortVersion.yaml")).exists()

            val jsonTree = jsonMapper.readTree(projectDir.resolve("build/generated/resources/openapi/static/v3/api-docs/questionnaire/$shortVersion.json"))
            assertThat(jsonTree["info"]["version"].textValue()).isEqualTo(semver)
            assertThat(jsonTree["components"]["schemas"].fields().asSequence().toList().map { it.key }).containsExactlyInAnyOrder(*getExpectedSchemaElements())

            val yamlTree = yamlMapper.readTree(projectDir.resolve("build/generated/resources/openapi/static/v3/api-docs/questionnaire/$shortVersion.yaml"))
            assertThat(yamlTree["info"]["version"].textValue()).isEqualTo(semver)
            assertThat(yamlTree["components"]["schemas"].fields().asSequence().toList().map { it.key }).containsExactlyInAnyOrder(*getExpectedSchemaElements())

            assertThat(projectDir.resolve("build/libs/questionnaire-$semver.jar")).exists()
            val jar = JarFile(projectDir.resolve("build/libs/questionnaire-$semver.jar"))
            assertThat(jar.entries().asSequence().find { it.name == "static/v3/api-docs/questionnaire/$shortVersion.json" }).isNotNull
            assertThat(jar.entries().asSequence().find { it.name == "static/v3/api-docs/questionnaire/$shortVersion.yaml" }).isNotNull
            assertThat(projectDir.resolve("build/tar/questionnaire.tar.gz")).exists()
            val entries = projectDir.resolve("build/tar/questionnaire.tar.gz").getArchiveEntries(true)
            assertThat(entries).containsExactlyInAnyOrder(
                "$shortVersion.json",
                "$shortVersion.yaml",
                "index.html"
            )
            assertThat(projectDir.resolve("build/openapidocs/index.html")).exists()
            assertThat(projectDir.resolve("openapitools.json")).doesNotExist()

            assertThat(result.output).contains("Task :downloadApiDependencies")

            assertThat(projectDir.resolve("src/main/openapi/.dependencies")).exists()
            assertThat((projectDir.resolve("src/main/openapi/.dependencies")).listFiles()).hasSize(1)

            assertThat(projectDir.resolve("src/main/openapi/.dependencies/contract-rest-clinical-data")).exists()
            assertThat(projectDir.resolve("src/main/openapi/.dependencies/contract-rest-clinical-data/contract-rest-clinical-data.json")).exists()

            assertThat(projectDir.resolve("build/generated/sources/openapi/$packageDir/$shortVersion/$modelPackageSuffix").listFiles()!!.map { it.name })
                .containsExactlyInAnyOrder(
                    *expectedModelFiles
                )
            assertThat(projectDir.resolve("build/generated/sources/openapi/$packageDir/$shortVersion/$controllerPackageSuffix").listFiles()!!.map { it.name })
                .containsExactlyInAnyOrder(
                    *expectedControllerFiles
                )

            assertThat(
                jar.entries().asSequence()
                    .filter { it.name.matches("$packageDir/$shortVersion/$modelPackageSuffix/.*class".toRegex()) }
                    .map { it.name.replace(".*/".toRegex(), "") }
                    .toList()
            ).containsExactlyInAnyOrder(
                *expectedModelClasses
            )
            assertThat(
                jar.entries().asSequence()
                    .filter { it.name.matches("$packageDir/$shortVersion/$controllerPackageSuffix/.*class".toRegex()) }
                    .map { it.name.replace(".*/".toRegex(), "") }
                    .toList()
            ).containsExactlyInAnyOrder(
                *expectedControllerClasses
            )
            // build/libs/questionnaire-1.4.7.jar
            // build/libs/questionnaire-1.4.7-sources.jar
            val sourcesJar = JarFile(projectDir.resolve("build/libs/questionnaire-$semver-sources.jar"))
            assertThat(sourcesJar.entries().asSequence().find { it.name == "static/v3/api-docs/questionnaire/$shortVersion.json" }).isNotNull
            assertThat(sourcesJar.entries().asSequence().find { it.name == "static/v3/api-docs/questionnaire/$shortVersion.yaml" }).isNotNull

            assertThat(
                sourcesJar.entries().asSequence()
                    .filter { it.name.matches("$packageDir/$shortVersion/$modelPackageSuffix/.*kt".toRegex()) }
                    .map { it.name.replace(".*/".toRegex(), "") }
                    .toList()
            ).containsExactlyInAnyOrder(
                *expectedModelFiles
            )
            assertThat(
                sourcesJar.entries().asSequence()
                    .filter { it.name.matches("$packageDir/$shortVersion/$controllerPackageSuffix/.*kt".toRegex()) }
                    .map { it.name.replace(".*/".toRegex(), "") }
                    .toList()
            ).containsExactlyInAnyOrder(
                *expectedControllerFiles
            )
            extraVerifiers()
        }
            .onFailure { t ->
                runCatching {
                    val failedTestDirectory = pluginBuildDirectory.resolve("failed-tests/${UUID.randomUUID()}")
                    failedTestDirectory.mkdirs()
                    logger.error { "Test failed.  Copying data to $failedTestDirectory" }
                    projectDir.copyRecursively(failedTestDirectory)
                }
                throw t
            }
    }

    override fun defaultExtraBuildFileText(): String {
        return """
               dependencies {
                   openapi("com.projectronin.rest.contract:contract-rest-clinical-data:1.0.0@json")
               }
        """.trimIndent()
    }

    override fun defaultProjectName(): String {
        return "questionnaire"
    }

    override fun defaultGroupId(): String {
        return "com.projectronin.rest.contract"
    }

    private fun getExpectedSchemaElements(): Array<String> {
        return arrayOf(
            "AssignmentRequestContext",
            "QuestionnaireAssignmentResponse",
            "AbstractResponse",
            "QuestionnaireAssignmentStateResponse",
            "QuestionnaireAssignmentState",
            "AbstractQuestionnaireAssignment",
            "QuestionnaireSummaryResponse",
            "ErrorResponse",
            "QuestionnaireAssignment",
            "QuestionnaireAssignmentId",
            "AnswerSubmission",
            "PatientId",
            "SummaryPeriodType",
            "TimeZone",
            "AlertTier",
            "ResponseType",
            "QuestionnaireState",
            "AbstractQuestionnaire",
            "QuestionnaireSummary",
            "QuestionGroupSummary",
            "QuestionGroupSummaryDetail",
            "PeriodDetail",
            "AnswerSummary",
            "Questionnaire",
            "Answer",
            "MultipleChoiceAnswer",
            "SingleChoiceAnswer",
            "NumericRangeAnswer",
            "FreeTextAnswer",
            "QuestionGroupState",
            "AbstractQuestionGroup",
            "RequiredTags",
            "QuestionGroup",
            "QuestionInputType",
            "QuestionGroupIdentifier",
            "QuestionIdentifier",
            "AnswerDefinitionIdentifier",
            "QuestionState",
            "Question",
            "Tags",
            "VersionHash",
            "AnswerDefinition",
            "Action",
            "ActionType",
            "JumpToQuestionAction",
            "NextQuestionAction",
            "EndQuestionGroupAction",
            "CreatePatientRequestBody",
            "PatientTelecom"
        )
    }

    private fun expectedModelFiles(): Array<String> = arrayOf(
        "PeriodDetail.kt",
        "QuestionIdentifier.kt",
        "QuestionInputType.kt",
        "ResponseType.kt",
        "QuestionGroupSummaryDetail.kt",
        "AssignmentRequestContext.kt",
        "QuestionGroupSummary.kt",
        "AnswerSummary.kt",
        "RequiredTagsOperator.kt",
        "QuestionState.kt",
        "AbstractQuestionnaire.kt",
        "AnswerDefinition.kt",
        "ActionType.kt",
        "QuestionnaireSummary.kt",
        "Answer.kt",
        "AnswerDefinitionIdentifier.kt",
        "QuestionGroupIdentifier.kt",
        "AnswerSubmission.kt",
        "AbstractQuestionGroup.kt",
        "AbstractQuestionnaireAssignment.kt",
        "SummaryPeriodType.kt",
        "AbstractResponse.kt",
        "Question.kt",
        "AlertTier.kt",
        "Action.kt",
        "ErrorResponseError.kt",
        "RequiredTags.kt",
        "PatientTelecom.kt",
        "CreatePatientRequestBody.kt"
    )

    private fun expectedModelClasses(): Array<String> = arrayOf(
        "ActionType.class",
        "QuestionGroupSummary.class",
        "QuestionnaireAssignmentStateResponse.class",
        "EndQuestionGroupAction.class",
        "AbstractResponse.class",
        "SummaryPeriodType.class",
        "AnswerDefinitionIdentifier.class",
        "ActionType\$Companion.class",
        "FreeTextAnswer.class",
        "QuestionInputType.class",
        "AbstractQuestionnaire.class",
        "QuestionGroupSummaryDetail.class",
        "MultipleChoiceAnswer.class",
        "QuestionnaireState.class",
        "QuestionState.class",
        "QuestionIdentifier.class",
        "QuestionGroupIdentifier.class",
        "SummaryPeriodType\$Companion.class",
        "AbstractQuestionnaireAssignment.class",
        "SingleChoiceAnswer.class",
        "ResponseType\$Companion.class",
        "AnswerSubmission.class",
        "AbstractQuestionGroup.class",
        "Question.class",
        "AlertTier.class",
        "NumericRangeAnswer.class",
        "RequiredTagsOperator\$Companion.class",
        "AlertTier\$Companion.class",
        "QuestionnaireAssignmentResponse.class",
        "AnswerSummary.class",
        "QuestionnaireAssignmentState.class",
        "Questionnaire.class",
        "ResponseType.class",
        "QuestionnaireSummary.class",
        "JumpToQuestionAction.class",
        "ErrorResponse.class",
        "PeriodDetail.class",
        "NextQuestionAction.class",
        "QuestionInputType\$Companion.class",
        "ErrorResponseError.class",
        "QuestionnaireAssignment.class",
        "AssignmentRequestContext.class",
        "AnswerDefinition.class",
        "QuestionGroupState.class",
        "RequiredTagsOperator.class",
        "Answer.class",
        "QuestionGroup.class",
        "RequiredTags.class",
        "QuestionnaireSummaryResponse.class",
        "Action.class",
        "PatientTelecom.class",
        "CreatePatientRequestBody.class"
    )

    private fun expectedControllerFiles(): Array<String> = arrayOf(
        "SummaryController.kt",
        "QuestionnaireController.kt"
    )

    private fun expectedControllerClasses(): Array<String> = arrayOf(
        "SummaryController.class",
        "QuestionnaireController.class"
    )
}
