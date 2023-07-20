package com.projectronin.buildconventions

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

interface RoninSonarConfig {
    val projectKey: Property<String>
    val projectName: Property<String>
    val coverageExclusions: ListProperty<String>
    val xmlReportPath: Property<String>
}
