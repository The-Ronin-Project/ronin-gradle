# ronin-gradle

Gradle plugins for Ronin Services

# Usage

See individual plugin README.md files for usage of any particular plugin.

To use one of these plugins, using gradle 8.2:

settings.gradle.kts:
```kotlin
rootProject.name = "your-project-name"

include("your-submodule-1")
include("your-submodule-2")

pluginManagement {
    repositories {
        maven {
            url = uri("https://repo.devops.projectronin.io/repository/maven-public/")
        }
        mavenLocal()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://repo.devops.projectronin.io/repository/maven-public/")
        }
        mavenLocal()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("roningradle") {
            from("com.projectronin.services.gradle:ronin-gradle-catalog:<current version>")
        }
    }
}
```

In a build file where you want to use one:
build.gradle.kts:
```kotlin
plugins {
    // alias(roningradle.plugins.<plugin.name>), e.g:
    alias(roningradle.plugins.json.contract)
}
```

# Versioning / Deploying / CI/CD

Versioning is done with [axion-release](https://axion-release-plugin.readthedocs.io/en/latest/).  This means that if a build is done from a tag, say, `v1.0.3`, and a build is published,
the build will use version `1.0.3`.  If commits have taken place since that tag, `x.y.z-SNAPSHOT` will be used, where `z` is the next increment.  It is expected that release tags will be created
manually through GitHub using the 'release' feature, and that they will be called `vX.Y.Z` using the standard semver semantics.  Branches will be included in the version,
so if you're on, say, branch `feature/foo`, your version will be `x.y.z-feature-foo-SNAPSHOT`.

To "prepare" the repo to publish a next major version, you can locally run `./gradlew markNextVersion -Prelease.version=X.Y.Z` and push the tag manually.

To see the current version, use `./gradlew currentVersion`.

Pull requests will publish snapshots on successful build; code coverage and analysis are published to codecov and to sonarqube.  Merges to main will also publish a snapshot, and
tags on main in the `vX.Y.Z` format will produce a release version.

# Adding plugins

The way this project works, it expects all subprojects under [gradle-plugins](gradle-plugins) to be gradle plugins.  Plugin implementations can produce more than one plugin,
can use the `kotlin-dsl` plugin to produce script plugins, or can use `java-gradle-plugin` to produce the plugin code instead.  The only requirement of the plugin itself is that it
be named with an id of `com.projectronin.SOMETHING`.

Most project configuration is applied by the root [build.gradle.kts](build.gradle.kts) file in this project.  That build file iterates through all subprojects and applies a group-id and
version, and maven publishing settings.  It applies kotlin-jvm and ktlint, jacoco, java, and java-gradle-plugin to all the subprojects as well.  It configures kotlin compile settings,
assumes junit platform for tests, and configures the jacoco code coverage tool.

Shared code (code shared between plugins) can be put into modules under [shared-libraries](shared-libraries).  Basic kotlin build setups will be automatically applied to them.

Outside of those conventions applied to all plugins here, module build files can supply their own dependencies, etc.  See [build.gradle.kts](ronin-contract-json-plugin%2Fbuild.gradle.kts)
for a simple example.
