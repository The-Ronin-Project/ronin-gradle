package com.projectronin.test

import org.gradle.api.Plugin
import org.gradle.api.Project

class Hello : Plugin<Project> {

    override fun apply(target: Project) {
        target.task("hello") {
            target.logger.info("Hello World!")
        }
    }
}
