package com.projectronin.gradle.test

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.lang.management.ManagementFactory
import java.util.UUID

@ExcludeAsIfGenerated
fun GradleRunner.withCoverage(pluginProjectDirectory: File, tempFolder: File, groupId: String = "com.projectronin.contract.json"): GradleRunner {
    val propertiesText = StringBuilder()

    propertiesText.append("group=$groupId\n")

    val runtimeMxBean = ManagementFactory.getRuntimeMXBean()
    val arguments = runtimeMxBean.inputArguments

    val ideaArguments = arguments.filter { it.matches("""-D.*coverage.*""".toRegex()) }
    val javaAgentArgument = arguments
        .firstOrNull { it.matches("""-javaagent.*(intellij-coverage-agent|jacocoagent.jar).*""".toRegex()) }
        ?.replace("build/jacoco/test.exec", "${pluginProjectDirectory.absolutePath}/build/jacoco/test-${UUID.randomUUID()}.exec")

    javaAgentArgument?.let { arg ->
        propertiesText.append("org.gradle.jvmargs=-Xmx512M ${arg}${ideaArguments.joinToString(" ", " ")}")
    }

    tempFolder.resolve("gradle.properties").writeText(propertiesText.toString())
    return this
}
