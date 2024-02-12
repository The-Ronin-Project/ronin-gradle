package com.projectronin

plugins {
    java
    `jvm-test-suite`
}

testing {
    suites {
        @Suppress("UNUSED_VARIABLE")
        val localContractTest by registering(JvmTestSuite::class) {
            useJUnitJupiter()

            testType.set(TestSuiteType.INTEGRATION_TEST)

            targets {
                all {
                    // This test suite should run after the built-in test suite has run its tests
                    testTask.configure {
                        shouldRunAfter("test")
                        testLogging {
                            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                            events(org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED)
                        }
                        systemProperty("ronin.contracttest.libdir", project.buildDir.resolve("libs"))
                    }
                }
            }
        }
    }
}

tasks.getByName("localContractTest") {
    dependsOn("bootJar")
}

tasks.getByName("processLocalContractTestResources", org.gradle.language.jvm.tasks.ProcessResources::class) {
    expand(
        "projectDir" to project.projectDir,
        "projectRoot" to project.rootDir,
        "projectBuild" to project.buildDir
    )
}

tasks.named<Task>("check") {
    // Include functionalTest as part of the check lifecycle
    dependsOn(testing.suites.named("localContractTest"))
}
