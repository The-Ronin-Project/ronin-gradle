dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.swagger.core.v3:swagger-core:2.2.8")
    implementation("io.swagger.parser.v3:swagger-parser:2.1.12")
    implementation("org.semver4j:semver4j:4.3.0")
    implementation("com.github.node-gradle:gradle-node-plugin:3.5.0")
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.5.0.202303070854-r")
    implementation("org.openapitools:openapi-generator-gradle-plugin:6.4.0") {
        exclude(group = "org.slf4j", module = "slf4j-simple")
    }

    testImplementation(libs.junit.jupiter)
    testImplementation("org.assertj:assertj-core:3.23.1")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.14.0")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.14.0")
    testImplementation("org.testcontainers:testcontainers:1.17.6")
    testImplementation("com.squareup.okhttp3:okhttp:4.10.0")
    testImplementation(gradleTestKit())
}

gradlePlugin {
    // Define the plugin
    val restContractSupport by plugins.creating {
        id = "com.projectronin.openapi.contract"
        implementationClass = "com.projectronin.rest.contract.RestContractSupportPlugin"
    }
}
