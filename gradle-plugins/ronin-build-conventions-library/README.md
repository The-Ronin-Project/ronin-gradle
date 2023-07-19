# Ronin Build Conventions Kotlin Library Plugin

Sets up the project in which it's included to be a basic kotlin build.

## Usage

Create a module in your project:

```
root-project
├── sub-project
│   └── build.gradle.kts
│   
```

In build.gradle.kts, assuming your project follows the instructions in [README.md](../../README.md), place the following code in the kotlin module's build.gradle.kts:

```kotlin
plugins {
    alias(roningradle.plugins.ronin.gradle.buildconventions.kotlin.library)
}
```

Adds all the same defaults as [README.md](../ronin-build-conventions-kotlin/README.md), as well as adding maven/artifactory publishying for the project and applying
the java-library gradle plugin, which allows for differentiating api and implementation dependencies.
