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

import io.github.bric3.fireplace.core.ui.StringClipper;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Objects;
import java.util.function.Function;

import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isMinimapMode;

/**
 * Single frame renderer.
 *
 * @param <T> The type of the frame node (depends on the source of profiling data).
 * @see FlamegraphView
 * @see FlamegraphRenderEngine
 */
class FrameRenderer<T> {
    @NotNull
    private FrameTextsProvider<@NotNull T> frameTextsProvider;
    @NotNull
    private FrameFontProvider<@NotNull T> frameFontProvider;
    @NotNull
    private FrameColorProvider<@NotNull T> frameColorProvider;

    /**
     * The space in pixels between the frame label text and the frame's border.
     */
    private final int frameTextPadding = 2;

    /**
     * A flag that controls whether a gap is shown at the right and bottom of each frame.
     */
    private boolean drawingFrameGap = true;

    /**
     * The size of the gap at the <strong>right</strong> and <strong>bottom</strong> of each frame.
     */
    private final int frameGapWidth = 1;

    /**
     * @param frameTextsProvider functions that create a label for a node
     * @param frameFontProvider  provides a font given a frame and some flags
     * @param frameColorProvider provides foreground and background color given a frame and some flags
     */
    public FrameRenderer(
            @NotNull FrameTextsProvider<@NotNull T> frameTextsProvider,
            @NotNull FrameColorProvider<@NotNull T> frameColorProvider,
            @NotNull FrameFontProvider<@NotNull T> frameFontProvider
    ) {
        this.frameTextsProvider = Objects.requireNonNull(frameTextsProvider, "nodeToTextProvider");
        this.frameColorProvider = Objects.requireNonNull(frameColorProvider, "frameColorProvider");
        this.frameFontProvider = Objects.requireNonNull(frameFontProvider, "frameFontProvider");
    }

    public int getFrameTextPadding() {
        return frameTextPadding;
    }

    public int getFrameGapWidth() {
        return frameGapWidth;
    }

    public void setDrawingFrameGap(boolean drawingFrameGap) {
        this.drawingFrameGap = drawingFrameGap;
    }

    public boolean isDrawingFrameGap() {
        return drawingFrameGap;
    }

    /**
     * The width of the border drawn around the hovered frame.
     */
    public final int frameBorderWidth = 1;

    /**
     * The stroke used to draw a border around the hovered frame.
     */
    @NotNull
    public Stroke frameBorderStroke = new BasicStroke(frameBorderWidth);

    public int getFrameBoxHeight(@NotNull Graphics2D g2) {
        return g2.getFontMetrics(frameFontProvider.getFont(null, 0)).getAscent() + (frameTextPadding * 2) + frameGapWidth * 2;
    }

    public float getFrameBoxTextOffset(@NotNull Graphics2D g2) {
        return getFrameBoxHeight(g2) - (g2.getFontMetrics(frameFontProvider.getFont(null, 0)).getDescent() / 2f) - frameTextPadding - frameGapWidth;
    }

    /**
     * Paints the frame.
     *
     * @param g2                    the graphics target.
     * @param frameRect             the frame region (may fall outside visible area).
     * @param frame                 the frame to paint
     * @param paintableIntersection the intersection between the frame rectangle and the visible region
     *                              (used to position the text label).
     * @param flags                 The rendering flags (minimap, selection, hovered, highlight).
     */
    void paintFrame(
            @NotNull Graphics2D g2,
            @NotNull FrameModel<@NotNull T> frameModel,
            @NotNull Rectangle2D frameRect,
            @NotNull FrameBox<@NotNull T> frame,
            @NotNull Rectangle2D paintableIntersection,
            int flags
    ) {
        boolean minimapMode = isMinimapMode(flags);
        // var bgColor = tweakBgColor(frameColorFunction.apply(frame), flags);
        var colorModel = frameColorProvider.getColors(frame, flags);

        paintFrameRectangle(g2, frameRect, colorModel.background, minimapMode);
        if (minimapMode) {
            return;
        }

        var frameFont = frameFontProvider.getFont(frame, flags);

        var text = calculateFrameText(
                g2,
                frameFont,
                paintableIntersection.getWidth() - frameTextPadding * 2 - frameGapWidth * 2,
                frameModel,
                frame
        );

        if (text == null || text.isEmpty()) {
            return;
        }

        g2.setFont(frameFont);
        g2.setColor(colorModel.foreground);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.drawString(
                text,
                (float) (paintableIntersection.getX() + frameTextPadding + frameBorderWidth),
                (float) (frameRect.getY() + getFrameBoxTextOffset(g2))
        );
    }

    private void paintFrameRectangle(
            @NotNull Graphics2D g2,
            @NotNull Rectangle2D frameRect,
            @NotNull Color bgColor,
            boolean minimapMode
    ) {
        var gapThickness = minimapMode ?
                           0 :
                           drawingFrameGap ? frameGapWidth : 0;

        var x = frameRect.getX();
        var y = frameRect.getY();
        var w = frameRect.getWidth() - gapThickness;
        var h = frameRect.getHeight() - gapThickness;
        frameRect.setRect(x, y, w, h);

        g2.setColor(bgColor);
        g2.fill(frameRect);
    }

    // layout text
    private String calculateFrameText(
            @NotNull Graphics2D g2,
            @NotNull Font font,
            double targetWidth,
            @NotNull FrameModel<@NotNull T> frameModel,
            @NotNull FrameBox<@NotNull T> frame
    ) {
        var metrics = g2.getFontMetrics(font);

        // don't use stream to avoid allocations during painting
        var textCandidate = frame.isRoot() ? frameModel.title : "";
        if (frame.isRoot() && !textCandidate.isBlank()) {
            var textBounds = metrics.getStringBounds(textCandidate, g2);
            if (textBounds.getWidth() <= targetWidth) {
                return textCandidate;
            }
        } else {
            for (Function<FrameBox<T>, String> nodeToTextCandidate : frameTextsProvider.frameToTextCandidates()) {
                textCandidate = nodeToTextCandidate.apply(frame);
                var textBounds = metrics.getStringBounds(textCandidate, g2);
                if (textBounds.getWidth() <= targetWidth) {
                    return textCandidate;
                }
            }
        }
        // only try clip the last candidate
        textCandidate = frameTextsProvider.clipStrategy().clipString(
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

    public void setFrameTextsProvider(@NotNull FrameTextsProvider<@NotNull T> frameTextsProvider) {
        this.frameTextsProvider = Objects.requireNonNull(frameTextsProvider, "frameTextsProvider");
    }

    @NotNull
    public FrameTextsProvider<@NotNull T> getFrameTextsProvider() {
        return frameTextsProvider;
    }

    public void setFrameFontProvider(@NotNull FrameFontProvider<@NotNull T> frameFontProvider) {
        this.frameFontProvider = Objects.requireNonNull(frameFontProvider, "frameFontProvider");
    }

    @NotNull
    public FrameFontProvider<@NotNull T> getFrameFontProvider() {
        return frameFontProvider;
    }

    public void setFrameColorProvider(@NotNull FrameColorProvider<@NotNull T> frameColorProvider) {
        this.frameColorProvider = Objects.requireNonNull(frameColorProvider, "frameColorProvider");
    }

    @NotNull
    public FrameColorProvider<@NotNull T> getFrameColorProvider() {
        return frameColorProvider;
    }
}