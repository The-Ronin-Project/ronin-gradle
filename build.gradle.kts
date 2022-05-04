

plugins {
    kotlin("jvm")
    `kotlin-dsl`
    id("org.jlleitschuh.gradle.ktlint")
    id("io.spring.dependency-management")

    id("com.projectronin.interop.gradle.base")
    id("com.projectronin.interop.gradle.junit")
    id("com.projectronin.interop.gradle.jacoco")
    id("com.projectronin.interop.gradle.publish")
}

repositories {
    maven {
        url = uri("https://repo.devops.projectronin.io/repository/maven-public/")
        mavenContent {
            releasesOnly()
        }
    }

    mavenCentral()
    maven(url = "https://plugins.gradle.org/m2/")
}

dependencies {
    implementation("com.projectronin.interop.gradle.base:com.projectronin.interop.gradle.base.gradle.plugin:1.0.0")
    implementation("com.projectronin.interop.gradle.junit:com.projectronin.interop.gradle.junit.gradle.plugin:1.0.0")
    implementation("com.projectronin.interop.gradle.jacoco:com.projectronin.interop.gradle.jacoco.gradle.plugin:1.0.0")
    implementation("com.projectronin.interop.gradle.publish:com.projectronin.interop.gradle.publish.gradle.plugin:1.0.0")

    implementation("org.springframework.boot:spring-boot-gradle-plugin:2.6.1")
    implementation("org.springframework.boot:spring-boot-dependencies:2.6.3")
    implementation("io.spring.gradle:dependency-management-plugin:1.0.11.RELEASE")

    implementation("gradle.plugin.com.google.cloud.tools:jib-gradle-plugin:3.1.4")
    implementation("de.undercouch:gradle-download-task:4.1.2")
}

// ktlint includes the generated-sources, which includes the classes created by Gradle for these plugins
ktlint {
    enableExperimentalRules.set(true)
    filter {
        // We should be able to just do a wildcard exclude, but it's not working.
        // This solution comes from https://github.com/JLLeitschuh/ktlint-gradle/issues/222#issuecomment-480758375
        exclude { projectDir.toURI().relativize(it.file.toURI()).path.contains("/generated-sources/") }
    }
}
