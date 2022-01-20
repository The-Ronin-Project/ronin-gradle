package com.projectronin.services.gradle

plugins {
    id("com.projectronin.services.gradle.base")
    id("com.projectronin.services.gradle.docker")
    id("org.springframework.boot")
}

dependencies {
    // auth stuff as implementation deps once it exists
    // ...

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // TODO: this should probably be our own ronin-security-test wrapper once we have our security stuff
    testImplementation("org.springframework.security:spring-security-test")
}
