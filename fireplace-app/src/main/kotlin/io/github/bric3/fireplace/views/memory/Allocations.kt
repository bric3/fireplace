/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.views.memory

import io.github.bric3.fireplace.JFRBinder
import io.github.bric3.fireplace.JfrAnalyzer
import io.github.bric3.fireplace.ui.FlameGraphPane
import io.github.bric3.fireplace.ui.ViewPanel

class Allocations(private val jfrBinder: JFRBinder) : ViewPanel {
    override val identifier = "Allocations"

    override val view by lazy {
        FlameGraphPane().apply {
            jfrBinder.bindEvents(
                JfrAnalyzer::stackTraceAllocationFun,
                this::setStacktraceTreeModel
            )
        }
    }
}