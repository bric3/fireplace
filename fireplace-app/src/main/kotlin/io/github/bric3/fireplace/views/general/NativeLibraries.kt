/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.views.general

import io.github.bric3.fireplace.JFRBinder
import io.github.bric3.fireplace.JfrAnalyzer
import io.github.bric3.fireplace.simpleReadOnlyTable
import io.github.bric3.fireplace.ui.ViewPanel
import io.github.bric3.fireplace.unwrappedTable

class NativeLibraries(private val jfrBinder: JFRBinder) : ViewPanel {
    override val identifier = "Native libraries"

    override val view by lazy {
        simpleReadOnlyTable(
            arrayOf(),
            arrayOf("Path")
        ).apply {
            jfrBinder.bindEvents(
                JfrAnalyzer::nativeLibraries
            ) { libs ->
                unwrappedTable().model.setData(
                    libs.map { arrayOf(it) }.toTypedArray()
                )
            }
        }
    }
}