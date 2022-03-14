/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.github.bric3.fireplace.flamegraph;

import com.github.bric3.fireplace.core.ui.Colors;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.List;
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
    public Color highlightedColor;
    public Color frameBorderColor;
    public boolean paintFrameBorder = true;
    public boolean paintHoveredFrameBorder = true;
    public int frameBorderWidth = 1;

    private final int depth;
    private int visibleDepth;
    private final int textBorder = 2;
    protected int frameWidthVisibilityThreshold = 4;
    private final int minimapFrameBoxHeight = 1;

    private FrameBox<T> hoveredFrame;
    private FrameBox<T> selectedFrame;
    private int visibleWidth;
    private double scaleX;
    private double scaleY;
    private double zoomFactor = 1d;
    private int flameGraphWidth;

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
        this.frames = frames;
        this.depth = this.frames.stream().mapToInt(fb -> fb.stackDepth).max().orElse(0);
        visibleDepth = depth;
        this.nodeToTextCandidates = nodeToTextCandidates;
        this.rootFrameToText = rootFrameToText;
        this.frameColorFunction = frameColorFunction;
        updateUI();
    }

    /**
     * This method is used to resync colors when the LaF changes
     */
    public void updateUI() {
        frameBorderColor = Colors.panelBackground;
        highlightedColor = Color.yellow;
    }

    private int getFrameBoxHeight(Graphics2D g2) {
        return g2.getFontMetrics().getAscent() + (textBorder * 2) + frameBorderWidth * 2;
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

    public Dimension computeFlameGraphDimension(Graphics2D g2, Rectangle visibleRect, Insets insets) {
        // as this method is invoked during layout, the dimension can be 0
        if (visibleRect.width == 0) {
            return new Dimension();
        }


        var visibleWidth = visibleRect.width - insets.left - insets.right;

        // compute the canvas height for the flamegraph width
        if (this.visibleWidth != visibleWidth) {
            var preferredFlameGraphWidth = ((int) (zoomFactor * visibleWidth)) - insets.left - insets.right;

            var visibleDepth = 0;
            for (var frame : frames) {
                if ((int) (preferredFlameGraphWidth * (frame.endX - frame.startX)) < frameWidthVisibilityThreshold) {
                    continue;
                }

                visibleDepth = Math.max(visibleDepth, frame.stackDepth);
            }

            this.flameGraphWidth = preferredFlameGraphWidth;
            this.visibleWidth = visibleWidth;
            this.visibleDepth = Math.min(visibleDepth, depth);
//            System.out.println("getFlameGraphDimension, fgWidth=" + flameGraphWidth +
//                               " new fgWidth=" + preferredFlameGraphWidth + " visibleWidth=" + visibleWidth + " visibleDepth=" + visibleDepth + " zoom=" + zoomFactor);
        }

//        System.out.println("getFlameGraphDimension, fgWidth=" + flameGraphWidth + " visibleWidth=" + visibleWidth + " visibleDepth=" + visibleDepth + " zoom=" + zoomFactor);

        return new Dimension(flameGraphWidth, this.visibleDepth * getFrameBoxHeight(g2));
    }

    private float getFrameBoxTextOffset(Graphics2D g2) {
        return getFrameBoxHeight(g2) - (g2.getFontMetrics().getDescent() / 2f) - textBorder - frameBorderWidth;
    }

    public void paint(Graphics2D g2, Rectangle visibleRect) {
        assert flameGraphWidth > 0 : "canvas sizing not done yet";
        paint(g2, visibleRect, false);
    }

    public void paintMinimap(Graphics2D g2, Rectangle visibleRect) {
        assert flameGraphWidth > 0 : "canvas sizing not done yet";
        paint(g2, visibleRect, true);
    }

    private void paint(Graphics2D g2, Rectangle visibleRect, boolean minimapMode) {
        long start = System.currentTimeMillis();
        var frameBoxHeight = minimapMode ? minimapFrameBoxHeight : getFrameBoxHeight(g2);
        var flameGraphWidth = minimapMode ? visibleRect.width : this.flameGraphWidth;
        var rectOnCanvas = new Rectangle(); // reusable rectangle
        var frameRect = new Rectangle(); // reusable rectangle

        identifyDisplayScale(g2);

        {
            var rootFrame = frames.get(0);
            rectOnCanvas.x = (int) (flameGraphWidth * rootFrame.startX) + internalPadding;
            rectOnCanvas.width = ((int) (flameGraphWidth * rootFrame.endX)) - rectOnCanvas.x - internalPadding;

            rectOnCanvas.y = frameBoxHeight * rootFrame.stackDepth;
            rectOnCanvas.height = frameBoxHeight;

            frameRect.width = rectOnCanvas.width;
            frameRect.height = rectOnCanvas.height;

            if (visibleRect.intersects(rectOnCanvas)) {
                paintRootFrameRectangle((Graphics2D) g2.create(rectOnCanvas.x, rectOnCanvas.y, rectOnCanvas.width, rectOnCanvas.height),
                                        frameRect,
                                        rootFrameToText.apply(rootFrame.actualNode),
                                        handleFocus(frameColorFunction.apply(rootFrame.actualNode),
                                                    hoveredFrame == rootFrame,
                                                    false,
                                                    selectedFrame != null && rootFrame.stackDepth < selectedFrame.stackDepth, minimapMode),
                                        minimapMode);
            }
        }

        // draw real flames
        for (int i = 1; i < frames.size(); i++) {
            var frame = frames.get(i);
            // TODO Can we do cheaper checks like depth is outside range etc

            rectOnCanvas.x = (int) (flameGraphWidth * frame.startX) + internalPadding;
            rectOnCanvas.width = ((int) (flameGraphWidth * frame.endX)) - rectOnCanvas.x - internalPadding;

            if ((rectOnCanvas.width < frameWidthVisibilityThreshold) && !minimapMode) {
                continue;
            }

            rectOnCanvas.y = frameBoxHeight * frame.stackDepth;
            rectOnCanvas.height = frameBoxHeight;

            frameRect.width = rectOnCanvas.width;
            frameRect.height = rectOnCanvas.height;

            if (visibleRect.intersects(rectOnCanvas)) {
                paintNodeFrameRectangle((Graphics2D) g2.create(rectOnCanvas.x, rectOnCanvas.y, rectOnCanvas.width, rectOnCanvas.height),
                                        frameRect,
                                        frame.actualNode,
                                        handleFocus(frameColorFunction.apply(frame.actualNode),
                                                    hoveredFrame == frame,
                                                    false,
                                                    selectedFrame != null && (
                                                            frame.stackDepth < selectedFrame.stackDepth
                                                            || frame.endX < selectedFrame.startX
                                                            || frame.startX > selectedFrame.endX),
                                                    minimapMode),
                                        frameBorderColor,
                                        minimapMode);
            }
        }

        if (!minimapMode) {
            paintHoveredFrameBorder(g2, visibleRect, frameBoxHeight, rectOnCanvas);
        }

        if (!minimapMode && paintDetails) {
            // timestamp
            var drawTimeMs = "FrameGraph width " + flameGraphWidth + " Zoom Factor " + zoomFactor + " Coordinate (" + visibleRect.x + ", " + visibleRect.y + ") size (" +
                             visibleRect.width + ", " + visibleRect.height +
                             ") , Draw time: " + (System.currentTimeMillis() - start) + " ms";
            var nowWidth = g2.getFontMetrics().stringWidth(drawTimeMs);
            g2.setColor(Color.darkGray);
            g2.fillRect(visibleRect.x + visibleRect.width - nowWidth - textBorder * 2,
                        visibleRect.y + visibleRect.height - frameBoxHeight,
                        nowWidth + textBorder * 2,
                        frameBoxHeight);

            g2.setColor(Color.yellow);
            g2.drawString(drawTimeMs,
                          visibleRect.x + visibleRect.width - nowWidth - textBorder,
                          visibleRect.y + visibleRect.height - textBorder);
        }

        g2.clip(visibleRect);
    }

    private void paintHoveredFrameBorder(Graphics2D g2, Rectangle visibleRect, int frameBoxHeight, Rectangle rect) {
        if (hoveredFrame == null || !paintHoveredFrameBorder) {
            return;
        }

        rect.x = (int) (flameGraphWidth * hoveredFrame.startX) + internalPadding;
        rect.width = ((int) (flameGraphWidth * hoveredFrame.endX)) - rect.x - internalPadding;

        if ((rect.width < frameWidthVisibilityThreshold)) {
            return;
        }

        rect.y = frameBoxHeight * hoveredFrame.stackDepth;
        rect.height = frameBoxHeight;

        if (visibleRect.intersects(rect)) {
            g2.setColor(Color.white);
            g2.setStroke(new BasicStroke(frameBorderWidth));
            g2.drawRect(rect.x + 1, rect.y + 1, rect.width - 1, rect.height - 1);
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

    private Color handleFocus(Color bgColor, boolean hovered, boolean highlighted, boolean dimmed, boolean minimapMode) {
        if (minimapMode) {
            return bgColor;
        }
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

    private void paintNodeFrameRectangle(Graphics2D g2, Rectangle frameRect, T node, Color bgColor, Color frameBorderColor, boolean minimapMode) {
        var frameRectSurface = paintFrameRectangle(g2, frameRect, bgColor, frameBorderColor, minimapMode);
        if (minimapMode) {
            return;
        }
        paintFrameText(node,
                       g2,
                       frameRectSurface.width - textBorder * 2 - frameBorderWidth * 2,
                       text -> {
                           g2.setColor(Colors.foregroundColor(bgColor));
                           g2.drawString(text, textBorder + frameBorderWidth, getFrameBoxTextOffset(g2));
                       });
        g2.dispose();
    }

    private void paintRootFrameRectangle(Graphics2D g2, Rectangle rect, String str, Color bgColor, boolean minimapMode) {
        var frameRectSurface = paintFrameRectangle(g2, rect, bgColor, frameBorderColor, minimapMode);
        if (minimapMode) {
            return;
        }
        paintFrameText(str,
                       g2,
                       frameRectSurface.width - textBorder * 2 - frameBorderWidth * 2,
                       text -> {
                           g2.setColor(Colors.foregroundColor(bgColor));
                           g2.drawString(text, textBorder + frameBorderWidth, getFrameBoxTextOffset(g2));
                       });
        g2.dispose();
    }

    private Rectangle2D.Double paintFrameRectangle(Graphics2D g2, Rectangle frameRect, Color bgColor, Color frameBorderColor, boolean minimapMode) {
        var borderWidth = minimapMode ?
                          0 :
                          paintFrameBorder ? frameBorderWidth : 0;

        if (paintFrameBorder && !minimapMode) {
            var growBoxSurface = new Rectangle.Double(frameRect.x,
                                                      frameRect.y,
                                                      frameRect.width + borderWidth,
                                                      frameRect.height + borderWidth);
            g2.setColor(frameBorderColor);
            g2.fill(growBoxSurface);
        }

        var frameRectSurface = new Rectangle2D.Double(frameRect.x + borderWidth,
                                                      frameRect.y + borderWidth,
                                                      frameRect.width - borderWidth,
                                                      frameRect.height - borderWidth);
        g2.setColor(bgColor);
        g2.fill(frameRectSurface);
        return frameRectSurface;
    }

    public Optional<FrameBox<T>> getFrameAt(Graphics2D g2, Point point) {
        int depth = point.y / getFrameBoxHeight(g2);
        double xLocation = ((double) point.x) / flameGraphWidth;

        return frames.stream()
                     .filter(node -> node.stackDepth == depth && node.startX <= xLocation && xLocation <= node.endX)
                     .findFirst();
    }

    public void toggleSelectedFrameAt(Graphics2D g2, Point point) {
        getFrameAt(
                g2,
                point
        ).ifPresent(frame -> selectedFrame = selectedFrame == frame ? null : frame);
    }

    public void hoverFrameAt(Graphics2D g2, Point point, Consumer<FrameBox<T>> hoverConsumer) {
        getFrameAt(
                g2,
                point
        ).ifPresentOrElse(frame -> {
                              hoveredFrame = frame;
                              if (hoverConsumer != null) {
                                  hoverConsumer.accept(frame);
                              }
                          },
                          this::stopHover);
    }

    public Optional<Point> zoomToFrameAt(Graphics2D g2, Point point) {
        return getFrameAt(
                g2,
                point
        ).map(frame -> {
            this.selectedFrame = frame;

            var frameWidthX = frame.endX - frame.startX;
            flameGraphWidth = (int) Math.max(visibleWidth / Math.min(1, frameWidthX), visibleWidth);
            zoomFactor = (double) flameGraphWidth / visibleWidth;

            var frameBoxHeight = getFrameBoxHeight(g2);
            int y = frameBoxHeight * frame.stackDepth;

            // Change offset to center the flame from this frame
            return new Point(
                    (int) (frame.startX * flameGraphWidth),
                    Math.max(0, y)
            );
        });
    }

    public void stopHover() {
        hoveredFrame = null;
    }

    private void paintFrameText(T node, Graphics2D g2, double targetWidth, Consumer<String> textConsumer) {
        var metrics = g2.getFontMetrics();

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
                                        var textBounds = metrics.getStringBounds("...", g2);
                                        if (!(textBounds.getWidth() > targetWidth)) {
                                            textConsumer.accept("...");
                                        }
                                    }
                            );
    }

    private static void paintFrameText(String text, Graphics2D g2, double targetWidth, Consumer<String> textConsumer) {
        var metrics = g2.getFontMetrics();

        var textBounds = metrics.getStringBounds(text, g2);
        if (!(textBounds.getWidth() > targetWidth)) {
            textConsumer.accept(text);
        }
        textBounds = metrics.getStringBounds("...", g2);
        if (!(textBounds.getWidth() > targetWidth)) {
            textConsumer.accept("...");
        }
        // don't draw text
    }

}
