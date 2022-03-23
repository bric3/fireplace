import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

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
}

repositories {
    mavenCentral()

}

dependencies {
    implementation("org.eclipse.platform:org.eclipse.swt.cocoa.macosx.x86_64:3.119.0") {
        exclude(group = "org.eclipse.platform", module = "org.eclipse.swt")
    }

    implementation(projects.fireplaceSwing)
    implementation(libs.flightrecorder)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

application {
    mainClass.set("com.github.bric3.fireplace.FirePlaceSwtMain")
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

    val os: OperatingSystem = DefaultNativePlatform.getCurrentOperatingSystem()
    if (os.isMacOsX) {
        jvmArgs("-XstartOnFirstThread")
    }

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
