@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

dependencies {
    api(libs.gradle.kotlin.jvm)
    api(libs.gradle.kotlin.noarg)
    api(libs.gradle.kotlin.allopen)
    compileOnly(libs.swaggerparser)
    compileOnly(libs.fabrikt)

    testImplementation(libs.assertj)
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
    }
}

gradlePlugin {
    plugins {
        create("openApiKotlinGenerator") {
            id = "com.projectronin.openapi"
            implementationClass = "com.projectronin.openapi.OpenApiKotlinGenerator"
        }
    }
}

tasks.getByName("processResources", org.gradle.language.jvm.tasks.ProcessResources::class) {
    expand(
        "fabriktSpec" to libs.fabrikt.get().toString(),
        "swaggerparserSpec" to libs.swaggerparser.get().toString()
    )
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use Kotlin Test framework
            useJUnitJupiter()
        }

        // Create a new test suite
        val functionalTest by registering(JvmTestSuite::class) {
            // Use Kotlin Test framework
            useJUnitJupiter()

            dependencies {
                // functionalTest test suite depends on the production code in tests
                implementation(project(":gradle-plugins:ronin-openapi-plugin"))
                implementation(libs.assertj)
            }

            targets {
                all {
                    // This test suite should run after the built-in test suite has run its tests
                    testTask.configure { shouldRunAfter(test) }
                }
            }
        }
    }
}

gradlePlugin.testSourceSets(sourceSets["functionalTest"])

tasks.named<Task>("check") {
    // Include functionalTest as part of the check lifecycle
    dependsOn(testing.suites.named("functionalTest"))
}
