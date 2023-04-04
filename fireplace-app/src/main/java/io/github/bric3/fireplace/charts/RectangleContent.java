package io.github.bric3.fireplace.charts;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.function.Supplier;

/**
 * A graphical item that can paint itself within an arbitrary two-dimensional
 * rectangle using the Java2D API.  Implementations of this interface can range
 * from simple (for example, filling an area with a single color) to complex
 * (for example, drawing a detailed visualisation for a set of data).
 */
public interface RectangleContent {

    /**
     * Draws the item within the specified bounds on the supplied Java2D target.
     *
     * @param g2     the graphics target ({@code null} not permitted).
     * @param bounds the bounds ({@code null} not permitted).
     */
    void draw(Graphics2D g2, Rectangle2D bounds);


    /**
     * An object that can fill an area with a single color. To be used as a background.
     * @param color the background color.
     * @return The blank rectangle content.
     */
    static RectangleContent blankCanvas(Supplier<Color> color) {
        return new RectangleContent() {
            @Override
            public void draw(Graphics2D g2, Rectangle2D bounds) {
                g2.setColor(color.get());
                g2.fill(bounds);
            }
        };
    }
}
