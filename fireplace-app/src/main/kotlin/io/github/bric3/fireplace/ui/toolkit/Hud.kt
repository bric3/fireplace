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

import java.awt.BorderLayout
import java.awt.GridBagLayout
import javax.swing.*

class Hud(private val mainComponent: JComponent) {
    private val dndPanel: JPanel = JPanelWithPainter(GridBagLayout(), Painter.blurOf(mainComponent)).apply {
        add(JLabel("<html><font size=+4>Drag and drop JFR file here</font></html>"))
        isOpaque = false
        isVisible = false
    }

    private val progressPanel: JPanel = JPanelWithPainter(BorderLayout(), Painter.blurOf(mainComponent)).apply {
        add(
            JLabel("<html><font size=+4>Loading in progress</font></html>", SwingConstants.CENTER),
            BorderLayout.CENTER
        )
        val progress = JProgressBar().apply {
            isIndeterminate = true
        }
        add(progress, BorderLayout.SOUTH)
    }

    private val hudPanel: JPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        add(dndPanel)
        add(progressPanel)
        isOpaque = false
    }

    private val frameResizeLabel = FrameResizeLabel()
    val component: JComponent

    init {
        component = JLayeredPane().apply {
            layout = OverlayLayout(this)
            isOpaque = false
            isVisible = true
            addLayer(mainComponent, JLayeredPane.DEFAULT_LAYER)
            addLayer(hudPanel, JLayeredPane.MODAL_LAYER)
            addLayer(frameResizeLabel.component, JLayeredPane.POPUP_LAYER)
        }
    }

    val dnDTarget: DragAndDropTarget
        get() = object : DragAndDropTarget {
            override val component = hudPanel

            override fun activate() {
                progressPanel.isVisible = false
                dndPanel.isVisible = true
            }

            override fun deactivate() {
                dndPanel.isVisible = false
            }
        }

    fun setProgressVisible(visible: Boolean) {
        if (visible) {
            dndPanel.isVisible = false
        }
        progressPanel.isVisible = visible
    }

    fun installResizeListener(frame: JFrame) = frameResizeLabel.installListener(frame)
}
