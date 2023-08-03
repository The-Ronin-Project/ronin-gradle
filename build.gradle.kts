import com.projectronin.gradle.internal.DependencyHelperExtension
import com.projectronin.gradle.internal.DependencyHelperGenerator
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
val mainProjects = gradlePluginSubprojects + librarySubprojects

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
                    name = "artifactoryReleases"
                    credentials {
                        username = System.getenv("NEXUS_USER")
                        password = System.getenv("NEXUS_TOKEN")
                    }
                    url = uri("https://repo.devops.projectronin.io/repository/maven-releases/")
                }
                maven {
                    name = "artifactorySnapshots"
                    credentials {
                        username = System.getenv("NEXUS_USER")
                        password = System.getenv("NEXUS_TOKEN")
                    }
                    url = uri("https://repo.devops.projectronin.io/repository/maven-snapshots/")
                }
            }
        }
    }

    tasks.withType<PublishToMavenRepository>().configureEach {
        val predicate = provider {
            (repository.name == "artifactorySnapshots" && version.toString().contains("SNAPSHOT")) ||
                (repository.name == "artifactoryReleases" && !version.toString().contains("SNAPSHOT"))
        }
        onlyIf("publishing snapshot to snapshots repo or release to releases repo") {
            predicate.get()
        }
    }
}

mainProjects.forEach { subProject ->
    subProject.apply {
        plugin(kotlinId)
        plugin(ktlintId)
        plugin("jacoco")
        plugin("java")
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
        doLast {
            val ft: ConfigurableFileTree = fileTree(subProject.buildDir).include("/jacoco/*.exec") as ConfigurableFileTree
            if (!ft.isEmpty) {
                while (ft.minOf { file -> System.currentTimeMillis() - file.lastModified() } < 1000) {
                    logger.debug("${subProject.name}:$name: waiting for .exec files to mature")
                    Thread.sleep(100)
                }
            }
        }
    }

    subProject.tasks.withType<JacocoReport> {
        executionData.setFrom(fileTree(subProject.buildDir).include("/jacoco/*.exec"))
        addDependentTaskByType(Test::class.java, subProject)
    }

    subProject.extensions.getByType(KtlintExtension::class).apply {
        filter {
            exclude { entry ->
                entry.file.toString().contains("generated")
            }
        }
    }
}

gradlePluginSubprojects.forEach { subProject ->
    subProject.apply {
        plugin("java")
        plugin("java-gradle-plugin")
    }

    val dependencyHelper = subProject.extensions.create("dependencyHelper", DependencyHelperExtension::class.java)
    dependencyHelper.helperDependencies.convention(emptyMap())
    dependencyHelper.helperPlugins.convention(emptyMap())

    subProject.task("generateDependencyHelper") {
        group = BasePlugin.BUILD_GROUP
        val outputDir: Provider<Directory> = subProject.layout.buildDirectory.dir("generated/sources/dependency-helper")
        val restResourcesOutputDir: Provider<Directory> = subProject.layout.buildDirectory.dir("generated/test-resources/functional-test-setup")

        (subProject.properties["sourceSets"] as SourceSetContainer?)?.getByName("main")?.java?.srcDir(outputDir)
        (subProject.properties["sourceSets"] as SourceSetContainer?)?.getByName("test")?.resources?.srcDir(restResourcesOutputDir)

        doLast {
            DependencyHelperGenerator.generateHelper(outputDir.get().asFile, dependencyHelper, subProject)
            with(restResourcesOutputDir.get().asFile) {
                mkdirs()
                resolve("functional-test.properties").writeText(
                    """
                    directory.project=${subProject.projectDir}
                    directory.build=${subProject.buildDir}
                    directory.resources=${subProject.buildDir.resolve("resources/test")}
                """.trimIndent()
                )
            }
        }
    }

    subProject.tasks.getByName("runKtlintCheckOverMainSourceSet").dependsOn("generateDependencyHelper")
    subProject.tasks.getByName("compileKotlin").dependsOn("generateDependencyHelper")
}

sonar {
    properties {
        property("sonar.projectKey", project.name)
        property("sonar.projectName", project.name)
        property("sonar.coverage.exclusions", "**/test/**,**/generated-sources/**,**/generated/sources/**,**/*.kts,**/kotlin/dsl/accessors/**")
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
                subprojects.forEach { subProject ->
                    addDependentTaskByType(Test::class.java, subProject)
                }
                classDirectories.setFrom(
                    subprojects.map { subproject ->
                        fileTree(subproject.buildDir.resolve("classes")).exclude("**/kotlin/dsl/accessors/**")
                    }
                )
            }
        }
    }
}

tasks.getByName("testCodeCoverageReport") {
    subprojects.forEach { subProject ->
        addDependentTaskByType(Test::class.java, subProject)
    }
}
tasks.getByName("sonar").dependsOn("testCodeCoverageReport")

fun Task.addDependentTaskByName(nameToAdd: String, targetProject: Project) {
    when (val targetProjectTask = targetProject.tasks.findByName(nameToAdd)) {
        null -> targetProject.tasks.whenTaskAdded {
            if (this.name == nameToAdd && this@addDependentTaskByName != this) {
                targetProject.logger.debug("Lazy adding ${this.name} to ${targetProject.path}:${this@addDependentTaskByName.name}")
                this@addDependentTaskByName.dependsOn(this)
            }
        }

        else -> {
            targetProject.logger.debug("Initially adding ${targetProjectTask.name} to ${targetProject.path}:$name")
            dependsOn(targetProjectTask)
        }
    }
}

fun Task.addDependentTaskByType(taskType: Class<out Task>, targetProject: Project) {
    val initialTasks = targetProject.tasks.withType(taskType)
    if (initialTasks.isNotEmpty()) {
        targetProject.logger.debug("Initially adding ${initialTasks.joinToString { it.name }} to ${targetProject.path}:$name")
        dependsOn(*initialTasks.toTypedArray())
    }
    targetProject.tasks.whenTaskAdded {
        if (taskType.isAssignableFrom(this.javaClass) && this@addDependentTaskByType != this) {
            targetProject.logger.debug("Lazy adding ${this.name} to ${targetProject.path}:${this@addDependentTaskByType.name}")
            this@addDependentTaskByType.dependsOn(this)
        }
    }
}
