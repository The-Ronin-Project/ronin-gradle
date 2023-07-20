# Ronin Build Conventions Spring Service Plugin

Sets up a module to produce a spring boot project, complete with boot-jar.

## Usage

In a module you want to be a spring service:

```
root-project
├── service-project
│   └── build.gradle.kts
│   
```

In build.gradle.kts, assuming your project follows the instructions in [README.md](../../README.md), place the following code in the service module's build.gradle.kts:

```kotlin
plugins {
    alias(roningradle.plugins.buildconventions.spring.servicve)
}
```

This plugin applies the following defaults to your service module:

- the same as [README.md](../ronin-build-conventions-kotlin/README.md)
- applies the spring boot plugins
- applies the spring dependency manager plugin
- applies the spring kotlin core and kotlin jpa plugins
- applies the kapt plugin
- generates spring buildInfo
- applies the spring boot annotation processor to the project
