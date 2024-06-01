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
    application
    id("fireplace.tests")
    id("org.jetbrains.kotlin.jvm")
}

val javaVersion = 22
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

kotlin {
    jvmToolchain(javaVersion)
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

tasks.withType(JavaCompile::class) {
    options.compilerArgs.addAll(arrayOf("-Xlint"))
    options.release.set(javaVersion)
}

// Due to https://github.com/gradle/gradle/issues/18426, tasks are not declared in the TaskContainerScope
tasks.withType<JavaExec>().configureEach {
    group = "class-with-main"
    classpath(sourceSets.main.get().runtimeClasspath)

    // Need to set the toolchain https://github.com/gradle/gradle/issues/16791
    javaLauncher.set(javaToolchains.launcherFor(java.toolchain))

    jvmArgs(
        "-ea",
        "-XX:+UnlockDiagnosticVMOptions",
        "-XX:+DebugNonSafepoints",
        "-XX:NativeMemoryTracking=summary",
    )

    projectDir.resolve(properties("hotswap-agent-location")).let {
        if (it.exists() && properties("dcevm-enabled").toBoolean()) {
            // DCEVM
            jvmArgs(
                "-XX:+AllowEnhancedClassRedefinition",
                "-XX:HotswapAgent=external",
                "-javaagent:$it"
            )
        }
    }
}

tasks.jar {
    manifest.attributes(
        "Implementation-Title" to project.name,
        "Implementation-Version" to project.version,
        "Automatic-Module-Name" to project.name.replace('-', '.'),
        "Created-By" to "${providers.systemProperty("java.version").get()} (${providers.systemProperty("java.specification.vendor").get()})",
    )
}
