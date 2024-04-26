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

import io.github.bric3.fireplace.charts.withAlpha
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.*

open class ColorIcon(
    private val width: Int,
    private val height: Int,
    private val colorWidth: Int,
    private val colorHeight: Int,
    private val color: Color,
    private val isPaintBorder: Boolean,
    private val arc: Int = 6
) : Icon {
    constructor(size: Int, colorSize: Int, color: Color, border: Boolean) : this(
        size,
        size,
        colorSize,
        colorSize,
        color,
        border,
        6
    )

    constructor(size: Int, color: Color, border: Boolean = false) : this(size, size, color, border)

    override fun paintIcon(component: Component, g: Graphics, i: Int, j: Int) {
        val g2d = g.create() as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE)
        g2d.color = color

        val width = colorWidth
        val height = colorHeight
        val arc = arc
        val x = i + (this.width - width) / 2
        val y = j + (this.height - height) / 2

        g2d.fillRoundRect(x, y, width, height, arc, arc)

        if (isPaintBorder) {
            g2d.color = Color(0, true).withAlpha(40) // TODO put in Colors
            g2d.drawRoundRect(x, y, width, height, arc, arc)
        }

        g2d.dispose()
    }

    override fun getIconWidth(): Int {
        return width
    }

    override fun getIconHeight(): Int {
        return height
    }
}