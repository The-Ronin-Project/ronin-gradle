dependencies {
    // Align versions of all Kotlin components
    implementation(platform(libs.kotlin.bom))

    // Use the Kotlin JDK 8 standard library.
    implementation(libs.kotlin.stdlib)
    implementation(libs.swagger.core)
    implementation(libs.swagger.parser)
    implementation(libs.commons.codec)
    implementation(libs.semver)
    implementation(libs.gradle.node)
    implementation(libs.jgit)
    implementation(libs.gradle.openapi.generator) {
        exclude(group = "org.slf4j", module = "slf4j-simple")
    }
    implementation(project(":shared-libraries:gradle-helpers"))

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testImplementation(libs.testcontainers)
    testImplementation(libs.okhttp)
    testImplementation(gradleTestKit())
    testImplementation(project(":shared-libraries:gradle-testkit-utilities"))
}

gradlePlugin {
    // Define the plugin
    val restContractSupport by plugins.creating {
        id = "com.projectronin.openapi.contract"
        implementationClass = "com.projectronin.rest.contract.RestContractSupportPlugin"
    }
}

dependencyHelper {
    helperPlugins.set(
        mapOf(
            "node" to libs.plugins.node
        )
    )
}
