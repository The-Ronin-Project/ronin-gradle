package com.projectronin.gradle.helpers

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskContainer
import org.junit.jupiter.api.Test

class ExtensionFunctionsTest {

    @Test
    fun `should properly handle service version - null`() {
        val project = mockk<Project>()
        every { project.properties } returns mutableMapOf()
        assertThat(project.maybeServiceVersion()).isNull()
    }

    @Test
    fun `should properly handle service version - empty`() {
        val project = mockk<Project>()
        every { project.properties } returns mutableMapOf("service-version" to "")
        assertThat(project.maybeServiceVersion()).isNull()
    }

    @Test
    fun `should properly handle service version - value`() {
        val project = mockk<Project>()
        every { project.properties } returns mutableMapOf("service-version" to "4.7.4-SNAPSHOT")
        assertThat(project.maybeServiceVersion()).isEqualTo("4.7.4-SNAPSHOT")
    }

    @Test
    fun `should add task immediately by name if it exists`() {
        val project = mockk<Project>()
        val tasks = mockk<TaskContainer>()
        val fooTask = mockk<Task>()
        val task = mockk<Task>()
        val logger = mockk<Logger>()

        every { project.logger } returns logger
        every { logger.debug(any()) } just Runs
        every { project.tasks } returns tasks
        every { project.path } returns "root"
        every { tasks.findByName("foo") } returns fooTask
        every { fooTask.name } returns "foo"
        every { task.dependsOn(fooTask) } returns fooTask
        every { task.project } returns project
        every { task.name } returns "owner"

        task.dependsOnTaskByName("foo")

        verify(exactly = 1) { task.dependsOn(fooTask) }
    }

    @Test
    fun `should lazily add task later`() {
        val project = mockk<Project>()
        val tasks = mockk<TaskContainer>()
        val fooTask = mockk<Task>()
        val task = mockk<Task>()
        val logger = mockk<Logger>()

        val actionSlot = slot<Action<Task>>()

        every { logger.debug(any()) } just Runs

        every { project.logger } returns logger
        every { project.tasks } returns tasks
        every { project.path } returns "root"

        every { task.project } returns project
        every { task.name } returns "owner"
        every { tasks.findByName("foo") } returns(null)
        every { tasks.whenTaskAdded(capture(actionSlot)) } answers { it.invocation.args[0] as Action<in Task> }
        every { task.dependsOn(fooTask) } returns fooTask

        every { fooTask.name } returns "foo"

        task.dependsOnTaskByName("foo")

        verify(exactly = 0) { task.dependsOn(*anyVararg()) }

        assertThat(actionSlot.captured).isNotNull

        val anotherTask = mockk<Task>()
        every { anotherTask.name } returns "another"
        actionSlot.captured.execute(anotherTask)
        verify(exactly = 0) { task.dependsOn(*anyVararg()) }

        actionSlot.captured.execute(task)
        verify(exactly = 0) { task.dependsOn(*anyVararg()) }

        actionSlot.captured.execute(fooTask)
        verify(exactly = 1) { task.dependsOn(fooTask) }
    }

    @Test
    fun `should add task immediately by type if it exists`() {
        val project = mockk<Project>()
        val tasks = mockk<TaskContainer>()
        val fooTask = mockk<FooTask>()
        val task = mockk<Task>()
        val logger = mockk<Logger>()
        val taskCollection = mockk<TaskCollection<FooTask>>()

        every { logger.debug(any()) } just Runs

        every { project.logger } returns logger
        every { project.tasks } returns tasks
        every { project.path } returns "root"

        every { tasks.withType(FooTask::class.java) } returns taskCollection
        every { tasks.whenTaskAdded(any<Action<Task>>()) } returns mockk<Action<Task>>()

        every { fooTask.name } returns "foo"

        every { task.dependsOn(fooTask) } returns fooTask
        every { task.project } returns project
        every { task.name } returns "owner"

        every { taskCollection.isEmpty() } returns false
        every { taskCollection.iterator() } returns mutableListOf(fooTask).listIterator()
        every { taskCollection.toTypedArray() } returns arrayOf(fooTask)

        task.dependsOnTasksByType(FooTask::class.java)

        verify(exactly = 1) { task.dependsOn(fooTask) }
    }

    @Test
    fun `should add task lazily by type`() {
        val project = mockk<Project>()
        val tasks = mockk<TaskContainer>()
        val fooTask = mockk<FooTask>()
        val task = mockk<Task>()
        val logger = mockk<Logger>()
        val taskCollection = mockk<TaskCollection<FooTask>>()

        val actionSlot = slot<Action<Task>>()

        every { logger.debug(any()) } just Runs

        every { project.logger } returns logger
        every { project.tasks } returns tasks
        every { project.path } returns "root"

        every { tasks.withType(FooTask::class.java) } returns taskCollection
        every { tasks.whenTaskAdded(capture(actionSlot)) } answers { it.invocation.args[0] as Action<in Task> }

        every { fooTask.name } returns "foo"

        every { task.dependsOn(*anyVararg()) } returns fooTask
        every { task.project } returns project
        every { task.name } returns "owner"

        every { taskCollection.isEmpty() } returns true

        task.dependsOnTasksByType(FooTask::class.java)

        verify(exactly = 0) { task.dependsOn(*anyVararg()) }

        val anotherTask = mockk<Task>()
        every { anotherTask.name } returns "foo"
        actionSlot.captured.execute(anotherTask)
        verify(exactly = 0) { task.dependsOn(*anyVararg()) }

        actionSlot.captured.execute(task)
        verify(exactly = 0) { task.dependsOn(*anyVararg()) }

        actionSlot.captured.execute(fooTask)
        verify(exactly = 1) { task.dependsOn(fooTask) }

        val anotherFooTask = mockk<FooTask>()
        every { anotherFooTask.name } returns "foo2"
        actionSlot.captured.execute(anotherFooTask)
        verify(exactly = 1) { task.dependsOn(anotherFooTask) }
        verify(exactly = 2) { task.dependsOn(*anyVararg()) }
    }

    interface FooTask : Task
}
