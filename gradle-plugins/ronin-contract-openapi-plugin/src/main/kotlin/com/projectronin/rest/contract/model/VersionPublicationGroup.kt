package com.projectronin.rest.contract.model

class VersionPublicationGroup(
    val versionNumber: Int,
    val extended: Boolean,
    val version: String,
    val extensions: List<VersionPublicationArtifact>
) {
    val isSnapshot = version.contains("SNAPSHOT") && !extended
    val isRelease = !isSnapshot
    val publicationName = "V$versionNumber${if (extended) "Extended" else ""}"
    val publishTaskNames: List<String> = listOf(
        "publish${publicationName}PublicationToArtifactorySnapshotsRepository",
        "publish${publicationName}PublicationToArtifactoryReleasesRepository"
    )
    val localPublishTaskName = "publish${publicationName}PublicationToMavenLocal"
}
