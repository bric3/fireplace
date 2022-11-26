/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
description = "Flamegraph or iciclegraph swing component"

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
                implementation.add(libs.assertj)
                implementation.bundle(libs.bundles.batik)
            }
        }

        withType(JvmTestSuite::class) {
            targets.configureEach {
                testTask.configure {
                    systemProperty("gradle.test.suite.report.location", reports.html.outputLocation.get().asFile)

                    testLogging {
                        showStackTraces = true
                        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

                        events = setOf(
                            org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
                            org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR,
                            org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
                        )
                    }
                }
            }
        }
    }
}
