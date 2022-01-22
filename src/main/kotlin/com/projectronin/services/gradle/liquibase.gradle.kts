package com.projectronin.services.gradle

plugins {
    id("com.projectronin.services.gradle.base")
    id("com.google.cloud.tools.jib")
}

val dockerTag = System.getenv("DOCKER_TAG") ?: "latest"

dependencies {
    runtimeOnly("org.liquibase:liquibase-core")
    runtimeOnly("info.picocli:picocli")
    runtimeOnly("mysql:mysql-connector-java")
}

jib {
    to {
        image = "projectronin.azurecr.io/ronin/${project.name}:$dockerTag"
    }
    container {
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
