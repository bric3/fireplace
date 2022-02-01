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

import com.github.bric3.fireplace.ui.Colors;
import com.github.bric3.fireplace.ui.Colors.Palette;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class FlameGraphPainter<T> {
    public FrameColorMode<T> frameColorMode;
    public Palette packageColorPalette = Palette.DARK_CUSTOM;
    public Color highlightedColor = Color.yellow;
    public Color frameBorderColor = Colors.panelBackGround;
    public boolean paintFrameBorder = true;
    public boolean paintHoveredFrameBorder = true;
    public int frameBorderWidth = 2;

    private final int depth;
    private int visibleDepth;
    private final int textBorder = 2;
    private final int frameWidthVisibilityThreshold = 4;
    private int minimapFrameBoxHeight = 1;

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
    private final Function<T, Color> frameColorFunction;


    public FlameGraphPainter(List<FrameBox<T>> frames,
                             List<Function<T, String>> nodeToTextCandidates,
                             Function<T, String> rootFrameToText,
                             Function<T, Color> frameColorFunction,
                             FrameColorMode<T> frameColorMode) {
        this.frames = frames;
        this.depth = this.frames.stream().mapToInt(fb -> fb.stackDepth).max().orElse(0);
        visibleDepth = depth;
        this.nodeToTextCandidates = nodeToTextCandidates;
        this.rootFrameToText = rootFrameToText;
        this.frameColorFunction = frameColorFunction;
        this.frameColorMode = frameColorMode;
    }

    private int getFrameBoxHeight(Graphics2D g2) {
        return g2.getFontMetrics().getAscent() + (textBorder * 2) + frameBorderWidth * 2;
    }

    public int computeFlameGraphThumbnailHeight(int thumbnailWidth) {
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
        var rect = new Rectangle(); // reusable rectangle

        identifyDisplayScale(g2);

        {
            var rootFrame = frames.get(0);
            rect.x = (int) (flameGraphWidth * rootFrame.startX);
            rect.width = ((int) (flameGraphWidth * rootFrame.endX)) - rect.x;

            rect.y = frameBoxHeight * rootFrame.stackDepth;
            rect.height = frameBoxHeight;

            if (visibleRect.intersects(rect)) {
                paintRootFrameRectangle((Graphics2D) g2.create(rect.x, rect.y, rect.width, rect.height),
                                        rootFrameToText.apply(rootFrame.jfrNode),
                                        handleFocus(frameColorFunction.apply(rootFrame.jfrNode),
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

            rect.x = (int) (flameGraphWidth * frame.startX);
            rect.width = ((int) (flameGraphWidth * frame.endX)) - rect.x;

            if ((rect.width < frameWidthVisibilityThreshold) && !minimapMode) {
                continue;
            }

            rect.y = frameBoxHeight * frame.stackDepth;
            rect.height = frameBoxHeight;

            if (visibleRect.intersects(rect)) {
                paintNodeFrameRectangle((Graphics2D) g2.create(rect.x, rect.y, rect.width, rect.height),
                                        frame.jfrNode,
                                        handleFocus(frameColorFunction.apply(frame.jfrNode),
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

        if (minimapMode) {
            // System.out.println("paint, minimapMode, draw time: " + (System.currentTimeMillis() - start) + "ms");
            return;
        }

        paintHoveredFrameBorder(g2, visibleRect, frameBoxHeight, rect);

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

        //        g2.clip(visibleRect);
    }

    private void paintHoveredFrameBorder(Graphics2D g2, Rectangle visibleRect, int frameBoxHeight, Rectangle rect) {
        if (hoveredFrame == null || !paintHoveredFrameBorder) {
            return;
        }

        rect.x = (int) (flameGraphWidth * hoveredFrame.startX);
        rect.width = ((int) (flameGraphWidth * hoveredFrame.endX)) - rect.x;

        if ((rect.width < frameWidthVisibilityThreshold)) {
            return;
        }

        rect.y = frameBoxHeight * hoveredFrame.stackDepth;
        rect.height = frameBoxHeight;

        if (visibleRect.intersects(rect)) {
            g2.setColor(Color.white);
            g2.setStroke(new BasicStroke(frameBorderWidth));
            g2.drawRect(rect.x + 1, rect.y + 1, rect.width, rect.height);
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

    private void paintNodeFrameRectangle(Graphics2D g2, T node, Color bgColor, Color frameBorderColor, boolean minimapMode) {
        var frameRectSurface = paintFrameRectangle(g2, bgColor, frameBorderColor, minimapMode);
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
    }

    private void paintRootFrameRectangle(Graphics2D g2, String str, Color bgColor, boolean minimapMode) {
        var frameRectSurface = paintFrameRectangle(g2, bgColor, frameBorderColor, minimapMode);
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
    }

    private Rectangle2D.Double paintFrameRectangle(Graphics2D g2, Color bgColor, Color frameBorderColor, boolean minimapMode) {
        var clipBounds = g2.getClipBounds();
        var borderWidth = minimapMode ?
                          0 :
                          paintFrameBorder ? frameBorderWidth : 0;

        if (paintFrameBorder && !minimapMode) {
            g2.setColor(frameBorderColor);
            var growBoxSurface = new Double(clipBounds.x, clipBounds.y, clipBounds.width + borderWidth, clipBounds.height + borderWidth);
            g2.setClip(growBoxSurface);
            g2.fill(growBoxSurface);
        }

        var frameRectSurface = new Rectangle2D.Double(clipBounds.x + borderWidth,
                                                      clipBounds.y + borderWidth,
                                                      clipBounds.width - borderWidth,
                                                      clipBounds.height - borderWidth);
        g2.setColor(bgColor);
        g2.fill(frameRectSurface);
        g2.clip(clipBounds);
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
