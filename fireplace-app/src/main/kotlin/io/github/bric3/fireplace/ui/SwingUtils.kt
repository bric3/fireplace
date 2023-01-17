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

import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JSplitPane

fun JSplitPane.autoSize(proportionalLocation: Double) {
    this.addComponentListener(object : ComponentAdapter() {
        private var firstResize = true
        override fun componentResized(e: ComponentEvent? ) {
            if (firstResize) {
                this@autoSize.setDividerLocation(proportionalLocation)
                firstResize = false
            }
        }
    })
}
