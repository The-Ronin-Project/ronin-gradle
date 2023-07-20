dependencies {
    compileOnly(libs.gradle.api)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testImplementation(libs.mockk)
    testImplementation(libs.gradle.api)
}

publishing {
    publications.register("Maven", MavenPublication::class.java) {
        from(components.getByName("java"))
    }
}
