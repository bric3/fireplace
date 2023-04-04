package io.github.bric3.fireplace.charts;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Objects;

/**
 * A chart that can be inlaid into a small space.  Generally a chart will render a single dataset, but it is
 * also possible to overlay multiple renderer/dataset pairs in the same space.
 */
public class Chart implements RectangleContent {

    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    /**
     * Optional background for the chart.
     */
    private RectangleContent background;

    /**
     * The insets (applied after the background has been drawn).
     */
    private RectangleMargin insets;

    /**
     * The insets for the plot area (defaults to zero but can be modified to add space for
     * annotations etc).
     */
    private RectangleMargin plotInsets;

    /** The dataset to be drawn on the chart. */
    private XYDataset dataset;

    /** The renderer for the dataset. */
    private final ChartRenderer renderer;
    
    // fixed xRange?  We can leave the renderer to look at the range of values in the dataset, but if there
    // are multiple charts we might want them to have a consistent range.

    // fixed yRange? 0-100 for example.


    /**
     * Creates a new chart without dataset for the specified dataset and renderer.
     * Set the dataset and renderer using the {@link #setDataset(XYDataset)}.
     *
     * @param renderer  the renderer ({@code null} not permitted).
     */
    public Chart(ChartRenderer renderer) {
        this(null, renderer);
    }

    /**
     * Creates a new chart for the specified dataset and renderer.
     *
     * @param dataset  the dataset ({@code null} not permitted).
     * @param renderer  the renderer ({@code null} not permitted).
     */
    public Chart(XYDataset dataset, ChartRenderer renderer) {
        this.dataset = dataset;
        this.renderer = renderer;
        this.insets = new RectangleMargin(2.0, 2.0, 2.0,2.0);
        this.plotInsets = new RectangleMargin(0.0, 0.0, 0.0, 0.0);
    }

    /**
     * Returns the background for the chart.
     *
     * @return A painter (possibly {@code null}).
     */
    public RectangleContent getBackground() {
        return this.background;
    }

    /**
     * Sets the background for the chart.
     *
     * @param background  the background ({@code null} permitted).
     */
    public void setBackground(RectangleContent background) {
        var oldBackground = this.background;
        if (Objects.equals(oldBackground, background)) {
            return;
        }
        this.background = background;
    }

    /**
     * Sets the dataset to be drawn on the chart.
     *
     * @param dataset the dataset ({@code null} permitted).
     */
    public void setDataset(XYDataset dataset) {
        var oldDataset = this.dataset;
        if (Objects.equals(oldDataset, dataset)) {
            return;
        }

        this.dataset = dataset;
        propertyChangeSupport.firePropertyChange("dataset", oldDataset, dataset);
    }

    /**
     * Returns the insets for the chart.
     *
     * @return The insets.
     */
    public RectangleMargin getInsets() {
        return this.insets;
    }

    /**
     * Sets the insets for the chart.
     *
     * @param insets the insets ({@code null} not permitted).
     */
    public void setInsets(RectangleMargin insets) {
        Objects.requireNonNull(insets);
        var oldInsets = this.insets;
        if (Objects.equals(oldInsets, insets)) {
            return;
        }

        this.insets = insets;
        propertyChangeSupport.firePropertyChange("insets", oldInsets, insets);
    }

    public void setPlotInsets(RectangleMargin insets) {
        var oldPlotInsets = this.plotInsets;
        if (Objects.equals(oldPlotInsets, insets)) {
            return;
        }
        this.plotInsets = insets;
        propertyChangeSupport.firePropertyChange("plotInsets", oldPlotInsets, insets);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Draws the chart to a Java2D graphics target.
     *
     * @param g2 the graphics target ({@code null} not permitted).
     * @param bounds the bounds within which the chart should be drawn.
     */
    public void draw(Graphics2D g2, Rectangle2D bounds) {
        // set up any rendering hints we want (should allow this to be controlled externally)
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (background != null) {
            background.draw(g2, bounds);
        }

        // handle background, margin, border, insets and fill
        insets.applyInsets(bounds);

        var plotArea = plotInsets.shrink(bounds);
        
        // get the renderer to draw its dataset in the inner bounds
        if (dataset != null) {
            renderer.draw(this, dataset, g2, plotArea);
        }
    }
}
