package com.example.subproject03

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HelloWorldTests {

    @Test
    fun saysHelloWorld() {
        assertThat(HelloWorld.sayHello()).isEqualTo("Hello world!")
    }

    @Test
    fun saysHello() {
        assertThat(HelloWorld.sayHello("Kathy Galland")).isEqualTo("Hello Kathy Galland!")
    }
}
