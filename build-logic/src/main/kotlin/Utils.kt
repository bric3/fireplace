/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.VersionConstraint
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.the

// https://github.com/gradle/gradle/issues/15383
val Project.libs
    get() = the<org.gradle.accessors.dm.LibrariesForLibs>()

// Don't name it libs otherwise it shadows the actual libs extension
val Project.libsCatalog
    get() = the<VersionCatalogsExtension>().named("libs")
fun VersionCatalog.getVersion(version: String): VersionConstraint =
    findVersion(version).orElseThrow { IllegalArgumentException("version $version not found in catalog $name") }
fun VersionCatalog.getLibrary(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow { IllegalArgumentException("library $alias not found in catalog $name") }
fun VersionCatalog.getBundle(alias: String): Provider<ExternalModuleDependencyBundle> =
    findBundle(alias).orElseThrow { IllegalArgumentException("bundle $alias not found in catalog $name") }

fun Project.properties(key: String) = findProperty(key).toString()