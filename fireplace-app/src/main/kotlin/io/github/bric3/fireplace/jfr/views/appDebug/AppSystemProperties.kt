/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.jfr.views.appDebug

import io.github.bric3.fireplace.Utils
import io.github.bric3.fireplace.ui.toolkit.simpleReadOnlyTable
import io.github.bric3.fireplace.ui.ViewPanel

class AppSystemProperties : ViewPanel {
    override val identifier: String = "App System properties"

    override val view by lazy {
        simpleReadOnlyTable(
            System.getProperties().map { arrayOf(it.key, it.value) }.toTypedArray(),
            arrayOf("Key", "Value")
        )
    }

    companion object {
        fun isActive(): Boolean = Utils.isDebugging || Utils.isFireplaceDebug
    }
}