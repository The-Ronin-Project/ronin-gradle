# OpenAPI Contract Tooling

This module contains the tooling used to validate OpenAPI schema contracts as well as generate documentation and Kotlin classes from them and publish them as artifacts to artifactory.
It's in the form of a gradle plugin, which takes OpenAPI specifications as inputs, validates them, and outputs combined specifications and jar files to Artifactory.

See the [Migrating](#migrating) section for information if you're updating from the older repository or version 2.1.2 or previous of this module.

# Overview

This plugin makes several assumptions about the format of the consuming project.

* The project will have only a single version of the spec to process
* The spec files will be located in `src/main/openapi`, and will be named `<projectname>.json`.
* Versioning will be done based on tags.  E.g. `v1.0.0`

The plugin will, when the normal gradle tasks are run (e.g. `build` or `assemble` or `publishToMavenLocal`), run the normal lifecycle tasks.  It will download spec dependencies, create a
tar file of the spec contents, publish that tar file to a maven repo, etc.  It will also generate a java library from the schema using fabrikt.  Both the tar and the jar file with
the kotlin classes will be published to maven under the indicated version.  The artifact id of the published artifact will _also_ have a version suffix (without this you couldn't consume
more than one version of the dependency in a project).

# Outputs

The plugin outputs five artifacts:
- a tar.gz file that contains the compiled schemas
- a .json copy of the schema
- a .yaml copy of the schema
- a .jar file with the compiled schema and compiled kotlin classes
- a -sources.jar file with the source files

# Tools

## Validation
Validation tooling is based on [spectral](https://meta.stoplight.io/docs/spectral/674b27b261c3c-overview) and [spectral OpenAPI](https://meta.stoplight.io/docs/spectral/4dec24461f3af-open-api-rules).

## Docs
Documentation generation uses [openapi-generator](https://openapi-generator.tech/)'s [html2](https://openapi-generator.tech/docs/generators/html2/) generator.

## Fabrikt
Code generation uses [fabrikt](https://github.com/cjbooms/fabrikt).  You may find it a little finicky, and you may need to adjust your spec to get the desired classes.

# Usage

## Project setup

Create a gradle 8.2 project.

Create a project with the following structure:

```
├── .gitignore
├── build.gradle.kts
├── settings.gradle.kts
├── ...
├── src
│   └── main
│       └── openapi
│           ├── <api-artifact-name>.json
│           └── schemas
│               ├── <referenced-spec-1>.json
│               ├── <referenced-spec-2>.json
│               ├── ...
│               └── <referenced-spec-n>.json
```

In src/main/openapi, put your OpenAPI V3 specification with the name of your project name / artifact id.  You can organize
your schema inside that any way you'd like.

The content of the configuration files should be similar to the following:

### .gitignore
```gitignore
docs/
build/
/.idea/
gen/
.gradle/
.dependencies/

!gradle/wrapper/gradle-wrapper.jar
```

### settings.gradle.kts
```kotlin
rootProject.name = "YOURPROJECTARTIFACTIDHERE"

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
            from("com.projectronin.services.gradle:ronin-gradle-catalog:<LATEST VERSION>")
        }
    }
}
```

### build.gradle.kts
```kotlin
group = "com.projectronin.rest.contract"

plugins {
    alias(roningradle.plugins.openapi.contract)
}
```

### Dependencies

If your contract depends on _other_ contracts, you can reference them as project dependencies.  Make entries in your build.gradle.kts file like this:

```kotlin
dependencies {
    openapi("<dependency group id>:<dependency artifact id>:<dependency version>")
}
```

When you run the commands described below, these dependencies will be downloaded and placed in the `src/main/openapi/.dependencies` directory.  If they
are archives, they'll be unzipped, and you can reference the dependent contracts, schemas, etc. directly by path from inside your OpenAPI schema.

### Configuration

See [RestContractSupportExtension.kt](src/main/kotlin/com/projectronin/rest/contract/RestContractSupportExtension.kt) for information about how this plugin can be configured.  None
of the configuration items are required.  Specifically, you may want to configure codegen options for package name and for the fabrikt setup.  For example:

```kotlin
restContractSupport {
    controllerOptions.add("SUSPEND_MODIFIER")
}
```

## Running

In general, either run:

`gradle <COMMAND> <ARGUMENTS>`

Available commands are mostly general gradle commands.  Important ones are:

`check`: Verifies the contract using spectral tooling, making sure it's a valid contract.

`downloadApiDependencies`: downloads any API dependencies as specified in the `Dependencies` section above.

`build`: Runs `check`, generates schema documentation, and generates simple combined schema files.

`clean`: removes all generated files

`assemble`: Assembles the schema into deployable archives.

`publishToMavenLocal`: publishes all outputs to the local maven repo (e.g. `$HOME/.m2/repository`).  If you are using the docker image, it will try and
copy files in and out of the host's repository directory so they can be used for builds later on the host.

`publish`: publishes all outputs to the remote Ronin maven repository.

# Migrating

## Versioning

Versioning will now be based on tags.  Whatever the current release version of your specification is (the version should be in the specification file itself at this point, in the highest `vN`
directory that you have), you should tag the last `main` branch commit with that version and push that tag to github.  For example, assuming the version of your spec is `1.3.7`, you should,
before making other changes, do the following:

```bash
git tag v1.3.7
git push origin refs/tags/v1.3.7
```

If the current version of your latest specification isn't stable (e.g. it has a `-SNAPSHOT` suffix), you should instead tag the repository with a `-alpha` version.  E.g. for 1.3.7-SNAPSHOT,
you can tag main like this:

```bash
git tag v1.3.7-alpha
git push origin refs/tags/v1.3.7-alpha
```

With the latest version of the plugin, branches other than main will produce versions like N.N.N-BRANCH-SNAPSHOT, and when merging a PR to main you will produce a version like
`N.N.N-SNAPSHOT`.  To make a release version, use GitHub's release functionality to create a new tag like `vN.N.N`, and a release version will be built and deployed to artifactory.

You can read [README.md](../ronin-build-conventions-versioning/README.md) for some more information on versioning.

This plugin will update your specification to have the correct version before publishing.

## Kotlin Build

The latest version of this plugin _also_ produces a jar with kotlin classes in it, representing the controllers and model objects in your specification.  This should mean that to consume the
specification you should only need to import that jar as a dependency of your service, rather than re-generating the classes from the spec.

## Plugin Change from ronin-contract-rest-tooling

If you're migrating from the [old repository](https://github.com/projectronin/ronin-contract-rest-tooling) (versions 1.2.1 of `com.projectronin.rest.contract:plugin` and previous):

The location, version, and artifact of the plugin has changed.  To utilize the new plugin in your contract repository:

Update the gradle wrapper version in your project:

```bash
./gradlew wrapper --gradle-version 8.2
```

Update your settings.gradle.kts to look like this:

```kotlin
rootProject.name = "YOURPROJECTARTIFACTIDHERE"

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
            from("com.projectronin.services.gradle:ronin-gradle-catalog:<LATEST VERSION>")
        }
    }
}
```

Update your build.gradle.kts to look like this:

```kotlin
group = "com.projectronin.rest.contract"

plugins {
    alias(roningradle.plugins.openapi.contract)
}
```

You should drop any `node` configuration blocks, but retain any `dependencies/vN` setup.

Update .github/workflows/cicd.yaml to look like this:

```yaml
name: Contract CI/CD

on:
  push:
    branches:
      - main
      - version/v*
    tags:
      - 'v*.*.*'
  pull_request:

jobs:
  build_and_test:
    runs-on: self-hosted
    steps:
      - name: test and build
        uses: projectronin/github/.github/actions/json-contract-cicd-gradle@json_contract_cicd_gradle/v1
        with:
          nexus_user: ${{ secrets.NEXUS_MAVEN_USER }}
          nexus_token: ${{ secrets.NEXUS_MAVEN_TOKEN }}
```

You will also have to follow the directions below for "Standardization Change"

## Standardization Change

If you're migrating from the [old repository](https://github.com/projectronin/ronin-contract-rest-tooling) (versions 1.2.1 of `com.projectronin.rest.contract:plugin` and previous) _or_ if you
are migrating from versions 2.1.2 and previous of this plugin:

Note that the latest version of this plugin only supports a single version of the openapi specification in a repository at a time, bringing it in-line with the new
[ronin-contract-json-plugin](../ronin-contract-json-plugin).

To make your contract work with the new "standardized" version of the plugin, you will need to first move the version of the specification you want to use going forward from
the `vN` directory into `src/main/openapi`.  This means moving all the files _under_ the `vN` directory except for `docs`, `build`, and `.dependencies` into `src/main/openapi`.
If you have multiple version directories (e.g. v1 and v2), you'll want to move the latest and delete the older ones.

Make sure your main specification file is named `<project-name>.json`.  For example, of your project is called `YOURPROJECTARTIFACTIDHERE`, the file should be named
`YOURPROJECTARTIFACTIDHERE.json`.

If you have schema dependencies, you should migrate the correct `vN` dependency specs to `openapi`, like this:

```kotlin
dependencies {
   openapi("com.projectronin.rest.contract:contract-rest-clinical-data:1.0.0@json")
}
```
