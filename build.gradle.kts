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
    id("java")
    id("pl.allegro.tech.build.axion-release") version "1.13.6"

    //    id("org.scm-manager.license") version "0.7.1"
    id("com.github.hierynomus.license") version "0.16.1"
}

group = "com.github.bric3"
version = scmVersion.version

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.formdev:flatlaf:2.0-rc1")
    implementation("org.openjdk.jmc:flightrecorder:8.1.0")

    testImplementation ("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly ("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
}

// Due to https://github.com/gradle/gradle/issues/18426, tasks are not declared in the TaskContainerScope
tasks.withType<JavaExec>().configureEach {
    group = "class-with-main"
    classpath(sourceSets.main.get().runtimeClasspath)

    // Need to set the toolchain https://github.com/gradle/gradle/issues/16791
    javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
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
            "java.template" to "SLASHSTAR_STYLE",
            "kt" to "SLASHSTAR_STYLE",
            "kts" to "SLASHSTAR_STYLE",
            "yaml" to "SCRIPT_STYLE",
            "yml" to "SCRIPT_STYLE",
            "svg" to "XML_STYLE",
            "md" to "XML_STYLE"
        )
    )
}
tasks.register("licenseCheckForKotlin", com.hierynomus.gradle.license.tasks.LicenseCheck::class) {
    source = fileTree(project.projectDir) {
        include("**/*.kt", "**/*.kts")
        exclude("**/buildSrc/build/generated-sources/**")
    }
}
tasks["license"].dependsOn("licenseCheckForKotlin")
tasks.register("licenseFormatForKotlin", com.hierynomus.gradle.license.tasks.LicenseFormat::class) {
    source = fileTree(project.projectDir) {
        include("**/*.kt", "**/*.kts")
        exclude("**/buildSrc/build/generated-sources/**")
    }
}
tasks["licenseFormat"].dependsOn("licenseFormatForKotlin")
