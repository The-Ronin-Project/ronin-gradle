import org.apache.tools.ant.filters.ReplaceTokens

dependencies {
    implementation(libs.json.schema.validator)
    implementation(libs.jsonschematopojo)
    implementation(libs.axion.release)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.commons.io)
    testImplementation(libs.assertj)
    testImplementation(gradleTestKit())
    testImplementation(libs.testcontainers)
    testImplementation(libs.okhttp)
    testImplementation(project(":shared-libraries:gradle-testkit-utilities"))
}

gradlePlugin {
    plugins {
        create("jsonContractPlugin") {
            id = "com.projectronin.json.contract"
            implementationClass = "com.projectronin.json.contract.JsonContractPlugin"
        }
    }
}

tasks.register<Copy>("copyInitializationFiles") {
    from(layout.projectDirectory.dir("src/main/initializer"))
    into(layout.buildDirectory.dir("initializer"))
    filter(
        ReplaceTokens::class,
        "tokens" to mapOf(
            "projectVersion" to project.version
        )
    )
}

tasks.getByName("build").dependsOn("copyInitializationFiles")
