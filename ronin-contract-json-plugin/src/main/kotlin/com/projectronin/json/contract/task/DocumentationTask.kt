package com.projectronin.json.contract.task

import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.nio.file.attribute.PosixFilePermission
import javax.inject.Inject
import kotlin.io.path.setPosixFilePermissions

/**
 * Task responsible for generating documentation for Event contracts.
 */
open class DocumentationTask @Inject constructor(private val execOperations: ExecOperations) : BaseJsonContractTask() {
    @TaskAction
    fun generateDocumentation() {
        DocumentationGenerator(execOperations, project).generate()
    }
}

/**
 * Logic for generating documentation. This is separated out to allow for mock testing.
 */
class DocumentationGenerator(private val execOperations: ExecOperations, private val project: Project) {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(DocumentationGenerator::class.java)
    }

    fun generate() {
        val tempFile = File.createTempFile("event-", ".sh")
        tempFile.deleteOnExit()
        tempFile.toPath().setPosixFilePermissions(
            setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.OTHERS_READ,
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_EXECUTE,
                PosixFilePermission.OWNER_WRITE
            )
        )
        tempFile.writeText(javaClass.classLoader.getResourceAsStream("documentation-task/build-docs.sh")!!.readAllBytes().toString(StandardCharsets.UTF_8))

        val stream = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        try {
            stream.use { out ->
                err.use { er ->
                    execOperations.exec {
                        it.workingDir(project.projectDir)
                        it.commandLine(
                            listOf(
                                "docker", "run", "--rm",
                                "-v", "${project.projectDir}:/app",
                                "-v", "${tempFile.absolutePath}:/usr/bin/generate-docs.sh",
                                "python:3",
                                "bash", "-c", "/usr/bin/generate-docs.sh"
                            )
                        )
                        it.standardOutput = out
                        it.errorOutput = er
                    }
                }
            }
        } finally {
            StringReader(stream.toString(StandardCharsets.UTF_8)).useLines { lines ->
                lines.forEach { logger.info(it) }
            }
            StringReader(err.toString(StandardCharsets.UTF_8)).useLines { lines ->
                lines.forEach { logger.warn(it) }
            }
        }
    }
}
