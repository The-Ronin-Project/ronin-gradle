# ronin-gradle-catalog

This produces a catalog of the plugins produced by this project.  It imports the project's main [libs.versions.toml](../gradle/libs.versions.toml), so the output will include any
inputs from that file.  Then it extracts from each subproject the appropriate gradle plugin identifier or library, and puts them in the output.
