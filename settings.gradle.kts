rootProject.name = "ronin-gradle"

include(":shared-libraries:gradle-testkit-utilities")
include(":shared-libraries:gradle-helpers")
include(":shared-libraries:openapi-processor")
include(":shared-libraries:database-test-helpers")

include(":gradle-plugins:ronin-contract-json-plugin")
include(":gradle-plugins:ronin-contract-openapi-plugin")
include(":gradle-plugins:ronin-build-conventions-kotlin")
include(":gradle-plugins:ronin-build-conventions-library")
include(":gradle-plugins:ronin-build-conventions-spring-service")
include(":gradle-plugins:ronin-build-conventions-spring-database")
include(":gradle-plugins:ronin-build-conventions-root")
include(":gradle-plugins:ronin-build-conventions-catalog")
include(":gradle-plugins:ronin-build-conventions-versioning")
include(":gradle-plugins:ronin-build-conventions-publishing")
include(":gradle-plugins:ronin-build-conventions-gradle")
include(":gradle-plugins:ronin-json-schema-plugin")
include(":gradle-plugins:ronin-openapi-plugin")
include(":gradle-plugins:ronin-local-ct-plugin")

include(":catalogs:ronin-gradle-catalog")

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
}
