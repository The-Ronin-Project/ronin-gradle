package com.projectronin.services.gradle

import de.undercouch.gradle.tasks.download.Download

plugins {
    id("com.projectronin.services.gradle.base")
    id("com.google.cloud.tools.jib")
    id("de.undercouch.download")
}

task<Download>("download-datadog-agent") {
    // TODO: is there a better way to handle the version of this instead of needing this gradle plugin to update for
    //  the agent to update?
    // Really, we should probably just have a base projectronin/java:11 image that has this baked in instead of having
    // jib put it into the container.
    // See https://projectronin.atlassian.net/browse/QUAL-578
    src("https://repo1.maven.org/maven2/com/datadoghq/dd-java-agent/0.90.0/dd-java-agent-0.90.0.jar")
    dest("$buildDir/datadog/dd-java-agent.jar")
}

val dockerTag = System.getenv("DOCKER_TAG") ?: "latest"

jib {
    to {
        // TODO: the GHA determines the tag, right?? need to figure out what envvar to use for that instead of hardcoded latest
        // Should probably just have the GHA pass the entire image name in... How to do that?
        image = "projectronin.azurecr.io/ronin/${project.name}:$dockerTag"
    }

    extraDirectories {
        paths {
            path {
                // https://github.com/GoogleContainerTools/jib/issues/2715
                setFrom("$buildDir/datadog")
                into = "/opt/datadog"
            }
        }
    }

    container {
        jvmFlags = listOf("-javaagent:/opt/datadog/dd-java-agent.jar")
        environment = mapOf(
            "DD_SERVICE" to project.name,
            "DD_VERSION" to dockerTag,
            "DD_LOGS_INJECTION" to "true"
        )
    }
}

// TODO: gross. is there a better way to do this? Or is this just how gradle works?
listOf("jib", "jibBuildTar", "jibDockerBuild").forEach {
    tasks.named(it) {
        dependsOn("download-datadog-agent")
    }
}

// tie jib into the publish task so "./gradlew publish" on github includes the docker image
tasks.named("publish") {
    dependsOn("jib")
}
