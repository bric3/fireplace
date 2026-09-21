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

    register("deployToJmcSources") {
        group = "publishing"
        description = "Publishes Fireplace locally and updates a JMC source checkout"
        dependsOn(provider { rootProject.getTasksByName("publishToMavenLocal", true) })

        doLast {
            val jmcRoot = jmcClonePath.map { path ->
                file(path.replaceFirstChar { if (it == '~') System.getProperty("user.home") else it.toString() }).canonicalFile
            }.orNull
                ?: throw GradleException("Set the JMC clone path with -Plocal.jmc.clone.path=/path/to/jmc")
            if (!jmcRoot.isDirectory) throw GradleException("JMC clone not found: $jmcRoot")

            val pomFile = jmcRoot.resolve("releng/third-party/pom.xml")
            val targetFile = jmcRoot.resolve("releng/platform-definitions")
                .listFiles()
                ?.filter { it.isDirectory && it.name.matches(Regex("""platform-definition-\d{4}-\d{2}""")) }
                ?.map { it.resolve("${it.name}.target") }
                ?.filter { it.isFile }
                ?.maxByOrNull { it.name }
                ?: throw GradleException("No JMC platform target found in $jmcRoot")
            val mavenVersion = project.version.toString()
            val bundleVersion = aQute.bnd.version.MavenVersion(mavenVersion).osGiVersion.toString()

            val updatedPom = pomFile.replacingExactly(
                Regex("""(<fireplace\.version>)[^<]+(</fireplace\.version>)"""),
                expectedMatches = 1,
            ) { "${it.groupValues[1]}$mavenVersion${it.groupValues[2]}" }
            val updatedTarget = targetFile.replacingExactly(
                Regex("""(<unit id="fireplace-(?:swing|swing-animation|swt-awt-bridge)" version=")[^"]+("/>)"""),
                expectedMatches = 3,
            ) { "${it.groupValues[1]}$bundleVersion${it.groupValues[2]}" }

            pomFile.writeText(updatedPom)
            targetFile.writeText(updatedTarget)

            logger.lifecycle("Updated JMC to Fireplace $mavenVersion (bundle $bundleVersion).")
            logger.warn("Do not commit the local Fireplace versions in $pomFile or $targetFile.")
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

fun File.replacingExactly(
    pattern: Regex,
    expectedMatches: Int,
    replacement: (MatchResult) -> CharSequence,
): String {
    if (!isFile) throw GradleException("File not found: $this")

    val content = readText()
    val matchCount = pattern.findAll(content).count()
    if (matchCount != expectedMatches) {
        throw GradleException("Expected $expectedMatches matches in $this, found $matchCount")
    }
    return pattern.replace(content, replacement)
}

