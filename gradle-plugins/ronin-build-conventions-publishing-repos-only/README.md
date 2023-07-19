# Ronin Build Conventions Publishing Repos Only Plugin

Sets up a project to publish to Ronin's maven/artifactory, but without adding the default java artifacts.  Use for plugins, for instance.  

## Usage

In a module you want published:

```
root-project
├── sub-project
│   └── build.gradle.kts
│   
```

In build.gradle.kts, assuming your project follows the instructions in [README.md](../../README.md), place the following code in the module's build.gradle.kts:

```kotlin
plugins {
    alias(roningradle.plugins.ronin.gradle.buildconventions.publishingreposonly)
}
```

This plugin applies the following defaults to your module:

- registers maven repositories for both snapshots and releases
- registers the compiled java / kotlin output of the project to be published
