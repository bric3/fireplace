/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("application")
    id("com.github.johnrengelman.shadow") version "7.1.2"

    // Playing with graal compiler
    id("org.graalvm.plugin.compiler") version "0.1.0-alpha2"

    id("com.github.hierynomus.license") version "0.16.1"
}

group = "com.github.bric3.fireplace"

repositories {
    mavenCentral()
}

dependencies {
    implementation(projects.fireplaceSwing)
    implementation(libs.bundles.flatlaf)
    implementation(libs.darklaf)
    implementation(libs.flightrecorder)

//    implementation(libs.graal.sdk)
    implementation(libs.bundles.graal.js)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

application {
    mainClass.set("com.github.bric3.fireplace.FirePlaceMain")
}

tasks.test {
    useJUnitPlatform()
}

// Due to https://github.com/gradle/gradle/issues/18426, tasks are not declared in the TaskContainerScope
tasks.withType<JavaExec>().configureEach {
    group = "class-with-main"
    classpath(sourceSets.main.get().runtimeClasspath)

    // Need to set the toolchain https://github.com/gradle/gradle/issues/16791
    // javaLauncher.set(javaToolchains.launcherFor(java.toolchain))  // Project toolchain

    projectDir.resolve(properties("hotswap-agent-location")).let {
        if (it.exists() && properties("dcevm-enabled").toBoolean()) {
            // DCEVM
            jvmArgs(
                "-XX:+IgnoreUnrecognizedVMOptions",
                "-XX:+AllowEnhancedClassRedefinition",
                "-XX:HotswapAgent=external",
                "-javaagent:$it"
            )
        }
    }
}

graal {
    version = libs.versions.graalvm.get()
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
