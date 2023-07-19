import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    kotlin("jvm")
    `maven-publish`
    jacoco
    java
    id("org.jlleitschuh.gradle.ktlint")
    id("org.jetbrains.dokka")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
    testImplementation("org.assertj:assertj-core:3.23.1")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.set(listOf("-Xjsr305=strict"))
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events(org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED)
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.withType<JacocoReport> {
    executionData.setFrom(fileTree(buildDir).include("/jacoco/*.exec"))
    dependsOn(*tasks.withType<Test>().toTypedArray())
}

extensions.getByType(KtlintExtension::class).apply {
    filter {
        exclude { entry ->
            entry.file.toString().contains("generated")
        }
    }
}
