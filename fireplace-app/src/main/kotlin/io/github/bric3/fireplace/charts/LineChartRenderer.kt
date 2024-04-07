package io.github.bric3.fireplace.charts

import java.awt.BasicStroke
import java.awt.Color
import java.awt.GradientPaint
import java.awt.Graphics2D
import java.awt.Paint
import java.awt.Shape
import java.awt.Stroke
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D

/**
 * A renderer that draws line charts.
 * 
 * @param stroke the line stroke.
 * @param paint  the line paint.
 */
class LineChartRenderer @JvmOverloads constructor(
    stroke: Stroke = BasicStroke(
        1.0f,
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND
    ),
    paint: Paint = Color.BLACK
) : ChartRenderer() {

    /**
     * The gradient fill colors, either `null`, one color, or at most two colors.
     * When two colors are passed, this pair of colors will be used to create
     * a vertical gradient fill.
     * If `null`, no fill will be drawn.
     */
    var fillColors: Array<Color>? = null

    /**
     * The stroke for rendering the line.
     */
    var lineStroke: Stroke = stroke

    /**
     * The paint for rendering the line.
     */
    var linePaint: Paint = paint

    /**
     * A shape that will be drawn at the point of the last value in the series.
     * Leave as null if you don't want anything drawn.
     */
    private val lastValueShape: Shape?

    /**
     * The color for the shape drawn at the location of the most recent value.
     */
    private val lastValueShapeColor: Color

    /**
     * A flag that controls whether zero must be included in the y-value range.
     */
    private val includeZeroInYRange: Boolean

    /**
     * Creates a new renderer that draws a line with the specified stroke and paint.
     */
    init {
        this.lastValueShape = Ellipse2D.Double(-2.0, -2.0, 4.0, 4.0)
        this.lastValueShapeColor = Color(25, 90, 25)
        this.includeZeroInYRange = true
    }

    /**
     * Draws a line chart within the specified bounds.
     *
     * @param chart      the chart being drawn.
     * @param dataset    the dataset.
     * @param g2         the Java2D graphics target.
     * @param plotBounds the bounds within which the data should be rendered.
     */
    override fun draw(chart: Chart, dataset: XYDataset, g2: Graphics2D, plotBounds: Rectangle2D) {
        super.draw(chart, dataset, g2, plotBounds)

        // within the bounds, draw a line chart
        val itemCount = dataset.itemCount
        require(itemCount != 0) { "No data" }
        val xRange = dataset.rangeOfX
        var yRange = dataset.rangeOfY
        if (includeZeroInYRange) {
            yRange = yRange.include(0.0)
        }

        // TODO - what if the yRange is a single value or all values are null?
        // Does the chart have a fixed display range?
        // Also, are there range constraints on the chart itself
        // (for example, y in the range 0 to 100)
        val path: Path2D = Path2D.Double()
        for (i in 0 until itemCount) {
            if (yRange.isZeroLength) continue
            val xx = plotBounds.x + xRange.calcFraction(dataset.xAt(i)) * plotBounds.width
            // if (dataset.yAt(i) == null) continue
            val yy = plotBounds.maxY - yRange.calcFraction(dataset.yAt(i)) * plotBounds.height
            if (i == 0) {
                path.moveTo(xx, yy)
            } else {
                path.lineTo(xx, yy)
            }
        }

        // Optionally, the area under the line can be filled - do this first...
        fillColors?.let {
            val path2: Path2D = Path2D.Double(path).apply {
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

        if (this.lastValueShape != null) {
            val saved = g2.transform
            g2.translate(path.currentPoint.x, path.currentPoint.y)
            g2.color = lastValueShapeColor
            g2.fill(this.lastValueShape)
            g2.transform = saved
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
        fun verticalGradientPaint(gradientColors: Array<Color>, bounds: Rectangle2D): GradientPaint {
            require(gradientColors.size == 2) { "gradientColors must be non-null and have 2 colors" }
            return GradientPaint(
                bounds.centerX.toFloat(), bounds.y.toFloat(), gradientColors[0],   // top
                bounds.centerX.toFloat(), bounds.maxY.toFloat(), gradientColors[1] // bottom
            )
        }
    }
}
