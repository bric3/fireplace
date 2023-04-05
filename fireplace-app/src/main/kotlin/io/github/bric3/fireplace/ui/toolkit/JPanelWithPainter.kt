/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.ui.toolkit

import io.github.bric3.fireplace.ui.toolkit.Painter
import java.awt.*
import javax.swing.JPanel

class JPanelWithPainter @JvmOverloads constructor(
    layout: LayoutManager = GridBagLayout(),
    private val backgroundPainter: Painter
) : JPanel(layout) {
    override fun paintComponent(g: Graphics) {
        backgroundPainter.paint(g as Graphics2D, this)
    }
}
