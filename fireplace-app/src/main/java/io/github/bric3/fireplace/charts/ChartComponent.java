package io.github.bric3.fireplace.charts;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * A component that draws an {@link Chart} within the bounds of the component.
 */
public class ChartComponent extends JComponent {

    private final Chart chart;

    /** A reusable rectangle to avoid creating work for the garbage collector. */
    private final Rectangle rect = new Rectangle();

    /**
     * Creates a new component that contains an inlay chart.
     *
     * @param chart  the chart ({@code null} not permitted).
     */
    public ChartComponent(Chart chart) {
        Objects.requireNonNull(chart);
        this.chart = chart;
        setBorder(null);
    }

    /**
     * Paints the component.  The chart will be drawn at a size matching the
     * bounds of the component.
     *
     * @param g the Java2D graphics target.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        getBounds(rect);
        g.translate(-rect.x, -rect.y);
        chart.draw(g2, rect);
        g.translate(rect.x, rect.y);
    }
}
