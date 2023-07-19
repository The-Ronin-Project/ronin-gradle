package com.projectronin.sample

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HelloTest {

    @Test
    fun testHello() {
        assertThat(Hello().sayHello("world")).isEqualTo("Hello world!")
    }
}
