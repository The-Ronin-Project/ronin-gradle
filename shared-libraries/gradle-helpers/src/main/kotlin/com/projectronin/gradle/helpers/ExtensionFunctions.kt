package com.projectronin.gradle.helpers

import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.internal.DefaultPublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import java.net.URI

fun Project.applyPlugin(id: String): Project {
    if (!pluginManager.hasPlugin(id)) {
        pluginManager.apply(id)
    }
    return this
}

fun Project.dependency(configuration: String, coordinates: String): Project {
    dependencies.add(configuration, coordinates)
    return this
}

fun Project.apiDependency(coordinates: String): Project = dependency(JavaPlugin.API_CONFIGURATION_NAME, coordinates)

fun Project.implementationDependency(coordinates: String): Project = dependency(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, coordinates)

fun Project.runtimeOnlyDependency(coordinates: String): Project = dependency(JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME, coordinates)

fun Project.compileOnlyDependency(coordinates: String): Project = dependency(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, coordinates)

fun Project.testImplementationDependency(coordinates: String): Project = dependency(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, coordinates)

fun Project.platformDependency(configuration: String, coordinates: String): Project {
    dependencies.add(configuration, dependencies.platform(coordinates))
    return this
}

fun Project.projectDependency(configuration: String, path: String): Project {
    dependencies.add(configuration, dependencies.project(mapOf("path" to path)))
    return this
}

const val RELEASES_REPO_NAME = "artifactoryReleases"
const val SNAPSHOTS_REPO_NAME = "artifactorySnapshots"
const val SNAPSHOT_SEGMENT = "SNAPSHOT"
const val DEFAULT_PUBLICATION_NAME = "Maven"
const val JAVA_COMPONENT_NAME = "java"

fun Project.nexusReleaseRepo(): String = properties.getOrDefault("nexus-release-repo", "https://repo.devops.projectronin.io/repository/maven-releases/").toString()
fun Project.nexusSnapshotRepo(): String = properties.getOrDefault("nexus-snapshot-repo", "https://repo.devops.projectronin.io/repository/maven-snapshots/").toString()
fun Project.nexusPublicRepo(): String = properties.getOrDefault("nexus-public-repo", "https://repo.devops.projectronin.io/repository/maven-public/").toString()
fun Project.nexusUsername(): String? = properties.getOrDefault("nexus-user", System.getenv("NEXUS_USER"))?.toString()
fun Project.nexusPassword(): String? = properties.getOrDefault("nexus-password", System.getenv("NEXUS_TOKEN"))?.toString()
fun Project.isNexusInsecure(): Boolean = properties.getOrDefault("nexus-insecure", "false").toString().toBoolean()

fun Project.registerMavenRepository(registerDefaultJavaPublication: Boolean = false): PublishingExtension {
    if (extensions.findByName(PublishingExtension.NAME) == null) {
        extensions.create(PublishingExtension::class.java, PublishingExtension.NAME, DefaultPublishingExtension::class.java)
    }
    return (extensions.getByName(PublishingExtension.NAME) as PublishingExtension).apply {
        if (repositories.find { r -> r is MavenArtifactRepository && r.name == RELEASES_REPO_NAME } == null) {
            repositories { rh ->
                rh.maven { mar ->
                    mar.name = RELEASES_REPO_NAME
                    mar.isAllowInsecureProtocol = project.isNexusInsecure()
                    mar.credentials { pc ->
                        pc.username = project.nexusUsername()
                        pc.password = project.nexusPassword()
                    }
                    mar.url = URI(project.nexusReleaseRepo())
                }
            }
        }
        if (repositories.find { r -> r is MavenArtifactRepository && r.name == SNAPSHOTS_REPO_NAME } == null) {
            repositories { rh ->
                rh.maven { mar ->
                    mar.name = SNAPSHOTS_REPO_NAME
                    mar.isAllowInsecureProtocol = project.isNexusInsecure()
                    mar.credentials { pc ->
                        pc.username = project.nexusUsername()
                        pc.password = project.nexusPassword()
                    }
                    mar.url = URI(project.nexusSnapshotRepo())
                }
            }
        }
        tasks.withType(PublishToMavenRepository::class.java).configureEach { task ->
            val predicate = provider {
                (task.repository.name == SNAPSHOTS_REPO_NAME && task.publication.version.toString().contains(SNAPSHOT_SEGMENT)) ||
                    (task.repository.name == RELEASES_REPO_NAME && !task.publication.version.toString().contains(SNAPSHOT_SEGMENT))
            }
            task.onlyIf("publishing snapshot to snapshots repo or release to releases repo") {
                predicate.get()
            }
        }
        if (registerDefaultJavaPublication) {
            publications { publications ->
                publications.register(DEFAULT_PUBLICATION_NAME, MavenPublication::class.java) { mp ->
                    mp.from(components.getByName(JAVA_COMPONENT_NAME))
                }
            }
        }
    }
}
