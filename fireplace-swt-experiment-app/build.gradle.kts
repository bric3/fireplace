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
    id("fireplace.application")
    id("fireplace.local-eclipse-swt-platform")
}

description = "SWT app that uses fireplace-swing"

dependencies {
    implementation(projects.fireplaceSwtAwtBridge)
    implementation(projects.fireplaceSwing)
    implementation(projects.fireplaceSwingAnimation)
    implementation(libs.flightrecorder)
}

application {
    mainClass.set("io.github.bric3.fireplace.swt.FirePlaceSwtMain")
}
