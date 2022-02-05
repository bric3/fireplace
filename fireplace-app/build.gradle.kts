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
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

dependencies {
    implementation(projects.fireplaceSwing)
    implementation(projects.fireplaceSwingAnimation)
    implementation(libs.bundles.flatlaf)
    // implementation(libs.bundles.darklaf)
    implementation("com.github.weisj:darklaf-platform-preferences:latest.integration")
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

tasks.jar {
    manifest.attributes(
        "Implementation-Title" to project.name,
        "Implementation-Version" to project.version,
        "Automatic-Module-Name" to project.name.replace('-', '.'),
        "Created-By" to "${System.getProperty("java.version")} (${System.getProperty("java.specification.vendor")})",
    )
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType(JavaCompile::class) {
    options.compilerArgs.addAll(arrayOf("-Xlint"))
    options.release.set(11)
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

