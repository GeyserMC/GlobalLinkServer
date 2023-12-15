@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenLocal()

        maven("https://repo.opencollab.dev/main")

        mavenCentral()

        maven("https://jitpack.io") {
            content { includeGroupByRegex("com\\.github\\..*") }
        }

        // Remove when adventure snapshot is no longer used
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}

rootProject.name = "GlobalLinkServer"
