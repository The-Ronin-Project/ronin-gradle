package com.projectronin.buildconventions

import com.projectronin.gradle.helpers.BaseGradlePluginIdentifiers
import com.projectronin.gradle.helpers.addDependentTaskByType
import com.projectronin.gradle.helpers.applyPlugin
import com.projectronin.gradle.helpers.projectDependency
import com.projectronin.roninbuildconventionsroot.DependencyHelper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.TestSuiteType
import org.gradle.api.reporting.ReportingExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoCoverageReport
import org.sonarqube.gradle.SonarExtension

class RootConventionsPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target
            .applyPlugin(BaseGradlePluginIdentifiers.base)
            .applyPlugin(BaseGradlePluginIdentifiers.jacocoReportAggregation)
            .applyPlugin(DependencyHelper.Plugins.sonar.id)
            .applyPlugin(DependencyHelper.Plugins.ktlint.id)
            .applyPlugin(DependencyHelper.Plugins.dokka.id)
            .applyPlugin(DependencyHelper.Plugins.releasehub.id)

        val meaningfulSubProjects = target.subprojects.filter { it.projectDir.resolve("build.gradle.kts").exists() }

        meaningfulSubProjects.forEach { subProject ->
            subProject.group = target.group
            target.projectDependency("jacocoAggregation", ":${subProject.path}")
        }

        target.extensions.create("roninSonar", RoninSonarConfig::class.java).apply {
            projectKey.convention(target.name)
            projectName.convention(target.name)
            coverageExclusions.convention(
                listOf(
                    "**/test/**",
                    "**/test-utilities/**",
                    "**/*.kts",
                    "**/kotlin/dsl/accessors/**",
                    "**/kotlin/test/**"
                )
            )
            xmlReportPath.convention("reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml")
        }

        target.afterEvaluate {
            val roninSonarConfig = target.extensions.getByType(RoninSonarConfig::class.java)
            target.extensions.getByType(SonarExtension::class.java).apply {
                properties {
                    it.property("sonar.projectKey", roninSonarConfig.projectKey.get())
                    it.property("sonar.projectName", roninSonarConfig.projectName.get())
                    it.property("sonar.coverage.exclusions", roninSonarConfig.coverageExclusions.get())
                    it.property("sonar.coverage.jacoco.xmlReportPaths", target.layout.buildDirectory.file(roninSonarConfig.xmlReportPath.get()).get())
                }
            }
        }

        @Suppress("UnstableApiUsage")
        target.extensions.getByType(ReportingExtension::class.java).apply {
            reports.apply {
                create("testCodeCoverageReport", JacocoCoverageReport::class.java).apply {
                    reportTask.get().apply {
                        executionData.setFrom(
                            meaningfulSubProjects.map { subproject ->
                                subproject.fileTree(subproject.buildDir).include("/jacoco/*.exec")
                            }
                        )
                        classDirectories.setFrom(
                            meaningfulSubProjects.map { subproject ->
                                subproject.fileTree(subproject.buildDir.resolve("classes")).exclude("**/kotlin/dsl/accessors/**", "**/kotlin/test/**")
                            }
                        )
                    }
                    testType.set(TestSuiteType.UNIT_TEST)
                }
            }
        }

        target.tasks.getByName("testCodeCoverageReport") { testCodeCoverageReport ->
            meaningfulSubProjects.forEach { subProject ->
                testCodeCoverageReport.addDependentTaskByType(Test::class.java, subProject)
            }
        }
        target.tasks.getByName("sonar").dependsOn("testCodeCoverageReport")

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
}
