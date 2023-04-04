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

import io.github.bric3.fireplace.core.ui.Colors
import io.github.bric3.fireplace.core.ui.LightDarkColor
import java.awt.*
import javax.swing.JPanel

class GlassJPanel @JvmOverloads constructor(layout: LayoutManager? = GridBagLayout()) : JPanel(layout) {
    override fun paintComponent(g: Graphics) {
        val g2 = g as Graphics2D
        g2.color = TRANSLUCENT_BACKGROUND
        g2.fillRect(0, 0, width, height)
    }

    companion object {
        private val TRANSLUCENT_BACKGROUND: Color = LightDarkColor(
            Colors.translucent_white_D0,
            Colors.translucent_black_80
        )
    }
}
