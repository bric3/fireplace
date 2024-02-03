/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.ui

import javax.swing.*

interface ViewPanel {
    annotation class Priority(val value: Int)
    val identifier: String
    val view: JComponent
}

const val CPU_BASE = 100
const val MEMORY_BASE = 200
const val PROCESS_INFO_BASE = 300
const val JVM_BASE = 400
const val JFR_BASE = 900
