dependencies {
    implementation(libs.junit.jupiter)
}

publishing {
    publications.register("Maven", MavenPublication::class.java) {
        from(components.getByName("java"))
    }
}
