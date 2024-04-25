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

import io.github.bric3.fireplace.charts.ChartSpecification.LineRendererDescriptor
import io.github.bric3.fireplace.charts.ChartSpecification.RendererDescriptor
import java.awt.Color
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
     * A list of cart specifications.
     *
     * @see ChartSpecification
     */
    var chartSpecifications: List<ChartSpecification> = emptyList()
        set(value) {
            val oldChartDatasetDescriptor = field
            if (oldChartDatasetDescriptor == value) {
                return
            }

            field = value
            propertyChangeSupport.firePropertyChange("charDatasetDescriptors", oldChartDatasetDescriptor, value)
        }

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
    var insets = RectangleMargin(2.0, 2.0, 2.0, 2.0)
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
    var plotInsets = RectangleMargin(0.0, 0.0, 0.0, 0.0)
        set(value) {
            val oldPlotInsets = field
            if (oldPlotInsets == value) {
                return
            }

            field = value
            propertyChangeSupport.firePropertyChange("plotInsets", oldPlotInsets, value)
        }

    /**
     * Creates a new chart with the given specifications, dataset and renderer.
     *
     * @param chartSpecifications The chart specifications
     */
    constructor(chartSpecifications: List<ChartSpecification>) {
        // TODO how to ensure consistent ranges across multiple charts?
        this.chartSpecifications = chartSpecifications
    }

    fun addPropertyChangeListener(listener: PropertyChangeListener?) {
        propertyChangeSupport.addPropertyChangeListener(listener)
    }

    fun removePropertyChangeListener(listener: PropertyChangeListener?) {
        propertyChangeSupport.removePropertyChangeListener(listener)
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

        background?.draw(g2, bounds)

        // handle background, margin, border, insets and fill
        insets.applyInsets(bounds)

        val plotArea = plotInsets.shrink(bounds)

        chartSpecifications.forEach {
            // plot its dataset in the inner bounds
            configureRenderer(it.renderer).draw(this, it.dataset, g2, plotArea)
        }
    }

    private val lineChartRenderer = LineChartRenderer()

    private fun configureRenderer(rendererSpec: RendererDescriptor): ChartRenderer {
        return when (rendererSpec) {
            is LineRendererDescriptor -> lineChartRenderer.apply {
                linePaint = rendererSpec.lineColor ?: Color.BLACK
                fillColors = rendererSpec.fillColors
            }

            else -> error("Unsupported render spec")
        }
    }
}
