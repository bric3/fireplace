/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

plugins {
    `java-library`
    id("fireplace.tests")
    id("biz.aQute.bnd.builder")
    id("fireplace.semver")
}

val libJavaVersion = 11

repositories {
    mavenCentral()
}

dependencies {
    compileOnlyApi(libs.jetbrains.annotations)
}

configure<JavaPluginExtension> {
    withJavadocJar()
    withSourcesJar()
}

val licenseSpec = copySpec {
    from("${project.rootDir}/LICENSE")
}


tasks {
    withType(JavaCompile::class) {
        options.encoding = "UTF-8"
        options.release.set(libJavaVersion)
    }
    
    withType(Jar::class) {
        metaInf.with(licenseSpec)
    }

    named<Jar>("jar") {
        // Sets OSGi bundle attributes
        // See https://en.wikipedia.org/wiki/OSGi#Bundles for a minimal introduction to the bundle manifest
        // See https://enroute.osgi.org/FAQ/520-bnd.html for a high level of what is the "bnd" tool
        // If we ever expose any shaded classes, then the bundle info will need to be added after the shadow step.
        // For now, though, generating the bundle info here results
        bundle {
            val version by archiveVersion
            bnd(providers.provider { "Bundle-Name=${project.name}" })
            bnd(providers.provider { "Bundle-Description=${project.description}" })
            bnd(
                mapOf(
                    "Bundle-License" to "https://www.mozilla.org/en-US/MPL/2.0/",
                    "-exportcontents" to listOf(
                        "!io.github.bric3.fireplace.internal.*",
                        "io.github.bric3.fireplace.*",
                    ).joinToString(";"),
                    "-removeheaders" to "Created-By"
                )
            )
        }

        manifest.attributes(
            "Implementation-Title" to providers.provider { project.name },
            "Implementation-Version" to providers.provider { project.version },
            "Automatic-Module-Name" to providers.provider { project.name.replace('-', '.') },
            "Created-By" to "${providers.systemProperty("java.version").get()} (${providers.systemProperty("java.specification.vendor").get()})",
        )
    }
}
