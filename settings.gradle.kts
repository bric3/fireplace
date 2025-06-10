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
    id("com.gradle.develocity") version "4.0.1"
    id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
}

rootProject.name = "fireplace"
includeBuild("build-logic")
include(
    "fireplace-swing",
    "fireplace-swing-animation",
    "fireplace-app",
    "fireplace-swt-awt-bridge",
    "fireplace-swt-experiment-app",
)
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
        // TODO: workaround for https://github.com/gradle/gradle/issues/22879.
        val isCI = providers.environmentVariable("CI").isPresent
        publishing.onlyIf { isCI }
        tag("CI")

        if (providers.environmentVariable("GITHUB_ACTIONS").isPresent) {
            link("GitHub Repository", "https://github.com/" + System.getenv("GITHUB_REPOSITORY"))
            link(
                "GitHub Commit",
                "https://github.com/" + System.getenv("GITHUB_REPOSITORY") + "/commits/" + System.getenv("GITHUB_SHA")
            )


            listOf(
                "GITHUB_ACTION_REPOSITORY",
                "GITHUB_EVENT_NAME",
                "GITHUB_ACTOR",
                "GITHUB_BASE_REF",
                "GITHUB_HEAD_REF",
                "GITHUB_JOB",
                "GITHUB_REF",
                "GITHUB_REF_NAME",
                "GITHUB_REPOSITORY",
                "GITHUB_RUN_ID",
                "GITHUB_RUN_NUMBER",
                "GITHUB_SHA",
                "GITHUB_WORKFLOW"
            ).forEach { e ->
                val v = System.getenv(e)
                if (v != null) {
                    value(e, v)
                }
            }

            providers.environmentVariable("GITHUB_SERVER_URL").orNull?.let { ghUrl ->
                val ghRepo = System.getenv("GITHUB_REPOSITORY")
                val ghRunId = System.getenv("GITHUB_RUN_ID")
                link("Summary", "$ghUrl/$ghRepo/actions/runs/$ghRunId")
                link("PRs", "$ghUrl/$ghRepo/pulls")

                // see .github/workflows/build.yaml
                providers.environmentVariable("GITHUB_PR_NUMBER")
                    .orNull
                    .takeUnless { it.isNullOrBlank() }
                    .let { prNumber ->
                        link("PR", "$ghUrl/$ghRepo/pulls/$prNumber")
                    }
            }
        }
    }
}