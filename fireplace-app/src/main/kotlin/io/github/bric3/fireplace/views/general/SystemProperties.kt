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
import javax.swing.JComponent

class SystemProperties(private val jfrBinder: JFRBinder) : ViewPanel {
    override val identifier = "System properties"

    override val view: JComponent by lazy {
        simpleReadOnlyTable(
            arrayOf(),
            arrayOf("Key", "Value")
        ).apply {
            jfrBinder.bindEvents(
                JfrAnalyzer::jvmSystemProperties
            ) { props ->
                unwrappedTable().model.setData(
                    props.map { arrayOf(it.key, it.value) }.toTypedArray()
                )
            }
        }
    }
}