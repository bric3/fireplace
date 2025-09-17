/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
import com.javiersc.semver.project.gradle.plugin.SemverExtension

plugins {
    id("com.javiersc.semver")
}

configure<SemverExtension> {
    tagPrefix.set("v")
}
