

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
}

gradlePlugin {
    plugins {
        create("kotlinJvmPlugin") {
            id = "com.projectronin.buildconventions.kotlin-jvm"
            implementationClass = "com.projectronin.buildconventions.KotlinJvmConventionsPlugin"
        }
    }
}

dependencyHelper {
    helperPlugins.set(
        mapOf(
            "kotlin" to libs.plugins.kotlin.jvm,
            "ktlint" to libs.plugins.ktlint,
            "dokka" to libs.plugins.dokka
        )
    )
    helperDependencies.set(
        mapOf(
            "junit" to libs.junit.jupiter,
            "assert" to libs.assertj
        )
    )
}
