/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.radianceanimation)
}

tasks {
    withType(JavaCompile::class) {
        options.release.set(11)
    }
}
