/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    id("fireplace.tests")
}

testing {
    suites {
        withType(JvmTestSuite::class).matching { it.name != "testUi" }.configureEach {
            targets.configureEach {
                testTask.configure {
                    // A tagged native UI test must never leak into an ordinary test suite.
                    useJUnitPlatform {
                        excludeTags("ui")
                    }
                }
            }
        }

        register<JvmTestSuite>("testUi") {
            useJUnitJupiter(libs.versions.junit.jupiter)
            dependencies {
                implementation(project())
            }
            targets.configureEach {
                testTask.configure {
                    description = "Runs tests that require a native desktop UI."
                    // Print JUnit timeout dumps even if native code never returns.
                    testLogging.showStandardStreams = true
                    useJUnitPlatform {
                        includeTags("ui")
                    }
                    shouldRunAfter(tasks.named("test"))
                }
            }
        }
    }
}

// Custom suites do not automatically inherit the dependencies available to the built-in test suite.
configurations.named("testUiImplementation") {
    extendsFrom(configurations.named("testImplementation"))
}
configurations.named("testUiRuntimeOnly") {
    extendsFrom(configurations.named("testRuntimeOnly"))
}
