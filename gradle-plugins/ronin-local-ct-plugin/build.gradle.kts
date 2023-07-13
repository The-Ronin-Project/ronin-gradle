@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    `kotlin-dsl`
}

dependencies {
    api(libs.gradle.kotlin.jvm)
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
    }
}
