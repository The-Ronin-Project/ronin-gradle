package com.projectronin.services.gradle

plugins {
    id("com.projectronin.services.gradle.base")
    id("com.google.cloud.tools.jib")
}

dependencies {
    // implementation instead of runtime since tests will likely call liquibase
    implementation("org.liquibase:liquibase-core")
    runtimeOnly("info.picocli:picocli")
    runtimeOnly("org.yaml:snakeyaml")
    runtimeOnly("mysql:mysql-connector-java")
}

val dockerTag = System.getenv("DOCKER_TAG") ?: "latest"

jib {
    to {
        image = "projectronin.azurecr.io/ronin/${project.name}:$dockerTag"
    }
    container {
        // liquibase provides a docker image, but getting mysql support inside of it in a way that gets along with jib
        // is pretty painful, so instead, we basically just make our own liquibase container. by having the project
        // depend on liquibase, we know liquibase will be available inside of jib, so this just customizes the
        // entrypoint to call liquibase with the classpath jib assembles and pointed at the changelog file that is
        // presumably being bundled into this image.
        mainClass = "liquibase.integration.commandline.LiquibaseCommandLine"
        entrypoint = listOf(
            "java",
            "-cp",
            "@/app/jib-classpath-file",
            "liquibase.integration.commandline.LiquibaseCommandLine",
            "--changelog-file=db/changelog/db.changelog-master.yaml"
        )
    }
}

tasks.named("publish") {
    dependsOn("jib")
}
