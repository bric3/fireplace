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
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.stream.Collectors.joining;

public class FlameGraphPainter {
    public ColorMode colorMode = ColorMode.BY_PACKAGE;
    public Palette colorPalette = Palette.DARK_CUSTOM;
    public Color selectionColor = Color.yellow;
    public Color frameBorderColor = Color.gray;
    public boolean paintFrameBorder = true;
    private final StacktraceTreeModel stacktraceTreeModel;
    private final java.util.List<FrameBox<Node>> nodes;
    private final int depth;
    private int visibleDepth;
    private final int textBorder = 2;
    private final int frameWidthVisibilityThreshold = 4;

    private FrameBox<Node> highlightedFrame;
    private FrameBox<Node> selectedFrame;
    private int visibleWidth;
    private double scaleX;
    private double scaleY;

    public FlameGraphPainter(Supplier<StacktraceTreeModel> stacktraceTreeModelSupplier) {
        this.stacktraceTreeModel = stacktraceTreeModelSupplier.get();
        this.nodes = FlameNodeBuilder.buildFlameNodes(this.stacktraceTreeModel);
        this.depth = this.nodes.stream().mapToInt(fb -> fb.stackDepth).max().orElse(0);
        visibleDepth = depth;
    }

    private int getFrameBoxHeight(Graphics2D g2) {
        return g2.getFontMetrics().getAscent() + (textBorder * 2);
    }

    public int getFlameGraphHeight(Graphics2D g2, Dimension dimension, Insets insets) {
        // as this method is invoked during layout, the dimension can be 0
        if (dimension.width == 0) {
            return 0;
        }

        var preferredWidth = dimension.width - insets.left - insets.right;

        if (this.visibleWidth != preferredWidth) {
            var visibleDepth = 0;
            for (var node : nodes) {
                if ((int) (preferredWidth * (node.endX - node.startX)) < frameWidthVisibilityThreshold) {
                    continue;
                }

                visibleDepth = Math.max(visibleDepth, node.stackDepth);
            }

            this.visibleWidth = preferredWidth;
            this.visibleDepth = Math.min(visibleDepth, depth);
        }

        return this.visibleDepth * getFrameBoxHeight(g2);
    }

    private float getFrameBoxTextOffset(Graphics2D g2) {
        return getFrameBoxHeight(g2) - (g2.getFontMetrics().getDescent() / 2f) - textBorder;
    }

    public void paint(Graphics2D g2, int canvasWidth, int canvasHeight, Rectangle visibleRect) {
        long start = System.currentTimeMillis();
        var frameBoxHeight = getFrameBoxHeight(g2);
        var rect = new Rectangle();

        identifyDisplayScale(g2);

        {
            // handle root node
            var events = stacktraceTreeModel.getItems()
                                            .stream()
                                            .map(iItems -> iItems.getType().getIdentifier())
                                            .collect(joining(", "));
            var rootNode = nodes.get(0);
            rect.x = (int) (canvasWidth * rootNode.startX);
            rect.width = ((int) (canvasWidth * rootNode.endX)) - rect.x;

            rect.y = frameBoxHeight * rootNode.stackDepth;
            rect.height = frameBoxHeight;

            if (visibleRect.intersects(rect)) {
                paintRootFrameRectangle((Graphics2D) g2.create(rect.x, rect.y, rect.width, rect.height),
                                        "all (" + events + ")",
                                        highlightedFrame == rootNode,
                                        selectedFrame == rootNode);
            }
        }

        for (int i = 1; i < nodes.size(); i++) {
            var node = nodes.get(i);
            // TODO Can we do cheaper checks like depth is outside range etc

            rect.x = (int) (canvasWidth * node.startX);
            rect.width = ((int) (canvasWidth * node.endX)) - rect.x;

            if ((rect.width < frameWidthVisibilityThreshold)) {
                continue;
            }

            rect.y = frameBoxHeight * node.stackDepth;
            rect.height = frameBoxHeight;

            if (visibleRect.intersects(rect)) {
                paintNodeFrameRectangle((Graphics2D) g2.create(rect.x, rect.y, rect.width, rect.height),
                                        node.jfrNode,
                                        highlightedFrame == node,
                                        selectedFrame == node);
            }
        }

        // timestamp
        var drawTimeMs = "Draw time: " + (System.currentTimeMillis() - start) + " ms";
        var nowWidth = g2.getFontMetrics().stringWidth(drawTimeMs);
        g2.setColor(Color.darkGray);
        g2.fillRect(canvasWidth - nowWidth - textBorder * 2,
                    visibleRect.y + visibleRect.height - frameBoxHeight,
                    nowWidth + textBorder * 2,
                    frameBoxHeight);
        g2.setColor(Color.yellow);

        g2.drawString(drawTimeMs,
                      canvasWidth - nowWidth - textBorder,
                      visibleRect.y + visibleRect.height - textBorder);
    }

    private void identifyDisplayScale(Graphics2D g2) {
        var transform = g2.getTransform();
        if ((transform.getType() & AffineTransform.TYPE_MASK_SCALE) == AffineTransform.TYPE_UNIFORM_SCALE) {
            // if true we're on a HiDPI display
            // https://github.com/libgdx/libgdx/commit/2bc16a08961dd303afe2d1c8df96a50d8cd639db
            System.out.println("HiDPI");
        }
        System.out.println(scaleX = transform.getScaleX());
        System.out.println(scaleY = transform.getScaleY());
    }

    private Color handleFocus(Color bgColor, boolean highlighted, boolean selected) {
        if (highlighted) {
            return bgColor.darker();
        }
        if (selected) {
            return selectionColor;
        }
        return bgColor;
    }

    private void paintNodeFrameRectangle(Graphics2D g2, Node childFrame, boolean highlighted, boolean selected) {
        var bgColor = handleFocus(colorMode.getColor(colorPalette, childFrame.getFrame()), highlighted, selected);
        var frameRectSurface = paintFrameRectangle(g2,
                                                   bgColor,
                                                   highlighted,
                                                   selected);
        paintFrameText(childFrame.getFrame(),
                       g2,
                       frameRectSurface.width - textBorder * 2,
                       text -> {
                           g2.setColor(Colors.foregroundColor(bgColor));
                           g2.drawString(text, textBorder, getFrameBoxTextOffset(g2));
                       });
    }

    private void paintRootFrameRectangle(Graphics2D g2, String str, boolean highlighted, boolean selected) {
        var frameRectSurface = paintFrameRectangle(g2,
                                                   handleFocus(ColorMode.rootNodeColor, highlighted, selected),
                                                   highlighted,
                                                   selected);
        paintFrameText(str,
                       g2,
                       frameRectSurface.width - textBorder * 2,
                       text -> {
                           g2.setColor(Colors.foregroundColor(ColorMode.rootNodeColor));
                           g2.drawString(text, textBorder, getFrameBoxTextOffset(g2));
                       });
    }

    private Rectangle2D.Double paintFrameRectangle(Graphics2D g2, Color bgColor, boolean highlighted, boolean selected) {
        var clipBounds = g2.getClipBounds();
        var borderWidth = paintFrameBorder ? 1 : 0;

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

    public Optional<FrameBox<Node>> getFrameAt(Graphics2D g2, Point point, Rectangle visibleRect) {
        int depth = point.y / getFrameBoxHeight(g2);
        double xLocation = (point.x * 1.0d) / visibleRect.width;

        return nodes.stream()
                    .filter(node -> node.stackDepth == depth && node.startX <= xLocation && xLocation <= node.endX)
                    .findFirst();
    }

    public void toggleSelectedFrameAt(Graphics2D g2, Point point, Rectangle visibleRect) {
        getFrameAt(
                g2,
                point,
                visibleRect
        ).ifPresent(frame -> selectedFrame = selectedFrame == frame ? null : frame);
    }

    public void highlightFrameAt(Graphics2D g2, Point point, Rectangle visibleRect) {
        getFrameAt(
                g2,
                point,
                visibleRect
        ).ifPresentOrElse(frame -> highlightedFrame = frame,
                          this::stopHighlight);
    }

    public void stopHighlight() {
        highlightedFrame = null;
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
