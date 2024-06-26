# Ronin Build Conventions Catalog Plugin

Creates a gradle Version Catalog for the current project.  Ideally, will be in a subproject of your main project, and is primarily useful for producing catalogs of libraries to be consumed.

## Usage

Create a module in your project:

```
root-project
├── catalog
│   └── build.gradle.kts
│   
```

In build.gradle.kts, assuming your project follows the instructions in [README.md](../../README.md), place the following code in the catalog module's build.gradle.kts:

```kotlin
plugins {
    alias(roningradle.plugins.buildconventions.catalog)
}
```

That's it.  The project should then produce a TOML catalog file which includes everything in _your_ project's catalog and all library or plugin
outputs of your project.

## Configuration

You can influence what gets published in the bundle:

```kotlin
roninCatalog {
   prefix.set("ronin")
}
```

See com.projectronin.buildconventions.RoninCatalogExtension for more information.
