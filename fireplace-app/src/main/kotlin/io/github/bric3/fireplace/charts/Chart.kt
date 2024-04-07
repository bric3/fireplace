package io.github.bric3.fireplace.charts

import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Rectangle2D
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport

/**
 * A chart that can be inlaid into a small space.  Generally a chart will render a single dataset, but it is
 * also possible to overlay multiple renderer/dataset pairs in the same space.
 */
class Chart : RectangleContent {
    private val propertyChangeSupport = PropertyChangeSupport(this)
    /**
     * The dataset to be drawn on the chart.
     */
    var dataset: XYDataset? = null
        set(value) {
            val oldDataset = field
            if (oldDataset == value) {
                return
            }

            field = value
            propertyChangeSupport.firePropertyChange("dataset", oldDataset, value)
        }

    /**
     * The renderer for the dataset.
     */
    private val renderer: ChartRenderer

    /**
     * A background painter for the chart, possibly `null`.
     */
    var background: RectangleContent? = null
        set(value) {
            val oldBackground = field
            if (oldBackground == value) {
                return
            }
            field = value
            propertyChangeSupport.firePropertyChange("background", oldBackground, value)
        }

    /**
     * The insets (applied after the background has been drawn).
     */
    var insets: RectangleMargin = RectangleMargin(2.0, 2.0, 2.0, 2.0)
        set(value) {
            val oldInsets = field
            if (oldInsets == field) {
                return
            }
            field = value
            propertyChangeSupport.firePropertyChange("insets", oldInsets, value)
        }

    /**
     * The insets for the plot area (defaults to zero but can be modified to add space for
     * annotations etc).
     */
    var plotInsets: RectangleMargin = RectangleMargin(0.0, 0.0, 0.0, 0.0)
        set(value) {
            val oldPlotInsets = field
            if (oldPlotInsets == value) {
                return
            }

            field = value
            propertyChangeSupport.firePropertyChange("plotInsets", oldPlotInsets, value)
        }

    // fixed xRange?  We can leave the renderer to look at the range of values in the dataset, but if there
    // are multiple charts we might want them to have a consistent range.
    // fixed yRange? 0-100 for example.
    /**
     * Creates a new chart without dataset for the specified dataset and renderer.
     * Set the dataset and renderer using the [.setDataset].
     *
     * @param dataset the dataset
     * @param renderer  the renderer (`null` not permitted).
     */
    constructor(dataset: XYDataset?, renderer: ChartRenderer) {
        this.dataset = dataset
        this.renderer = renderer
    }

    fun addPropertyChangeListener(propertyName: String?, listener: PropertyChangeListener?) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener)
    }

    /**
     * Draws the chart to a Java2D graphics target.
     *
     * @param g2 the graphics target (`null` not permitted).
     * @param bounds the bounds within which the chart should be drawn.
     */
    override fun draw(g2: Graphics2D, bounds: Rectangle2D) {
        // set up any rendering hints we want (should allow this to be controlled externally)
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        if (background != null) {
            background!!.draw(g2, bounds)
        }

        // handle background, margin, border, insets and fill
        insets.applyInsets(bounds)

        val plotArea = plotInsets.shrink(bounds)

        // get the renderer to draw its dataset in the inner bounds
        if (dataset != null) {
            renderer.draw(this, dataset!!, g2, plotArea)
        }
    }
}
