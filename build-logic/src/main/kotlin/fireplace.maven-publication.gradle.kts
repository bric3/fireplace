/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
import com.javiersc.gradle.version.GradleVersion
import com.javiersc.gradle.version.isSnapshot

plugins {
    `maven-publish`
    signing
}

publishing {
    publications {
        withType<MavenPublication>().configureEach {
            val gitRepo = providers.provider { "https://github.com/bric3/fireplace" }

            pom {
                name.set(artifactId)
                description.set(providers.provider { project.description })

                url.set(gitRepo)

                issueManagement {
                    system.set("Github")
                    url.set(gitRepo.map { "$it/issues" })
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

                scm {
                    connection.set(gitRepo.map { "scm:git:${it}.git" })
                    developerConnection.set(gitRepo.map { "scm:git:${it}.git" })
                    url.set(gitRepo)
                }
            }
        }
    }

    repositories {
        val isGithubRelease = providers.environmentVariable("GITHUB_JOB").orNull
            .equals("release-publish", true)

        val isSnapshot = providers.provider { GradleVersion(project.version.toString()).isSnapshot }

        val isPublishToCentral = providers.gradleProperty("publish.central").orNull.toBoolean()

        if (isPublishToCentral) {
            maven {
                name = "central"
                setUrl(isSnapshot.map {
                    if (isGithubRelease && !it) "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                    else "https://s01.oss.sonatype.org/content/repositories/snapshots"
                }.map(::uri))
                credentials {
                    username = properties("ossrhUsername")
                    password = properties("ossrhPassword")
                }
            }

            // if (isGithubRelease) {
            //     logger.lifecycle("Publishing to GitHubPackages")
            //     maven {
            //         name = "GitHubPackages"
            //         url = uri("https://maven.pkg.github.com/bric3/fireplace")
            //         credentials {
            //             username = providers.environmentVariable("GITHUB_USER").get()
            //             password = providers.environmentVariable("GITHUB_TOKEN").get()
            //         }
            //     }
            // }
        } else {
            maven {
                name = "build-dir"
                setUrl(rootProject.layout.buildDirectory.map { "$it/publishing-repository" }.zip(isSnapshot) { dir, isSnapshot ->
                    if (isSnapshot) "$dir/snapshots" else "$dir/releases"
                }.map(::uri))
            }
        }
    }
}

signing {
    setRequired({ gradle.taskGraph.hasTask("publish") })
    useInMemoryPgpKeys(
        // findProperty("signingKeyId") as? String,
        findProperty("signingKey") as? String,
        findProperty("signingPassword") as? String
    )
    sign(publishing.publications)
}

tasks {
    register("cleanLocalPublishingRepository") {
        doLast {
            rootProject.layout.buildDirectory.get().asFile.resolve("publishing-repository").deleteRecursively()
        }
    }

    withType<PublishToMavenRepository>().configureEach {
        doFirst {
            logger.lifecycle("Publishing version '${this@configureEach.publication.version}' to ${this@configureEach.repository.url}")
        }
    }

    withType<PublishToMavenLocal>().configureEach {
        doFirst {
            logger.lifecycle("Publishing version '${this@configureEach.publication.version}' locally")
        }
    }
}
