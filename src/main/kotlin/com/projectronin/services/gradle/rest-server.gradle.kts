package com.projectronin.services.gradle

plugins {
    id("com.projectronin.services.gradle.base")
    id("com.projectronin.services.gradle.boot")
    id("com.projectronin.services.gradle.docker")
}

dependencies {
    // auth stuff as implementation deps once it exists
    // ...

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // implementation("org.springframework.boot:spring-boot-starter-security")
    // testImplementation("org.springframework.security:spring-security-test")
}
