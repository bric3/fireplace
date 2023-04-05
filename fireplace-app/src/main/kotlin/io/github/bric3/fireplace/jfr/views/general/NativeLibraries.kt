/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.jfr.views.general

import io.github.bric3.fireplace.jfr.JFRLoaderBinder
import io.github.bric3.fireplace.jfr.JfrAnalyzer
import io.github.bric3.fireplace.ui.toolkit.simpleReadOnlyTable
import io.github.bric3.fireplace.ui.ViewPanel
import io.github.bric3.fireplace.ui.toolkit.unwrappedTable

class NativeLibraries(private val jfrBinder: JFRLoaderBinder) : ViewPanel {
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