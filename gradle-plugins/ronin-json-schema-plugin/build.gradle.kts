plugins {
    `kotlin-dsl`
}

dependencies {
    api(libs.gradle.kotlin.jvm)
    api(libs.jsonschematopojo)
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
    }
}
