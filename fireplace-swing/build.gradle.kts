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
    id("fireplace.published-java-library")
}

description = "Flamegraph or iciclegraph swing component"

dependencies {
    testImplementation(libs.bundles.batik)
    testImplementation(libs.bundles.mockito)
}

tasks {
    withType(Javadoc::class) {
        options.overview = "src/main/javadoc/overview.html"
        (options as StandardJavadocDocletOptions).linkSource(true)
    }
}
