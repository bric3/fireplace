/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
import com.adarshr.gradle.testlogger.theme.ThemeType

plugins {
    `jvm-test-suite`
    id("com.adarshr.test-logger")
}

testlogger {
    theme = ThemeType.MOCHA_PARALLEL
    isShowPassed = false
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter(libs.versions.junit.jupiter)
            dependencies {
                implementation.add(libs.assertj)
            }
        }

        withType(JvmTestSuite::class) {
            targets.configureEach {
                testTask.configure {
                    systemProperty("gradle.test.suite.report.location", reports.html.outputLocation.get().asFile)

                    // Note: Replaced by testLogger
                    // testLogging {
                    //     showStackTraces = true
                    //     exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                    //
                    //     events = setOf(
                    //         org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
                    //         org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR,
                    //         org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
                    //     )
                    // }
                }
            }
        }
    }
}
