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
    dest("${buildDir}/datadog/dd-java-agent.jar")
}


jib {
    to {
        // TODO: the GHA determines the tag, right?? need to figure out what envvar to use for that instead of hardcoded latest
        // Should probably just have the GHA pass the entire image name in... How to do that?
        image = "projectronin.azurecr.io/${project.name}:latest"
    }

    extraDirectories {
        paths {
            path {
                // https://github.com/GoogleContainerTools/jib/issues/2715
                setFrom("${buildDir}/datadog")
                into = "/opt/datadog"
            }
        }
    }

    container {
        jvmFlags = listOf("-javaagent:/opt/datadog/dd-java-agent.jar")
        environment = mapOf(
            "DD_SERVICE" to project.name,
            // TODO: how to get git commit hash as the version?
            "DD_VERSION" to "",
            // TODO: does this totally disable datadog? if not, how to do that?
            // the goal is to have datadog disabled when running containers locally and then override it in the helm chart
            // so that it runs in dev/stage/prod
            "DD_APM_ENABLED" to "false",
            // automatically include dd.correlation_id and dd.span_id in log MDC
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