# Ronin Build Conventions Versioning Plugin

Sets up a module to produce git tag-based semantic version to a project.  It should be applied at the root of the project

## Usage

In a module you want to be a spring/liquibase DB module:

```
root-project
├── build.gradle.kts
│   
```

In the root build.gradle.kts, assuming your project follows the instructions in [README.md](../../README.md), place the following code:

```kotlin
plugins {
    alias(roningradle.plugins.buildconventions.versioning)
}
```

Versioning takes place using the [axion release plugin](https://github.com/allegro/axion-release-plugin).  This means that if a build is done from a tag, say, `v1.0.3`, and a build is published,
the build will use version `1.0.3`.  If commits have taken place since that tag, `x.y.z-SNAPSHOT` will be used, where `z` is the next increment.  It is expected that release tags will be created
manually through GitHub using the 'release' feature, and that they will be called `vX.Y.Z` using the standard semver semantics.

To "prepare" the repo to publish a next major version, you can locally run `gradle markNextVersion -Prelease.version=X.Y.Z` and push the tag manually.

To _revise an older version_, you will need to create a `vX` branch where `X` is the major version you want to publish, from the tag you want to revise.  E.g.:

```bash
git checkout v1.7.3
git checkout -b v1
# do some work
git add '.'
git commit -m "I did some work"
```

This should produce a new version, e.g. `1.7.4-SNAPSHOT`.

You _probably_ don't want to merge this branch back to main, because that will confuse the versioning algorithm.
