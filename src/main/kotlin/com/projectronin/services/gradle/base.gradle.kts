package com.projectronin.services.gradle

plugins {
    id("com.projectronin.interop.gradle.base")
    id("com.projectronin.interop.gradle.publish")
}

val kotlinVersion = "1.6.10"

dependencyManagement {
    imports {
        mavenBom("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion")
        mavenBom("org.springframework.boot:spring-boot-dependencies:2.6.3")
        mavenBom("io.ktor:ktor-bom:1.6.7")
    }

    dependencies {
        dependency("io.github.microutils:kotlin-logging-jvm:2.1.21")
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
