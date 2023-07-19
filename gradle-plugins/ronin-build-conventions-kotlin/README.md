# Ronin Build Conventions Kotlin Plugin

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
    alias(roningradle.plugins.ronin.gradle.buildconventions.kotlin.jvm)
}
```

The plugin configures your subproject with:

- kotlin-jvm
- ktlint
- dokka
- jacoco code coverage
- junit tests
- assertJ available for tests
- standard test logging
