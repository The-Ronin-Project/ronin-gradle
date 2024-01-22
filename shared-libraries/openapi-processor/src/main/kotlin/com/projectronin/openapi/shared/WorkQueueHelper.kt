package com.projectronin.openapi.shared

import com.projectronin.openapiprocessor.DependencyHelper
import org.gradle.api.Project
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor
import java.io.File

/**
 * Generates a WorkQueue instance with specific dependencies for fabrikt and swaggerparser.  Necessary because their deps conflict with others in Gradle's classpath.
 */
fun WorkerExecutor.createWorkQueueWithDependencies(project: Project): WorkQueue {
    val fabriktConfigName = "fabrikt"
    if (project.configurations.findByName(fabriktConfigName) == null) {
        project.configurations.maybeCreate(fabriktConfigName)
        project.dependencies.add(fabriktConfigName, DependencyHelper.fabrikt)
        project.dependencies.add(fabriktConfigName, DependencyHelper.swaggerParser)
    }
    val fabriktDependencies = project.configurations.getByName(fabriktConfigName).resolve()

    return classLoaderIsolation { workerSpec -> workerSpec.classpath.from(*fabriktDependencies.toTypedArray<File?>()) }
}
