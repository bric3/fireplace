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
    id("fireplace.application")
    id("fireplace.licence-report")
    // fork of com.github.johnrengelman.shadow with support for recent versions of Java and Gradle
    // https://github.com/johnrengelman/shadow/pull/876
    // https://github.com/johnrengelman/shadow/issues/908
    id("io.github.goooler.shadow") version "8.1.8"
    kotlin("jvm") version "2.0.21"
}

description = "Opens a JFR file to inspect its content."

dependencies {
    implementation(libs.jetbrains.annotations)
    implementation(projects.fireplaceSwing)
    implementation(projects.fireplaceSwingAnimation)
    implementation(libs.classgraph)
    implementation(libs.bundles.flatlaf)
    implementation(libs.bundles.darklaf)
    implementation(libs.flightrecorder)
    implementation(libs.bundles.kotlinx.coroutines)
}

application {
    mainClass.set("io.github.bric3.fireplace.FireplaceMain")
}

tasks.shadowJar {
    archiveClassifier.set("shaded")
    dependsOn(tasks.generateLicenseReport)
    from(tasks.generateLicenseReport.map { it.outputFolder }) {
        include("*.md")
        include("*.txt")
    }
    // TODO relocation ?
    // val newLocation = "io.github.bric3.fireplace.shaded_.do_not_use"
    // relocate("kotlin", "$newLocation.kotlin")
    // relocate("kotlinx", "$newLocation.kotlinx")
    // relocate("org.jetbrains", "$newLocation.org.jetbrains")
    // relocate("org.intellij", "$newLocation.org.intellij")
    dependencies {
        // Remove all Kotlin metadata so that it looks like an ordinary Java Jar
        exclude("**/*.kotlin_metadata")
        exclude("**/*.kotlin_module")
        exclude("**/*.kotlin_builtins")
        // Eliminate dependencies' pom files
        exclude("**/pom.*")
    }
}