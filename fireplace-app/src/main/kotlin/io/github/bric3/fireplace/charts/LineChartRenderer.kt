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

import io.github.bric3.fireplace.core.ui.Colors
import io.github.bric3.fireplace.core.ui.LightDarkColor
import java.awt.BasicStroke
import java.awt.Color
import java.awt.GradientPaint
import java.awt.Graphics2D
import java.awt.Paint
import java.awt.Point
import java.awt.Stroke
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import kotlin.math.abs


/**
 * A renderer that draws line charts.
 *
 * @param stroke the line stroke.
 * @param paint  the line paint.
 */
class LineChartRenderer(
    stroke: Stroke = BasicStroke(
        1.2f,
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND
    ),
    paint: Paint = LightDarkColor(Color.BLACK, Color.WHITE),
) : ChartRenderer {

    /**
     * The gradient fill colors, either `null`, one color, or at most two colors.
     * When two colors are passed, this pair of colors will be used to create
     * a vertical gradient fill.
     * If `null`, no fill will be drawn.
     */
    var fillColors: List<Color>? = null

    /**
     * The stroke for rendering the line.
     */
    var lineStroke = stroke

    /**
     * The paint for rendering the line.
     */
    var linePaint = paint

    /**
     * A shape that will be drawn at the point of the last value in the series.
     * Leave as null if you don't want anything drawn.
     */
    private val lastValueShape = Ellipse2D.Double(-2.0, -2.0, 4.0, 4.0)

    /**
     * The color for the shape drawn at the location of the most recent value.
     */
    private val lastValueShapeColor = Color(25, 90, 25)

    /**
     * A flag that controls whether zero value must be included in the y-value range.
     */
    private val includeZeroInYRange = true

    override fun drawTrackerLine(
        chart: Chart,
        dataset: ChartDataset,
        g2: Graphics2D,
        plotBounds: Rectangle2D,
        mousePosition: Point?
    ) {
        val itemCount = dataset.itemCount
        if (itemCount == 0) return;
        val xRange = dataset.rangeOfX
        var yRange = dataset.rangeOfY
        if (includeZeroInYRange) {
            yRange = yRange.include(0.0)
        }

        val showTracker = mousePosition != null && mousePosition.x >= plotBounds.x && mousePosition.x <= plotBounds.maxX
        if (!showTracker) return

        var closestItemIndex = 0
        var closestItemX = Double.MAX_VALUE
        for (i in 0 until itemCount) {
            if (yRange.isZeroLength) continue
            val xx = plotBounds.x + xRange.ratioFor(dataset.xAt(i)) * plotBounds.width
            // discover the closest item to the mouse adjusted position
            val distance = abs(xx - mousePosition!!.x)
            if (distance < closestItemX) {
                closestItemX = distance
                closestItemIndex = i
            }
        }

        // draw vertical line
        val centerX = plotBounds.x + xRange.ratioFor(dataset.xAt(closestItemIndex)) * plotBounds.width
        val centerY = plotBounds.maxY - yRange.ratioFor(dataset.yAt(closestItemIndex)!!) * plotBounds.height

        g2.paint = Color.GRAY // TODO get color from chart
        g2.drawLine(
            centerX.toInt(),
            plotBounds.y.toInt(),
            centerX.toInt(),
            plotBounds.maxY.toInt()
        )
    }

    /**
     * Draws a line chart within the specified bounds.
     *
     * @param chart         the chart being drawn.
     * @param dataset       the dataset.
     * @param g2            the Java2D graphics target.
     * @param plotBounds    the bounds within which the data should be rendered.
     * @param mousePosition the mouse position.
     */
    override fun draw(
        chart: Chart,
        dataset: ChartDataset,
        g2: Graphics2D,
        plotBounds: Rectangle2D,
        mousePosition: Point?
    ) {
        // within the bounds, draw a line chart
        val itemCount = dataset.itemCount
        if (itemCount == 0) return;
        val xRange = dataset.rangeOfX
        var yRange = dataset.rangeOfY
        if (includeZeroInYRange) {
            yRange = yRange.include(0.0)
        }

        val showTracker = mousePosition != null && mousePosition.x >= plotBounds.x && mousePosition.x <= plotBounds.maxX

        var closestItemIndex = 0
        var closestItemX = Double.MAX_VALUE

        // TODO - what if the yRange is a single value or all values are null?
        // Does the chart have a fixed display range?
        // Also, are there range constraints on the chart itself
        // (for example, y in the range 0 to 100)
        val path: Path2D = Path2D.Double()
        for (i in 0 until itemCount) {
            if (yRange.isZeroLength) continue
            val xx = plotBounds.x + xRange.ratioFor(dataset.xAt(i)) * plotBounds.width
            // discover the closest item to the mouse adjusted position
            if (showTracker) {
                val distance = abs(xx - mousePosition!!.x)
                if (distance < closestItemX) {
                    closestItemX = distance
                    closestItemIndex = i
                }
            }

            val yValue = dataset.yAt(i) ?: continue
            val yy = plotBounds.maxY - yRange.ratioFor(yValue) * plotBounds.height
            if (i == 0) {
                path.moveTo(xx, yy)
            } else {
                path.lineTo(xx, yy)
            }
        }

        // Optionally, the area under the line can be filled - do this first...
        fillColors?.let {
            val path2 = Path2D.Double(path).apply {
                lineTo(path.currentPoint.x, plotBounds.maxY)
                lineTo(plotBounds.x, plotBounds.maxY)
                closePath()
            }
            g2.paint = it.singleOrNull() ?: verticalGradientPaint(it, plotBounds)
            g2.fill(path2)
        }


        g2.paint = linePaint
        g2.stroke = lineStroke
        g2.draw(path)

        // paint last value
        run {
            val saved = g2.transform
            g2.translate(path.currentPoint.x, path.currentPoint.y)
            g2.color = lastValueShapeColor
            g2.fill(this.lastValueShape)
            g2.transform = saved
        }

        if (showTracker) {
            val centerX = plotBounds.x + xRange.ratioFor(dataset.xAt(closestItemIndex)) * plotBounds.width
            val centerY = plotBounds.maxY - yRange.ratioFor(dataset.yAt(closestItemIndex)!!) * plotBounds.height
            
            val dotRadius = 5.0
            val dot = Ellipse2D.Double(
                centerX - dotRadius,
                centerY - dotRadius,
                2.0 * dotRadius,
                2.0 * dotRadius
            )

            val dotBorderRadius = 5.0
            val dotBorder = Ellipse2D.Double(
                centerX - dotBorderRadius,
                centerY - dotBorderRadius,
                2.0 * dotBorderRadius,
                2.0 * dotBorderRadius
            )

            g2.paint = linePaint
            g2.fill(dot)

            g2.paint = Colors.panelBackground
            g2.draw(dotBorder)
        }
    }

    companion object {
        /**
         * Creates a new [GradientPaint] instance based on the supplied colors and with coordinates
         * set to run the gradient from the top to the bottom of the supplied bounds.
         *
         * @param gradientColors the top color (`null` not permitted).
         * @param bounds         the bounds (`null` not permitted).
         * @return A GradientPaint.
         */
        fun verticalGradientPaint(gradientColors: List<Color>, bounds: Rectangle2D): GradientPaint {
            require(gradientColors.size == 2) { "gradientColors must be non-null and have 2 colors" }
            return GradientPaint(
                bounds.centerX.toFloat(), bounds.y.toFloat(), gradientColors[0],   // top
                bounds.centerX.toFloat(), bounds.maxY.toFloat(), gradientColors[1] // bottom
            )
        }
    }
}
