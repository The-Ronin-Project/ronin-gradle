import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    `maven-publish`
    base
    id("jacoco-report-aggregation")
    alias(libs.plugins.axion.release)
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.sonarqube)
}

val gradlePluginSubprojects = subprojects.filter { it.parent?.name == "gradle-plugins" }
val librarySubprojects = subprojects.filter { it.parent?.name == "shared-libraries" }

dependencies {
    gradlePluginSubprojects.forEach { project ->
        jacocoAggregation(project(":${project.path}"))
    }
}

scmVersion {
    tag {
        initialVersion { _, _ -> "1.0.0" }
    }
    versionCreator { versionFromTag, position ->
        val branchName = System.getenv("REF_NAME")?.ifBlank { null } ?: position.branch
        val supportedHeads = setOf("master", "main")
        // this code ensures that we get a labeled version for anything that's not master, main, or version/v<NUMBER> or v<NUMBER>.<NUMBER>.<NUMBER>,
        // but that we get a PLAIN version for  master, main, or version/v<NUMBER> or v<NUMBER>.<NUMBER>.<NUMBER>
        // The jiraBranchRegex tries to identify a ticket project-<NUMBER> format and uses that as the label
        if (!supportedHeads.contains(branchName) && !branchName.matches("^version/v\\d+$".toRegex()) && !branchName.matches("^v\\d+\\.\\d+\\.\\d+$".toRegex())) {
            val jiraBranchRegex = Regex("(?:.*/)?(\\w+)-(\\d+)(?:-(.+))?")
            val match = jiraBranchRegex.matchEntire(branchName)
            val branchExtension = match?.let {
                val (jiraProject, ticketNumber, _) = it.destructured
                "$jiraProject$ticketNumber"
            } ?: branchName

            "$versionFromTag-$branchExtension"
        } else {
            versionFromTag
        }
    }
}

val projectVersion: String = scmVersion.version
val kotlinId: String = libs.plugins.kotlin.jvm.get().pluginId
val ktlintId: String = libs.plugins.ktlint.get().pluginId

allprojects {
    group = "com.projectronin.services.gradle"
    version = projectVersion

    apply {
        plugin("maven-publish")
    }

    publishing {
        publications {
            repositories {
                maven {
                    name = "nexus"
                    credentials {
                        username = System.getenv("NEXUS_USER")
                        password = System.getenv("NEXUS_TOKEN")
                    }
                    url = if (project.version.toString().endsWith("SNAPSHOT")) {
                        uri("https://repo.devops.projectronin.io/repository/maven-snapshots/")
                    } else {
                        uri("https://repo.devops.projectronin.io/repository/maven-releases/")
                    }
                }
            }
        }
    }
}


gradlePluginSubprojects.forEach { subProject ->
    subProject.apply {
        plugin(kotlinId)
        plugin(ktlintId)
        plugin("jacoco")
        plugin("java")
        plugin("java-gradle-plugin")
    }

    subProject.tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.set(listOf("-Xjsr305=strict"))
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    subProject.tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events(org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED)
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    subProject.tasks.withType<JacocoReport> {
        executionData.setFrom(fileTree(buildDir).include("/jacoco/*.exec"))
        dependsOn(*subProject.tasks.withType<Test>().toTypedArray())
    }
}

librarySubprojects.forEach { subProject ->
    subProject.apply {
        plugin(kotlinId)
        plugin(ktlintId)
        plugin("jacoco")
        plugin("java")
    }

    subProject.extensions.getByType(KtlintExtension::class).apply {
        filter {
            exclude { entry ->
                entry.file.toString().contains("generated-sources")
            }
        }
    }

    subProject.tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.set(listOf("-Xjsr305=strict"))
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    subProject.tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events(org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED)
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    subProject.tasks.withType<JacocoReport> {
        executionData.setFrom(fileTree(buildDir).include("/jacoco/*.exec"))
        dependsOn(*subProject.tasks.withType<Test>().toTypedArray())
    }
}

sonar {
    properties {
        property("sonar.projectKey", project.name)
        property("sonar.projectName", project.name)
        property("sonar.coverage.exclusions", "**/test/**")
        property("sonar.coverage.jacoco.xmlReportPaths", layout.buildDirectory.file("reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml").get())
    }
}

@Suppress("UnstableApiUsage")
reporting {
    reports {
        create("testCodeCoverageReport", JacocoCoverageReport::class) {
            testType.set(TestSuiteType.UNIT_TEST)
            reportTask {
                executionData.setFrom(
                    subprojects.map { subproject ->
                        fileTree(subproject.buildDir).include("/jacoco/*.exec")
                    }
                )
                dependsOn(*subprojects.mapNotNull { p -> p.tasks.findByName("jacocoTestReport") }.toTypedArray())
            }
        }
    }
}

tasks.getByName("testCodeCoverageReport").dependsOn(*subprojects.mapNotNull { p -> p.tasks.findByName("jacocoTestReport") }.toTypedArray())
tasks.getByName("sonar").dependsOn("testCodeCoverageReport")
