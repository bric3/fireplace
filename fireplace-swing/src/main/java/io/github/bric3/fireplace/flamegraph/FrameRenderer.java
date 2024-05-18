package io.github.bric3.fireplace.flamegraph;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;

/**
 * Single frame renderer.
 *
 * @param <T> The type of the frame node (depends on the source of profiling data).
 * @see FlamegraphView
 */
// TODO root frame renderer ?
public interface FrameRenderer<T> {
    /**
     * The size of the gap at between each side of a frame.
     *
     * @return the size of the gap at between each side of a frame.
     * @see #isDrawingFrameGap()
     */
    default int getFrameGapWidth() {return 1;}

    /**
     * Whether a gap is shown between each frame.
     *
     * @return true if a gap is shown between each frame.
     * @see #getFrameGapWidth()
     */
    default boolean isDrawingFrameGap() {return true;}

    /**
     * Compute the height of the frame box according to the passed {@link Graphics2D}.
     *
     * @param g2 the graphics context
     * @return the height of the frame box
     */
    int getFrameBoxHeight(@NotNull Graphics2D g2);

    /**
     * Paint the frame.
     * The {@code renderFlags} parameter can be used to alter the rendering of the frame,
     * this value is computed by the {@link FlamegraphRenderEngine} method, and
     * depends on which settings are used, and the context of the frame. This value can be decoded by
     * using the {@link FrameRenderingFlags} methods.
     *
     * @param g2                    the graphics context
     * @param frameModel            the frame model
     * @param frameRect             the frame region (may fall outside visible area).
     * @param frame                 the frame to paint
     * @param paintableIntersection the intersection between the frame rectangle and the visible region
     *                              (can be used to position the text label).
     * @param renderFlags           the rendering flags (minimap, selection, hovered, highlight, etc.)
     * @see FrameRenderingFlags
     */
    void paintFrame(
            @NotNull Graphics2D g2,
            @NotNull FrameModel<T> frameModel,
            @NotNull RectangularShape frameRect,
            @NotNull FrameBox<T> frame,
            @NotNull Rectangle2D paintableIntersection,
            int renderFlags
    );

    default RectangularShape reusableFrameRect() {
        return new Rectangle2D.Double();
    }
}
