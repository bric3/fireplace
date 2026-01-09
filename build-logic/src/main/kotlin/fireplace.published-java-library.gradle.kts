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
    id("fireplace.java-library")
    id("fireplace.maven-publication")
    `maven-publish`
    signing
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            // Gradle feature variants can't be mapped to Maven's pom
            // suppressAllPomMetadataWarnings()

            // versionMapping {
            //     usage(Usage.JAVA_API) {
            //         fromResolutionResult()
            //     }
            //
            //     usage(Usage.JAVA_RUNTIME) {
            //         fromResolutionOf("runtimeClasspath")
            //     }
            // }
        }
    }
}