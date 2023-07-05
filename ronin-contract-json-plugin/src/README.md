# ronin-contract-json-plugin

This plugin provides validation and documentation generation for event contracts.

To use this plugin, include the following in your plugins section of the Gradle build file:

```kotlin
plugins {
    id("com.projectronin.event.contract") version "..."
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

### testEvents

The `testEvents` (or just `check`) task will validate the project's schemas and test them against any provided example files.  Examples must be located in `src/test/resources/examples`.  If there
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

### generateEventDocs

The `generateEventDocs` task uses the Docker image built by the parent project to generate documentation for all schemas
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
    schemaDependency("com.projectronin.contract.event:<some artifact id>-v<some major version>:<some version>:schemas@tar.gz")
}
```

When you do this, and then run `./gradlew downloadSchemaDependencies`, the named dependencies are downloaded and unzipped under `src/main/resources/schemas/.dependencies/<artifactid>`.  You
can then reference those schemas from yours, and they schemas and final classes will be included in the final jar/tar.

## Versioning

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
