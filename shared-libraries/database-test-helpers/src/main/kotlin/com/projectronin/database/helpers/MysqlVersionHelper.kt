package com.projectronin.database.helpers

import org.junit.jupiter.params.provider.Arguments

object MysqlVersionHelper {
    const val MYSQL_VERSION_OCI: String = "mysql:8.0.33"
    const val MYSQL_VERSION_LATEST_WORKING: String = "mysql:8.2.0"
    const val MYSQL_SPRING_DATASOURCE_OCI: String = "spring.datasource.url=jdbc:tc:mysql:8.0.33:///unit_test?TC_DAEMON=true"
    const val MYSQL_SPRING_DATASOURCE_LATEST_WORKING: String = "spring.datasource.url=jdbc:tc:mysql:8.2.0:///unit_test?TC_DAEMON=true"

    @JvmStatic
    fun versionsForTest(): List<Arguments> = listOf(Arguments.of(MYSQL_VERSION_OCI))
}
