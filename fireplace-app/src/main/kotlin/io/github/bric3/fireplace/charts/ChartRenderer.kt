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

import java.awt.Graphics2D
import java.awt.Point
import java.awt.geom.Rectangle2D

/**
 * A renderer for an [Chart] that draws data with a particular representation
 * (for example, lines or bars).
 */
interface ChartRenderer {
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
    fun draw(chart: Chart, dataset: ChartDataset, g2: Graphics2D, plotBounds: Rectangle2D, mousePosition: Point?)

    /**
     * Draws a tracker line for the supplied dataset within the plot bounds of the supplied
     * Java2D graphics target.  The chart can be used to provide some global attributes for the
     * rendering (such as the x-range and y-range for display).
     *
     * @param chart the chart.
     * @param dataset the dataset.
     * @param g2 the Java2D graphics target.
     * @param plotBounds the plot bounds.
     */
    fun drawTrackerLine(
        chart: Chart,
        dataset: ChartDataset,
        g2: Graphics2D,
        plotBounds: Rectangle2D,
        mousePosition: Point?
    ) { }
}
