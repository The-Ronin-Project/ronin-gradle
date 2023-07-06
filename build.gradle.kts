@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    `version-catalog`
    `maven-publish`
    alias(libs.plugins.axion.release)
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.kover)
    id("org.sonarqube") version "4.0.0.2929"
}

dependencies {
    subprojects.forEach { project ->
        kover(project(":${project.name}"))
    }
}

scmVersion {
    tag {
        initialVersion { _, _ -> "1.0.0" }
    }
    versionCreator { versionFromTag, position ->
        val branchName = System.getenv("REF_NAME")?.ifBlank { null } ?: position.branch
        val supportedHeads = setOf("master", "main")
        if (!supportedHeads.contains(branchName) && !branchName.matches("^version/v\\d+$".toRegex())) {
            val jiraBranchRegex = Regex("(?:.*/)?(\\w+)-(\\d+)-(.+)")
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

version = scmVersion.version

val projectVersion: String = scmVersion.version
val kotlinId: String = libs.plugins.kotlin.jvm.get().pluginId
val ktlintId: String = libs.plugins.ktlint.get().pluginId
val koverId: String = libs.plugins.kover.get().pluginId

subprojects {
    group = "com.projectronin.services.gradle"
    version = projectVersion

    apply {
        plugin(kotlinId)
        plugin(ktlintId)
        plugin(koverId)
        plugin("java")
        plugin("maven-publish")
        plugin("java-gradle-plugin")
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

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.set(listOf("-Xjsr305=strict"))
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    tasks.withType<org.gradle.api.tasks.testing.Test> {
        useJUnitPlatform()
        testLogging {
            events(org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED)
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    // TODO: detect?
    // TODO: sonar?
}

koverReport {
    defaults {}
}

sonar {
    properties {
        property("sonar.projectKey", project.name)
        property("sonar.projectName", project.name)
        property("sonar.coverage.jacoco.xmlReportPaths", layout.buildDirectory.file("coverage/reports/kover/report.xml").get())
    }
}


// TODO: Below
// fun extractPlugins(currentProject: Project): List<Pair<String, String>> {
//     val basicGradlePluginPattern = "(.*)\\.gradle\\.kts".toRegex()
//     val pluginIdReplacerPattern = "[^A-Za-z]".toRegex()
//     val buildFileIdPattern = "(?ms).*gradlePlugin.*id *= *\"([^\"]+)\".*".toRegex()
//     val pluginIdSegmentPattern = """com\.projectronin\.product\.(.+)""".toRegex()
//
//     val pluginScripts = currentProject.projectDir.walk()
//         .filter { file -> file.path.contains("src/main/kotlin") && file.name.matches(basicGradlePluginPattern) }
//         .map { file ->
//             file.name.replace(basicGradlePluginPattern, "$1")
//         }
//         .toList()
//     return if (pluginScripts.isNotEmpty()) {
//         pluginScripts
//             .map { idSegment ->
//                 "product-${idSegment.replace(pluginIdReplacerPattern, "")}" to "com.projectronin.product.$idSegment"
//             }
//     } else {
//         val buildFileText = currentProject.buildFile.readText()
//         if (buildFileText.contains(buildFileIdPattern)) {
//             val pluginId = buildFileText.replace(buildFileIdPattern, "$1")
//             val segment = pluginId.replace(pluginIdSegmentPattern, "$1")
//             listOf("product-$segment" to pluginId)
//         } else {
//             logger.warn("Couldn't find plugin ID in ${currentProject.name}")
//             emptyList()
//         }
//     }
// }
//
// catalog {
//     versionCatalog {
//         from(files("./gradle/libs.versions.toml"))
//         // This whole mess tries to supplement the TOML file by adding _this project's_ version to it dynamically,
//         // and by recursing the project structure and declaring libraries for each module.  The primary problem is that
//         // gradle plugins are messy, so it tries to guess the IDS using various kinds of file searches and string grepping.
//         version("product-common", targetVersion)
//
//         fun handleProject(currentProject: Project) {
//             if (currentProject.parent?.name == "gradle-plugins") {
//                 extractPlugins(currentProject)
//                     .forEach { pluginPair ->
//                         plugin(pluginPair.first, pluginPair.second).versionRef("product-common")
//                     }
//             } else {
//                 library(currentProject.name, currentProject.group.toString(), currentProject.name).versionRef("product-common")
//             }
//         }
//
//         subprojects
//             .forEach { handleProject(it) }
//
//         // for backward compatibility
//         library("product-starter-web", rootProject.group.toString(), "product-spring-web-starter").versionRef("product-common")
//         library("product-starter-webflux", rootProject.group.toString(), "product-spring-webflux-starter").versionRef("product-common")
//         library("product-contracttest", rootProject.group.toString(), "product-contract-test-common").versionRef("product-common")
//         library("spring-productcommon", rootProject.group.toString(), "product-spring-common").versionRef("product-common")
//     }
// }
//
// publishing {
//     repositories {
//         maven {
//             name = "nexus"
//             credentials {
//                 username = System.getenv("NEXUS_USER")
//                 password = System.getenv("NEXUS_TOKEN")
//             }
//             url = if (project.version.toString().endsWith("SNAPSHOT")) {
//                 uri("https://repo.devops.projectronin.io/repository/maven-snapshots/")
//             } else {
//                 uri("https://repo.devops.projectronin.io/repository/maven-releases/")
//             }
//         }
//     }
//
//     publications {
//         create<MavenPublication>("maven") {
//             from(components["versionCatalog"])
//         }
//     }
// }
