# contract project name here

This project holds json contract (e.g. events, messaging, json storage, redis cache, etc.).  See the `ronin-contract-json-tooling` repository for more information.

To use this plugin, include the following in your plugins section of the Gradle build file:

## Layout

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

# Dependencies

You may use other schema files as dependencies of this one.  In your `build.gradle.kts` file, you can do something like the following:

```kotlin
dependencies {
    schemaDependency("com.projectronin.contract.json:<some artifact id>-v<some major version>:<some version>:schemas@tar.gz")
}
```

When you do this, and then run `./gradlew downloadSchemaDependencies`, the named dependencies are downloaded and unzipped under `src/main/resources/schemas/.dependencies/<artifactid>`.  You
can then reference those schemas from yours, and they schemas and final classes will be included in the final jar/tar.
