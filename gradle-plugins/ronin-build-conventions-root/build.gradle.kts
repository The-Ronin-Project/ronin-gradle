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
    testImplementation(libs.kotlin.coroutines.core)
    testImplementation(libs.kotlin.retry)
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

tasks.getByName("test").dependsOn(
    ":gradle-plugins:ronin-build-conventions-spring-service:assemble",
    ":gradle-plugins:ronin-build-conventions-spring-service:generateMetadataFileForPluginMavenPublication",
    ":gradle-plugins:ronin-build-conventions-spring-service:generateMetadataFileForSpringServicePluginPluginMarkerMavenPublication",
    ":gradle-plugins:ronin-build-conventions-spring-service:generatePomFileForPluginMavenPublication",
    ":gradle-plugins:ronin-build-conventions-spring-service:generatePomFileForSpringServicePluginPluginMarkerMavenPublication",
    ":gradle-plugins:ronin-build-conventions-kotlin:assemble",
    ":gradle-plugins:ronin-build-conventions-kotlin:generateMetadataFileForKotlinJvmPluginPluginMarkerMavenPublication",
    ":gradle-plugins:ronin-build-conventions-kotlin:generateMetadataFileForPluginMavenPublication",
    ":gradle-plugins:ronin-build-conventions-kotlin:generatePomFileForKotlinJvmPluginPluginMarkerMavenPublication",
    ":gradle-plugins:ronin-build-conventions-kotlin:generatePomFileForPluginMavenPublication",
    ":shared-libraries:gradle-helpers:assemble",
    ":shared-libraries:gradle-helpers:generateMetadataFileForMavenPublication",
    ":shared-libraries:gradle-helpers:generatePomFileForMavenPublication"
)
