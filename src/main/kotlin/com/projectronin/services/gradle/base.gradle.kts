package com.projectronin.services.gradle

plugins {
    id("com.projectronin.interop.gradle.base")
}

repositories {
    maven {
        url = uri("https://maven.pkg.github.com/projectronin/package-repo")
        credentials {
            username = System.getenv("PACKAGE_USER")
            password = System.getenv("PACKAGE_TOKEN")
        }
    }
    mavenCentral()
    maven(url = "https://plugins.gradle.org/m2/")
}
