# Ronin Build Conventions Versioning Plugin

Sets up a module to produce git tag-based semantic version to a project.  It should be applied at the root of the project

## Usage

In a module you want to be a spring/liquibase DB module:

```
root-project
├── build.gradle.kts
│   
```

In the root build.gradle.kts, assuming your project follows the instructions in [README.md](../../README.md), place the following code:

```kotlin
plugins {
    alias(roningradle.plugins.buildconventions.versioning)
}
```

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
