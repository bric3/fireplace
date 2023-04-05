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

import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import javax.swing.*

class BalloonToolTip : JToolTip() {
    @Transient
    private var listener: HierarchyListener? = null
    override fun updateUI() {
        removeHierarchyListener(listener)
        super.updateUI()
        listener = HierarchyListener { e: HierarchyEvent ->
            val c = e.component
            if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L && c.isShowing) {
                // This makes all parent container non-opaque
                // it seems that some LaF change that by default, so it's overridden for this tooltip.
                var container: Component? = c.parent
                while (container != null) {
                    if (container !is JFrame || container.isUndecorated) {
                        container.background = Color(0x0, true)
                    }
                    if (container is JComponent) {
                        container.isOpaque = false
                    }
                    container = container.parent
                }
                val window = SwingUtilities.windowForComponent(this)
                window.background = Color(0x0, true)
            }
        }
        addHierarchyListener(listener)
        isOpaque = false
        border = BorderFactory.createEmptyBorder(8, 5, 1, 5)
    }

    override fun paintComponent(g: Graphics) {
        val s = makeBalloonShape()
        (g.create() as Graphics2D).run {
            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            color = background
            fill(s)
            color = foreground
            draw(s)
            dispose()
        }
        super.paintComponent(g)
    }

    private fun makeBalloonShape(): Shape {
        val insets = insets
        val w = width - 1f
        val h = height - 1f
        val triangleHeight = insets.top * .8f
        val area = Area(
            RoundRectangle2D.Float(
                0f,
                triangleHeight,
                w,
                h - insets.bottom - triangleHeight,
                insets.top.toFloat(),
                insets.top.toFloat()
            )
        )
        val triangle = Path2D.Float().apply {
            moveTo((insets.left + triangleHeight * 2).toDouble(), 0.0)
            lineTo((insets.left + triangleHeight).toDouble(), triangleHeight.toDouble())
            lineTo((insets.left + triangleHeight * 3).toDouble(), triangleHeight.toDouble())
        }
        area.add(Area(triangle))
        return area
    }
}
