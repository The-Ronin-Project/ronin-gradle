# Gradle Json Schema plugin

Configures a project to generate Java classes with JSR 305 annotations from one or more json schemas in nexus

```kotlin
jsonSchema {
    schema(name = "emr-patient", version = "v1", tag = "20221115184349-274b84e0dd6883cb200b6b75c62bd6867a3f594a")
    schema(name = "emr-appointment", version = "v1", url = "git url", branch = "main")
}
```
