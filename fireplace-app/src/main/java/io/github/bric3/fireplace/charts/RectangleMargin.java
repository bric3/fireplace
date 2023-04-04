package io.github.bric3.fireplace.charts;

import java.awt.geom.Rectangle2D;

/**
 * An object representing rectangular padding or insets.  Padding defines additional area
 * outside a given rectangle, and insets defines an area inside a given rectangle.
 *
 * @param top    the top margin.
 * @param left   the left margin.
 * @param bottom the bottom margin.
 * @param right  the right margin.
 */
public record RectangleMargin(double top, double left, double bottom, double right) {
    /**
     * Calculates a new rectangle by applying the margin as insets on the supplied
     * source.
     *
     * @param source the source rectangle ({@code null} not permitted).
     * @return The new rectangle.
     */
    public Rectangle2D shrink(Rectangle2D source) {
        return shrink(source, null);
    }

    public Rectangle2D shrink(Rectangle2D source, Rectangle2D result) {
        if (result == null) {
            result = (Rectangle2D) source.clone();
        }
        applyInsets(result);
        return result;
    }

    /**
     * Directly updates the supplied rectangle, reducing its bounds by the margin amounts.
     *
     * @param rect the rectangle to be updated.
     */
    public void applyInsets(Rectangle2D rect) {
        rect.setRect(
                rect.getX() + left,
                rect.getY() + top,
                rect.getWidth() - left - right,
                rect.getHeight() - top - bottom
        );
    }

    /**
     * Increases (directly) the supplied rectangle by applying the margin to the bounds.
     *
     * @param rect the rectangle.
     */
    public void applyMargin(Rectangle2D rect) {
        rect.setRect(
                rect.getX() - left,
                rect.getY() - top,
                rect.getWidth() + left + right,
                rect.getHeight() + top + bottom
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RectangleMargin that = (RectangleMargin) o;
        return Double.compare(that.left, left) == 0
               && Double.compare(that.right, right) == 0
               && Double.compare(that.top, top) == 0
               && Double.compare(that.bottom, bottom) == 0;
    }

}
