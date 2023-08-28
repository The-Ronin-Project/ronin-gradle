dependencies {
    implementation(libs.gradle.kotlin.jvm)
    implementation(project(":shared-libraries:gradle-helpers"))
    implementation(project(":shared-libraries:openapi-processor"))

    testImplementation(libs.assertj)
    testImplementation(libs.junit.jupiter)
    testImplementation(gradleTestKit())
    testImplementation(project(":shared-libraries:gradle-testkit-utilities"))
}

gradlePlugin {
    plugins {
        create("openApiKotlinGenerator") {
            id = "com.projectronin.openapi"
            implementationClass = "com.projectronin.openapi.OpenApiKotlinGenerator"
        }
    }
}

dependencyHelper {
    helperDependencies.set(
        mapOf(
            "jakarta" to libs.jakarta.validation.api
        )
    )
}
