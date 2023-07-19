dependencies {
    implementation(project(":shared-libraries:gradle-helpers"))
    implementation(libs.gradle.sonarqube)
    implementation(libs.gradle.dokka)
    implementation(libs.gradle.ktlint)
    implementation(libs.gradle.kotlin.jvm)
    implementation(libs.gradle.releasehub)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testImplementation(gradleTestKit())
    testImplementation(project(":shared-libraries:gradle-testkit-utilities"))
    testImplementation(project(":gradle-plugins:ronin-build-conventions-kotlin"))
}

gradlePlugin {
    plugins {
        create("rootConventionPlugin") {
            id = "com.projectronin.buildconventions.root"
            implementationClass = "com.projectronin.buildconventions.RootConventionsPlugin"
        }
    }
}

dependencyHelper {
    helperPlugins.set(
        mapOf(
            "sonar" to libs.plugins.sonarqube,
            "dokka" to libs.plugins.dokka,
            "ktlint" to libs.plugins.ktlint,
            "kotlin" to libs.plugins.kotlin.jvm,
            "releasehub" to libs.plugins.releasehub
        )
    )
}
