/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

description = "SWT-AWT utils that bridge the two toolkits"


dependencies {
    implementation(libs.eclipse.swt) // don't use api, the consuming platform will provide it
    
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType(JavaCompile::class) {
    options.compilerArgs.addAll(arrayOf("-Xlint"))
    options.release.set(17)
}
