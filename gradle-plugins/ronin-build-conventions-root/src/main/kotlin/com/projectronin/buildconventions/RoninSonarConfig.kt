package com.projectronin.buildconventions

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

interface RoninSonarConfig {
    /**
     * Project key to use for sonar.  Defaults to the project name
     */
    val projectKey: Property<String>

    /**
     * Project name to use for sonar.  Defaults to the project name
     */
    val projectName: Property<String>

    /**
     * A list of exclusions. Default exclusions are in com.projectronin.buildconventions.RootConventionsPlugin.COVERAGE_EXCLUSIONS
     */
    val coverageExclusions: ListProperty<String>

    /**
     * The input jacoco report path.  Defaults to reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml
     */
    val xmlReportPath: Property<String>

    /**
     * If your project doesn't supply versions, you can use this to set a reference branch (e.g. `main`) as the source for new
     * code comparisons.  Defaults to empty.
     */
    val referenceBranch: Property<String>
}
