val rootSubprojects = project.rootProject.subprojects.filter { it.parent?.name == "gradle-plugins" }

val buildProjects: Configuration by configurations.creating {
    isVisible = false
    isCanBeResolved = true
    isCanBeConsumed = false
}

plugins {
    `version-catalog`
    base
}

dependencies {
    rootSubprojects.forEach { subProject -> buildProjects(project(":${subProject.path}")) }
}

fun extractPlugins(currentProject: Project): List<Pair<String, String>> {
    return fileTree(currentProject.buildDir)
        .apply {
            include("/pluginDescriptors/*.properties")
        }
        .files
        .map { propertiesFile ->
            val name = propertiesFile.name
            val pluginId = name.replace(".properties", "")
            val catalogName = pluginId.replace("com.projectronin.", "").replace(".", "-")
            Pair("ronin-$catalogName", pluginId)
        }
}

catalog {
    versionCatalog {
        from(files("${rootProject.projectDir}/gradle/libs.versions.toml"))
        // This whole mess tries to supplement the TOML file by adding _this project's_ version to it dynamically,
        // and by recursing the project structure and declaring libraries for each module.
        // TODO: library(currentProject.name, currentProject.group.toString(), currentProject.name).versionRef("product-common")
        version("ronin-gradle", project.version.toString())

        fun handleProject(currentProject: Project) {
            extractPlugins(currentProject)
                .forEach { pluginPair ->
                    plugin(pluginPair.first, pluginPair.second).versionRef("ronin-gradle")
                }
        }

        rootSubprojects
            .forEach { handleProject(it) }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["versionCatalog"])
        }
    }
}
