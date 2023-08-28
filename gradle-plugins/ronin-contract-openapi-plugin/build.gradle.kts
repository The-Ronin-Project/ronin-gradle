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
    implementation(project(":shared-libraries:openapi-processor"))
    implementation(project(":gradle-plugins:ronin-build-conventions-versioning"))
    implementation(project(":gradle-plugins:ronin-build-conventions-kotlin"))

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
    helperDependencies.set(
        mapOf(
            "jakarta" to libs.jakarta.validation.api,
            "springBom" to libs.spring.boot.bom,
            "springWeb" to libs.spring.web,
            "springContext" to libs.spring.context,
            "jacksonAnnotations" to libs.jackson.annotations
        )
    )
}
