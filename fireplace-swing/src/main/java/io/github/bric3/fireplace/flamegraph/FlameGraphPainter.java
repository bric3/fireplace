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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Engine that paint a flamegraph.
 * <p>
 * Note this class a some field that are public and non final; this allows
 * to quickly toy with this tool, use with caution, or not at all.
 * </p>
 *
 * @param <T> The type of the node
 * @see FlameGraph
 */
public class FlameGraphPainter<T> {

    /**
     * A short string to display in place of labels that are too long to fit the
     * available space.
     */
    private static final String LONG_TEXT_PLACEHOLDER = "...";

    /**
     * The font used to display frame labels
     */
    private Font frameLabelFont;

    /**
     * If a frame is clipped, we'll shift the label to make it visible but show it with
     * a modified (italicised by default) font to highlight that the frame is only partially
     * visible.
     */
    private Font frameLabelFontForPartialFrames;

    public Color highlightedColor;
    public Color frameGapColor;
    public boolean frameGapEnabled = true;
    public int frameGapWidth = 1;

    public boolean paintHoveredFrameBorder = true;
    public int frameBorderWidth = 1;
    public Stroke frameBorderStroke = new BasicStroke(frameBorderWidth);
    public Color frameBorderColor = Colors.panelForeground;

    private final int depth;
    private int visibleDepth;
    private final int textBorder = 2;
    protected int frameWidthVisibilityThreshold = 4;
    private final int minimapFrameBoxHeight = 1;

    private FrameBox<T> hoveredFrame;
    private FrameBox<T> selectedFrame;
    private double scaleX;
    private double scaleY;

    private final List<FrameBox<T>> frames;
    private final List<Function<T, String>> nodeToTextCandidates;
    // handle root node
    private final Function<T, String> rootFrameToText;
    Function<T, Color> frameColorFunction;
    private final int internalPadding = 2;
    protected boolean paintDetails = true;

    public FlameGraphPainter(List<FrameBox<T>> frames,
                             List<Function<T, String>> nodeToTextCandidates,
                             Function<T, String> rootFrameToText,
                             Function<T, Color> frameColorFunction) {

        this.frameLabelFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        this.frameLabelFontForPartialFrames = new Font(Font.SANS_SERIF, Font.ITALIC, 12);

        this.frames = frames;
        this.depth = this.frames.stream().mapToInt(fb -> fb.stackDepth).max().orElse(0);
        visibleDepth = depth;
        this.nodeToTextCandidates = nodeToTextCandidates;
        this.rootFrameToText = rootFrameToText;
        this.frameColorFunction = frameColorFunction;
        updateUI();
    }

    public Font getFrameLabelFont() {
        return this.frameLabelFont;
    }

    /**
     * Sets the font used to display frame labels.  Internally an italicised version is also
     * created for use in special cases.
     *
     * @param font the font ({@code null} not permitted).
     */
    public void setFrameLabelFont(Font font) {
        Objects.requireNonNull(font);
        this.frameLabelFont = font;
        this.frameLabelFontForPartialFrames = font.deriveFont(Font.ITALIC);
    }

    /**
     * This method is used to resync colors when the LaF changes
     */
    public void updateUI() {
        frameGapColor = Colors.panelBackground;
        highlightedColor = Color.yellow;
    }

    private int getFrameBoxHeight(Graphics2D g2) {
        return g2.getFontMetrics(this.frameLabelFont).getAscent() + (textBorder * 2) + frameGapWidth * 2;
    }

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
     * @param g2     the graphics target.
     * @param width  the preferred width.
     * @param insets the insets.
     * @return The dimensions required to draw the whole fra
     */
    public Dimension computeFlameGraphDimension(Graphics2D g2, int width, Insets insets) {
        // as this method is invoked during layout, the dimension can be 0
        if (width == 0) {
            return new Dimension();
        }

        return new Dimension(width + insets.left + insets.right, depth * getFrameBoxHeight(g2));
    }

    private float getFrameBoxTextOffset(Graphics2D g2) {
        return getFrameBoxHeight(g2) - (g2.getFontMetrics(frameLabelFont).getDescent() / 2f) - textBorder - frameGapWidth;
    }

    public void paint(Graphics2D g2, Rectangle2D bounds, Rectangle2D viewRect) {
        paint(g2, bounds, viewRect, false);
    }

    /**
     * Paints the minimap (always the entire flame graph).
     *
     * @param g2     the graphics target.
     * @param bounds the bounds.
     */
    public void paintMinimap(Graphics2D g2, Rectangle2D bounds) {
        paint(g2, bounds, bounds, true);
    }

    private void paint(Graphics2D g2, Rectangle2D bounds, Rectangle2D viewRect, boolean minimapMode) {
        long start = System.currentTimeMillis();
        Graphics2D g2d = (Graphics2D) g2.create();
        var frameBoxHeight = minimapMode ? minimapFrameBoxHeight : getFrameBoxHeight(g2);
        var flameGraphWidth = minimapMode ? viewRect.getWidth() : bounds.getWidth();
        var rect = new Rectangle(); // reusable rectangle

        identifyDisplayScale(g2d);

        {
            var rootFrame = frames.get(0);
            rect.x = (int) (flameGraphWidth * rootFrame.startX) + internalPadding;
            rect.width = ((int) (flameGraphWidth * rootFrame.endX)) - rect.x - internalPadding;
            rect.y = frameBoxHeight * rootFrame.stackDepth;
            rect.height = frameBoxHeight;

            Rectangle intersection = viewRect.createIntersection(rect).getBounds();
            if (!intersection.isEmpty()) {
                paintRootFrameRectangle(g2d,
                                        rect,
                                        rootFrameToText.apply(rootFrame.actualNode),
                                        intersection,
                                        handleFocus(frameColorFunction.apply(rootFrame.actualNode),
                                                    hoveredFrame == rootFrame,
                                                    false,
                                                    selectedFrame != null && rootFrame.stackDepth < selectedFrame.stackDepth),
                                        frameGapColor,
                                        minimapMode);
            }
        }

        // draw real flames
        for (int i = 1; i < frames.size(); i++) {
            var frame = frames.get(i);
            // TODO Can we do cheaper checks like depth is outside range etc

            rect.x = (int) (flameGraphWidth * frame.startX) + internalPadding;
            rect.width = ((int) (flameGraphWidth * frame.endX)) - rect.x - internalPadding;

            if ((rect.width < frameWidthVisibilityThreshold) && !minimapMode) {
                continue;
            }

            rect.y = frameBoxHeight * frame.stackDepth;
            rect.height = frameBoxHeight;

            Rectangle intersection = viewRect.createIntersection(rect).getBounds();
            if (!intersection.isEmpty()) {
                paintNodeFrameRectangle(g2d,
                                        rect,
                                        frame.actualNode,
                                        intersection,
                                        handleFocus(frameColorFunction.apply(frame.actualNode),
                                                    hoveredFrame == frame,
                                                    false,
                                                    selectedFrame != null && (
                                                            frame.stackDepth < selectedFrame.stackDepth
                                                            || frame.endX <= selectedFrame.startX
                                                            || frame.startX >= selectedFrame.endX)),
                                        frameGapColor,
                                        minimapMode);
            }
        }

        if (!minimapMode) {
            paintHoveredFrameBorder(g2d, bounds, viewRect, frameBoxHeight, rect);
        }

        if (!minimapMode && paintDetails) {
            // timestamp
            var zoomFactor = bounds.getWidth() / viewRect.getWidth();
            var drawTimeMs = "FrameGraph width " + flameGraphWidth + " Zoom Factor " + zoomFactor + " Coordinate (" + viewRect.getX() + ", " + viewRect.getY() + ") size (" +
                             viewRect.getWidth() + ", " + viewRect.getHeight() +
                             ") , Draw time: " + (System.currentTimeMillis() - start) + " ms";
            var nowWidth = g2d.getFontMetrics(frameLabelFont).stringWidth(drawTimeMs);
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillRect((int) (viewRect.getX() + viewRect.getWidth() - nowWidth - textBorder * 2),
                         (int) (viewRect.getY() + viewRect.getHeight() - frameBoxHeight),
                         nowWidth + textBorder * 2,
                         frameBoxHeight);

            g2d.setColor(Color.YELLOW);
            g2d.drawString(drawTimeMs,
                           (int) (viewRect.getX() + viewRect.getWidth() - nowWidth - textBorder),
                           (int) (viewRect.getY() + viewRect.getHeight() - textBorder));
        }

        g2d.dispose();
    }

    private void paintHoveredFrameBorder(Graphics2D g2, Rectangle2D bounds, Rectangle2D viewRect, int frameBoxHeight, Rectangle rect) {
        if (hoveredFrame == null || !paintHoveredFrameBorder) {
            return;
        }

        rect.x = (int) (bounds.getWidth() * hoveredFrame.startX) + internalPadding;
        rect.width = ((int) (bounds.getWidth() * hoveredFrame.endX)) - rect.x - internalPadding - frameBorderWidth;

        if ((rect.width < frameWidthVisibilityThreshold)) {
            return;
        }

        rect.y = frameBoxHeight * hoveredFrame.stackDepth;
        rect.height = frameBoxHeight - frameGapWidth;

        if (viewRect.intersects(rect)) {
            g2.setColor(frameBorderColor);
            g2.setStroke(frameBorderStroke);
            g2.draw(rect);
        }
    }

    private void identifyDisplayScale(Graphics2D g2) {
        // if true we're on a HiDPI display
        // https://github.com/libgdx/libgdx/commit/2bc16a08961dd303afe2d1c8df96a50d8cd639db
        var transform = g2.getTransform();
        //        System.out.printf("%sscale factor, x=%s y=%s%n",
        //                          (transform.getType() & AffineTransform.TYPE_MASK_SCALE) == AffineTransform.TYPE_UNIFORM_SCALE ? "HiDPI " : "",
        //                          scaleX = transform.getScaleX(),
        //                          scaleY = transform.getScaleY());
    }

    private Color handleFocus(Color bgColor, boolean hovered, boolean highlighted, boolean dimmed) {
        if (dimmed) {
            return Colors.blend(bgColor, Colors.translucent_black_B0);
        }
        if (hovered) {
            return Colors.blend(bgColor, Colors.translucent_black_40);
        }
        if (highlighted) {
            return highlightedColor;
        }
        return bgColor;
    }

    /**
     * Paints a standard frame.
     *
     * @param g2            the graphics target.
     * @param frameRect     the frame rectangle (may fall partly outside the visible region).
     * @param node          the underlying node (used for label generation).
     * @param intersection  the intersection between the frame rectangle and the visible region
     *                      (used to position text labels).
     * @param bgColor       the background color.
     * @param frameGapColor the frame gap color.
     * @param minimapMode   is the minimap in the process of being rendered?
     */
    private void paintNodeFrameRectangle(Graphics2D g2, Rectangle frameRect, T node, Rectangle intersection, Color bgColor, Color frameGapColor, boolean minimapMode) {
        var frameRectSurface = paintFrameRectangle(g2, frameRect, bgColor, frameGapColor, minimapMode);
        if (minimapMode) {
            return;
        }
        // choose font depending on whether the left-side of the frame is clipped
        final Font labelFont = (frameRect.x == intersection.x) ? frameLabelFont : frameLabelFontForPartialFrames;
        paintFrameText(node,
                       g2,
                       labelFont,
                       intersection.width - textBorder * 2 - frameBorderWidth * 2,
                       text -> {
                           g2.setFont(labelFont);
                           g2.setColor(Colors.foregroundColor(bgColor));
                           g2.drawString(text, intersection.x + textBorder, frameRect.y + getFrameBoxTextOffset(g2));
                       });
    }

    /**
     * Paints the root frame.
     *
     * @param g2           the graphics target.
     * @param rect         the frame region (may fall outside visible area).
     * @param str          the text to display.
     * @param intersection the intersection between the frame rectangle and the visible region
     *                     (used to position the text label).
     * @param bgColor      the background color.
     * @param gapColor     the gap color.
     * @param minimapMode  is the minimap in the process of being rendered?
     */
    private void paintRootFrameRectangle(Graphics2D g2, Rectangle rect, String str, Rectangle intersection, Color bgColor, Color gapColor, boolean minimapMode) {
        var frameRectSurface = paintFrameRectangle(g2, rect, bgColor, gapColor, minimapMode);
        if (minimapMode) {
            return;
        }
        // choose a font depending on whether the left-side of the frame is clipped
        final Font labelFont = (rect.x == intersection.x) ? frameLabelFont : frameLabelFontForPartialFrames;
        paintRootFrameText(str,
                           g2,
                           labelFont,
                           intersection.width - textBorder * 2 - frameGapWidth * 2,
                           text -> {
                               g2.setFont(labelFont);
                               g2.setColor(Colors.foregroundColor(bgColor));
                               g2.drawString(text, intersection.x + textBorder + frameBorderWidth, getFrameBoxTextOffset(g2));
                           });
    }

    private Rectangle paintFrameRectangle(Graphics2D g2, Rectangle frameRect, Color bgColor, Color frameGapColor, boolean minimapMode) {
        var borderWidth = minimapMode ?
                          0 :
                          frameGapEnabled ? frameGapWidth : 0;

        if (frameGapEnabled && !minimapMode) {
            g2.setColor(frameGapColor);
            g2.fill(frameRect);
        }

        frameRect.width = frameRect.width - borderWidth;
        frameRect.height = frameRect.height - borderWidth;
        g2.setColor(bgColor);
        g2.fill(frameRect);
        return frameRect;
    }

    public Optional<FrameBox<T>> getFrameAt(Graphics2D g2, Rectangle2D bounds, Point point) {
        int depth = point.y / getFrameBoxHeight(g2);
        double xLocation = point.x / bounds.getWidth();

        return frames.stream()
                     .filter(node -> node.stackDepth == depth && node.startX <= xLocation && xLocation <= node.endX)
                     .findFirst();
    }

    public void toggleSelectedFrameAt(Graphics2D g2, Rectangle2D bounds, Point point) {
        getFrameAt(g2, bounds, point)
                .ifPresent(frame -> selectedFrame = selectedFrame == frame ? null : frame);
    }

    public void hoverFrameAt(Graphics2D g2, Rectangle2D bounds, Point point, Consumer<FrameBox<T>> hoverConsumer) {
        getFrameAt(g2, bounds, point)
                .ifPresentOrElse(frame -> {
                                     hoveredFrame = frame;
                                     if (hoverConsumer != null) {
                                         hoverConsumer.accept(frame);
                                     }
                                 },
                                 this::stopHover);
    }

    public Optional<ZoomTarget> calculateZoomTargetForFrameAt(Graphics2D g2, Rectangle2D bounds, Rectangle2D viewRect, Point point) {
        return getFrameAt(g2, bounds, point).map(frame -> {
            this.selectedFrame = frame;

            var frameWidthX = frame.endX - frame.startX;
            var frameBoxHeight = getFrameBoxHeight(g2);
            int y = frameBoxHeight * frame.stackDepth;

            // Change offset to center the flame from this frame
            double factor = (1.0 / frameWidthX) * (viewRect.getWidth() / bounds.getWidth());
            return new ZoomTarget(new Dimension((int) (bounds.getWidth() * factor), (int) (bounds.getHeight() * factor)),
                                  new Point((int) (frame.startX * bounds.getWidth() * factor), Math.max(0, y)));
        });
    }

    public void stopHover() {
        hoveredFrame = null;
    }

    private void paintFrameText(T node, Graphics2D g2, Font font, double targetWidth, Consumer<String> textConsumer) {
        var metrics = g2.getFontMetrics(font);

        nodeToTextCandidates.stream()
                            .map(f -> f.apply(node))
                            .filter(text -> {
                                var textBounds = metrics.getStringBounds(text, g2);
                                return !(textBounds.getWidth() > targetWidth);
                            })
                            .findFirst()
                            .ifPresentOrElse(
                                    textConsumer,
                                    () -> {
                                        var textBounds = metrics.getStringBounds(LONG_TEXT_PLACEHOLDER, g2);
                                        if (!(textBounds.getWidth() > targetWidth)) {
                                            textConsumer.accept(LONG_TEXT_PLACEHOLDER);
                                        }
                                    }
                            );
    }

    private static void paintRootFrameText(String text, Graphics2D g2, Font font, double targetWidth, Consumer<String> textConsumer) {
        var metrics = g2.getFontMetrics(font);

        var textBounds = metrics.getStringBounds(text, g2);
        if (!(textBounds.getWidth() > targetWidth)) {
            textConsumer.accept(text);
        }
        textBounds = metrics.getStringBounds(LONG_TEXT_PLACEHOLDER, g2);
        if (!(textBounds.getWidth() > targetWidth)) {
            textConsumer.accept(LONG_TEXT_PLACEHOLDER);
        }
        // don't draw text
    }

}
