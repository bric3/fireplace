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

import io.github.bric3.fireplace.jfr.support.JFRLoaderBinder
import io.github.bric3.fireplace.jfr.support.JfrAnalyzer
import io.github.bric3.fireplace.ui.PROCESS_INFO_BASE
import io.github.bric3.fireplace.ui.ViewPanel
import io.github.bric3.fireplace.ui.ViewPanel.Priority
import io.github.bric3.fireplace.ui.toolkit.simpleReadOnlyTable
import io.github.bric3.fireplace.ui.toolkit.unwrappedTable
import javax.swing.*

@Priority(PROCESS_INFO_BASE + 1)
class SystemProperties(private val jfrBinder: JFRLoaderBinder) : ViewPanel {
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