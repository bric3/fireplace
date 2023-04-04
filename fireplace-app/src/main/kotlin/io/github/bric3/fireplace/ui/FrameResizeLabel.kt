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
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*

class FrameResizeLabel {
    private val dimensionLabel: JLabel = JLabel("hello").apply {
        verticalAlignment = JLabel.CENTER
        horizontalAlignment = JLabel.CENTER
        isOpaque = true
        border = BorderFactory.createLineBorder(
            LightDarkColor(
                Colors.panelForeground,
                Colors.panelBackground
            )
        )
    }
    private val dimensionOverlayPanel: JPanel = JPanel(BorderLayout()).apply {
        add(dimensionLabel, BorderLayout.CENTER)
        background = LightDarkColor(
            Colors.translucent_white_D0,
            Colors.translucent_black_80
        )
        isOpaque = false
        isVisible = false
    }
    private val panelHider: Timer

    init {
        dimensionOverlayPanel.maximumSize = dimensionLabel.getFontMetrics(dimensionLabel.font).let {
            val border = 10
            val textWidth = it.stringWidth("1000 x 1000") + 10
            val textHeight = it.height
            Dimension(textWidth + border, textHeight + border)
        }

        panelHider = Timer(2000) { _ -> dimensionOverlayPanel.isVisible = false }.apply {
            isCoalesce = true
        }
    }

    fun installListener(frame: JFrame) {
        frame.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                val height = frame.height
                val width = frame.width
                dimensionLabel.text = "$height x $width"
                dimensionOverlayPanel.isVisible = true
                panelHider.restart()
            }
        })
    }

    val component: JComponent
        get() = dimensionOverlayPanel
}
