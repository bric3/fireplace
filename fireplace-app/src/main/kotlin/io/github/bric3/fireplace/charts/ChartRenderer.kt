package io.github.bric3.fireplace.charts

import java.awt.Graphics2D
import java.awt.geom.Rectangle2D

/**
 * A renderer for an [Chart] that draws data with a particular representation
 * (for example, lines or bars).
 *
 * @param background the background (`null` permitted).
 */
abstract class ChartRenderer @JvmOverloads constructor(private val background: RectangleContent? = null) {
    /**
     * Draws a representation of the supplied dataset within the plot bounds of the supplied
     * Java2D graphics target.  The chart can be used to provide some global attributes for the
     * rendering (such as the x-range and y-range for display).
     *
     * @param chart the chart.
     * @param dataset the dataset.
     * @param g2 the Java2D graphics target.
     * @param plotBounds the plot bounds.
     */
    open fun draw(chart: Chart, dataset: XYDataset, g2: Graphics2D, plotBounds: Rectangle2D) {
        if (this.background != null) {
            background.draw(g2, plotBounds)
        }
    }
}
