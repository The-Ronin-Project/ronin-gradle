dependencies {
    api(project(":gradle-plugins:ronin-build-conventions-kotlin"))
    implementation(project(":shared-libraries:gradle-helpers"))

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj)
    testImplementation(gradleTestKit())
    testImplementation(project(":shared-libraries:gradle-testkit-utilities"))
}

gradlePlugin {
    plugins {
        create("springDatabasePlugin") {
            id = "com.projectronin.buildconventions.spring-database"
            implementationClass = "com.projectronin.buildconventions.SpringDatabaseConventionsPlugin"
        }
    }
}

dependencyHelper {
    helperDependencies.set(
        mapOf(
            "springBootBom" to libs.spring.boot.bom,
            "liquibaseCore" to libs.liquibase.core,
            "mysqlConnector" to libs.mysql.connector,
            "springBootTest" to libs.spring.boot.test,
            "testcontainersMySql" to libs.testcontainers.mysql
        )
    )
}
