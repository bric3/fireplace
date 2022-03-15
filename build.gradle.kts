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
    group = "com.github.bric3.fireplace"
}


// Doc : https://github.com/qoomon/gradle-git-versioning-plugin
gitVersioning.apply {
    val dirty = if (properties("showDirtiness", "true") != "true") "" else "\${dirty}"
    refs {
        considerTagsOnBranches = true
        tag("v(?<tagVersion>[0-9].*)") {
            version = "\${ref.tagVersion}${dirty}"
        }
        branch("master") {
            version = "\${commit.short}${dirty}"
        }
        branch(".+") {
            version = "\${ref}-\${commit.short}${dirty}"
        }
    }

    rev {
        version = "\${commit.short}${dirty}"
    }
}

val isSnapshot = gitVersioning.gitVersionDetails.refType != GitRefType.TAG

val fireplaceModules = subprojects.filter { it.name != projects.fireplaceApp.name }
configure(fireplaceModules) {
    apply(plugin = "java-library") // needed to get the java component
    apply(plugin = "maven-publish")

    println("Publishing ${name}")
    println("Publishing ${project.name}")

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

        val sourcesJar by registering(Jar::class) {
            archiveClassifier.set("sources")
            from(
                // take extension from project instance
                project.the<SourceSetContainer>().named("main").get().allJava
            )
        }
    }


    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                artifact(tasks["sourcesJar"])
                groupId = project.group.toString()
                artifactId = project.name

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
                url = uri("${rootProject.buildDir}/publishing-repository")
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
