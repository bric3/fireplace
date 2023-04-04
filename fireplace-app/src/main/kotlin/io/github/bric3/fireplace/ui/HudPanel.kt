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

import java.awt.BorderLayout
import java.awt.GridBagLayout
import javax.swing.*

class HudPanel {
    private val dndPanel: JPanel
    private val progressPanel: JPanel
    private val hudPanel: JPanel

    init {
        dndPanel = GlassJPanel(GridBagLayout()).apply {
            add(JLabel("<html><font size=+4>Drag and drop JFR file here</font></html>"))
            setOpaque(false)
            setVisible(false)
        }
        progressPanel = GlassJPanel(BorderLayout()).apply {
            add(
                JLabel("<html><font size=+4>Loading in progress</font></html>", SwingConstants.CENTER),
                BorderLayout.CENTER
            )
            val progress = JProgressBar().apply {
                isIndeterminate = true
            }
            add(progress, BorderLayout.SOUTH)
        }
        hudPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(dndPanel)
            add(progressPanel)
            isOpaque = false
        }
    }

    val component: JComponent
        get() = hudPanel

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
}
