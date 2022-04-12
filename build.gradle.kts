/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
import me.qoomon.gitversioning.commons.GitRefType

fun properties(key: String, defaultValue: Any? = null) = (project.findProperty(key) ?: defaultValue).toString()

plugins {
    id("com.github.hierynomus.license") version "0.16.1"
    id("me.qoomon.git-versioning") version "5.1.5"
    `maven-publish`
}

allprojects {
    group = "io.github.bric3.fireplace"
}


// Doc : https://github.com/qoomon/gradle-git-versioning-plugin
gitVersioning.apply {
    val dirty = if (!properties("version.showDirtiness", "true").toBoolean()) "" else "\${dirty}"
    refs {
        considerTagsOnBranches = true
        tag("v(?<tagVersion>[0-9].*)") {
            version = "\${ref.tagVersion}${dirty}"
        }
        branch("master") {
            version = "${project.version}${dirty}"
        }
        branch(".+") {
            version = "\${ref}-\${commit.short}${dirty}"
        }
    }

    rev {
        version = "\${commit.short}${dirty}"
    }
}

val isSnapshot =
    version.toString().endsWith("SNAPSHOT")
            || properties("version.forceSnapshot").toBoolean()
            || (!properties("version.forceRelease").toBoolean()
            && gitVersioning.gitVersionDetails.refType != GitRefType.TAG)

tasks.create("v") {
    doLast {
        println("Version : ${project.version}, snapshot : ${isSnapshot}")
    }
}

val fireplaceModules = subprojects.filter { it.name != projects.fireplaceApp.name }
configure(fireplaceModules) {
    apply(plugin = "java-library") // needed to get the java component
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    // configure<JavaPluginExtension> {
    //     withSourcesJar()
    // }

    val licenseSpec = copySpec {
        from("${project.rootDir}/LICENSE")
    }

    tasks {
        withType(Jar::class) {
            metaInf.with(licenseSpec)
        }

        // todo replace by java.withSourcesJar()
        val sourcesJar by registering(Jar::class) {
            archiveClassifier.set("sources")
            from(
                // take extension from project instance
                project.the<SourceSetContainer>().named("main").get().allJava
            )
        }

        val jar = named<Jar>("jar") {
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
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                artifact(tasks["sourcesJar"])
                groupId = project.group.toString()
                artifactId = project.name
                // OSSRH enforces the `-SNAPSHOT` suffix on snapshot repository
                // https://central.sonatype.org/faq/400-error/#question
                version = when {
                    isSnapshot -> project.version.toString().replace("-(DIRTY)", "")
                    else -> project.version.toString()
                }
                project.extra["publishingVersion"] = version

                afterEvaluate {
                    description = project.description
                }

                val gitRepo = "https://github.com/bric3/fireplace"
                pom {
                    url.set(gitRepo)

                    scm {
                        connection.set("scm:git:${gitRepo}.git")
                        developerConnection.set("scm:git:${gitRepo}.git")
                        url.set(gitRepo)
                    }
                    issueManagement {
                        system.set("GitHub")
                        url.set("https://github.com/spring-projects/spring-framework/issues")
                    }

                    licenses {
                        license {
                            distribution.set("repo")
                            name.set("Mozilla Public License Version 2.0")
                            url.set("https://www.mozilla.org/en-US/MPL/2.0/")
                        }
                    }
                }
            }
        }
        repositories {
            maven {
                if (properties("publish.central").toBoolean()) {
                    val isGithubRelease = System.getenv("GITHUB_EVENT_NAME").equals("release", true)
                    name = "central"
                    url = uri(when {
                                  isGithubRelease && !isSnapshot -> "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                                  else -> "https://s01.oss.sonatype.org/content/repositories/snapshots"
                              })
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
