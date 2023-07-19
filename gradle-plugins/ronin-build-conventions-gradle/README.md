# Ronin Build Conventions Gradle Plugin

Builds a gradle plugin, written in kotlin

## Usage

Create a module in your project:

```
root-project
├── plugin
│   └── build.gradle.kts
│   
```

Assuming your project follows the instructions in [README.md](../../README.md), place the following code in the plugin module's build.gradle.kts:

```kotlin
plugins {
    alias(roningradle.plugins.ronin.gradle.buildconventions.gradleplugin)
}

gradlePlugin {
    plugins {
        create("myPlugin") {
            id = "com.projectronin.foo.bar"
            implementationClass = "com.projectronin.foo.BarPlugin"
        }
    }
}
```

That's it.  The project should then produce a gradle plugin, written in kotlin.
