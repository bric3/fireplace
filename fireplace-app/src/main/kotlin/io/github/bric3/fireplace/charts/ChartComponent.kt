package io.github.bric3.fireplace.charts

import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.beans.PropertyChangeSupport
import javax.swing.*

/**
 * A component that draws an [Chart] within the bounds of the component.
 *
 * @param chart  the chart.
 */
class ChartComponent(chart: Chart) : JComponent() {
    private val propertyChangeSupport = PropertyChangeSupport(this)

    private var chart = chart
        set(value) {
            val oldChart = field
            if (oldChart == value) {
                return
            }
            field = value
            propertyChangeSupport.firePropertyChange("chart", oldChart, value)
        }

    /** A reusable rectangle to avoid creating work for the garbage collector.  */
    private val rect = Rectangle()

    init {
        border = null
        propertyChangeSupport.addPropertyChangeListener("chart") {
            repaint()
        }
    }

    /**
     * Paints the component.  The chart will be drawn at a size matching the
     * bounds of the component.
     *
     * @param g the Java2D graphics target.
     */
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        getBounds(rect)
        g.translate(-rect.x, -rect.y)
        chart.draw(g2, rect)
        g.translate(rect.x, rect.y)
    }
}
