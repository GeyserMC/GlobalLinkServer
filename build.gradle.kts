plugins {
    application
    alias(libs.plugins.indra)
    alias(libs.plugins.indra.licenser.spotless)
}

group = "org.geysermc.globallinkserver"

dependencies {
    implementation(libs.gson) // newer version required for record support
    implementation(libs.fastutil.common)

    implementation(libs.bundles.protocol)
    implementation(libs.mcprotocollib) {
        exclude("io.netty", "netty-all")
    }

    // mcprotocollib won't work without this
    implementation(libs.netty.handler)

    implementation(libs.adventure.text.legacy)
    implementation(libs.mariadb.client)

    compileOnly(libs.checker.qual)
}

application {
    mainClass.set("org.geysermc.globallinkserver.GlobalLinkServer")
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

    archiveBaseName.set("GlobalLinkServer")
    archiveVersion.set("")
    archiveClassifier.set("")

    manifest {
        attributes["Main-Class"] = application.mainClass
    }
}