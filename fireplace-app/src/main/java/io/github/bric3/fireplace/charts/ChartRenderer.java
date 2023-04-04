package io.github.bric3.fireplace.charts;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * A renderer for an {@link Chart} that draws data with a particular representation
 * (for example, lines or bars).
 */
public abstract class ChartRenderer {

    private final RectangleContent background;

    public ChartRenderer() {
        this(null);
    }

    /**
     * Creates a new renderer.
     *
     * @param background the background ({@code null} permitted).
     */
    public ChartRenderer(RectangleContent background) {
        this.background = background;
    }

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
    public void draw(Chart chart, XYDataset dataset, Graphics2D g2, Rectangle2D plotBounds) {
        if (this.background != null) {
            background.draw(g2, plotBounds);
        }
    }
}
