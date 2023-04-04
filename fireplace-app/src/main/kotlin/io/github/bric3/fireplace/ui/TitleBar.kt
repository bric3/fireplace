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

import com.github.weisj.darklaf.platform.SystemInfo
import com.github.weisj.darklaf.platform.decorations.ExternalLafDecorator
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Rectangle
import javax.swing.JComponent
import javax.swing.JPanel

class TitleBar(component: JComponent) : JPanel(BorderLayout()) {
    init {
        add(component, BorderLayout.CENTER)
    }

    override fun doLayout() {
        if (SystemInfo.isMac) {
            add(WindowButtonSpace.INSTANCE, BorderLayout.WEST)
        }
        if (SystemInfo.isWindows || SystemInfo.isLinux) {
            add(WindowButtonSpace.INSTANCE)
        }
        super.doLayout()
    }

    private class WindowButtonSpace private constructor() : JComponent() {
        private val windowButtonRect: Rectangle by lazy {
            ExternalLafDecorator.instance()
                .decorationsManager()
                .titlePaneLayoutInfo(rootPane)
                .windowButtonRect()
        }

        override fun getPreferredSize(): Dimension {
            val size = windowButtonRect.size
            if (SystemInfo.isMac) {
                size.width = size.width + windowButtonRect.x
            }
            if (SystemInfo.isWindows || SystemInfo.isLinux) {
                val rightAdjustment = rootPane.width - windowButtonRect.x - windowButtonRect.width
                size.width = size.width + rightAdjustment
            }
            return size
        }

        override fun getMinimumSize(): Dimension {
            return preferredSize
        }

        override fun getMaximumSize(): Dimension {
            return preferredSize
        }

        companion object {
            private const val serialVersionUID = 1L
            val INSTANCE = WindowButtonSpace()
        }
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
