val meaningfulSubProjects = rootProject.subprojects.filter {
    it.buildFile.exists() && it != project
}

meaningfulSubProjects.forEach { evaluationDependsOn(it.path) }

plugins {
    `version-catalog`
    base
}

fun VersionCatalogBuilder.extractPlugins(versionRef: String, currentProject: Project) {
    (currentProject.extensions.findByType(GradlePluginDevelopmentExtension::class.java)?.plugins?.toList() ?: emptyList())
        .forEach { plugin ->
            val pluginId = plugin.id
            val catalogName = pluginId.replace("com.projectronin.", "").sanitizeName()
            plugin(catalogName, pluginId).versionRef(versionRef)
        }
}

fun String.sanitizeName(): String = replace("[^a-zA-z]+".toRegex(), "-")

catalog {
    versionCatalog {
        // This whole mess tries to supplement the TOML file by adding _this project's_ version to it dynamically,
        // and by recursing the project structure and declaring libraries for each module.
        version("ronin-gradle", project.version.toString())

        val gradlePluginSubprojects = meaningfulSubProjects.filter { it.extensions.findByName("gradlePlugin") != null }
        val librarySubprojects = meaningfulSubProjects - gradlePluginSubprojects

        gradlePluginSubprojects
            .forEach { extractPlugins("ronin-gradle", it) }
        librarySubprojects
            .forEach {
                library(it.name, it.group.toString(), it.name).versionRef("ronin-gradle")
            }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["versionCatalog"])
        }
    }
}
