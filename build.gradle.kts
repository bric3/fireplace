/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
import org.gradle.api.logging.configuration.ConsoleOutput

plugins {
    id("com.github.hierynomus.license") version "0.16.1"
    id("fireplace.semver")
    // id("fireplace.central-publication")
}

allprojects {
    group = "io.github.bric3.fireplace"
}

dependencies {
    // published modules
    nmcpAggregation(projects.fireplaceSwing)
    nmcpAggregation(projects.fireplaceSwingAnimation)
    nmcpAggregation(projects.fireplaceSwtAwtBridge)
}

license {
    ext["year"] = "2021, Today"
    ext["name"] = "Brice Dutheil"
    header = project.file("HEADER")

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

tasks {
    register("v") {
        description = "Print version"
        doLast {
            println(project.version.toString())
        }
    }

    val licenseCheckForProjectFiles =
        register("licenseCheckForProjectFiles", com.hierynomus.gradle.license.tasks.LicenseCheck::class) {
            description = "Check licenses on Kotlin and TOML files"
            source = fileTree(project.projectDir) {
                include("**/*.kt", "**/*.kts")
                include("**/*.toml")
                exclude("**/buildSrc/build/generated-sources/**")
            }
        }
    named("license") { dependsOn(licenseCheckForProjectFiles) }

    val licenseFormatForProjectFiles =
        register("licenseFormatForProjectFiles", com.hierynomus.gradle.license.tasks.LicenseFormat::class) {
            description = "Apply licences on Kotlin and TOML files"
            source = fileTree(project.projectDir) {
                include("**/*.kt", "**/*.kts")
                include("**/*.toml")
                exclude("**/buildSrc/build/generated-sources/**")
            }
        }
    named("licenseFormat") { dependsOn(licenseFormatForProjectFiles) }

    val jmcClonePath = providers.gradleProperty("local.jmc.clone.path")
    val localPublicationTasks = provider { rootProject.getTasksByName("publishToMavenLocal", true) }

    register("deployToJmcSources") {
        group = "publishing"
        description = "Publishes Fireplace locally and updates a JMC source checkout"
        dependsOn(localPublicationTasks)

        doLast {
            // Infer published projects from their tasks instead of hard-coding module paths.
            val publicationTasks = localPublicationTasks.get()
            val publishedRuntimeClasspath = project.configurations.detachedConfiguration(
                *publicationTasks.map { publicationTask ->
                    project.dependencies.project(
                        mapOf(
                            "path" to publicationTask.project.path,
                            "configuration" to "runtimeElements",
                        )
                    )
                }.toTypedArray()
            )

            val jmcRoot = jmcClonePath.map { path ->
                file(path.replaceFirstChar { if (it == '~') System.getProperty("user.home") else it.toString() }).canonicalFile
            }.orNull
                ?: throw GradleException("Set the JMC clone path with -Plocal.jmc.clone.path=/path/to/jmc")
            if (!jmcRoot.isDirectory) throw GradleException("JMC clone not found: $jmcRoot")

            // Locate JMC files
            val pomFile = jmcRoot.resolve("releng/third-party/pom.xml")
            val targetFile = jmcRoot.resolve("releng/platform-definitions")
                .listFiles()
                ?.filter { it.isDirectory && it.name.matches(Regex("""platform-definition-\d{4}-\d{2}""")) }
                ?.map { it.resolve("${it.name}.target") }
                ?.filter { it.isFile }
                ?.maxByOrNull { it.name }
                ?: throw GradleException("No JMC platform target found in $jmcRoot")

            // Resolve runtime graph, including external transitive dependencies.
            val dependencyVersions = publishedRuntimeClasspath.incoming.resolutionResult.allComponents
                .asSequence()
                .mapNotNull { component ->
                    (component.id as? ModuleComponentIdentifier)?.let {
                        "${it.group}:${it.module}" to it.version
                    }
                }
                .toMap()
                .toMutableMap()
            publicationTasks
                .map { it.project }
                .distinct()
                .forEach { publishedProject ->
                    dependencyVersions["${publishedProject.group}:${publishedProject.name}"] =
                        publishedProject.version.toString()
                }

            val pomContent = pomFile.readText()
            val targetContent = targetFile.readText()
            // JMC artifact entries map Maven coordinates to version properties and target units.
            val jmcArtifacts = Regex(
                """(?s)<artifact>\s*<id>([^:]+):([^:]+):\$\{([^}]+)}</id>(.*?)</artifact>"""
            ).findAll(pomContent).associateBy {
                "${it.groupValues[1]}:${it.groupValues[2]}"
            }
            val pomVersions = mutableMapOf<String, String>()
            val targetVersions = mutableMapOf<String, String>()
            dependencyVersions.forEach { (coordinate, dependencyVersion) ->
                val artifact = jmcArtifacts[coordinate]
                if (artifact == null) {
                    logger.warn("Dependency $coordinate:$dependencyVersion is not declared in $pomFile")
                    return@forEach
                }
                val propertyName = artifact.groupValues[3]
                val unitId = Regex("""<Bundle-SymbolicName>([^<]+)</Bundle-SymbolicName>""")
                    .find(artifact.groupValues[4])
                    ?.groupValues
                    ?.get(1)
                    ?: artifact.groupValues[2]
                pomVersions[propertyName] = dependencyVersion
                if (Regex("""<unit id="${Regex.escape(unitId)}" version="[^"]+"/>""")
                        .containsMatchIn(targetContent)
                ) {
                    targetVersions[unitId] =
                        aQute.bnd.version.MavenVersion(dependencyVersion).osGiVersion.toString()
                } else {
                    logger.warn("Dependency $coordinate:$dependencyVersion has no unit $unitId in $targetFile")
                }
            }

            val updatedPom = pomContent.replaceExactly(
                source = pomFile,
                pattern = Regex(
                    """(<(${pomVersions.keys.joinToString("|") { Regex.escape(it) }})>)[^<]+(</\2>)"""
                ),
                expectedMatches = pomVersions.size,
            ) { "${it.groupValues[1]}${pomVersions.getValue(it.groupValues[2])}${it.groupValues[3]}" }
            val updatedTarget = targetContent.replaceExactly(
                source = targetFile,
                pattern = Regex(
                    """(<unit id="(${targetVersions.keys.joinToString("|") { Regex.escape(it) }})" version=")[^"]+("/>)"""
                ),
                expectedMatches = targetVersions.size,
            ) { "${it.groupValues[1]}${targetVersions.getValue(it.groupValues[2])}${it.groupValues[3]}" }

            pomFile.writeText(updatedPom)
            targetFile.writeText(updatedTarget)

            logger.lifecycle("Updated ${pomVersions.size} JMC version properties and ${targetVersions.size} target units.")
            logger.lifecycle("Changes in $jmcRoot:")
            logger.lifecycle(providers.exec {
                workingDir(jmcRoot)
                commandLine(
                    "git",
                    "--no-pager",
                    "diff",
                    "--no-ext-diff",
                    "--color=${if (gradle.startParameter.consoleOutput == ConsoleOutput.Plain) "never" else "always"}",
                )
            }.standardOutput.asText.get().trimEnd())

            logger.warn("Do not commit the local Fireplace or dependency versions in $pomFile or $targetFile.")
            logger.lifecycle(
                """
            Next, from $jmcRoot:
              mvn clean --file releng/third-party/pom.xml
              ./build.sh --packageJmc
              ./build.sh --run
            """.trimIndent()
            )
        }
    }
}

fun String.replaceExactly(
    source: File,
    pattern: Regex,
    expectedMatches: Int,
    replacement: (MatchResult) -> CharSequence,
): String {
    val matchCount = pattern.findAll(this).count()
    if (matchCount != expectedMatches) {
        throw GradleException("Expected $expectedMatches matches in $source, found $matchCount")
    }
    return pattern.replace(this, replacement)
}
