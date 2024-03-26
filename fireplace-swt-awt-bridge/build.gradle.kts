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
    id("fireplace.published-java-library")
    id("fireplace.local-eclipse-swt-platform")
}

description = "SWT-AWT utils that bridge the two toolkits"

dependencies {
    implementation(libs.eclipse.swt) // don't use api, the consuming platform will provide it
}
