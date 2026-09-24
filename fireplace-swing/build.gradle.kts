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
    id("fireplace.tests-ui")
    `java-test-fixtures`
}

description = "Flamegraph or iciclegraph swing component"

dependencies {
    testFixturesApi(libs.junit.jupiter.api)
    testFixturesImplementation(libs.mockito.core)

    testImplementation(libs.bundles.batik)
    testImplementation(libs.bundles.mockito)
}

testing.suites.named<JvmTestSuite>("testUi") {
    dependencies {
        implementation(testFixtures(project()))
    }
}

// java-test-fixtures publishes variants by default; keep these internal to this build.
components.named<AdhocComponentWithVariants>("java") {
    withVariantsFromConfiguration(configurations.named<ConsumableConfiguration>("testFixturesApiElements")) { skip() }
    withVariantsFromConfiguration(configurations.named<ConsumableConfiguration>("testFixturesRuntimeElements")) { skip() }
}

tasks {
    named<Test>("test") {
        systemProperty("java.awt.headless", "true")
    }

    named<Test>("testUi") {
        systemProperty("java.awt.headless", "false")
        systemProperty("junit.jupiter.execution.parallel.enabled", "false")
        // CompletableFuture otherwise uses untracked per-task threads on small CI runners.
        systemProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "2")
        maxParallelForks = 1
        forkEvery = 1
        failFast = true
    }

    withType(Javadoc::class) {
        options.overview = "src/main/javadoc/overview.html"
        (options as StandardJavadocDocletOptions).linkSource(true)
    }
}
