plugins {
    alias(libs.plugins.indra)
    alias(libs.plugins.indra.licenser.spotless)
    alias(libs.plugins.paperweight)
    alias(libs.plugins.runpaper)
}

group = "org.geysermc.globallinkserver"

dependencies {
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")

    compileOnly(libs.floodgate.api)
    implementation(libs.mariadb.client)
    implementation(libs.bundles.fastutil)

    compileOnly(libs.checker.qual)
}

indra {
    github("GeyserMC", "GlobalLinkServer") {
        ci(true)
        issues(true)
        scm(true)
    }

    mitLicense()

    javaVersions {
        target(21)
    }

    spotless {
        java {
            // Broken until paperweight updates to a newer version of spotless (6.23.3+)
//            palantirJavaFormat()
            formatAnnotations()
        }
        ratchetFrom("origin/master")
    }
}

repositories {
    mavenLocal()

    maven("https://repo.opencollab.dev/main")
    maven("https://repo.papermc.io/repository/maven-public/")

    mavenCentral()

    maven("https://jitpack.io") {
        content { includeGroupByRegex("com\\.github\\..*") }
    }
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

    archiveBaseName = "GlobalLinkPlugin"
    archiveVersion = ""
    archiveClassifier = ""
}