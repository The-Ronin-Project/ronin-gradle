package com.projectronin.buildconventions

import com.projectronin.gradle.helpers.BaseGradlePluginIdentifiers
import com.projectronin.gradle.helpers.applyPlugin
import com.projectronin.gradle.helpers.dependsOnTasksByType
import com.projectronin.gradle.helpers.testImplementationDependency
import com.projectronin.roninbuildconventionskotlin.DependencyHelper
import org.eclipse.jgit.api.Git
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.dokka.gradle.GradleDokkaSourceSetBuilder
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import java.net.URL

class KotlinJvmConventionsPlugin : Plugin<Project> {

    companion object {
        fun applyBasicKotlin(target: Project) {
            with(target) {
                testImplementationDependency(DependencyHelper.junit)
                testImplementationDependency(DependencyHelper.assert)

                extensions.getByType(JavaPluginExtension::class.java).apply {
                    withSourcesJar()
                    withJavadocJar()
                }

                tasks.apply {
                    withType(KotlinCompile::class.java) { task ->
                        task.compilerOptions {
                            freeCompilerArgs.addAll(listOf("-Xjsr305=strict"))
                            jvmTarget.set(JvmTarget.JVM_17)
                        }
                    }

                    withType(Test::class.java) { task ->
                        task.useJUnitPlatform()
                        task.testLogging { container ->
                            container.events(TestLogEvent.FAILED)
                            container.exceptionFormat = TestExceptionFormat.FULL
                        }
                        task.doLast(object : Action<Task> {
                            override fun execute(t: Task) {
                                val ft: ConfigurableFileTree = fileTree(target.buildDir).include("/jacoco/*.exec") as ConfigurableFileTree
                                if (!ft.isEmpty) {
                                    while (ft.minOf { file -> System.currentTimeMillis() - file.lastModified() } < 1000) {
                                        logger.debug("${target.name}:$name: waiting for .exec files to mature")
                                        Thread.sleep(100)
                                    }
                                }
                            }
                        })
                    }

                    withType(JacocoReport::class.java) { task ->
                        task.executionData.setFrom(fileTree(buildDir).include("/jacoco/*.exec"))
                        task.dependsOnTasksByType(Test::class.java)
                        task.classDirectories.setFrom(
                            target.fileTree(target.buildDir.resolve("classes")).exclude("**/kotlin/dsl/accessors/**", "**/kotlin/test/**")
                        )
                    }
                }

                extensions.getByName("ktlint").apply {
                    if (this is KtlintExtension) {
                        filter {
                            it.exclude { entry ->
                                entry.file.toString().contains("generated")
                            }
                        }
                    }
                }
            }
        }
    }

    override fun apply(target: Project) {
        target
            .applyPlugin(DependencyHelper.Plugins.kotlin.id)
            .applyPlugin(DependencyHelper.Plugins.ktlint.id)
            .applyPlugin(DependencyHelper.Plugins.dokka.id)
            .applyPlugin(BaseGradlePluginIdentifiers.jacoco)
            .applyPlugin(BaseGradlePluginIdentifiers.java)

        applyBasicKotlin(target)
        applyDokka(target)
    }

    private fun applyDokka(target: Project) {
        with(target) {
            tasks.apply {
                if (parent == null) {
                    withType(DokkaTask::class.java) { task ->
                        task.dokkaSourceSets.addSourceLink(target, true)
                    }
                } else {
                    withType(DokkaTaskPartial::class.java) { task ->
                        task.dokkaSourceSets.addSourceLink(target, false)
                    }
                }
            }
        }
    }

    private fun findRepoLocation(target: Project, isRoot: Boolean): String? {
        return runCatching {
            val git = Git.open(target.rootProject.projectDir)
            git.remoteList().call().firstOrNull()?.urIs?.firstOrNull()?.let { remoteResult ->
                val base = "https://${remoteResult.host}/${remoteResult.path.replace(".git", "")}/blob/${git.repository.exactRef("HEAD").objectId.name}"
                if (isRoot) {
                    "$base/src/main/kotlin"
                } else {
                    "$base${target.path.replace(":", "/")}/src/main/kotlin"
                }
            }
        }.onFailure { target.logger.error("An error occurred getting the remotes", it) }
            .getOrNull()
    }

    private fun NamedDomainObjectContainer<GradleDokkaSourceSetBuilder>.addSourceLink(target: Project, isRoot: Boolean) {
        configureEach { builder ->
            val remoteRepoLocation = findRepoLocation(target, isRoot)
            if (remoteRepoLocation != null) {
                builder.sourceLink { linkBuilder ->
                    linkBuilder.localDirectory.set(target.file("src/${builder.name}/kotlin"))
                    linkBuilder.remoteLineSuffix.set("#L")
                    linkBuilder.remoteUrl.set(URL(remoteRepoLocation))
                }
            }
        }
    }
}
