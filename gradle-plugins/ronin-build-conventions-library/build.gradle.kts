dependencies {
    api(project(":gradle-plugins:ronin-build-conventions-kotlin"))
    api(project(":gradle-plugins:ronin-build-conventions-publishing"))
    implementation(project(":shared-libraries:gradle-helpers"))

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testImplementation(gradleTestKit())
    testImplementation(project(":shared-libraries:gradle-testkit-utilities"))
}

gradlePlugin {
    plugins {
        create("kotlinLibraryPlugin") {
            id = "com.projectronin.buildconventions.kotlin-library"
            implementationClass = "com.projectronin.buildconventions.KotlinLibraryConventionsPlugin"
        }
    }
}
