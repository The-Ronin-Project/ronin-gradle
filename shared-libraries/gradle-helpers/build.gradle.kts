dependencies {
    compileOnly(libs.gradle.api)
    compileOnly(libs.jgit)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testImplementation(libs.mockk)
    testImplementation(libs.gradle.api)
    testImplementation(libs.jgit)
}

publishing {
    publications.register("Maven", MavenPublication::class.java) {
        from(components.getByName("java"))
    }
}
