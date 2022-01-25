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
import org.openjdk.jmc.flightrecorder.stacktrace.tree.AggregatableFrame;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.stream.Collectors.joining;

public class FlameGraphPainter {
    public FrameColorMode frameColorMode = FrameColorMode.BY_PACKAGE;
    public Palette packageColorPalette = Palette.DARK_CUSTOM;
    public Color highlightedColor = Color.yellow;
    public Color frameBorderColor = Colors.panelBackGround;
    public boolean paintFrameBorder = true;
    public boolean paintHoveredFrameBorder = true;
    private int frameBorderWidth = 2;
    private final StacktraceTreeModel stacktraceTreeModel;

    private final int depth;
    private int visibleDepth;
    private final int textBorder = 2;
    private final int frameWidthVisibilityThreshold = 4;

    private FrameBox<Node> hoveredFrame;
    private FrameBox<Node> selectedFrame;
    private int visibleWidth;
    private double scaleX;
    private double scaleY;
    private double zoomFactor = 1d;
    private int flameGraphWidth;

    private final java.util.List<FrameBox<Node>> nodes;

    public FlameGraphPainter(Supplier<StacktraceTreeModel> stacktraceTreeModelSupplier) {
        this.stacktraceTreeModel = stacktraceTreeModelSupplier.get();
        this.nodes = FlameNodeBuilder.buildFlameNodes(this.stacktraceTreeModel);
        this.depth = this.nodes.stream().mapToInt(fb -> fb.stackDepth).max().orElse(0);
        visibleDepth = depth;
    }

    private int getFrameBoxHeight(Graphics2D g2) {
        return g2.getFontMetrics().getAscent() + (textBorder * 2) + frameBorderWidth * 2;
    }

    public Dimension getFlameGraphDimension(Graphics2D g2, Dimension dimension, Rectangle visibleRect, Insets insets) {
        // as this method is invoked during layout, the dimension can be 0
        if (visibleRect.width == 0) {
            return new Dimension();
        }


        var visibleWidth = visibleRect.width - insets.left - insets.right;

        // compute the canvas height for the flamegraph width
        if (this.visibleWidth != visibleWidth) {
            var preferredFlameGraphWidth = ((int) (zoomFactor * visibleWidth)) - insets.left - insets.right;

            var visibleDepth = 0;
            for (var node : nodes) {
                if ((int) (preferredFlameGraphWidth * (node.endX - node.startX)) < frameWidthVisibilityThreshold) {
                    continue;
                }

                visibleDepth = Math.max(visibleDepth, node.stackDepth);
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

    public void paint(Graphics2D g2, int canvasWidth, int canvasHeight, Rectangle visibleRect) {
        assert flameGraphWidth > 0 : "canvas sizing not done yet";
//        System.out.println("paint: visibleRect: " + visibleRect);
//        System.out.println("paint: canvasWidth: " + canvasWidth + ", canvasHeight: " + canvasHeight + ", flameGraphWidth: " + flameGraphWidth);

        long start = System.currentTimeMillis();
        var frameBoxHeight = getFrameBoxHeight(g2);
        var rect = new Rectangle(); // reusable rectangle

        identifyDisplayScale(g2);

        {
            // handle root node
            var events = stacktraceTreeModel.getItems()
                                            .stream()
                                            .map(iItems -> iItems.getType().getIdentifier())
                                            .collect(joining(", "));
            var rootNode = nodes.get(0);
            rect.x = (int) (flameGraphWidth * rootNode.startX);
            rect.width = ((int) (flameGraphWidth * rootNode.endX)) - rect.x;

            rect.y = frameBoxHeight * rootNode.stackDepth;
            rect.height = frameBoxHeight;

            if (visibleRect.intersects(rect)) {
                paintRootFrameRectangle((Graphics2D) g2.create(rect.x, rect.y, rect.width, rect.height),
                                        "all (" + events + ")",
                                        handleFocus(FrameColorMode.rootNodeColor,
                                                    hoveredFrame == rootNode,
                                                    false,
                                                    selectedFrame != null && rootNode.stackDepth < selectedFrame.stackDepth));
            }
        }

        // draw real flames
        for (int i = 1; i < nodes.size(); i++) {
            var node = nodes.get(i);
            // TODO Can we do cheaper checks like depth is outside range etc

            rect.x = (int) (flameGraphWidth * node.startX);
            rect.width = ((int) (flameGraphWidth * node.endX)) - rect.x;

            if ((rect.width < frameWidthVisibilityThreshold)) {
                continue;
            }

            rect.y = frameBoxHeight * node.stackDepth;
            rect.height = frameBoxHeight;

            if (visibleRect.intersects(rect)) {
                paintNodeFrameRectangle((Graphics2D) g2.create(rect.x, rect.y, rect.width, rect.height),
                                        node.jfrNode,
                                        handleFocus(frameColorMode.getColor(packageColorPalette, node.jfrNode.getFrame()),
                                                    hoveredFrame == node,
                                                    false,
                                                    selectedFrame != null && (
                                                            node.stackDepth < selectedFrame.stackDepth
                                                            || node.endX < selectedFrame.startX
                                                            || node.startX > selectedFrame.endX)),
                                        frameBorderColor);
            }
        }

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

    private void paintNodeFrameRectangle(Graphics2D g2, Node childFrame, Color bgColor, Color frameBorderColor) {
        var frameRectSurface = paintFrameRectangle(g2, bgColor, frameBorderColor);
        paintFrameText(childFrame.getFrame(),
                       g2,
                       frameRectSurface.width - textBorder * 2 - frameBorderWidth * 2,
                       text -> {
                           g2.setColor(Colors.foregroundColor(bgColor));
                           g2.drawString(text, textBorder + frameBorderWidth, getFrameBoxTextOffset(g2));
                       });
    }

    private void paintRootFrameRectangle(Graphics2D g2, String str, Color bgColor) {
        var frameRectSurface = paintFrameRectangle(g2, bgColor, frameBorderColor);
        paintFrameText(str,
                       g2,
                       frameRectSurface.width - textBorder * 2 - frameBorderWidth * 2,
                       text -> {
                           g2.setColor(Colors.foregroundColor(bgColor));
                           g2.drawString(text, textBorder + frameBorderWidth, getFrameBoxTextOffset(g2));
                       });
    }

    private Rectangle2D.Double paintFrameRectangle(Graphics2D g2, Color bgColor, Color frameBorderColor) {
        var clipBounds = g2.getClipBounds();
        var borderWidth = paintFrameBorder ? frameBorderWidth : 0;

        if (paintFrameBorder) {
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
        return frameRectSurface;
    }

    public Optional<FrameBox<Node>> getFrameAt(Graphics2D g2, Point point) {
        int depth = point.y / getFrameBoxHeight(g2);
        double xLocation = ((double) point.x) / flameGraphWidth;

        return nodes.stream()
                    .filter(node -> node.stackDepth == depth && node.startX <= xLocation && xLocation <= node.endX)
                    .findFirst();
    }

    public void toggleSelectedFrameAt(Graphics2D g2, Point point, Rectangle viewRect) {
        getFrameAt(
                g2,
                point
        ).ifPresent(frame -> selectedFrame = selectedFrame == frame ? null : frame);
    }

    public void hoverFrameAt(Graphics2D g2, Point point) {
        getFrameAt(
                g2,
                point
        ).ifPresentOrElse(frame -> hoveredFrame = frame, this::stopHover);
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


    private static void paintFrameText(AggregatableFrame frame, Graphics2D g2, double targetWidth, Consumer<String> textConsumer) {
        var metrics = g2.getFontMetrics();

        var adaptedText = frame.getHumanReadableShortString();
        var textBounds = metrics.getStringBounds(adaptedText, g2);

        if (!(textBounds.getWidth() > targetWidth)) {
            textConsumer.accept(adaptedText);
            return;
        }
        adaptedText = frame.getMethod().getMethodName();
        textBounds = metrics.getStringBounds(adaptedText, g2);
        if (!(textBounds.getWidth() > targetWidth)) {
            textConsumer.accept(adaptedText);
            return;
        }
        textBounds = metrics.getStringBounds("...", g2);
        if (!(textBounds.getWidth() > targetWidth)) {
            textConsumer.accept("...");
        }
        // don't draw text
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
