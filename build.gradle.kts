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
    id("com.github.hierynomus.license") version "0.16.1"
}

allprojects {
    group = "io.github.bric3.fireplace"
}

tasks.register("v") {
    doLast {
        println(project.version.toString())
    }
}

license {
    ext["year"] = "2021, Today"
    ext["name"] = "Brice Dutheil"
    header = file("HEADER")

    strictCheck = true
    ignoreFailures = false
    excludes(
        listOf(
            "**/*.java.template",
            "**/testData/*.java",
        )
    )

    mapping(
        mapOf(
            "java" to "SLASHSTAR_STYLE",
            "kt" to "SLASHSTAR_STYLE",
            "kts" to "SLASHSTAR_STYLE",
            "yaml" to "SCRIPT_STYLE",
            "yml" to "SCRIPT_STYLE",
            "svg" to "XML_STYLE",
            "md" to "XML_STYLE",
            "toml" to "SCRIPT_STYLE"
        )
    )
}
tasks.register("licenseCheckForProjectFiles", com.hierynomus.gradle.license.tasks.LicenseCheck::class) {
    source = fileTree(project.projectDir) {
        include("**/*.kt", "**/*.kts")
        include("**/*.toml")
        exclude("**/buildSrc/build/generated-sources/**")
    }
}
tasks["license"].dependsOn("licenseCheckForProjectFiles")
tasks.register("licenseFormatForProjectFiles", com.hierynomus.gradle.license.tasks.LicenseFormat::class) {
    source = fileTree(project.projectDir) {
        include("**/*.kt", "**/*.kts")
        include("**/*.toml")
        exclude("**/buildSrc/build/generated-sources/**")
    }
}
tasks["licenseFormat"].dependsOn("licenseFormatForProjectFiles")
