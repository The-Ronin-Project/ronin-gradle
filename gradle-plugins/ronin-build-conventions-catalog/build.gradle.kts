dependencies {
    implementation(project(":shared-libraries:gradle-helpers"))

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testImplementation(gradleTestKit())
    testImplementation(project(":shared-libraries:gradle-testkit-utilities"))
}

gradlePlugin {
    plugins {
        create("catalogPlugin") {
            id = "com.projectronin.buildconventions.catalog"
            implementationClass = "com.projectronin.buildconventions.CatalogConventionsPlugin"
        }
    }
}
