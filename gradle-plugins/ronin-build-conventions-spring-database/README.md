# Ronin Build Conventions Spring Database Plugin

Sets up a module to produce a spring database library.

## Usage

In a module you want to be a spring/liquibase DB module:

```
root-project
├── database-project
│   └── build.gradle.kts
│   
```

In build.gradle.kts, assuming your project follows the instructions in [README.md](../../README.md), place the following code in the db module's build.gradle.kts:

```kotlin
plugins {
    alias(roningradle.plugins.ronin.gradle.buildconventions.spring.database)
}
```

This plugin applies the following defaults to your spring liquibase DB module:

- the same as [README.md](../ronin-build-conventions-kotlin/README.md)
- applies the spring boot bom as a platform dependency
- adds liquibase, mysql (runtime only), spring boot test, and testcontainers as dependencies
