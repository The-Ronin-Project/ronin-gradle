package com.projectronin.services.gradle

plugins {
    id("com.projectronin.services.gradle.base")
    id("org.springframework.boot")
    kotlin("plugin.spring")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
}
