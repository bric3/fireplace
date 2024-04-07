package io.github.bric3.fireplace.charts

import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Rectangle2D
import java.util.function.Supplier

/**
 * A graphical item that can paint itself within an arbitrary two-dimensional
 * rectangle using the Java2D API.  Implementations of this interface can range
 * from simple (for example, filling an area with a single color) to complex
 * (for example, drawing a detailed visualisation for a set of data).
 */
interface RectangleContent {
    /**
     * Draws the item within the specified bounds on the supplied Java2D target.
     *
     * @param g2     the graphics target (`null` not permitted).
     * @param bounds the bounds (`null` not permitted).
     */
    fun draw(g2: Graphics2D, bounds: Rectangle2D)

    companion object {
        /**
         * An object that can fill an area with a single color. To be used as a background.
         * @param color the background color.
         * @return The blank rectangle content.
         */
        fun blankCanvas(color: Supplier<Color?>): RectangleContent {
            return object : RectangleContent {
                override fun draw(g2: Graphics2D, bounds: Rectangle2D) {
                    g2.color = color.get()
                    g2.fill(bounds)
                }
            }
        }
    }
}
