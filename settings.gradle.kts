rootProject.name = "ronin-gradle"

pluginManagement {
    plugins {
        kotlin("jvm") version "1.6.10"
        
        id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
        id("io.spring.dependency-management") version "1.0.11.RELEASE"

        id("com.projectronin.interop.gradle.base") version "1.0.0"
        id("com.projectronin.interop.gradle.junit") version "1.0.0"
        id("com.projectronin.interop.gradle.jacoco") version "1.0.0"
        id("com.projectronin.interop.gradle.publish") version "1.0.0"

    }
    repositories {
        maven {
            url = uri("https://repo.devops.projectronin.io/repository/maven-public/")
            mavenContent {
                releasesOnly()
            }
        }
        maven {
            url = uri("https://maven.pkg.github.com/projectronin/package-repo")
            credentials {
                username = System.getenv("PACKAGE_USER")
                password = System.getenv("PACKAGE_TOKEN")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
