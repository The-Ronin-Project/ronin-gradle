@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

dependencies {
    api(libs.gradle.kotlin.jvm)
    api(libs.gradle.kotlin.noarg)
    api(libs.gradle.kotlin.allopen)
    compileOnly(libs.swaggerparser)
    compileOnly(libs.fabrikt)

    testImplementation(libs.assertj)
    testImplementation(libs.junit.jupiter)
    testImplementation(gradleTestKit())
    testImplementation(project(":shared-libraries:gradle-testkit-utilities"))
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
    }
}

gradlePlugin {
    plugins {
        create("openApiKotlinGenerator") {
            id = "com.projectronin.openapi"
            implementationClass = "com.projectronin.openapi.OpenApiKotlinGenerator"
        }
    }
}

tasks.getByName("processResources", org.gradle.language.jvm.tasks.ProcessResources::class) {
    expand(
        "fabriktSpec" to libs.fabrikt.get().toString(),
        "swaggerparserSpec" to libs.swaggerparser.get().toString()
    )
}
