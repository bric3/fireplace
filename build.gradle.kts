/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
fun properties(key: String, defaultValue: Any? = null) = (project.findProperty(key) ?: defaultValue).toString()

plugins {
    id("com.github.hierynomus.license") version "0.16.1"
    id("com.javiersc.semver.gradle.plugin") version "0.3.0-alpha.5"
    id("biz.aQute.bnd.builder") version "6.4.0" apply false
    `maven-publish`
}

allprojects {
    group = "io.github.bric3.fireplace"
}

fun isSnapshot(version: Any) = version.toString().endsWith("-SNAPSHOT") || version.toString().matches(Regex(".*\\.\\d+\\+[0-9a-f]+")) // .54+6a08d70

tasks.register("v") {
    doLast {
        println("Version : ${project.version}")
    }
}

val fireplaceModules = subprojects - project(":fireplace-app") - project(":fireplace-swt-experiment-app")
configure(fireplaceModules) {
    apply(plugin = "java-library") // needed to get the java component
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "biz.aQute.bnd.builder")

    repositories {
        mavenCentral()
    }

    configure<JavaPluginExtension> {
        withJavadocJar()
        withSourcesJar()
    }

    val licenseSpec = copySpec {
        from("${project.rootDir}/LICENSE")
    }

    tasks {
        withType(JavaCompile::class) {
            options.encoding = "UTF-8"
        }

        withType(Jar::class) {
            metaInf.with(licenseSpec)
        }

        val jar = named<Jar>("jar") {
            bundle {
                val version by archiveVersion
                bnd(
                    mapOf(
                        "Bundle-License" to "https://www.mozilla.org/en-US/MPL/2.0/",
                        "Bundle-Name" to project.name,
                        // "Bundle-Description" to project.description,
                        "-exportcontents" to listOf(
                            "!io.github.bric3.fireplace.internal.*",
                            "io.github.bric3.fireplace.*",
                        ).joinToString(";"),
                        "-removeheaders" to "Created-By"
                    )
                )
            }
            manifest.attributes(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Automatic-Module-Name" to project.name.replace('-', '.'),
                "Created-By" to "${System.getProperty("java.version")} (${System.getProperty("java.specification.vendor")})",
            )
        }

        named("publish") {
            doFirst {
                logger.lifecycle("Uploading version '${project.extra["publishingVersion"]}' to ${project.extra["publishingRepositoryUrl"]}")
            }
        }
    }

    // Doc https://docs.gradle.org/current/userguide/signing_plugin.html#sec:signatory_credentials
    // Details in https://github.com/bric3/fireplace/issues/25
    configure<SigningExtension> {
        setRequired({ gradle.taskGraph.hasTask("publish") })
        useInMemoryPgpKeys(findProperty("signingKey") as? String, findProperty("signingPassword") as? String)
        sign(publishing.publications)
    }

    // test run via (deploy on local repo 'build/publishing-repository')
    // ORG_GRADLE_PROJECT_signingKey=$(cat armoredKey) ORG_GRADLE_PROJECT_signingPassword=$(cat passphrase) ./gradlew publish --console=verbose
    publishing {
        publications {
            register<MavenPublication>("mavenJava") {
                from(components["java"])

                project.extra["publishingVersion"] = version

                val gitRepo = "https://github.com/bric3/fireplace"
                pom {
                    name.set(project.name)
                    url.set(gitRepo)

                    scm {
                        connection.set("scm:git:${gitRepo}.git")
                        developerConnection.set("scm:git:${gitRepo}.git")
                        url.set(gitRepo)
                    }

                    issueManagement {
                        system.set("GitHub")
                        url.set("https://github.com/bric3/fireplace/issues")
                    }

                    licenses {
                        license {
                            distribution.set("repo")
                            name.set("Mozilla Public License Version 2.0")
                            url.set("https://www.mozilla.org/en-US/MPL/2.0/")
                        }
                    }

                    developers {
                        developer {
                            id.set("bric3")
                            name.set("Brice Dutheil")
                            email.set("brice.dutheil@gmail.com")
                        }
                    }
                }
                // subprojects properties (like description) will be available
                // after they have been configured
                afterEvaluate {
                    pom {
                        description.set(project.description)
                    }
                }

            }
        }
        repositories {
            maven {
                if (properties("publish.central").toBoolean()) {
                    val isGithubRelease =
                        providers.environmentVariable("GITHUB_EVENT_NAME").get().equals("release", true)
                    name = "central"
                    url = uri(
                        when {
                            isGithubRelease && !isSnapshot(project.version) -> "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                            else -> "https://s01.oss.sonatype.org/content/repositories/snapshots"
                        }
                    )
                    credentials {
                        username = findProperty("ossrhUsername") as? String
                        password = findProperty("ossrhPassword") as? String
                    }
                } else {
                    name = "build-dir"
                    url = uri("${rootProject.buildDir}/publishing-repository")
                }
                project.extra["publishingRepositoryUrl"] = url
            }
        }
    }
}

// Configure the right SWT dependency for the current platform
// Gradle do not offer a way to resolve a "property" like ${property}, instead it is necessary
// to configure the dependency substitution.
val os: OperatingSystem = org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem()
val arch: String = providers.systemProperty("os.arch").get()
configure(listOf(project(":fireplace-swt-experiment-app"), project(":fireplace-swt-awt-bridge"))) {
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

                substitute(module("org.eclipse.platform:org.eclipse.swt.\${osgi.platform}"))
                    .using(module("org.eclipse.platform:org.eclipse.swt.$osId.$archId:${rootProject.libs.versions.swt.get()}"))
                    .because("The maven property '\${osgi.platform}' that appear in the artifact coordinate is not handled by Gradle, it is required to replace the dependency")
            }
        }
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
