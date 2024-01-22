package com.projectronin.gradle.helpers

import org.eclipse.jgit.api.Git
import org.gradle.api.Project

data class RoninProjectVersion(
    val tagBasedVersion: String,
    val serviceVersion: String?,
    val lastTag: String?,
    val commitDistance: Int?,
    val gitHash: String?,
    val gitHashFull: String?,
    val branchName: String,
    val dirty: Boolean
)

val Project.roninProjectVersion
    get(): RoninProjectVersion = buildRoninProjectVersion()

private fun Project.maybeServiceVersion(): String? = properties.getOrDefault("service-version", System.getenv("SERVICE_VERSION"))?.toString()?.takeIf { it.isNotBlank() }
private fun Project.buildRoninProjectVersion(): RoninProjectVersion {
    val git = runCatching {
        Git.open(rootProject.projectDir)
    }
        .onFailure {
            logger.warn("No git repository at project root.")
        }
        .getOrNull()
    val ref = git?.repository?.exactRef("HEAD")
    val reader = git?.repository?.newObjectReader()
    // disable-gha-refs is for testing, so that tests don't fail in a GHA
    val branchName = System.getenv("REF_NAME")?.takeIf { it.isNotBlank() && !hasProperty("disable-gha-refs") } ?: git?.repository?.branch ?: "uninitialized-git-repository"
    val supportedHeads = setOf("master", "main")
    // this code ensures that we get a labeled version for anything that's not master, main, or version/v<NUMBER> or v<NUMBER>.<NUMBER>.<NUMBER>,
    // but that we get a PLAIN version for  master, main, or version/v<NUMBER> or v<NUMBER>.<NUMBER>.<NUMBER>
    // The jiraBranchRegex tries to identify a ticket project-<NUMBER> format and uses that as the label
    val branchInfix = if (!supportedHeads.contains(branchName) && !branchName.matches("^version/v\\d+$".toRegex()) && !branchName.matches("^v\\d+\\.\\d+\\.\\d+$".toRegex())) {
        val jiraBranchRegex = Regex("(?:.*/)?(\\w+)-(\\d+)(?:-(.+))?")
        val match = jiraBranchRegex.matchEntire(branchName)
        val branchExtension = match?.let {
            val (jiraProject, ticketNumber, _) = it.destructured
            "$jiraProject$ticketNumber"
        } ?: branchName

        "-${branchExtension.replace("[^A-Za-z0-9_-]".toRegex(), "-")}"
    } else {
        ""
    }

    val nonServicePrefix = "v"
    val servicePrefix = ""

    fun findVersionDescription(prefix: String): String? = kotlin.runCatching {
        git?.describe()
            ?.setTags(true)
            ?.setAlways(false)
            ?.setMatch("$prefix[0-9]*.[0-9]*.[0-9]*")
            ?.setTarget("HEAD")
            ?.call()
    }
        .getOrNull()

    val versionDescription = findVersionDescription(nonServicePrefix) ?: findVersionDescription(servicePrefix)

    val descriptionPattern = """^(v)?([0-9]+)\.*([0-9]+)\.*([0-9]+)(-alpha)?(?:-([0-9]+)-g.?[0-9a-fA-F]{3,})?$""".toRegex()

    val (latestTag, commitDistance, tagVersion) = when (val match = versionDescription?.let { descriptionPattern.find(it) }) {
        null -> {
            Triple(null, null, "1.0.0$branchInfix-SNAPSHOT")
        }

        else -> {
            val (prefix, major, minor, patch, suffix, commitDistance) = match.destructured
            Triple(
                "$prefix$major.$minor.$patch${suffix.takeIf { it.isNotBlank() } ?: ""}",
                commitDistance.takeIf { it.isNotBlank() }?.toInt() ?: 0,
                if (suffix.isNotBlank()) {
                    "$major.$minor.$patch$branchInfix-SNAPSHOT"
                } else {
                    when (commitDistance) {
                        "0", "" -> "$major.$minor.$patch"
                        else -> "$major.$minor.${patch.toInt() + 1}$branchInfix-SNAPSHOT"
                    }
                }
            )
        }
    }
    return RoninProjectVersion(
        tagBasedVersion = tagVersion,
        serviceVersion = maybeServiceVersion(),
        lastTag = latestTag,
        commitDistance = commitDistance,
        gitHash = ref?.objectId?.let { reader?.abbreviate(it)?.name() },
        gitHashFull = ref?.objectId?.name,
        branchName = branchName,
        dirty = git?.status()?.call()?.isClean == false
    )
}
