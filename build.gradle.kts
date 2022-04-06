plugins {
    `kotlin-dsl`

    id("com.projectronin.interop.gradle.base")
    id("com.projectronin.interop.gradle.junit")
    id("com.projectronin.interop.gradle.jacoco")
    id("com.projectronin.interop.gradle.publish")
}

dependencies {
    implementation("com.projectronin.interop.gradle.base:com.projectronin.interop.gradle.base.gradle.plugin:1.0.0-SNAPSHOT")
    implementation("com.projectronin.interop.gradle.junit:com.projectronin.interop.gradle.junit.gradle.plugin:1.0.0-SNAPSHOT")
    implementation("com.projectronin.interop.gradle.jacoco:com.projectronin.interop.gradle.jacoco.gradle.plugin:1.0.0-SNAPSHOT")
    implementation("com.projectronin.interop.gradle.publish:com.projectronin.interop.gradle.publish.gradle.plugin:1.0.0-SNAPSHOT")

    implementation("org.springframework.boot:spring-boot-gradle-plugin:2.6.1")
    implementation("io.spring.gradle:dependency-management-plugin:1.0.11.RELEASE")

    implementation("de.undercouch:gradle-download-task:4.1.2")
}
