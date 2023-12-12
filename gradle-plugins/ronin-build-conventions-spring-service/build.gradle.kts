dependencies {
    api(project(":gradle-plugins:ronin-build-conventions-kotlin"))
    api(libs.gradle.spring.boot)
    api(libs.gradle.spring.depmanager)
    api(libs.gradle.spring.kotlin.core)
    api(libs.gradle.spring.kotlin.jpa)
    implementation(project(":shared-libraries:gradle-helpers"))
    implementation(libs.jgit)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testImplementation(gradleTestKit())
    testImplementation(project(":shared-libraries:gradle-testkit-utilities"))
    testImplementation(libs.mockk)
}

gradlePlugin {
    plugins {
        create("springServicePlugin") {
            id = "com.projectronin.buildconventions.spring-service"
            implementationClass = "com.projectronin.buildconventions.SpringServiceConventionsPlugin"
        }
    }
}

dependencyHelper {
    helperDependencies.set(
        mapOf("springAnnotationProcessor" to libs.spring.boot.annotation.processor)
    )
    helperPlugins.set(
        mapOf(
            "springBoot" to libs.plugins.spring.boot,
            "springDependencyManager" to libs.plugins.spring.depmanager,
            "springKotlinCore" to libs.plugins.spring.kotlin.core,
            "springKotlinJpa" to libs.plugins.spring.kotlin.jpa,
            "kapt" to libs.plugins.kotlin.kapt
        )
    )
}
