# OpenApiKotlinGenerator

A simple OpenAPI (3.0) spec to kotlin generator plugin.  Should generate clean and functional kotlin code with
`Jackson` and `javax.validation` annotations, and spring controller interfaces.

All you _should_ need to do to your project is, in `build.gradle.kts`, add:

```kotlin
import OpenApiKotlinGeneratorExtension
import OpenApiKotlinGeneratorInputSpec

// any other stuff

plugins {
    // other plugins
    alias(libs.plugins.product.openapi)
}

// other stuff defining a standard spring boot mvc project

configure<OpenApiKotlinGeneratorExtension> {
    schemas.add(
        extensions.create("api-name-of-some-sort", OpenApiKotlinGeneratorInputSpec::class.java).apply {
            packageName.set("com.projectronin.services.apiname.api.v1")
            inputDependency.set("com.projectronin.rest.contract:contract-rest-questionnaire:1.0.0")
            finalResourcePath.set("v1/questionnaire.json")
        }
    )
}

// etc
```

The dependency has to be real, at least in your local maven repository.  The `finalResourcePath` is the path the downloaded dependency ends up under `META-INF/resources` in your jar.
This dependency mechanism assumes a file created by [this tooling](https://github.com/projectronin/ronin-contract-rest-tooling).

For local files you can do:

```kotlin
import OpenApiKotlinGeneratorExtension
import OpenApiKotlinGeneratorInputSpec

// any other stuff

plugins {
    // other plugins
    alias(libs.plugins.product.openapi)
}

// other stuff defining a standard spring boot mvc project

configure<OpenApiKotlinGeneratorExtension> {
    schemas.add(
        extensions.create("api-name-of-some-sort", OpenApiKotlinGeneratorInputSpec::class.java).apply {
            packageName.set("com.projectronin.services.apiname.api.v1")
            inputFile.set(layout.projectDirectory.file("src/main/resources/META-INF/resources/v1/apiname.yml"))
        }
    )
}

// etc
```

Of course, the referenced files have to exist.  You should be able to generate multiple APIs, as:

```kotlin
configure<OpenApiKotlinGeneratorExtension> {
    schemas.add(
        extensions.create("api-v1", OpenApiKotlinGeneratorInputSpec::class.java).apply {
            packageName.set("com.projectronin.services.apiname.api.v1")
            inputFile.set(layout.projectDirectory.file("src/main/resources/META-INF/resources/v1/apiname.yml"))
        }
    )
    schemas.add(
        extensions.create("api-v2", OpenApiKotlinGeneratorInputSpec::class.java).apply {
            packageName.set("com.projectronin.services.apiname.api.v2")
            inputFile.set(layout.projectDirectory.file("src/main/resources/META-INF/resources/v2/apiname.yml"))
        }
    )
}
```

Task outputs should be automatically included in your kotlin compile.
