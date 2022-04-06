rootProject.name = "ronin-gradle"

pluginManagement {
    val interopGradleVersion: String by settings
    plugins {
        id("com.projectronin.interop.gradle.base") version interopGradleVersion
        id("com.projectronin.interop.gradle.junit") version interopGradleVersion
        id("com.projectronin.interop.gradle.jacoco") version interopGradleVersion
        id("com.projectronin.interop.gradle.publish") version interopGradleVersion
    }

    repositories {
        maven {
            url = uri("https://repo.devops.projectronin.io/repository/maven-snapshots/")
            mavenContent {
                snapshotsOnly()
            }
        }
        maven {
            url = uri("https://repo.devops.projectronin.io/repository/maven-releases/")
            mavenContent {
                releasesOnly()
            }
        }
        maven {
            url = uri("https://repo.devops.projectronin.io/repository/maven-public/")
            mavenContent {
                releasesOnly()
            }
        }
        mavenLocal()
        gradlePluginPortal()
    }
}
