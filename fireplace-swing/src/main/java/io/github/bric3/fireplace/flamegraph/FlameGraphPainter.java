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
import io.github.bric3.fireplace.core.ui.StringClipper;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Engine that paint a flamegraph.
 * <p>
 * Note this class a some field that are public and non final; this allows
 * to quickly toy with this tool, use with caution, or not at all.
 * </p>
 *
 * @param <T> The type of the frame node (depends on the source of profiling data).
 * @see FlameGraph
 */
class FlameGraphPainter<T> {
    /**
     * A flag that controls whether a gap is shown at the right and bottom of each frame.
     */
    public boolean frameGapEnabled = true;

    // /**
    //  * The size of the gap at the right and bottom of each frame.
    //  */
    // public int frameGapWidth = 1;

    /**
     * A flag that controls whether a frame is drawn around the frame that the mouse pointer
     * hovers over.
     */
    public boolean paintHoveredFrameBorder = true;

    /**
     * The width of the border drawn around the hovered frame.
     */
    public int frameBorderWidth = 1;

    /**
     * The stroke used to draw a border around the hovered frame.
     */
    public Stroke frameBorderStroke = new BasicStroke(frameBorderWidth);

    /**
     * The color used to draw a border around the hovered frame.
     */
    public Color frameBorderColor = Colors.panelForeground;

    private final int depth;
    private int visibleDepth;
    // private final int textPadding = 2;

    /**
     * The minimum width threshold for a frame to be rendered.
     */
    protected int frameWidthVisibilityThreshold = 2;
    private final int minimapFrameBoxHeight = 1;

    private FrameBox<T> hoveredFrame;
    private FrameBox<T> selectedFrame;
    private double scaleX;
    private double scaleY;

    private final List<FrameBox<T>> frames;
    private final NodeDisplayStringProvider<T> nodeToTextProvider;
    Function<FrameBox<T>, Color> frameColorFunction;

    /**
     * Internal padding with the component bounds.
     */
    private final int internalPadding = 2;

    /**
     * A flag that controls the display of rendering info and statistics.
     *
     * @see FlameGraph#SHOW_STATS
     */
    protected boolean paintDetails = true;
    private Set<FrameBox<T>> toHighlight = Collections.emptySet();
    private FrameRender<T> frameRenderer;

    /**
     * Creates a new instance to render the specified list of frames.
     *
     * @param frames             the frames to be displayed.
     * @param nodeToTextProvider functions that create a label for a node
     * @param frameColorFunction a function that maps frames to colors.
     */
    public FlameGraphPainter(
            List<FrameBox<T>> frames,
            NodeDisplayStringProvider<T> nodeToTextProvider,
            Function<FrameBox<T>, Color> frameColorFunction,
            FrameRender<T> frameRenderer
    ) {
        this.frameRenderer = frameRenderer;

        // this.frameLabelFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        // this.partialFrameLabelFont = new Font(Font.SANS_SERIF, Font.ITALIC, 12);
        // this.highlightedFrameLabelFont = new Font(Font.SANS_SERIF, Font.PLAIN | Font.BOLD, 12);
        // this.highlightedPartialFrameLabelFont = new Font(Font.SANS_SERIF, Font.ITALIC | Font.BOLD, 12);

        this.frames = frames;
        this.depth = this.frames.stream().mapToInt(fb -> fb.stackDepth).max().orElse(0);
        visibleDepth = depth;
        this.nodeToTextProvider = nodeToTextProvider;
        this.frameColorFunction = frameColorFunction;
        updateUI();
    }
    
    /**
     * This method is used to resync colors when the LaF changes
     */
    public void updateUI() {
    }


    /**
     * Returns the height of the minimap for the specified width.
     *
     * @param thumbnailWidth the minimap width.
     * @return The height.
     */
    public int computeFlameGraphMinimapHeight(int thumbnailWidth) {
        assert thumbnailWidth > 0 : "minimap width must be superior to 0";

        //        var visibleDepth = 0;
        //        for (var frame : frames) {
        //            if (thumbnailWidth * (frame.endX - frame.startX) < 0.1 /* thumbnail visibility threshold */) {
        //                continue;
        //            }
        //
        //            visibleDepth = Math.max(visibleDepth, frame.stackDepth);
        //        }
        //        visibleDepth = Math.min(visibleDepth, depth);

        return visibleDepth * minimapFrameBoxHeight;
    }

    /**
     * Computes the dimensions of the flamegraph for the specified width (just the height needs calculating,
     * and this depends on the font metrics).
     *
     * @param g2     the graphics target ({@code null} not permitted).
     * @param width  the preferred width.
     * @param insets the insets.
     * @return The dimensions required to draw the whole fra
     */
    public Dimension computeFlameGraphDimension(Graphics2D g2, int width, Insets insets) {
        // as this method is invoked during layout, the dimension can be 0
        if (width == 0) {
            return new Dimension();
        }

        return new Dimension(width + insets.left + insets.right, depth * frameRenderer.getFrameBoxHeight(g2) + insets.top + insets.bottom);
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
        internalPaint(g2, bounds, viewRect, false);
    }

    /**
     * Paints the minimap (always the entire flame graph).
     *
     * @param g2     the graphics target ({@code null} not permitted).
     * @param bounds the bounds ({@code null} not permitted).
     */
    public void paintMinimap(Graphics2D g2, Rectangle2D bounds) {
        internalPaint(g2, bounds, bounds, true);
    }

    private void internalPaint(
            Graphics2D g2,
            Rectangle2D bounds,
            Rectangle2D viewRect,
            boolean minimapMode
    ) {
        Objects.requireNonNull(g2);
        Objects.requireNonNull(bounds);
        Objects.requireNonNull(viewRect);
        long start = System.currentTimeMillis();
        Graphics2D g2d = (Graphics2D) g2.create();
        identifyDisplayScale(g2d);
        var frameBoxHeight = minimapMode ? minimapFrameBoxHeight : frameRenderer.getFrameBoxHeight(g2);
        var flameGraphWidth = minimapMode ? viewRect.getWidth() : bounds.getWidth();
        var frameRect = new Rectangle2D.Double(); // reusable rectangle


        // paint root
        {
            var rootFrame = frames.get(0);
            frameRect.x = (int) (flameGraphWidth * rootFrame.startX) + internalPadding;
            frameRect.width = ((int) (flameGraphWidth * rootFrame.endX)) - frameRect.x - internalPadding;
            frameRect.y = frameBoxHeight * rootFrame.stackDepth;
            frameRect.height = frameBoxHeight;

            var intersection = viewRect.createIntersection(frameRect);
            if (!intersection.isEmpty()) {
                paintFrame(
                        g2d,
                        frameRect,
                        rootFrame,
                        intersection,
                        tweakLabelFont(frameRect, intersection, false),
                        tweakBgColor(frameColorFunction.apply(rootFrame),
                                     hoveredFrame == rootFrame,
                                     false,
                                     selectedFrame != null && rootFrame.stackDepth < selectedFrame.stackDepth),
                        minimapMode
                );
            }
        }

        // paint real flames
        for (int i = 1; i < frames.size(); i++) {
            var frame = frames.get(i);
            // TODO Can we do cheaper checks like depth is outside range etc

            frameRect.x = (int) (flameGraphWidth * frame.startX); //+ internalPadding;
            frameRect.width = ((int) (flameGraphWidth * frame.endX)) - frameRect.x; //- internalPadding;

            if ((frameRect.width < frameWidthVisibilityThreshold) && !minimapMode) {
                continue;
            }

            frameRect.y = frameBoxHeight * frame.stackDepth;
            frameRect.height = frameBoxHeight;

            var paintableIntersection = viewRect.createIntersection(frameRect);
            if (!paintableIntersection.isEmpty()) {
                paintFrame(
                        g2d,
                        frameRect,
                        frame,
                        paintableIntersection,
                        // choose font depending on whether the left-side of the frame is clipped
                        tweakLabelFont(frameRect, paintableIntersection, toHighlight.contains(frame)),
                        tweakBgColor(frameColorFunction.apply(frame),
                                     hoveredFrame == frame,
                                     toHighlight.contains(frame),
                                     selectedFrame != null && (
                                             frame.stackDepth < selectedFrame.stackDepth
                                             || frame.endX <= selectedFrame.startX
                                             || frame.startX >= selectedFrame.endX)),
                        minimapMode
                );
            }
        }

        if (!minimapMode) {
            paintHoveredFrameBorder(g2d, viewRect, flameGraphWidth, frameBoxHeight, frameRect);
        }

        if (!minimapMode && paintDetails) {
            // timestamp
            var zoomFactor = bounds.getWidth() / viewRect.getWidth();
            var stats = "FrameGraph width " + flameGraphWidth +
                        " Zoom Factor " + zoomFactor +
                        " Coordinate (" + viewRect.getX() + ", " + viewRect.getY() + ") " +
                        "size (" + viewRect.getWidth() + ", " + viewRect.getHeight() + "), " +
                        "Draw time: " + (System.currentTimeMillis() - start) + " ms";
            var nowWidth = g2d.getFontMetrics(frameRenderer.getFrameLabelFont()).stringWidth(stats);
            g2d.setColor(Color.DARK_GRAY);
            var frameTextPadding = frameRenderer.getFrameTextPadding();
            g2d.fillRect((int) (viewRect.getX() + viewRect.getWidth() - nowWidth - frameTextPadding * 2),
                         (int) (viewRect.getY() + viewRect.getHeight() - frameBoxHeight),
                         nowWidth + frameTextPadding * 2,
                         frameBoxHeight);

            g2d.setColor(Color.YELLOW);
            g2d.drawString(stats,
                           (int) (viewRect.getX() + viewRect.getWidth() - nowWidth - frameTextPadding),
                           (int) (viewRect.getY() + viewRect.getHeight() - frameTextPadding));
        }

        g2d.dispose();
    }

    // TODO move this method to renderer ?
    private Font tweakLabelFont(
            Rectangle2D rect,
            Rectangle2D intersection,
            boolean highlighted
    ) {
        if (highlighted) {
            if (rect.getX() == intersection.getX()) {
                return frameRenderer.getHighlightedFrameLabelFont();
            } else {
                return frameRenderer.getHighlightedPartialFrameLabelFont();
            }
        }
        if (rect.getX() == intersection.getX()) {
            return frameRenderer.getFrameLabelFont();
        } else {
            return frameRenderer.getPartialFrameLabelFont();
        }
    }

    private void paintHoveredFrameBorder(
            Graphics2D g2,
            Rectangle2D viewRect,
            double flameGraphWidth,
            int frameBoxHeight,
            Rectangle2D frameRect
    ) {
        if (hoveredFrame == null || !paintHoveredFrameBorder) {
            return;
        }
        var gapThickness = frameGapEnabled ? frameRenderer.getFrameGapWidth() : 0;

        // DISCLAIMER: it happens that drawing perfectly aligned rect is very difficult with
        // Graphics2D.
        // 1. I t may depend on the current Screen scale (Retina is 2, other monitors like 1x)
        //    g2.getTransform().getScaleX() / getScaleY(), (so in pixels that would 1 / scale)
        // 2. When drawing a rectangle, it seems that the current sun implementation draws
        //    the line on 50% outside and 50% inside. I don;t know how to avoid that
        //
        // In some of my test what is ok on a retina is ugly on a 1.x monitor,
        // adjusting the rectangle with the scale wasn't very pretty, as sometime
        // the border starts inside the frame.
        // Played with Area subtraction, but this wasn't successful.

        var x = flameGraphWidth * hoveredFrame.startX;
        var y = frameBoxHeight * hoveredFrame.stackDepth;
        var w = (flameGraphWidth * hoveredFrame.endX) - x - gapThickness;
        var h = frameBoxHeight - gapThickness;
        frameRect.setRect(x, y, w, h);

        if ((frameRect.getWidth() < frameWidthVisibilityThreshold)) {
            return;
        }

        if (viewRect.intersects(frameRect)) {
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setColor(frameBorderColor);
            // TODO use floor / ceil ?
            g2.drawRect((int) x, (int) y, (int) w, (int) h);
        }
    }

    private void identifyDisplayScale(Graphics2D g2) {
        // if > 1 we're on a HiDPI display
        // https://github.com/libgdx/libgdx/commit/2bc16a08961dd303afe2d1c8df96a50d8cd639db
        var transform = g2.getTransform();
        scaleX = transform.getScaleX();
        scaleY = transform.getScaleY();
    }

    private Color tweakBgColor(
            Color bgColor,
            boolean hovered,
            boolean highlighted,
            boolean dimmed
    ) {
        Color color = bgColor;
        if (dimmed) {
            color = Colors.blend(bgColor, Colors.translucent_black_80);
        }
        if (!toHighlight.isEmpty()) {
            color = Colors.isDarkMode() ? Colors.blend(color, Colors.translucent_black_B0) : Colors.blend(color, Color.WHITE);
            if (highlighted) {
                color = bgColor;
            }
        }
        if (hovered) {
            color = Colors.blend(color, Colors.translucent_black_40);
        }
        return color;
    }

    /**
     * Paints the frame.
     *
     * @param g2                    the graphics target.
     * @param frameRect             the frame region (may fall outside visible area).
     * @param frame                 the frame to paint
     * @param paintableIntersection the intersection between the frame rectangle and the visible region
     *                              (used to position the text label).
     * @param bgColor               the background color.
     * @param minimapMode           is the minimap in the process of being rendered?
     */
    private void paintFrame(
            Graphics2D g2,
            Rectangle2D frameRect,
            FrameBox<T> frame,
            Rectangle2D paintableIntersection,
            Font labelFont,
            Color bgColor,
            boolean minimapMode
    ) {
        paintFrameRectangle(g2, frameRect, bgColor, minimapMode);
        if (minimapMode) {
            return;
        }

        var text = calculateFrameText(
                g2,
                labelFont,
                paintableIntersection.getWidth() - frameRenderer.getFrameTextPadding() * 2 - frameRenderer.getFrameGapWidth() * 2,
                frame
        );

        if (text == null) {
            return;
        }

        g2.setFont(labelFont);
        g2.setColor(Colors.foregroundColor(bgColor));
        g2.drawString(
                text,
                (float) (paintableIntersection.getX() + frameRenderer.getFrameTextPadding() + frameBorderWidth),
                (float) (frameRect.getY() + frameRenderer.getFrameBoxTextOffset(g2))
        );
    }

    private void paintFrameRectangle(
            Graphics2D g2,
            Rectangle2D frameRect,
            Color bgColor,
            boolean minimapMode
    ) {
        var gapThickness = minimapMode ?
                           0 :
                           frameGapEnabled ? frameRenderer.getFrameGapWidth() : 0;

        var x = frameRect.getX();
        var y = frameRect.getY();
        var w = frameRect.getWidth() - gapThickness;
        var h = frameRect.getHeight() - gapThickness;
        frameRect.setRect(x, y, w, h);

        g2.setColor(bgColor);
        g2.fill(frameRect);
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
        // TODO delegate to frame renderer ?

        var frameBoxHeight = frameRenderer.getFrameBoxHeight(g2);
        var frameGapWidth = frameRenderer.getFrameGapWidth();

        var rect = new Rectangle();
        rect.x = (int) (bounds.getWidth() * frame.startX) - frameGapWidth; // + internalPadding;
        rect.width = (int) (bounds.getWidth() * frame.endX) - rect.x + 2 * frameGapWidth; // - internalPadding;
        rect.y = frameBoxHeight * frame.stackDepth - frameGapWidth;
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
        int depth = point.y / frameRenderer.getFrameBoxHeight(g2);
        double xLocation = point.x / bounds.getWidth();
        double visibilityThreshold = frameWidthVisibilityThreshold / bounds.getWidth();

        return frames.stream()
                     .filter(node -> node.stackDepth == depth
                                     && node.startX <= xLocation
                                     && xLocation <= node.endX
                                     && visibilityThreshold < node.endX - node.startX)
                     .findFirst();
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
        if (frame == null) {
            stopHover();
            return;
        }
        var oldHoveredFrame = hoveredFrame;
        hoveredFrame = frame;
        if (hoverConsumer != null) {
            hoverConsumer.accept(getFrameRectangle(g2, bounds, frame));
            if (oldHoveredFrame != null) {
                hoverConsumer.accept(getFrameRectangle(g2, bounds, oldHoveredFrame));
            }
        }
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
        return getFrameAt(g2, bounds, point).map(frame -> {
            this.selectedFrame = frame;

            return calculateZoomTargetFrame(g2, bounds, viewRect, frame, 0, 0);
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
     * @param contextBefore    number of contextual parents
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
        var frameWidthX = frame.endX - frame.startX;
        var frameBoxHeight = frameRenderer.getFrameBoxHeight(g2);
        int y = frameBoxHeight * (Math.max(frame.stackDepth - contextBefore, 0));

        /*
         * The new scale factor is
         *
         *                viewRect.width
         * factor = ----------------------------
         *           frameWidthX * bounds.width
         */
        double factor = viewRect.getWidth() / (bounds.getWidth() * frameWidthX);
        // Change offset to center the flame from this frame
        return new ZoomTarget(
                new Dimension(
                        (int) (bounds.getWidth() * factor),
                        (int) (bounds.getHeight() * factor)
                ),
                new Point(
                        (int) (frame.startX * bounds.getWidth() * factor),
                        Math.max(0, y)
                )
        );
    }

    /**
     * Clears the hovered frame (to indicate that no frame is hovered).
     */
    public void stopHover() {
        hoveredFrame = null;
    }

    // layout text
    private String calculateFrameText(
            Graphics2D g2,
            Font font,
            double targetWidth,
            FrameBox<T> frame
    ) {
        var metrics = g2.getFontMetrics(font);

        // don't use stream to avoid allocations during painting
        var textCandidate = "";
        for (Function<FrameBox<T>, String> nodeToTextCandidate : nodeToTextProvider.frameToTextCandidates()) {
            textCandidate = nodeToTextCandidate.apply(frame);
            var textBounds = metrics.getStringBounds(textCandidate, g2);
            if (textBounds.getWidth() <= targetWidth) {
                return textCandidate;
            }
        }
        // only try clip the last candidate
        textCandidate = nodeToTextProvider.clipStrategy().clipString(
                font,
                metrics,
                targetWidth,
                textCandidate,
                StringClipper.LONG_TEXT_PLACEHOLDER
        );
        var textBounds = metrics.getStringBounds(textCandidate, g2);
        if (textBounds.getWidth() > targetWidth || textCandidate.length() <= StringClipper.LONG_TEXT_PLACEHOLDER.length() + 1) {
            // don't draw text, if too long or too short (like "r…")
            return null;
        }
        return textCandidate;

    }

    public void setHighlightFrames(Set<FrameBox<T>> toHighlight, String searchedText) {
        this.toHighlight = toHighlight;
    }
}
