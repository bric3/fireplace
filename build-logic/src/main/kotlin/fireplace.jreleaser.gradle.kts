/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import org.jreleaser.model.Active
import org.jreleaser.model.Http
import org.jreleaser.model.api.deploy.maven.MavenCentralMavenDeployer

plugins {
    // jreleaser needs the `clean` task to be registered https://github.com/jreleaser/jreleaser/issues/1770
    base
    id("org.jreleaser")
}

jreleaser {
    dryrun = true

    gitRootSearch = true // search for .git in parent directories, important for multi-module projects
    project {
        name.set("Fireplace")
        authors.add("bric3")
        description = "A Java library to render Flamegraph / Iciclegraph in Swing applications"
        license = "MPL-2.0"
        links {
            homepage = "https://github.com/bric3/fireplace"
            bugTracker = "https://github.com/bric3/fireplace/issues"
        }
        inceptionYear = "2021"
    }

    // Note if applied on the root project, need to look to all subprojects for published artifacts
    files {
        subprojects.forEach {
            glob {
                pattern = it.layout.buildDirectory.dir("libs").get()
                    .asFile.absolutePath + "/**.jar"
            }
        }
    }

    release {
        // https://jreleaser.org/guide/latest/reference/release/github.html
        github {
            enabled = false
            skipRelease = true
            skipTag = true
            commitAuthor {
                name = "Brice Dutheil"
                email = "brice.dutheil@gmail.com"
            }
            repoOwner = "bric3"
        }
    }

    signing {
        active = Active.ALWAYS
        armored = true
        // TODO verify.set(false) // requires the GPG public key to be set up
    }

    deploy {
        maven {
            // https://jreleaser.org/guide/latest/reference/deploy/maven/maven-central.html
            mavenCentral {
                register("sonatype") {
                    active = Active.RELEASE_PRERELEASE
                    stage.set(MavenCentralMavenDeployer.Stage.FULL)
                    applyMavenCentralRules = true
                    authorization = Http.Authorization.BEARER

                    // Note if applied on the root project, need to look to all subprojects for published artifacts
                    subprojects.forEach {
                        stagingRepository(
                            it.layout.buildDirectory.dir("staging-deploy").get()
                                .asFile.absolutePath
                        )
                    }
                }
            }
        }
    }
}
