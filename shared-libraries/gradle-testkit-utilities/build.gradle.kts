dependencies {
    api(libs.junit.jupiter)
    api(libs.testcontainers)
    api(libs.okhttp)
    api(gradleTestKit())
    api(libs.kotlin.logging)
    api(libs.jgit)
    api(libs.testcontainers)
    api(libs.okhttp)
    api(libs.jackson.databind)
    api(libs.assertj)
}

publishing {
    publications.register("Maven", MavenPublication::class.java) {
        from(components.getByName("java"))
    }
}
