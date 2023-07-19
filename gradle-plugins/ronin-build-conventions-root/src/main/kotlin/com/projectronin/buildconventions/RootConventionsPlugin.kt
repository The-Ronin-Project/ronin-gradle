package com.projectronin.buildconventions

import com.projectronin.gradle.helpers.BaseGradlePluginIdentifiers
import com.projectronin.gradle.helpers.applyPlugin
import com.projectronin.gradle.helpers.projectDependency
import com.projectronin.roninbuildconventionsroot.DependencyHelper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.TestSuiteType
import org.gradle.api.reporting.ReportingExtension
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

        target.extensions.getByType(SonarExtension::class.java).apply {
            properties {
                it.property("sonar.projectKey", target.name)
                it.property("sonar.projectName", target.name)
                it.property("sonar.coverage.exclusions", "**/test/**,**/generated-sources/**,**/generated/sources/**,**/*.kts,**/kotlin/dsl/accessors/**,**/kotlin/test/**")
                it.property("sonar.coverage.jacoco.xmlReportPaths", target.layout.buildDirectory.file("reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml").get())
            }
        }

        @Suppress("UnstableApiUsage")
        target.extensions.getByType(ReportingExtension::class.java).apply {
            reports.apply {
                create("testCodeCoverageReport", JacocoCoverageReport::class.java).apply {
                    testType.set(TestSuiteType.UNIT_TEST)
                    reportTask.get().apply {
                        executionData.setFrom(
                            meaningfulSubProjects.map { subproject ->
                                subproject.fileTree(subproject.buildDir).include("/jacoco/*.exec")
                            }
                        )
                        dependsOn(*meaningfulSubProjects.mapNotNull { p -> p.tasks.findByName("jacocoTestReport") }.toTypedArray())
                        classDirectories.setFrom(
                            meaningfulSubProjects.map { subproject ->
                                subproject.fileTree(subproject.buildDir.resolve("classes")).exclude("**/kotlin/dsl/accessors/**", "**/kotlin/test/**")
                            }
                        )
                    }
                }
            }
        }

        target.tasks.getByName("testCodeCoverageReport").dependsOn(*meaningfulSubProjects.mapNotNull { p -> p.tasks.findByName("jacocoTestReport") }.toTypedArray())
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
