import com.projectronin.gradle.internal.DependencyHelperExtension
import com.projectronin.gradle.internal.DependencyHelperGenerator

dependencies {
    compileOnly(libs.gradle.api)
    compileOnly(libs.swaggerparser)
    compileOnly(libs.fabrikt)

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

val dependencyHelper = extensions.create("dependencyHelper", DependencyHelperExtension::class.java).apply {
    helperDependencies.set(
        mapOf(
            "fabrikt" to libs.fabrikt,
            "swaggerParser" to libs.swaggerparser,
            "jakarta" to libs.jakarta.validation.api
        )
    )
    helperPlugins.convention(emptyMap())
}

task("generateDependencyHelper") {
    group = BasePlugin.BUILD_GROUP
    val outputDir: Provider<Directory> = layout.buildDirectory.dir("generated/sources/dependency-helper")

    (properties["sourceSets"] as SourceSetContainer?)?.getByName("main")?.java?.srcDir(outputDir)

    doLast {
        DependencyHelperGenerator.generateHelper(outputDir.get().asFile, dependencyHelper, project)
    }
}

tasks.getByName("runKtlintCheckOverMainSourceSet").dependsOn("generateDependencyHelper")
tasks.getByName("compileKotlin").dependsOn("generateDependencyHelper")
