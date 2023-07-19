

dependencies {
    api(libs.gradle.kotlin.jvm)
    api(libs.gradle.ktlint)
    api(libs.gradle.dokka)
    implementation(libs.jgit)
    implementation(project(":shared-libraries:gradle-helpers"))

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testImplementation(gradleTestKit())
    testImplementation(project(":shared-libraries:gradle-testkit-utilities"))
    testImplementation(libs.testcontainers)
    testImplementation(libs.okhttp)
}

gradlePlugin {
    plugins {
        create("publishingPlugin") {
            id = "com.projectronin.buildconventions.publishing"
            implementationClass = "com.projectronin.buildconventions.PublishingConventionsPlugin"
        }
    }
}
