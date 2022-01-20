plugins {
    `kotlin-dsl`

    id("com.projectronin.interop.gradle.base") version "1.0.0-SNAPSHOT"
    id("com.projectronin.interop.gradle.junit") version "1.0.0-SNAPSHOT"
    id("com.projectronin.interop.gradle.jacoco") version "1.0.0-SNAPSHOT"
    id("com.projectronin.interop.gradle.publish") version "1.0.0-SNAPSHOT"
}

repositories {
    maven {
        name = "ronin"
        url = uri("https://maven.pkg.github.com/projectronin/package-repo")
        credentials {
            username = System.getenv("PACKAGE_USER")
            password = System.getenv("PACKAGE_TOKEN")
        }
    }
    mavenCentral()
    maven(url = "https://plugins.gradle.org/m2/")
}

dependencies {
    implementation("com.projectronin.interop.gradle.base:com.projectronin.interop.gradle.base.gradle.plugin:1.0.0-SNAPSHOT")
    implementation("com.projectronin.interop.gradle.junit:com.projectronin.interop.gradle.junit.gradle.plugin:1.0.0-SNAPSHOT")
    implementation("com.projectronin.interop.gradle.jacoco:com.projectronin.interop.gradle.jacoco.gradle.plugin:1.0.0-SNAPSHOT")
    implementation("com.projectronin.interop.gradle.publish:com.projectronin.interop.gradle.publish.gradle.plugin:1.0.0-SNAPSHOT")

    implementation("org.springframework.boot:spring-boot-gradle-plugin:2.6.1")

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
