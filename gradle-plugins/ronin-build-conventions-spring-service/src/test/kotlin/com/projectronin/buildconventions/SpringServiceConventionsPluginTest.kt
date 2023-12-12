package com.projectronin.buildconventions

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.junit.jupiter.api.Test

class SpringServiceConventionsPluginTest {

    @Test
    fun `should do nothing if it's not os x`() {
        val project = mockk<Project>()
        val plugin = SpringServiceConventionsPlugin()
        plugin.potentiallyAddOsXNettyResolver(
            project,
            "Windows NT",
            "x86"
        )
    }

    @Test
    fun `should do nothing if it's not aarch64`() {
        val project = mockk<Project>()
        val plugin = SpringServiceConventionsPlugin()
        plugin.potentiallyAddOsXNettyResolver(
            project,
            "Mac OS X",
            "x86"
        )
    }

    @Test
    fun `should not add dependency for not webflux`() {
        val project = mockk<Project>()
        val configurationContainer = mockk<ConfigurationContainer>()
        val configuration = mockk<Configuration>()
        val dependencies = mockk<DependencySet>()

        every { project.configurations } returns configurationContainer
        every { configurationContainer.getByName("implementation") } returns configuration
        every { configuration.dependencies } returns dependencies
        every { dependencies.isEmpty() } returns false
        every { dependencies.iterator() } returns mutableSetOf(
            mockk<Dependency>().also {
                every { it.name } returns "spring-boot-starter"
            },
            mockk<Dependency>().also {
                every { it.name } returns "spring-boot-web"
            },
            mockk<Dependency>().also {
                every { it.name } returns "product-spring-starter"
            }
        ).iterator()

        val plugin = SpringServiceConventionsPlugin()
        plugin.potentiallyAddOsXNettyResolver(
            project,
            "Mac OS X",
            "aarch64"
        )
    }

    @Test
    fun `should add dependency for webflux`() {
        val project = mockk<Project>()
        val configurationContainer = mockk<ConfigurationContainer>()
        val configuration = mockk<Configuration>()
        val dependencies = mockk<DependencySet>()
        val dependencyHandler = mockk<DependencyHandler>()
        val dependency = mockk<Dependency>()

        every { project.configurations } returns configurationContainer
        every { configurationContainer.getByName("implementation") } returns configuration
        every { configuration.dependencies } returns dependencies
        every { dependencies.isEmpty() } returns false
        every { dependencies.iterator() } returns mutableSetOf(
            mockk<Dependency>().also {
                every { it.name } returns "asdf"
            },
            mockk<Dependency>().also {
                every { it.name } returns "jkl;"
            },
            mockk<Dependency>().also {
                every { it.name } returns "product-spring-webflux-starter"
            }
        ).iterator()

        every { project.dependencies } returns dependencyHandler
        every { dependencyHandler.add("runtimeOnly", "io.netty:netty-resolver-dns-native-macos::osx-aarch_64") } returns dependency

        val plugin = SpringServiceConventionsPlugin()
        plugin.potentiallyAddOsXNettyResolver(
            project,
            "Mac OS X",
            "aarch64"
        )

        verify(exactly = 1) { dependencyHandler.add("runtimeOnly", "io.netty:netty-resolver-dns-native-macos::osx-aarch_64") }
    }
}
