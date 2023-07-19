# Ronin Build Conventions Root Plugin

Sets up the root of your multi-module project to be a proper root project.

## Usage

Create a module in your project:

```
root-project
├─ build.gradle.kts
│   
```

In build.gradle.kts, assuming your project follows the instructions in [README.md](../../README.md), place the following code in the root module's build.gradle.kts:

```kotlin
plugins {
    alias(roningradle.plugins.ronin.gradle.buildconventions.root)
}
```

This plugin applies the following defaults to your root project:

- jacoco report aggregation
- sonarqube analysis
- ktlint
- dokka multi-module aggregation
- releasehub dependency version checking
