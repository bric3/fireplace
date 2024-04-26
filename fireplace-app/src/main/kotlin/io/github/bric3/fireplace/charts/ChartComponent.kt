/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.charts

import java.awt.AWTEvent
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.event.MouseEvent
import java.beans.PropertyChangeSupport
import javax.swing.*

/**
 * A component that draws an [Chart] within the bounds of the component.
 *
 * @param chart  the chart.
 */
class ChartComponent(chart: Chart? = null) : JComponent() {
    private val propertyChangeSupport = PropertyChangeSupport(this)

    var chart: Chart? = chart
        set(value) {
            val oldChart = field
            if (oldChart == value) {
                return
            }
            field = value
            propertyChangeSupport.firePropertyChange("chart", oldChart, value)
        }

    val toolTipComponent: JComponent?
        get() = chart?.createToolTipComponent(getBounds(rect), mousePosition)

    /** A reusable rectangle to avoid creating work for the garbage collector.  */
    private val rect = Rectangle()

    init {
        enableEvents(AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK)
        border = null
        propertyChangeSupport.addPropertyChangeListener("chart") {
            revalidate()
            repaint()
        }
    }

    /**
     * Paints the component.
     * The chart will be drawn at a size matching the bounds of the component.
     *
     * @param g the Java2D graphics target.
     */
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        chart?.let {
            val g2 = g as Graphics2D
            getBounds(rect)
            g.translate(-rect.x, -rect.y)
            it.draw(g2, rect, mousePosition)
            g.translate(rect.x, rect.y)
        }
    }

    override fun processMouseEvent(e: MouseEvent) {
        // handle clicks?
        super.processMouseEvent(e)
    }

    override fun processMouseMotionEvent(e: MouseEvent) {
        if (e.id == MouseEvent.MOUSE_MOVED) {
            repaint()
        }
        super.processMouseMotionEvent(e)
    }
}
