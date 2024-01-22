# ronin-contract-json-plugin

This plugin provides validation and documentation generation for json contracts (e.g. events, messaging, json storage, redis cache, etc.).

To use this plugin, include the following in your plugins section of the Gradle build file:

```kotlin
plugins {
    id("com.projectronin.json.contract") version "..."
}
```

## Expected Setup

This plugin makes several assumptions about the format of the consuming project.

* The project will have only a single version of the schema to process
* The schema files will be located in `src/main/resources/schemas`, and only primary schema files will be located in that directory.
* Schema files will be named `{name}.schema.json`
* Schema files referenced in those main schema files will be in a subdirectory of `src/main/resources/schemas`
* Versioning will be done based on tags.  E.g. `v1.0.0`

The plugin will, when the normal gradle tasks are run (e.g. `build` or `assemble` or `publishToMavenLocal`), run the normal lifecycle tasks.  It will download schema dependencies, create a
tar file of the schema contents, publish that tar file to a maven repo, etc.  It will also generate a java library from the schema using jsonschema2pojo.  Both the tar and the jar file with
the java classes will be published to maven under the indicated version.  The artifact id of the published artifact will _also_ have a version suffix (without this you couldn't consume
more than one version of the dependency in a project).

## Tasks

### testContracts

The `testContracts` (or just `check`) task will validate the project's schemas and test them against any provided example files.  Examples must be located in `src/test/resources/examples`.  If there
is only a single primary schema file, then all examples will be tested against it.  If there are multiple primary schema files, the main name of that schema file is assumed to prefix
applicable examples.

For example, for this layout, all examples are tested against the listed schema.

    .
    ├── src/main/resources/schemas
    │   └── my-schema-v1.schema.json
    ├── src/test/resources/examples
    │   ├── example1.json
    │   └── example2.json

But in this example, `my-schema-example.json` is tested against `my-schema-v2.schema.json`, and `my-other-schema-example1.json` and `my-other-schema-example2.json` are tested
against `my-other-schema-v2.schema.json`.

    .
    ├── src/main/resources/schemas
    │   ├── my-other-schema-v2.schema.json
    │   └── my-schema-v2.schema.json
    ├── src/test/resources/examples
    │   ├── my-other-schema-example1.json
    │   ├── my-other-schema-example2.json
    │   └── my-schema-example.json

### generateContractDocs

The `generateContractDocs` task uses the Docker image built by the parent project to generate documentation for all schemas
in a `docs` folder within each version folder.

### clean

Deletes all project outputs and temporary files

### createSchemaTar

Run when `assemble` is used, creates a tar file with just the schemas.

### downloadSchemaDependencies

Downloads schema dependencies.  See below

## Dependencies

You may use other schema files as dependencies of this one.  In your `build.gradle.kts` file, you can do something like the following:

```kotlin
dependencies {
    schemaDependency("com.projectronin.contract.json:<some artifact id>-v<some major version>:<some version>:schemas@tar.gz")
}
```

When you do this, and then run `./gradlew downloadSchemaDependencies`, the named dependencies are downloaded and unzipped under `src/main/resources/schemas/.dependencies/<artifactid>`.  You
can then reference those schemas from yours, and they schemas and final classes will be included in the final jar/tar.

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

- create a new gradle module in your project, e.g.: `myservice-contract-event`
- add a build.gradle.kts to your new module like:
```kotlin
group = "com.projectronin.contract.event"

plugins {
    alias(roningradle.plugins.ronin.json.contract)
}
```
- add openapi specs as described in this document under the new module's `src/main/resources/schemas` directory, e.g. `myservice-contract-event/src/main/resources/schemas`
- use the new module as a dependency in your service, e.g.: `implementation(project(":myservice-contract-event"))`
- assuming the contract should be available to consumers, you will need to modify your CI/CD builds to do a maven publish to get the service contract out there.
- use the whole gradle build as usual.

## To maintain multiple versions of the contract in the service

Assuming you are moving from `vX` to `vY` for your service, and that you will provide both versions simultaneously from the same service, you can do the following.

- tag your service as `Y.0.0-alpha`.  This should produce the new `Y.0.0` version
- create a new module in your service to represent the _old_ version of the contract.  E.g. if you have `myservice-contract-event`, create one called `myservice-contract-event-vX`
- copy the old contracts and gradle build file from the current module to the "old" module, e.g. from `myservice-contract-event` to `myservice-contract-event-vX`
- modify the build file in the new (old) version to include a `versionOverride` configuration.  E.g.:
```kotlin
group = "com.projectronin.contract.event"

plugins {
  alias(roningradle.plugins.ronin.json.contract)
}

contracts {
    versionOverride.set("X.n.n") // this should be whatever the PREVIOUS version of the contract was.
}
- ```
- add the new (old) contract artifact to your service, e.g. with `implementation(project(":myservice-contract-event-vX"))`
- begin to evolve your new contract version

If done right, this _should_ generate two separate contract versions with associated classes in two separate packages, and you should be able to receive both of them via messages, etc
in your service.  The newly published versions of the _old_ contract will be published under the old version with a suffix, like `X.n.n-Y.n.n`.  This might be awkward,
but should allow you to track what actual version of the old contract to use.  Consumers, of course, could stay on the last un-suffixed version of the old contract,
only updating if they need some fix / change in it to the suffixed version.  Ideally they'll move to the new version soon.
