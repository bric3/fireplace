/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.flamegraph;

import io.github.bric3.fireplace.core.ui.Colors;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Engine that paint a flamegraph.
 *
 * <p>
 * This controls the global flamegraph rendering, and allow to
 * the UI to ask where frames are.
 * The rendering of a single frame is delegated to its {@link FrameRenderer}.
 * </p>
 *
 * <p>
 * Note this class have some field that are public and non final; this allows
 * to quickly toy with this tool, use with caution, or better not at all.
 * </p>
 *
 * @param <T> The type of the frame node (depends on the source of profiling data).
 * @see FlamegraphView
 * @see FrameRenderer
 */
class FlamegraphRenderEngine<T> {
    private final int minimapFrameBoxHeight = 1;
    /**
     * The minimum width threshold for a frame to be rendered.
     */
    private final double frameWidthVisibilityThreshold = 2d;

    /**
     * A flag that controls whether a frame is drawn around the frame that the mouse pointer
     * hovers over.
     */
    public final boolean paintHoveredFrameBorder = false;

    /**
     * The color used to draw a border around the hovered frame.
     */
    public final Supplier<Color> frameBorderColor = () -> Colors.panelForeground;

    /**
     * A flag that controls whether siblings of hovered frames are also rendered
     * as <em>hovered</em>.
     */
    private boolean showHoveredSiblings = true;


    private final FrameRenderer<T> frameRenderer;

    private FrameBox<T> hoveredFrame;
    private FrameBox<T> selectedFrame;


    private Set<FrameBox<T>> toHighlight = Collections.emptySet();
    private Set<FrameBox<T>> hoveredSiblingFrames = Collections.emptySet();


    private FrameModel<T> frameModel;
    private int depth;

    public int getVisibleDepth() {
        return visibleDepth;
    }

    private int visibleDepth;

    /**
     * Cache for the pre-computed depth given the canvas width.
     *
     * <p>
     *     This cache leverages the WeakHashMap to cleanup keys that
     *     are not anymore referenced, this is is useful when the canvas
     *     changes width to avoid re-computation (i.e traversing the framebox
     *     list again).
     * </p>
     * <p>
     *     <strong>Note about {@link Integer} cache:</strong>
     *     The default Integer cache goes from -128 to 127 by default (this is tunable),
     *     these entries won't be reclaimed by GC ! However his code assumes the
     *     canvas width will be higher, or way higher, in practice than 127.
     *     If a width in the Integer cache range is entered this means it's
     *     value won't be reclaimed as well, this is might be acceptable given the
     *     size of the value, an Integer.
     *     Currently there's no contingency plan if this get a problem, but if
     *     if is we might want to look at VM params like {@code -XX:AutoBoxCacheMax}
     *     and/or {@code java.lang.Integer.IntegerCache.high} property.
     * </p>
     */
    private final WeakHashMap<Integer, Integer> visibleDepthCache = new WeakHashMap<>();

    private boolean icicle = true;


    /**
     * Creates a new instance of the flame graph render.
     *
     * <p>
     * To render frames, the render <strong>must</strong> be initialized
     * first with the {@link #init(FrameModel)} method.
     * </p>
     *
     * @param frameRenderer a configured single frame renderer.
     * @see #init(FrameModel)
     */
    public FlamegraphRenderEngine(FrameRenderer<T> frameRenderer) {
        this.frameRenderer = Objects.requireNonNull(frameRenderer, "frameRenderer");
        reset();
    }

    /**
     * Initializes the render with the given frame model.
     *
     * @param frameModel the frames to be displayed.
     * @see #reset()
     */
    public FlamegraphRenderEngine<T> init(FrameModel<T> frameModel) {
        this.frameModel = Objects.requireNonNull(frameModel, "frameModel");
        this.depth = frameModel.frames.stream().mapToInt(fb -> fb.stackDepth).max().orElse(0) + 1;
        visibleDepth = depth;
        visibleDepthCache.clear();
        selectedFrame = null;
        hoveredFrame = null;
        toHighlight = Collections.emptySet();
        hoveredSiblingFrames = Collections.emptySet();
        return this;
    }

    /**
     * The inverse operation of {@link #init(FrameModel)}.
     *
     * @see #init(FrameModel)
     */
    public FlamegraphRenderEngine<T> reset() {
        this.frameModel = FrameModel.empty();
        this.depth = 1;
        visibleDepth = 1;
        visibleDepthCache.clear();
        return this;
    }

    /**
     * Returns the height of the minimap for the specified width.
     *
     * @param thumbnailWidth the minimap width.
     * @return The height.
     */
    public int computeVisibleFlamegraphMinimapHeight(int thumbnailWidth) {
        checkReady();
        assert thumbnailWidth > 0 : "minimap width must be superior to 0";

        // Somewhat it is a best-effort to draw something that shows
        // something representative. The canvas recompute this, if its
        // size change so there's a chance the minimap can be updated
        // with higher details (previously invisible frames)
        return visibleDepth * minimapFrameBoxHeight;
    }

    /**
     * Computes the height of the flamegraph for the specified width (just the height needs calculating,
     * and this depends on the font metrics).
     *
     * <p>
     *     This method doesn't update internal fields.
     * </p>
     *
     * @param g2           the graphics target ({@code null} not permitted), used for font metrics.
     * @param canvasWidth  the current canvas width
     * @return The height of the visible frames in this flamegraph
     */
    public int computeVisibleFlamegraphHeight(
            Graphics2D g2,
            int canvasWidth
    ) {
        return computeVisibleFlamegraphHeight(g2, canvasWidth, false);
    }

    /**
     * Computes the height of the flamegraph for the specified width (just the height needs calculating,
     * and this depends on the font metrics).
     *
     * @param g2           the graphics target ({@code null} not permitted), used for font metrics.
     * @param canvasWidth  the current canvas width
     * @param update       whether to update the internal fields.
     * @return The height of the visible frames in this flamegraph
     */
    public int computeVisibleFlamegraphHeight(
            Graphics2D g2,
            int canvasWidth,
            boolean update
    ) {
        checkReady();

        var visibleDepth = visibleDepthCache.computeIfAbsent(canvasWidth, width -> {
            // as this method is invoked during layout, the dimension can be 0
            if (canvasWidth == 0) {
                return 0;
            }

            // compute the canvas height for the flamegraph width
            var vDepth = 0;
            for (var frame : frameModel.frames) {
                if (canvasWidth * (frame.endX - frame.startX) < frameWidthVisibilityThreshold) {
                    continue;
                }
                vDepth = Math.max(vDepth, frame.stackDepth + 1);
            }
            vDepth = Math.min(vDepth, depth);

            return vDepth;
        });

        if (update) {
            this.visibleDepth = visibleDepth;
        }

        return visibleDepth * frameRenderer.getFrameBoxHeight(g2);
    }

    /**
     * Draws the subset of the flame graph that fits within {@code viewRect} assuming that the whole
     * flame graph is being rendered within the specified {@code bounds}.
     *
     * @param g2       the graphics target ({@code null} not permitted).
     * @param bounds   the flame graph bounds ({@code null} not permitted).
     * @param viewRect the subset that is being viewed/rendered ({@code null} not permitted).
     */
    public void paint(Graphics2D g2, Rectangle2D bounds, Rectangle2D viewRect) {
        internalPaint(g2, bounds, viewRect, false, icicle);
    }

    /**
     * Draws the subset of the flame graph that fits within {@code viewRect} assuming that the whole
     * flame graph is being rendered within the specified {@code bounds}.
     *
     * @param g2       the graphics target ({@code null} not permitted).
     * @param size     the flame graph bounds ({@code null} not permitted).
     */
    public void paintToImage(Graphics2D g2, Rectangle2D size, boolean icicle) {
        internalPaint(g2, size, size, false, icicle);
    }

    /**
     * Paints the minimap (always the entire flame graph).
     *
     * @param g2     the graphics target ({@code null} not permitted).
     * @param bounds the bounds ({@code null} not permitted).
     */
    public void paintMinimap(Graphics2D g2, Rectangle2D bounds) {
        internalPaint(g2, bounds, bounds, true, icicle);
    }

    private void internalPaint(
            Graphics2D g2,
            Rectangle2D bounds,
            Rectangle2D viewRect,
            boolean minimapMode,
            boolean icicle
    ) {
        checkReady();
        if (frameModel.frames.isEmpty()) {
            return;
        }

        Objects.requireNonNull(g2);
        Objects.requireNonNull(bounds);
        Objects.requireNonNull(viewRect);
        Graphics2D g2d = (Graphics2D) g2.create();
        identifyDisplayScale(g2d);
        var frameBoxHeight = minimapMode ? minimapFrameBoxHeight : frameRenderer.getFrameBoxHeight(g2);
        var flameGraphWidth = minimapMode ? viewRect.getWidth() : bounds.getWidth();
        var frameRect = new Rectangle2D.Double(); // reusable rectangle

        var frames = frameModel.frames;
        // paint root
        {
            var rootFrame = frames.get(0);

            int internalPadding = 0; // Remove ?
            frameRect.x = (int) (flameGraphWidth * rootFrame.startX) + internalPadding;
            frameRect.width = ((int) (flameGraphWidth * rootFrame.endX)) - frameRect.x - internalPadding;
            frameRect.y = computeFrameRectY(bounds, frameBoxHeight, rootFrame.stackDepth, icicle);
            frameRect.height = frameBoxHeight;

            var paintableIntersection = viewRect.createIntersection(frameRect);
            if (!paintableIntersection.isEmpty()) {
                frameRenderer.paintFrame(
                        g2d,
                        frameModel,
                        frameRect,
                        rootFrame,
                        paintableIntersection,
                        FrameRenderingFlags.toFlags(
                                minimapMode,
                                false,
                                false, // never make root part of highlighting
                                hoveredFrame == rootFrame,
                                false,
                                selectedFrame != null,
                                selectedFrame == rootFrame,
                                frameRect.getX() == paintableIntersection.getX()
                        )
                );
            }
        }

        // paint real flames
        for (int i = 1; i < frames.size(); i++) {
            var frame = frames.get(i);

            frameRect.x = (int) (flameGraphWidth * frame.startX); //+ internalPadding;
            frameRect.width = ((int) (flameGraphWidth * frame.endX)) - frameRect.x; //- internalPadding;

            if ((frameRect.width < frameWidthVisibilityThreshold) && !minimapMode) {
                continue;
            }

            frameRect.y = computeFrameRectY(bounds, frameBoxHeight, frame.stackDepth, icicle);
            frameRect.height = frameBoxHeight;

            var paintableIntersection = viewRect.createIntersection(frameRect);
            if (!paintableIntersection.isEmpty()) {
                frameRenderer.paintFrame(
                        g2d,
                        frameModel,
                        frameRect,
                        frame,
                        paintableIntersection,
                        // choose font depending on whether the left-side of the frame is clipped
                        FrameRenderingFlags.toFlags(
                                minimapMode,
                                !toHighlight.isEmpty(),
                                toHighlight.contains(frame),
                                hoveredFrame == frame,
                                hoveredFrame != frame && hoveredSiblingFrames.contains(frame),
                                selectedFrame != null,
                                (selectedFrame != null
                                 && frame.stackDepth >= selectedFrame.stackDepth
                                 && frame.startX >= selectedFrame.startX
                                 && frame.endX <= selectedFrame.endX),
                                frameRect.getX() < paintableIntersection.getX()
                        )
                );
            }
        }

        if (!minimapMode) {
            paintHoveredFrameBorder(g2d, bounds, viewRect, flameGraphWidth, frameBoxHeight, frameRect, icicle);
        }

        g2d.dispose();
    }

    private static int computeFrameRectY(Rectangle2D bounds, int frameBoxHeight, int stackDepth, boolean icicle) {
        if (icicle) {
            return frameBoxHeight * stackDepth;
        }

        var flamegraphHeight = bounds.getHeight();

        return (int) (flamegraphHeight - frameBoxHeight) - (frameBoxHeight * stackDepth);
    }

    private void checkReady() {
        assert frameModel != null : "The flamegraph is not initialized, call init(FrameModel) first";
    }

    // TODO move to FrameRenderer
    private void paintHoveredFrameBorder(
            Graphics2D g2,
            Rectangle2D bounds,
            Rectangle2D viewRect,
            double flameGraphWidth,
            int frameBoxHeight,
            Rectangle2D frameRect,
            boolean icicle
    ) {
        if (hoveredFrame == null || !paintHoveredFrameBorder) {
            return;
        }
        var gapThickness = frameRenderer.isDrawingFrameGap() ? frameRenderer.getFrameGapWidth() : 0;

        /*
         * DISCLAIMER: it happens that drawing perfectly aligned rect is very difficult with
         * Graphics2D.
         * 1. I t may depend on the current Screen scale (Retina is 2, other monitors like 1x)
         *    g2.getTransform().getScaleX() / getScaleY(), (so in pixels that would 1 / scale)
         * 2. When drawing a rectangle, it seems that the current sun implementation draws
         *    the line on 50% outside and 50% inside. I don;t know how to avoid that
         *
         * In some of my test what is ok on a retina is ugly on a 1.x monitor,
         * adjusting the rectangle with the scale wasn't very pretty, as sometimes
         * the border starts inside the frame.
         * Played with Area subtraction, but this wasn't successful.
         */

        var x = flameGraphWidth * hoveredFrame.startX;
        var y = computeFrameRectY(bounds, frameBoxHeight, hoveredFrame.stackDepth, icicle);
        var w = (flameGraphWidth * hoveredFrame.endX) - x - gapThickness;
        var h = frameBoxHeight - gapThickness;
        frameRect.setRect(x, y, w, h);

        if ((frameRect.getWidth() < frameWidthVisibilityThreshold)) {
            return;
        }

        if (viewRect.intersects(frameRect)) {
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setColor(frameBorderColor.get());
            g2.draw(frameRect);
        }
    }

    private void identifyDisplayScale(Graphics2D g2) {
        // if > 1 we're on a HiDPI display
        // https://github.com/libgdx/libgdx/commit/2bc16a08961dd303afe2d1c8df96a50d8cd639db
        var transform = g2.getTransform();
        var scaleX = transform.getScaleX();
        var scaleY = transform.getScaleY();
    }

    /**
     * Creates and returns the bounds for the specified frame, assuming that the whole flame graph is to
     * be rendered within the specified {@code bounds}.
     *
     * @param g2     the graphics target ({@code null} not permitted).
     * @param bounds the flame graph bounds ({@code null} not permitted)
     * @param frame  the frame ({@code null} not permitted)
     * @return The bounds for the specified frame.
     */
    public Rectangle getFrameRectangle(
            Graphics2D g2,
            Rectangle2D bounds,
            FrameBox<T> frame
    ) {
        checkReady();
        // TODO delegate to frame renderer ?

        var frameBoxHeight = frameRenderer.getFrameBoxHeight(g2);
        var frameGapWidth = frameRenderer.getFrameGapWidth();

        var rect = new Rectangle();
        rect.x = (int) (bounds.getWidth() * frame.startX) - frameGapWidth; // + internalPadding;
        rect.width = (int) (bounds.getWidth() * frame.endX) - rect.x + 2 * frameGapWidth; // - internalPadding;
        rect.y = computeFrameRectY(bounds, frameBoxHeight, frame.stackDepth, icicle) - frameGapWidth;
        rect.height = frameBoxHeight + 2 * frameGapWidth;
        return rect;
    }

    /**
     * Returns the frame at the specified point, assuming that the full flame graph is rendered within
     * the specified bounds.
     *
     * @param g2     the graphics target ({@code null} not permitted).
     * @param bounds the bounds in which the full flame graph is rendered ({@code null} not permitted).
     * @param point  the point of interest ({@code null} not permitted).
     * @return An optional frame box.
     */
    public Optional<FrameBox<T>> getFrameAt(
            Graphics2D g2,
            Rectangle2D bounds,
            Point point
    ) {
        checkReady();

        int depth = computeFrameDepth(g2, bounds, point);
        double xLocation = point.x / bounds.getWidth();
        double visibilityThreshold = frameWidthVisibilityThreshold / bounds.getWidth();

        return frameModel.frames.stream()
                                .filter(node -> node.stackDepth == depth
                                                && node.startX <= xLocation
                                                && xLocation <= node.endX
                                                && visibilityThreshold < node.endX - node.startX)
                                .findFirst();
    }

    private int computeFrameDepth(Graphics2D g2, Rectangle2D bounds, Point point) {
        if (icicle) {
            return point.y / frameRenderer.getFrameBoxHeight(g2);
        }
        return (int) ((bounds.getHeight() - point.y)) / frameRenderer.getFrameBoxHeight(g2);
    }

    /**
     * Toggles the selection status of the frame at the specified point, if there is one, and notifies
     * the supplied consumer.
     *
     * @param g2             the graphics target ({@code null} not permitted).
     * @param bounds         the bounds in which the full flame graph is rendered ({@code null} not permitted).
     * @param point          the point of interest ({@code null} not permitted).
     * @param toggleConsumer the function that is called to notify about the frame selection ({@code null} not permitted).
     */
    public void toggleSelectedFrameAt(
            Graphics2D g2,
            Rectangle2D bounds,
            Point point,
            BiConsumer<FrameBox<T>, Rectangle> toggleConsumer
    ) {
        getFrameAt(g2, bounds, point)
                .ifPresent(frame -> {
                    selectedFrame = selectedFrame == frame ? null : frame;
                    toggleConsumer.accept(frame, getFrameRectangle(g2, bounds, frame));
                });
    }

    /**
     * Toggles the hover status of the frame
     *
     * @param frame         the hovered frame, or null to clear old hover
     * @param g2            the graphics target ({@code null} not permitted).
     * @param bounds        the bounds in which the full flame graph is rendered ({@code null} not permitted).
     * @param hoverConsumer the function that is called to notify about the frame selection ({@code null} not permitted).
     */
    public void hoverFrame(
            FrameBox<T> frame,
            Graphics2D g2,
            Rectangle2D bounds,
            Consumer<Rectangle> hoverConsumer
    ) {
        checkReady();

        if (frame == null) {
            stopHover(g2, bounds, hoverConsumer);
            return;
        }
        var oldHoveredFrame = hoveredFrame;
        if (frame == oldHoveredFrame) {
            return;
        }
        var oldHoveredSiblingFrames = hoveredSiblingFrames;
        hoveredFrame = frame;
        hoveredSiblingFrames = getSiblingFrames(frame);
        if (hoverConsumer != null) {
            hoveredSiblingFrames.forEach(hovered -> hoverConsumer.accept(getFrameRectangle(g2, bounds, hovered)));
            if (oldHoveredFrame != null) {
                oldHoveredSiblingFrames.forEach(hovered -> hoverConsumer.accept(getFrameRectangle(g2, bounds, hovered)));
            }
        }
    }

    /**
     * Clears the hovered frame (to indicate that no frame is hovered).
     */
    public void stopHover(Graphics2D g2, Rectangle2D bounds, Consumer<Rectangle> hoverConsumer) {
        checkReady();

        var oldHoveredFrame = hoveredFrame;
        var oldHoveredSiblingFrame = hoveredSiblingFrames;
        hoveredFrame = null;
        hoveredSiblingFrames = Collections.emptySet();
        if (oldHoveredFrame != null) {
            oldHoveredSiblingFrame.forEach(hovered -> hoverConsumer.accept(getFrameRectangle(g2, bounds, hovered)));
        }
    }

    private Set<FrameBox<T>> getSiblingFrames(FrameBox<T> frame) {
        if (!showHoveredSiblings) {
            return Set.of(frame);
        }

        return frameModel.frames.stream()
                                .filter(node -> frameModel.frameEquality.equal(node, frame))
                                .collect(Collectors.toSet());
    }

    /**
     * Finds the frame at {@code point} and, if there is one, returns the new canvas size and the offset that
     * will make the frame fully visible at the top of the specified {@code viewRect}.  A side effect of this
     * method is that the frame is marked as the "selected" frame.
     *
     * @param g2       the graphics target ({@code null} not permitted).
     * @param bounds   the bounds within which the flame graph is currently rendered.
     * @param viewRect the subset of the bounds that is actually visible
     * @param point    the coordinates at which to look for a frame.
     * @return An optional zoom target.
     */
    public Optional<ZoomTarget> calculateZoomTargetForFrameAt(
            Graphics2D g2,
            Rectangle2D bounds,
            Rectangle2D viewRect,
            Point point
    ) {
        checkReady();

        return getFrameAt(g2, bounds, point).map(frame -> {
            // TODO refactor to make frame selection explicit, possibly via toggleSelectedFrameAt
            this.selectedFrame = frame;
            return calculateZoomTargetFrame(g2, bounds, viewRect, frame, 0, 0);
        });
    }

    /**
     * As for {@link #calculateZoomTargetForFrameAt(Graphics2D, Rectangle2D, Rectangle2D, Point)} but
     * only adjusts the horizontal zoom.
     *
     * @param g2       the graphics target ({@code null} not permitted).
     * @param bounds   the bounds within which the flame graph is currently rendered.
     * @param viewRect the subset of the bounds that is actually visible
     * @param point    the coordinates at which to look for a frame.
     * @return An optional zoom target.
     */
    public Optional<ZoomTarget> calculateHorizontalZoomTargetForFrameAt(
            Graphics2D g2,
            Rectangle2D bounds,
            Rectangle2D viewRect,
            Point point
    ) {
        checkReady();

        return getFrameAt(g2, bounds, point).map(frame -> {
            this.selectedFrame = frame;
            return calculateZoomTargetFrame(g2, bounds, viewRect, frame, -1, 0);
        });
    }

    /**
     * Compute the {@code ZoomTarget} for the passed frame.
     * <p>
     * Returns the new canvas size and the offset that
     * will make the frame fully visible at the top of the specified {@code viewRect}.
     *
     * @param g2               the graphics target ({@code null} not permitted).
     * @param bounds           the bounds within which the flame graph is currently rendered.
     * @param viewRect         the subset of the bounds that is actually visible
     * @param frame            the frame.
     * @param contextBefore    number of contextual parents, if -1 then zoom will be horizontal only
     * @param contextLeftRight the contextual frames on the left and right (unused at this time)
     * @return A zoom target.
     */
    public ZoomTarget calculateZoomTargetFrame(
            Graphics2D g2,
            Rectangle2D bounds,
            Rectangle2D viewRect,
            FrameBox<T> frame,
            int contextBefore,
            int contextLeftRight
    ) {
        checkReady();

        var frameWidthX = frame.endX - frame.startX;
        var frameBoxHeight = frameRenderer.getFrameBoxHeight(g2);
        var factor = getScaleFactor(viewRect.getWidth(), bounds.getWidth(), frameWidthX);
        // calculate the depth of the frame(s) at the top of the current view
        int frameDepthAtTopOfView = icicle ?
                (int) viewRect.getY() / frameBoxHeight
                : (int) (bounds.getHeight() - viewRect.getMaxY()) / frameBoxHeight;
        // calculate the new width so the current frame will occupy the full width of the view
        var newViewWidth = (int) (bounds.getWidth() * factor);
        var newViewHeight = computeVisibleFlamegraphHeight(
                g2,
                newViewWidth
        );

        var newDimension = new Rectangle2D.Double(
                bounds.getX(),
                bounds.getY(),
                newViewWidth,
                newViewHeight
        );
        int newFrameDepthAtTopOfView = contextBefore >= 0 ?
                Math.max(frame.stackDepth - contextBefore, 0) :
                frameDepthAtTopOfView; // contextBefore is -1, so keep the same vertical location

        var frameY = computeFrameRectY(
                newDimension,
                frameBoxHeight,
                newFrameDepthAtTopOfView, icicle
        );
        var viewLocationY = icicle ?
                            Math.max(0, frameY) :
                            Math.min(
                                    (int) (newViewHeight - viewRect.getHeight()),
                                    (int) (frameY + frameBoxHeight - viewRect.getHeight())
                            );

        return new ZoomTarget(
                - (int) (frame.startX * newViewWidth),
                - viewLocationY,
                newViewWidth,
                newViewHeight
        );
    }

    /**
     * Compute the scale factor (or zoom factor)
     * <p>
     * The new scale factor is
     * <pre>
     *
     *                viewRect.width
     * factor = ----------------------------
     *           frameWidthX * bounds.width
     * </pre>
     *
     * Note that to retrieve the zoom factor one should use {@code 1 / factor}.
     */
    protected static double getScaleFactor(double visibleWidth, double canvasWidth, double frameWidthX) {
        return visibleWidth / (canvasWidth * frameWidthX);
    }

    public void setHighlightFrames(Set<FrameBox<T>> toHighlight, String searchedText) {
        this.toHighlight = Objects.requireNonNull(toHighlight);
    }

    public void setShowHoveredSiblings(boolean showHoveredSiblings) {
        this.showHoveredSiblings = showHoveredSiblings;
    }

    public boolean isShowHoveredSiblings() {
        return showHoveredSiblings;
    }

    public FrameRenderer<T> getFrameRenderer() {
        return frameRenderer;
    }

    public void setIcicle(boolean icicle) {
        this.icicle = icicle;
    }

    public boolean isIcicle() {
        return icicle;
    }

    public FrameModel<T> getFrameModel() {
        return frameModel;
    }
}
