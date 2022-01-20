package com.projectronin.services.gradle

plugins {
    id("com.projectronin.services.gradle.base")
}

dependencies {
    // spring-boot-starter-jdbc for setting up a connection pool and leverage Spring Boot config capabilities
    implementation("org.springframework.boot:spring-boot-starter-jdbc")

    // ktorm for actual data access
    // TODO: do we want to standardize on a data access library? this might not belong in this plugin
    implementation("org.ktorm:ktorm-core")
    implementation("org.ktorm:ktorm-support-mysql")

    // liquibase for db migrations
    implementation("org.liquibase:liquibase-core")

    // and of course we need the jdbc driver to actually connect to mysql
    runtimeOnly("mysql:mysql-connector-java")
}
