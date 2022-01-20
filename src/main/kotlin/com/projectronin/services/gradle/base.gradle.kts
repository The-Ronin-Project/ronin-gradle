package com.projectronin.services.gradle

plugins {
    id("com.projectronin.interop.gradle.base")
    id("com.projectronin.interop.gradle.junit")
    id("com.projectronin.interop.gradle.jacoco")
    id("com.projectronin.interop.gradle.publish")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:2.6.1")
        mavenBom("org.testcontainers:testcontainers-bom:1.16.0")
        mavenBom("org.jetbrains.kotlin:kotlin-bom:1.5.31")
    }

    dependencies {
        dependency("org.ktorm:ktorm-core:3.4.1")
        dependency("org.ktorm:ktorm-support-mysql:3.4.1")

        dependency("io.github.microutils:kotlin-logging-jvm:2.0.11")
    }
}
