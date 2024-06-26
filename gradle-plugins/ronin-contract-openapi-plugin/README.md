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
- a .json copy of the schema at `build/generated/resources/openapi/static/v3/api-docs/<project name>/v<n>.json, included in jar`
- a .yaml copy of the schema at `build/generated/resources/openapi/static/v3/api-docs/<project name>/v<n>.yaml, included in jar`
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

#### Using openapi-generator instead of Fabrikt

You can use something like the following to switch from the default fabrikt generator to the openapi-generator one:

```kotlin
restContractSupport {
    generatorType.set(com.projectronin.rest.contract.GeneratorType.OPENAPI_GENERATOR)
    openApiGeneratorAdditionalProperties.put("reactive", true)
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

## Versioning

Versioning is done via tags.  The most recent tag in the format `vN.N.N` or `N.N.N` is considered (but please don't mix the two formats in your repository).  The plugin will consider the latest
tag, and use these rules:

- If the most recent tag is on the current commit, the version from the tag is used directly.  E.g. if commit `04be8081befb138aeddbfde95310392a3daec610` is current, and that commit is tagged `v3.7.0`,
  the build will use the version 3.7.0
- If there's been a commit since the latest tag, the build will produce a patch-level increment with a snapshot.  E.g. if the most recent tag is `v3.7.0` and there has been at least one commit since
  then, the build will use version `3.7.1-SNAPSHOT`.
- If the build is on a branch other than `main` or `vN` where `N` is a major version, the version used will include some version of the branch name.  E.g. if you are on branch `feature/DASH-9943-something`,
  the build will produce a version like `3.7.1-DASH9943-SNAPSHOT`.

To set the next version (e.g. to move from 1.x.y to 2.x.y or from 1.x.y to 1.y.y), tag the repository with the desired version plus `-alpha`.  E.g., if your current tag is `v3.7.0` and you
want the next build to be `v3.8.0` not `v3.7.1`, do:

```bash
git tag v3.8.0-alpha
git push v3.8.0-alpha
```

It is recommended you don't do this at the _same_ location as an existing release tag; do it on the first commit you want to have the new version.  It's also probably important to tag a
commit on the main branch, rather than on your feature branch.

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

# Using inside a service repository

You can also put your contract directly inside your service repository.  The procedure for doing so is:

- create a new gradle module in your project, e.g.: `myservice-contract-openapi`
- Add a spectral.yaml file to the root of the new module with content like:
```yaml
- extends: ["spectral:oas"]

rules:
  oas3-unused-component: info
```
- add a build.gradle.kts to your new module like:
```kotlin
- group = "com.projectronin.rest.contract"

plugins {
    alias(roningradle.plugins.openapi.contract)
}

restContractSupport {
    packageName.set("desired.package.name")
}
```
- add openapi specs as described in this document under the new module's `src/main/openapi` directory, e.g. `myservice-contract-openapi/src/main/openapi`
- use the new module as a dependency in your service, e.g.: `implementation(project(":myservice-contract-openapi"))`
- assuming the contract should be available to consumers, you will need to modify your CI/CD builds to do a maven publish to get the service contract out there.
- use the whole gradle build as usual.

## To maintain multiple versions of the contract in the service

Assuming you are moving from `vX` to `vY` for your service, and that you will provide both versions simultaneously from the same service, you can do the following.

- tag your service as `Y.0.0-alpha`.  This should produce the new `Y.0.0` version
- create a new module in your service to represent the _old_ version of the contract.  E.g. if you have `myservice-contract-openapi`, create one called `myservice-contract-openapi-vX`
- copy the old contracts, yaml, and gradle build file from the current module to the "old" module, e.g. from `myservice-contract-openapi` to `myservice-contract-openapi-vX`
- modify the build file in the new (old) version to include a `versionOverride` configuration.  E.g.:
```kotlin
group = "com.projectronin.rest.contract"

plugins {
    alias(roningradle.plugins.openapi.contract)
}

restContractSupport {
    packageName.set("desired.package.name")
    inputFile.set(file("src/main/openapi/myservice-contract-openapi.json")) // you might need this because the file name no longer matches the module name
    versionOverride.set("X.n.n") // this should be whatever the PREVIOUS version of the contract was.
}
- ```
- add the new (old) contract artifact to your service, e.g. with `implementation(project(":myservice-contract-openapi-vX"))`
- begin to evolve your new contract version

If done right, this _should_ generate two separate contract versions with associated classes in two separate packages, and you should be able to serve them both from separate controllers
in your service.  The newly published versions of the _old_ contract will be published under the old version with a suffix, like `X.n.n-Y.n.n`.  This might be awkward,
but should allow you to track what actual version of the old contract to use.  Consumers, of course, could stay on the last un-suffixed version of the old contract,
only updating if they need some fix / change in it to the suffixed version.  Ideally they'll move to the new version soon.

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
      - name: Setup Node
        uses: actions/setup-node@v3
        with:
          node-version: 18
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
