/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
description = "Animate zoom transitions of a flamegraph or iciclegraph swing component"

dependencies {
    implementation(projects.fireplaceSwing)
    implementation(libs.radiance.animation)
}

tasks {
    withType(JavaCompile::class) {
        options.release.set(11)
    }
}
