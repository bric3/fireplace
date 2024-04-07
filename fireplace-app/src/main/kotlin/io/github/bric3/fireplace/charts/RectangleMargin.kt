package io.github.bric3.fireplace.charts

import java.awt.geom.Rectangle2D

/**
 * An object representing rectangular padding or insets.  Padding defines additional area
 * outside a given rectangle, and insets defines an area inside a given rectangle.
 *
 * @param top    the top margin.
 * @param left   the left margin.
 * @param bottom the bottom margin.
 * @param right  the right margin.
 */
@JvmRecord
data class RectangleMargin(val top: Double, val left: Double, val bottom: Double, val right: Double) {
    /**
     * Calculates a new rectangle by applying the margin as insets on the supplied
     * source.
     *
     * @param source the source rectangle (`null` not permitted).
     * @return The new rectangle.
     */
    @JvmOverloads
    fun shrink(source: Rectangle2D, result: Rectangle2D? = null): Rectangle2D {
        return (result ?: source.clone() as Rectangle2D).also {
            applyInsets(it)
        } 
    }

    /**
     * Directly updates the supplied rectangle, reducing its bounds by the margin amounts.
     *
     * @param rect the rectangle to be updated.
     */
    fun applyInsets(rect: Rectangle2D?) {
        rect!!.setRect(
            rect.x + left,
            rect.y + top,
            rect.width - left - right,
            rect.height - top - bottom
        )
    }

    /**
     * Increases (directly) the supplied rectangle by applying the margin to the bounds.
     *
     * @param rect the rectangle.
     */
    fun applyMargin(rect: Rectangle2D) {
        rect.setRect(
            rect.x - left,
            rect.y - top,
            rect.width + left + right,
            rect.height + top + bottom
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as RectangleMargin
        return that.left.compareTo(left) == 0
                && that.right.compareTo(right) == 0
                && that.top.compareTo(top) == 0
                && that.bottom.compareTo(bottom) == 0
    }

    override fun hashCode(): Int {
        var result = top.hashCode()
        result = 31 * result + left.hashCode()
        result = 31 * result + bottom.hashCode()
        result = 31 * result + right.hashCode()
        return result
    }
}
