package io.github.bric3.fireplace.charts;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.Objects;

/**
 * A renderer that draws line charts.
 */
public class LineChartRenderer extends ChartRenderer {

    /**
     * The stroke for rendering the line.
     */
    private final Stroke lineStroke;

    /**
     * The paint for rendering the line.
     */
    private final Paint linePaint;

    /**
     * The fill color - if null, then the line is drawn without a fill.
     */
    private Color fillColor;

    /**
     * The gradient fill colors - an alternative to the fill color, this pair of
     * colors will be used to create a vertical gradient fill.  If null, no fill
     * will be drawn.
     */
    private Color[] gradientFillColors;

    /**
     * A shape that will be drawn at the point of the last value in the series.
     * Leave as null if you don't want anything drawn.
     */
    private final Shape lastValueShape;

    /**
     * The color for the shape drawn at the location of the most recent value.
     */
    private final Color lastValueShapeColor;

    /**
     * A flag that controls whether zero must be included in the y-value range.
     */
    private final boolean includeZeroInYRange;

    /**
     * Creates a new renderer with default attributes.
     */
    public LineChartRenderer() {
        this(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND), Color.BLACK);
    }

    /**
     * Creates a new renderer that draws a line with the specified stroke and paint.
     *
     * @param stroke the line stroke ({@code null} not permitted).
     * @param paint  the line paint ({@code null} not permitted).
     */
    public LineChartRenderer(Stroke stroke, Paint paint) {
        Objects.requireNonNull(stroke);
        Objects.requireNonNull(paint);
        this.lineStroke = stroke;
        this.linePaint = paint;
        this.fillColor = null; // no fill
        this.gradientFillColors = null; // no gradient fill
        this.lastValueShape = new Ellipse2D.Double(-2, -2, 4, 4);
        this.lastValueShapeColor = new Color(25, 90, 25);
        this.includeZeroInYRange = true;
    }

    /**
     * Sets the color used to fill the area under the line.  If {@code null}, the area will not be filled.
     *
     * @param color the new fill color ({@code null} permitted).
     */
    public void setFillColor(Color color) {
        this.fillColor = color;
    }

    /**
     * Sets the gradient fill colors.
     *
     * @param colors the top color ({@code null} permitted).
     */
    public void setGradientFillColors(Color[] colors) {
        this.gradientFillColors = colors;
    }

    /**
     * Draws a line chart within the specified bounds.
     *
     * @param chart      the chart being drawn.
     * @param dataset    the dataset.
     * @param g2         the Java2D graphics target.
     * @param plotBounds the bounds within which the data should be rendered.
     */
    @Override
    public void draw(Chart chart, XYDataset dataset, Graphics2D g2, Rectangle2D plotBounds) {
        super.draw(chart, dataset, g2, plotBounds);

        // within the bounds, draw a line chart
        int itemCount = dataset.getItemCount();
        if (itemCount == 0) {
            // what to do? FIXME
            throw new IllegalArgumentException("No data");
        }
        var xRange = dataset.rangeOfX;
        var yRange = dataset.rangeOfY;
        if (includeZeroInYRange) {
            yRange = yRange.include(0.0);
        }
        // TODO - what if the yRange is a single value or all values are null?

        // does the chart have a fixed display range?
        // PLUS are there range constraints on the chart itself (for example, y in the range 0 to 100)
        Path2D path = new Path2D.Double();
        for (int i = 0; i < itemCount; i++) {
            if (yRange == null || yRange.isZeroLength()) continue;
            double xx = plotBounds.getX() + xRange.calcFraction(dataset.xAt(i)) * plotBounds.getWidth();
            if (dataset.yAt(i) == null) continue;
            double yy = plotBounds.getMaxY() - yRange.calcFraction(dataset.yAt(i)) * plotBounds.getHeight();
            if (i == 0) {
                path.moveTo(xx, yy);
            } else {
                path.lineTo(xx, yy);
            }
        }

        // optionally the area under the line can be filled - do this first...
        if (fillColor != null || gradientFillColors != null) {
            Path2D path2 = new Path2D.Double(path);
            path2.lineTo(path.getCurrentPoint().getX(), plotBounds.getMaxY());
            path2.lineTo(plotBounds.getX(), plotBounds.getMaxY());
            path2.closePath();
            g2.setPaint(Objects.requireNonNullElseGet(
                    fillColor,
                    () -> verticalGradientPaint(gradientFillColors, plotBounds)
            ));
            g2.fill(path2);
        }

        g2.setPaint(linePaint);
        g2.setStroke(lineStroke);
        g2.draw(path);

        if (this.lastValueShape != null) {
            AffineTransform saved = g2.getTransform();
            g2.translate(path.getCurrentPoint().getX(), path.getCurrentPoint().getY());
            g2.setColor(this.lastValueShapeColor);
            g2.fill(this.lastValueShape);
            g2.setTransform(saved);
        }
    }

    /**
     * Creates a new {@link GradientPaint} instance based on the supplied colors and with coordinates
     * set to run the gradient from the top to the bottom of the supplied bounds.
     *
     * @param gradientColors the top color ({@code null} not permitted).
     * @param bounds         the bounds ({@code null} not permitted).
     * @return A GradientPaint.
     */
    public static GradientPaint verticalGradientPaint(Color[] gradientColors, Rectangle2D bounds) {
        if (gradientColors == null || gradientColors.length != 2 || gradientColors[0] == null || gradientColors[1] == null) {
            throw new IllegalArgumentException("gradientColors must be non-null and have 2 colors");
        }
        return new GradientPaint(
                (float) bounds.getCenterX(), (float) bounds.getY(), gradientColors[0], // top
                (float) bounds.getCenterX(), (float) bounds.getMaxY(), gradientColors[1] // bottom
        );
    }
}
