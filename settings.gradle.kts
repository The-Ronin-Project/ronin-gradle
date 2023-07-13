rootProject.name = "ronin-gradle"

include(":gradle-plugins:ronin-contract-json-plugin")
include(":gradle-plugins:ronin-contract-openapi-plugin")
include(":gradle-plugins:ronin-json-schema-plugin")
include(":gradle-plugins:ronin-openapi-plugin")
include(":gradle-plugins:ronin-local-ct-plugin")
include(":gradle-plugins:ronin-spring-plugin")

include(":shared-libraries:gradle-testkit-utilities")

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
