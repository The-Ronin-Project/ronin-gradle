package com.projectronin.services.gradle

plugins {
    id("com.projectronin.interop.gradle.base")
    id("com.projectronin.interop.gradle.publish")

    id("io.spring.gradle:dependency-management-plugin")
}

dependencies {
    testImplementation("io.kotest:kotest-runner-junit5:5.1.0")
    testImplementation("io.kotest:kotest-assertions-core:5.1.0")
}

val kotlinVersion = "1.6.10"

dependencyManagement {
    imports {
        mavenBom("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion")
        mavenBom("org.springframework.boot:spring-boot-dependencies:2.6.3")
        mavenBom("io.ktor:ktor-bom:1.6.7")
        mavenBom("org.testcontainers:testcontainers-bom:1.16.3")
    }

    dependencies {
        dependency("io.github.microutils:kotlin-logging-jvm:2.1.21")

        // spring boot pulls in 1.5 of this, but kotest needs 1.6.0
        dependency("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.0")

        dependency("io.kotest:kotest-runner-junit5:5.1.0")
        dependency("io.kotest:kotest-assertions-core:5.1.0")
        dependency("io.kotest.extensions:kotest-extensions-spring:1.1.0")

        // used by our liquibase dockerization since liquibase-cli needs this but liquibase-core doesn't bundle it
        dependency("info.picocli:picocli:4.6.1")
        // also used by liquibase
        dependency("org.yaml:snakeyaml:1.30")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register<Test>("integrationTest") {
    group = "Verification"
    description = "Run integration tests matching format *IntegrationTest"
    filter {
        isFailOnNoMatchingTests = false
        includeTestsMatching("*IntegrationTest")
    }
}

tasks.register<Test>("unitTest") {
    group = "Verification"
    description = "Run unit tests matching format *UnitTest"
    filter {
        isFailOnNoMatchingTests = false
        includeTestsMatching("*UnitTest")
    }
}
