dependencies {
    implementation(project(":gradle-plugins:ronin-build-conventions-kotlin"))
    implementation(project(":shared-libraries:gradle-helpers"))
    implementation(libs.gradle.kotlin.dsl)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testImplementation(gradleTestKit())
    testImplementation(project(":shared-libraries:gradle-testkit-utilities"))
}

gradlePlugin {
    plugins {
        create("gradlePlugin") {
            id = "com.projectronin.buildconventions.gradleplugin"
            implementationClass = "com.projectronin.buildconventions.GradlePluginConventionsPlugin"
        }
        create("gradleDslPlugin") {
            id = "com.projectronin.buildconventions.gradledslplugin"
            implementationClass = "com.projectronin.buildconventions.GradleDslPluginConventionsPlugin"
        }
    }
}

dependencyHelper {
    helperPlugins.set(
        mapOf(
            "ktlint" to libs.plugins.ktlint,
            "kotlinDsl" to libs.plugins.kotlin.dsl
        )
    )
    helperDependencies.set(
        mapOf(
            "junit" to libs.junit.jupiter,
            "assert" to libs.assertj
        )
    )
}
