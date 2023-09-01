package com.projectronin.gradle.test

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.lang.management.ManagementFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private object GradleCoverageHelper {
    val jarCache = ConcurrentHashMap<File, File>()
}

@ExcludeAsIfGenerated
fun GradleRunner.withCoverage(pluginProjectDirectory: File, tempFolder: File, groupId: String = "com.projectronin.contract.json"): GradleRunner {
    val propertiesText = StringBuilder()

    propertiesText.append("group=$groupId\n")

    val runtimeMxBean = ManagementFactory.getRuntimeMXBean()
    val arguments = runtimeMxBean.inputArguments

    val ideaArguments = arguments.filter { it.matches("""-D.*coverage.*""".toRegex()) }
    val javaAgentArgument = arguments.firstOrNull { it.matches("""-javaagent.*?(intellij-coverage-agent.*?\.jar|jacocoagent.jar).*""".toRegex()) }

    if (javaAgentArgument != null) {
        val sourceJarLocation = File(javaAgentArgument.replace(".*-javaagent:(.*?(intellij-coverage-agent.*?\\.jar|jacocoagent.jar)).*".toRegex(), "$1"))
        val destinationJarLocation = GradleCoverageHelper.jarCache.getOrPut(sourceJarLocation) {
            val tf = File.createTempFile("coverage-agent", ".jar")
            tf.deleteOnExit()
            sourceJarLocation.copyTo(tf, overwrite = true)
            tf
        }

        val newJavaAgentArgument = javaAgentArgument
            .replace("-javaagent:.*?(intellij-coverage-agent.*?\\.jar|jacocoagent.jar)".toRegex(), "-javaagent:$destinationJarLocation")
            .replace("build/jacoco/test.exec", "${pluginProjectDirectory.absolutePath}/build/jacoco/test-${UUID.randomUUID()}.exec")

        propertiesText.append("org.gradle.jvmargs=-Xmx512M ${newJavaAgentArgument}${ideaArguments.joinToString(" ", " ")}")

        tempFolder.resolve("gradle.properties").writeText(propertiesText.toString())
    }

    return this
}
