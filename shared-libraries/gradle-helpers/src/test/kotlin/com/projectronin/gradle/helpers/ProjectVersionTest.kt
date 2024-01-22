package com.projectronin.gradle.helpers

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ProjectVersionTest {

    @field:TempDir
    private lateinit var _tempFolder: File

    @Test
    fun `should properly handle service version - null`() {
        val projectSetup = setupProject()
        assertThat(projectSetup.project.roninProjectVersion.serviceVersion).isNull()
    }

    @Test
    fun `should properly handle service version - empty`() {
        val projectSetup = setupProject()
        projectSetup.properties += "service-version" to ""
        assertThat(projectSetup.project.roninProjectVersion.serviceVersion).isNull()
    }

    @Test
    fun `should properly handle service version - value`() {
        val projectSetup = setupProject()
        projectSetup.withServiceVersion("4.7.4-SNAPSHOT")
        assertThat(projectSetup.project.roninProjectVersion.serviceVersion).isEqualTo("4.7.4-SNAPSHOT")
    }

    @Test
    fun `should generate the expected data with no commits`() {
        executeTest {
            withServiceVersion("4.7.4-SNAPSHOT")
            RoninProjectVersion(
                tagBasedVersion = "1.0.0-SNAPSHOT",
                serviceVersion = "4.7.4-SNAPSHOT",
                lastTag = null,
                commitDistance = null,
                gitHash = null,
                gitHashFull = null,
                branchName = "main",
                dirty = false
            )
        }
    }

    @Test
    fun `should succeed with no git repository`() {
        executeTest {
            withoutGitFolder()
            withServiceVersion("4.7.4-SNAPSHOT")
            RoninProjectVersion(
                tagBasedVersion = "1.0.0-uninitialized-git-repository-SNAPSHOT",
                serviceVersion = "4.7.4-SNAPSHOT",
                lastTag = null,
                commitDistance = null,
                gitHash = null,
                gitHashFull = null,
                branchName = "uninitialized-git-repository",
                dirty = false
            )
        }
    }

    @Test
    fun `should generate the expected data with dirty repo`() {
        executeTest {
            withSimpleTextFile()
            RoninProjectVersion(
                tagBasedVersion = "1.0.0-SNAPSHOT",
                serviceVersion = null,
                lastTag = null,
                commitDistance = null,
                gitHash = null,
                gitHashFull = null,
                branchName = "main",
                dirty = true
            )
        }
    }

    @Test
    fun `should generate the expected data with a commit`() {
        executeTest {
            withSimpleTextFile()

            withCommit()

            RoninProjectVersion(
                tagBasedVersion = "1.0.0-SNAPSHOT",
                serviceVersion = null,
                lastTag = null,
                commitDistance = null,
                gitHash = shortHash(),
                gitHashFull = gitHash(),
                branchName = "main",
                dirty = false
            )
        }
    }

    @Test
    fun `should generate the expected data with a commit and a tag and a dirty repo`() {
        executeTest {
            withSimpleTextFile()

            withCommit()
            withTag("v1.3.2")

            withSimpleTextFile("bar")

            RoninProjectVersion(
                tagBasedVersion = "1.3.2",
                serviceVersion = null,
                lastTag = "v1.3.2",
                commitDistance = 0,
                gitHash = shortHash(),
                gitHashFull = gitHash(),
                branchName = "main",
                dirty = true
            )
        }
    }

    @Test
    fun `should handle multiple version tags`() {
        executeTest {
            withSimpleTextFile()

            withCommit()
            withTag("v1.3.2")
            withSimpleTextFile("bar")

            withCommit()
            withTag("v1.4.7")

            RoninProjectVersion(
                tagBasedVersion = "1.4.7",
                serviceVersion = null,
                lastTag = "v1.4.7",
                commitDistance = 0,
                gitHash = shortHash(),
                gitHashFull = gitHash(),
                branchName = "main",
                dirty = false
            )
        }
    }

    @Test
    fun `should generate the expected data with a service-style tag`() {
        executeTest {
            withSimpleTextFile()

            withCommit()
            withTag("10.3.1")

            withSimpleTextFile("bar")

            RoninProjectVersion(
                tagBasedVersion = "10.3.1",
                serviceVersion = null,
                lastTag = "10.3.1",
                commitDistance = 0,
                gitHash = shortHash(),
                gitHashFull = gitHash(),
                branchName = "main",
                dirty = true
            )
        }
    }

    @Test
    fun `should generate the expected data with an alpha tag`() {
        executeTest {
            withSimpleTextFile()

            withCommit()
            withTag("2.0.0-alpha")

            RoninProjectVersion(
                tagBasedVersion = "2.0.0-SNAPSHOT",
                serviceVersion = null,
                lastTag = "2.0.0-alpha",
                commitDistance = 0,
                gitHash = shortHash(),
                gitHashFull = gitHash(),
                branchName = "main",
                dirty = false
            )
        }
    }

    @Test
    fun `should generate the expected data a commit after a tag`() {
        executeTest {
            withSimpleTextFile()

            withCommit()
            withTag("v1.3.2")

            withSimpleTextFile("bar")
            withCommit()

            RoninProjectVersion(
                tagBasedVersion = "1.3.3-SNAPSHOT",
                serviceVersion = null,
                lastTag = "v1.3.2",
                commitDistance = 1,
                gitHash = shortHash(),
                gitHashFull = gitHash(),
                branchName = "main",
                dirty = false
            )
        }
    }

    @Test
    fun `should generate the expected data a branch`() {
        executeTest {
            withSimpleTextFile()

            withCommit()
            withTag("v1.3.2")

            // withSimpleTextFile("bar")
            // withCommit()
            withBranch("some-branch")

            RoninProjectVersion(
                tagBasedVersion = "1.3.2",
                serviceVersion = null,
                lastTag = "v1.3.2",
                commitDistance = 0,
                gitHash = shortHash(),
                gitHashFull = gitHash(),
                branchName = "some-branch",
                dirty = false
            )
        }
    }

    @Test
    fun `should generate the expected data a branch with some work`() {
        executeTest {
            withCommitTagBranchAndCommit("some-branch")

            RoninProjectVersion(
                tagBasedVersion = "1.3.3-some-branch-SNAPSHOT",
                serviceVersion = null,
                lastTag = "v1.3.2",
                commitDistance = 1,
                gitHash = shortHash(),
                gitHashFull = gitHash(),
                branchName = "some-branch",
                dirty = false
            )
        }
    }

    @Test
    fun `should generate the expected data a feature branch`() {
        executeTest {
            withCommitTagBranchAndCommit("feature/some-feature")

            RoninProjectVersion(
                tagBasedVersion = "1.3.3-feature-some-feature-SNAPSHOT",
                serviceVersion = null,
                lastTag = "v1.3.2",
                commitDistance = 1,
                gitHash = shortHash(),
                gitHashFull = gitHash(),
                branchName = "feature/some-feature",
                dirty = false
            )
        }
    }

    @Test
    fun `should generate the expected data a jira branch`() {
        executeTest {
            withCommitTagBranchAndCommit("DASH-2064-some-thing")

            RoninProjectVersion(
                tagBasedVersion = "1.3.3-DASH2064-SNAPSHOT",
                serviceVersion = null,
                lastTag = "v1.3.2",
                commitDistance = 1,
                gitHash = shortHash(),
                gitHashFull = gitHash(),
                branchName = "DASH-2064-some-thing",
                dirty = false
            )
        }
    }

    @Test
    fun `should generate the expected data a jira branch with prefix`() {
        executeTest {
            withCommitTagBranchAndCommit("bug/DASH-2064-some-thing")

            RoninProjectVersion(
                tagBasedVersion = "1.3.3-DASH2064-SNAPSHOT",
                serviceVersion = null,
                lastTag = "v1.3.2",
                commitDistance = 1,
                gitHash = shortHash(),
                gitHashFull = gitHash(),
                branchName = "bug/DASH-2064-some-thing",
                dirty = false
            )
        }
    }

    private fun ProjectSetup.withCommitTagBranchAndCommit(branchName: String) {
        withSimpleTextFile()

        withCommit()
        withTag("v1.3.2")

        withBranch(branchName)

        withSimpleTextFile("baz")
        withCommit()
    }

    private fun ProjectSetup.withBranch(branchName: String) {
        git.branchCreate().setName(branchName).call()
        git.checkout().setName(branchName).call()
    }

    private fun ProjectSetup.shortHash() = gitHash().substring(0, 7)

    private fun ProjectSetup.withTag(tagName: String) {
        git.tag().setName(tagName).call()
    }

    private fun ProjectSetup.withCommit() {
        git.add().addFilepattern(".").call()
        git.commit().setMessage("Foo").call()
    }

    private fun ProjectSetup.withSimpleTextFile(txt: String = "foo") {
        tempFolder.resolve("$txt.txt").writeText("txt")
    }

    private fun ProjectSetup.withServiceVersion(value: String) {
        properties += "service-version" to value
    }

    private fun ProjectSetup.withoutGitFolder() {
        tempFolder.resolve(".git").deleteRecursively()
    }

    private fun ProjectSetup.gitHash(): String = git.log().setMaxCount(1).call().first().name

    private fun executeTest(testSetup: ProjectSetup.() -> RoninProjectVersion) {
        val projectSetup = setupProject()
        val expectedValue = testSetup(projectSetup)

        val rpv = projectSetup.project.roninProjectVersion

        assertThat(rpv).isEqualTo(expectedValue)
    }

    private fun setupProject(): ProjectSetup {
        val git = Git.init().setDirectory(this._tempFolder).setInitialBranch("main").call()
        val project = mockk<Project>()
        val rootProject = mockk<Project>()
        val logger = mockk<Logger>(relaxed = true)

        every { project.rootProject } returns rootProject
        val properties = mutableMapOf<String, Any?>()
        every { project.properties } returns properties
        every { project.logger } returns logger
        every { project.hasProperty("disable-gha-refs") } returns true

        every { rootProject.projectDir } returns _tempFolder

        return ProjectSetup(
            project = project,
            rootProject = rootProject,
            git = git,
            properties = properties,
            logger = logger,
            tempFolder = _tempFolder
        )
    }

    private data class ProjectSetup(
        val project: Project,
        val rootProject: Project,
        val git: Git,
        val properties: MutableMap<String, Any?>,
        val logger: Logger,
        val tempFolder: File
    )
}
