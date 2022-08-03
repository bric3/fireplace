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
    `jvm-test-suite`
}

tasks {
    withType(JavaCompile::class) {
        options.release.set(11)
    }

    withType(Javadoc::class) {
        options.overview = "src/main/javadoc/overview.html"
        (options as StandardJavadocDocletOptions).linkSource(true)
    }
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter(libs.versions.junit.jupiter.get())
            dependencies {
                implementation(libs.assertj)
                implementation(libs.bundles.batik)
            }
        }
    }
}
