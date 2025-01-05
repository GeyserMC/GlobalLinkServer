plugins {
    alias(libs.plugins.indra)
    alias(libs.plugins.indra.licenser.spotless)
}

group = "org.geysermc.globallinkserver"

dependencies {
    implementation(libs.gson) // newer version required for record support

    implementation(libs.bundles.fastutil)

    compileOnly(libs.spigot.api)
    compileOnly(libs.geyser.api)

    implementation(libs.mariadb.client)

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
        target(17)
    }

    spotless {
        java {
            palantirJavaFormat()
            formatAnnotations()
        }
        ratchetFrom("origin/master")
    }
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

    archiveBaseName = "GlobalLinkServer"
    archiveVersion = ""
    archiveClassifier = ""
}