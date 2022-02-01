package com.projectronin.services.gradle

plugins {
    id("com.projectronin.interop.gradle.base")
    id("com.projectronin.interop.gradle.publish")
}

dependencyManagement {
    imports {
        mavenBom("org.jetbrains.kotlin:kotlin-bom:1.5.31")
        mavenBom("org.springframework.boot:spring-boot-dependencies:2.6.3")
        mavenBom("io.ktor:ktor-bom:1.6.7")
    }

    dependencies {
        dependency("io.github.microutils:kotlin-logging-jvm:2.1.21")
    }
}
