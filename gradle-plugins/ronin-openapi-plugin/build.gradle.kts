dependencies {
    implementation(libs.gradle.kotlin.jvm)
    implementation(libs.swaggerparser)
    implementation(libs.fabrikt)
    implementation(project(":shared-libraries:gradle-helpers"))

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
            "fabrikt" to libs.fabrikt,
            "swaggerParser" to libs.swaggerparser,
            "jakarta" to libs.jakarta.validation.api
        )
    )
}
