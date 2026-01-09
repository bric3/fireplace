/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    `java-library`
}

dependencies {
    implementation(libs.bundles.eclipse.swt)
}

// Configure the right SWT dependency for the current platform
// Gradle do not offer a way to resolve a "property" like ${property}, instead it is necessary
// to configure the dependency substitution.
val os: OperatingSystem = org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem()
val arch: String = providers.systemProperty("os.arch").get()
configurations.all {
    resolutionStrategy {
        dependencySubstitution {
            // Available SWT packages https://repo1.maven.org/maven2/org/eclipse/platform/
            val osId = when {
                os.isWindows -> "win32.win32"
                os.isLinux -> "gtk.linux"
                os.isMacOsX -> "cocoa.macosx"
                else -> throw GradleException("Unsupported OS: $os")
            }

            val archId = when (arch) {
                "x86_64", "amd64" -> "x86_64"
                "aarch64" -> "aarch64"
                else -> throw GradleException("Unsupported architecture: $arch")
            }

            // SWT versions <= 3.130.0 declared a transitive dependency using ${osgi.platform} placeholder
            // in the artifactId (e.g., org.eclipse.swt.${osgi.platform}). Gradle cannot resolve Maven
            // properties in coordinates, so we substitute it with the platform-specific artifact.
            substitute(module("org.eclipse.platform:org.eclipse.swt.\${osgi.platform}"))
                .using(module("org.eclipse.platform:org.eclipse.swt.$osId.$archId:${libs.versions.eclipse.swt.get()}"))
                .because("The maven property '\${osgi.platform}' that appear in the artifact coordinate is not handled by Gradle, it is required to replace the dependency")

            // Starting with SWT 3.131.0, Eclipse removed the ${osgi.platform} dependency and switched
            // to Maven profiles for platform selection. Since Gradle doesn't support Maven profiles,
            // the base org.eclipse.swt POM resolves to an empty artifact. We substitute it directly
            // with the platform-specific artifact.
            substitute(module("org.eclipse.platform:org.eclipse.swt"))
                .using(module("org.eclipse.platform:org.eclipse.swt.$osId.$archId:${libs.versions.eclipse.swt.get()}"))
                .because("The base SWT POM uses Maven profiles for platform selection which Gradle does not support, substitute with platform-specific artifact")
        }
    }
}

tasks.withType<JavaExec> {
    if (DefaultNativePlatform.getCurrentOperatingSystem().isMacOsX) {
        doFirst {
            logger.lifecycle("Added JVM argument -XstartOnFirstThread")
        }
        jvmArgs("-XstartOnFirstThread")
    }
}