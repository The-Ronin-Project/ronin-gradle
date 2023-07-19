package com.projectronin.sample

class Hello {

    @com.fasterxml.jackson.annotation.JsonProperty
    var foo: String = ""

    fun sayHello(who: String): String = "Hello $who"
}
