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
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;
import java.util.function.Supplier;

import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isHovered;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isMinimapMode;

/**
 * Default single frame renderer.
 *
 * @param <T> The type of the frame node (depends on the source of profiling data).
 * @see FlamegraphView
 * @see FlamegraphRenderEngine
 * @see FrameRenderer
 * @see FrameTextsProvider
 * @see FrameFontProvider
 * @see FrameColorProvider
 */
public class DefaultFrameRenderer<T> implements FrameRenderer<T> {
    @NotNull
    private FrameTextsProvider<@NotNull T> frameTextsProvider;
    @NotNull
    private FrameFontProvider<@NotNull T> frameFontProvider;
    @NotNull
    private FrameColorProvider<@NotNull T> frameColorProvider;

    /**
     * The space in pixels between the frame label text and the frame's border.
     */
    private static final int frameTextPadding = 2;

    /**
     * The width of the border drawn around the hovered frame.
     */
    private static final int frameBorderWidth = 1;

    /**
     * A flag that controls whether a gap is shown at the right and bottom of each frame.
     */
    private boolean drawingFrameGap = true;

    /**
     * A flag that controls whether the frame is drawn with rounded corners.
     */
    private boolean roundedFrame = false;

    /**
     * A flag that controls whether a frame is drawn around the frame that the mouse pointer
     * hovers over.
     */
    private boolean paintHoveredFrameBorder = false;

    /**
     * The color used to draw a border around the hovered frame.
     */
    // TODO move to FrameColorProvider / ColorModel
    public final Supplier<Color> frameBorderColor = () -> {
        var color = UIManager.getColor("Component.focusColor");
        return color == null ? Colors.panelForeground : color;
    };

    /**
     * @param frameTextsProvider functions that create a label for a node
     * @param frameFontProvider  provides a font given a frame and some flags
     * @param frameColorProvider provides foreground and background color given a frame and some flags
     */
    public DefaultFrameRenderer(
            @NotNull FrameTextsProvider<@NotNull T> frameTextsProvider,
            @NotNull FrameColorProvider<@NotNull T> frameColorProvider,
            @NotNull FrameFontProvider<@NotNull T> frameFontProvider
    ) {
        this.frameTextsProvider = Objects.requireNonNull(frameTextsProvider, "nodeToTextProvider");
        this.frameColorProvider = Objects.requireNonNull(frameColorProvider, "frameColorProvider");
        this.frameFontProvider = Objects.requireNonNull(frameFontProvider, "frameFontProvider");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintFrame(
            @NotNull Graphics2D g2,
            @NotNull FrameModel<@NotNull T> frameModel,
            @NotNull RectangularShape frameRect,
            @NotNull FrameBox<@NotNull T> frame,
            @NotNull Rectangle2D paintableIntersection,
            int renderFlags
    ) {
        boolean minimapMode = isMinimapMode(renderFlags);
        var colorModel = frameColorProvider.getColors(frame, renderFlags);

        paintFrameRectangle(
                g2,
                frameRect,
                Objects.requireNonNull(
                        colorModel.background,
                        "colorModel.background is nullable; however, at when rendering it is not anymore allowed"
                ),
                minimapMode
        );
        if (minimapMode) {
            return;
        }

        var frameFont = frameFontProvider.getFont(frame, renderFlags);

        var text = calculateFrameText(
                g2,
                frameFont,
                paintableIntersection.getWidth() - frameTextPadding * 2 - getFrameGapWidth() * 2,
                frameModel,
                frame
        );

        if (text != null && !text.isBlank()) {
            g2.setFont(frameFont);
            g2.setColor(Objects.requireNonNull(
                    colorModel.foreground,
                    "colorModel.background is nullable; however, at when rendering it is not anymore allowed"
            ));
            var oldRenderingHint = g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.drawString(
                    text,
                    (float) (paintableIntersection.getX() + frameTextPadding + frameBorderWidth),
                    (float) (frameRect.getY() + getFrameBoxTextOffset(g2))
            );

            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldRenderingHint);
        }

        if (isHovered(renderFlags)) {
            paintHoveredFrameBorder(
                    g2,
                    frameRect
            );
        }
    }

    private void paintFrameRectangle(
            @NotNull Graphics2D g2,
            @NotNull RectangularShape frameRect,
            @NotNull Color bgColor,
            boolean minimapMode
    ) {
        var gapThickness = minimapMode ?
                           0 :
                           drawingFrameGap ? getFrameGapWidth() : 0;

        var x = frameRect.getX();
        var y = frameRect.getY();
        var w = frameRect.getWidth() - gapThickness;
        var h = frameRect.getHeight() - gapThickness;
        frameRect.setFrame(x, y, w, h);

        g2.setColor(bgColor);
        g2.fill(frameRect);
    }

    private void paintHoveredFrameBorder(
            @NotNull Graphics2D g2,
            @NotNull RectangularShape frameRect
    ) {
        if (!paintHoveredFrameBorder) {
            return;
        }

        /*
         * DISCLAIMER: it happens that drawing perfectly aligned rect is very challenging with
         * Graphics2D.
         * 1. I t may depend on the current Screen scale (Retina is 2, other monitors like 1x)
         *    g2.getTransform().getScaleX() / getScaleY(), (so in pixels that would 1 / scale)
         * 2. When drawing a rectangle, it seems that the current sun implementation draws
         *    the line on 50% outside and 50% inside. I don't know how to avoid that.
         *
         * In some of my tests, what is ok on a retina is ugly on a 1.x monitor,
         * adjusting the rectangle with the scale wasn't very pretty, as sometimes
         * the border starts inside the frame.
         * Played with Area subtraction, but this wasn't successful.
         */

        var gapThickness = drawingFrameGap ? getFrameGapWidth() : 0;

        var x = frameRect.getX() - (double) gapThickness / 2;
        var y = frameRect.getY() - (double) gapThickness / 2;
        var w = frameRect.getWidth() + (double) gapThickness;
        var h = frameRect.getHeight() + (double) gapThickness;
        frameRect.setFrame(x, y, w, h);

        var prevStrokeControl = g2.getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);

        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setColor(frameBorderColor.get());
        g2.draw(frameRect);

        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, prevStrokeControl);
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
            for (var nodeToTextCandidate : frameTextsProvider.frameToTextCandidates()) {
                // While the function is supposed to return a non-null value, it is not enforced
                // in byte code, so let's default to empty string
                textCandidate = Objects.requireNonNullElse(nodeToTextCandidate.apply(frame), "");
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
            // don't draw the text, if too long or too short (like "r…")
            return null;
        }
        return textCandidate;
    }

    @Override
    public RectangularShape reusableFrameRect() {
        return roundedFrame ? new RoundRectangle2D.Double(
                0, 0, 0, 0, 5, 5
        ) : new Rectangle2D.Double();
    }

    /**
     * Sets whether to draw a gap between each frame.
     *
     * @param drawingFrameGap true to draw a gap between each frame
     */
    public void setDrawingFrameGap(boolean drawingFrameGap) {
        this.drawingFrameGap = drawingFrameGap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDrawingFrameGap() {
        return drawingFrameGap;
    }

    /**
     * Whether the frame is drawn with rounded corners.
     *
     * @return true if the frame is drawn with rounded corners
     */
    public boolean isRoundedFrame() {
        return roundedFrame;
    }

    /**
     * Sets whether the frame is drawn with rounded corners.
     *
     * @param roundedFrame true if the frame is drawn with rounded corners
     */
    public void setRoundedFrame(boolean roundedFrame) {
        this.roundedFrame = roundedFrame;
    }
    
    /**
     * Sets whether to draw a border around the hovered frame.
     *
     * @param paintHoveredFrameBorder true to draw a border around the hovered frame
     */
    public void setPaintHoveredFrameBorder(boolean paintHoveredFrameBorder) {
        this.paintHoveredFrameBorder = paintHoveredFrameBorder;
    }

    /**
     * Whether to draw a border around the hovered frame.
     * @return true if a border is drawn around the hovered frame
     */
    public boolean isPaintHoveredFrameBorder() {
        return paintHoveredFrameBorder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getFrameBoxHeight(@NotNull Graphics2D g2) {
        return g2.getFontMetrics(frameFontProvider.getFont(null, 0)).getAscent() + (frameTextPadding * 2) + getFrameGapWidth() * 2;
    }

    private float getFrameBoxTextOffset(@NotNull Graphics2D g2) {
        return getFrameBoxHeight(g2) - (g2.getFontMetrics(frameFontProvider.getFont(null, 0)).getDescent() / 2f) - frameTextPadding - getFrameGapWidth();
    }

    /**
     * Set the frame text provider.
     *
     * @param frameTextsProvider the frame text provider
     */
    public void setFrameTextsProvider(@NotNull FrameTextsProvider<@NotNull T> frameTextsProvider) {
        this.frameTextsProvider = Objects.requireNonNull(frameTextsProvider, "frameTextsProvider");
    }

    /**
     * Get the frame text provider.
     *
     * @return the frame text provider
     */
    @NotNull
    public FrameTextsProvider<@NotNull T> getFrameTextsProvider() {
        return frameTextsProvider;
    }

    /**
     * Set the frame font provider.
     *
     * @param frameFontProvider the frame font provider
     */
    public void setFrameFontProvider(@NotNull FrameFontProvider<@NotNull T> frameFontProvider) {
        this.frameFontProvider = Objects.requireNonNull(frameFontProvider, "frameFontProvider");
    }

    /**
     * Get the frame font provider.
     *
     * @return the frame font provider
     */
    @NotNull
    public FrameFontProvider<@NotNull T> getFrameFontProvider() {
        return frameFontProvider;
    }

    /**
     * Set the frame color provider.
     *
     * @param frameColorProvider the frame color provider
     */
    public void setFrameColorProvider(@NotNull FrameColorProvider<@NotNull T> frameColorProvider) {
        this.frameColorProvider = Objects.requireNonNull(frameColorProvider, "frameColorProvider");
    }

    /**
     * Get the frame color provider.
     *
     * @return the frame color provider
     */
    @NotNull
    public FrameColorProvider<@NotNull T> getFrameColorProvider() {
        return frameColorProvider;
    }
}