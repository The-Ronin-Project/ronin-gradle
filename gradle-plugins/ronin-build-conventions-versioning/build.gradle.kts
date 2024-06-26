dependencies {
    implementation(project(":shared-libraries:gradle-helpers"))
    implementation(libs.jgit)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testImplementation(gradleTestKit())
    testImplementation(project(":shared-libraries:gradle-testkit-utilities"))
}

gradlePlugin {
    plugins {
        create("roninVersioningPlugin") {
            id = "com.projectronin.buildconventions.versioning"
            implementationClass = "com.projectronin.versioning.VersioningPlugin"
        }
    }
}
