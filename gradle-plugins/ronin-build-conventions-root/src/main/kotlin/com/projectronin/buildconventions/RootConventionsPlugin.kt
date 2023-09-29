package com.projectronin.buildconventions

import com.projectronin.gradle.helpers.BaseGradlePluginIdentifiers
import com.projectronin.gradle.helpers.applyPlugin
import com.projectronin.gradle.helpers.dependsOnTasksByType
import com.projectronin.gradle.helpers.maybeServiceVersion
import com.projectronin.gradle.helpers.projectDependency
import com.projectronin.roninbuildconventionsroot.DependencyHelper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.TestSuiteType
import org.gradle.api.reporting.ReportingExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoCoverageReport
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.sonarqube.gradle.SonarExtension

class RootConventionsPlugin : Plugin<Project> {

    companion object {
        private const val BUILD_FILE_NAME = "build.gradle.kts"
        private const val JACOCO_AGGREGATION_DEPENDENCY_SCOPE = "jacocoAggregation"
        private const val RONIN_SONAR_CONFIG_NAME = "roninSonar"
        private const val DEFAULT_XML_REPORT_LOCATION = "reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml"
        private const val CLASS_DIR_NAME = "classes"
        private const val SONAR_TASK_NAME = "sonar"
        private const val SINGLE_REPORT_TASK_NAME = "jacocoTestReport"
        private const val AGGREGATED_REPORT_TASK_NAME = "testCodeCoverageReport"
        private val COVERAGE_EXCLUSIONS: List<String> = listOf(
            "**/test/**",
            "**/test-utilities/**",
            "**/*.kts",
            "**/kotlin/dsl/accessors/**",
            "**/kotlin/test/**"
        )
        private val jacocoIncludes: String = "/jacoco/*.exec"
    }

    override fun apply(target: Project) {
        val meaningfulSubProjects = target.subprojects.filter { it.projectDir.resolve(BUILD_FILE_NAME).exists() }

        val isAggregationProject = meaningfulSubProjects.isNotEmpty()

        if (isAggregationProject) {
            target.logger.info("Applying root to a project with no sub-projects.")
        }

        applyOtherPlugins(target, isAggregationProject)

        meaningfulSubProjects.forEach { subProject ->
            subProject.group = target.group
            target.projectDependency(JACOCO_AGGREGATION_DEPENDENCY_SCOPE, ":${subProject.path}")
        }

        applySonarConfigs(target)

        if (isAggregationProject) {
            applyAggregationConfigs(target, meaningfulSubProjects)
        } else {
            applySingleConfig(target)
        }
    }

    private fun applySingleConfig(target: Project) {
        target.afterEvaluate {
            it.tasks.getByName(SINGLE_REPORT_TASK_NAME).apply {
                if (this is JacocoReport) {
                    val roninSonarConfig = target.extensions.getByType(RoninSonarConfig::class.java)
                    reports.xml.required.set(true)
                    reports.xml.outputLocation.set(target.layout.buildDirectory.file(roninSonarConfig.xmlReportPath.get()))
                    executionData.setFrom(target.fileTree(target.buildDir).include(jacocoIncludes))
                    classDirectories.setFrom(
                        target.fileTree(target.buildDir.resolve(CLASS_DIR_NAME)).exclude(COVERAGE_EXCLUSIONS)
                    )
                }
                dependsOnTasksByType(Test::class.java, target)
            }
            it.tasks.getByName(SONAR_TASK_NAME).dependsOn(SINGLE_REPORT_TASK_NAME)
        }
    }

    private fun applyAggregationConfigs(target: Project, meaningfulSubProjects: List<Project>) {
        @Suppress("UnstableApiUsage")
        target.extensions.getByType(ReportingExtension::class.java).apply {
            reports.apply {
                create(AGGREGATED_REPORT_TASK_NAME, JacocoCoverageReport::class.java).apply {
                    reportTask.get().apply {
                        executionData.setFrom(
                            meaningfulSubProjects.map { subproject ->
                                subproject.fileTree(subproject.buildDir).include(jacocoIncludes)
                            }
                        )
                        classDirectories.setFrom(
                            meaningfulSubProjects.map { subproject ->
                                subproject.fileTree(subproject.buildDir.resolve(CLASS_DIR_NAME)).exclude(COVERAGE_EXCLUSIONS)
                            }
                        )
                    }
                    testType.set(TestSuiteType.UNIT_TEST)
                }
            }
        }

        target.tasks.getByName(AGGREGATED_REPORT_TASK_NAME) { testCodeCoverageReport ->
            meaningfulSubProjects.forEach { subProject ->
                testCodeCoverageReport.dependsOnTasksByType(Test::class.java, subProject)
            }
        }
        target.tasks.getByName(SONAR_TASK_NAME).dependsOn(AGGREGATED_REPORT_TASK_NAME)

        target.subprojects.forEach { subProject ->
            if (subProject.childProjects.isNotEmpty()) {
                subProject.tasks.whenTaskAdded { task ->
                    if (task.name == "dokkaHtmlMultiModule") {
                        task.enabled = false
                    }
                }
            }
        }
    }

    private fun applySonarConfigs(target: Project) {
        target.extensions.create(RONIN_SONAR_CONFIG_NAME, RoninSonarConfig::class.java).apply {
            projectKey.convention(target.name)
            projectName.convention(target.name)
            coverageExclusions.convention(COVERAGE_EXCLUSIONS)
            xmlReportPath.convention(DEFAULT_XML_REPORT_LOCATION)
        }

        target.afterEvaluate {
            val roninSonarConfig = target.extensions.getByType(RoninSonarConfig::class.java)
            target.extensions.getByType(SonarExtension::class.java).apply {
                properties {
                    it.property("sonar.projectKey", roninSonarConfig.projectKey.get())
                    it.property("sonar.projectName", roninSonarConfig.projectName.get())
                    it.property("sonar.coverage.exclusions", roninSonarConfig.coverageExclusions.get())
                    it.property("sonar.coverage.jacoco.xmlReportPaths", target.layout.buildDirectory.file(roninSonarConfig.xmlReportPath.get()).get())
                    if (roninSonarConfig.referenceBranch.isPresent) {
                        it.property("sonar.newCode.referenceBranch", roninSonarConfig.referenceBranch.get())
                    }
                    if (target.version == Project.DEFAULT_VERSION) {
                        target.maybeServiceVersion()?.let { sv ->
                            it.property("sonar.projectVersion", sv)
                        }
                    }
                }
            }
        }
    }

    private fun applyOtherPlugins(target: Project, isAggregationProject: Boolean) {
        target
            .applyPlugin(BaseGradlePluginIdentifiers.base)
            .run {
                if (isAggregationProject) {
                    applyPlugin(BaseGradlePluginIdentifiers.jacocoReportAggregation)
                } else {
                    applyPlugin(BaseGradlePluginIdentifiers.jacoco)
                }
            }
            .applyPlugin(DependencyHelper.Plugins.sonar.id)
            .applyPlugin(DependencyHelper.Plugins.ktlint.id)
            .applyPlugin(DependencyHelper.Plugins.dokka.id)
            .applyPlugin(DependencyHelper.Plugins.releasehub.id)
    }
}
