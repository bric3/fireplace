/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.github.bric3;

import org.openjdk.jmc.flightrecorder.stacktrace.tree.AggregatableFrame;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.time.LocalTime;
import java.util.function.Supplier;

import static java.util.stream.Collectors.joining;

public class FlameGraphPanel extends JPanel {
    private final Supplier<StacktraceTreeModel> stacktraceTreeModelSupplier;

    public FlameGraphPanel(Supplier<StacktraceTreeModel> stacktraceTreeModelSupplier) {
        super(new BorderLayout());
        this.stacktraceTreeModelSupplier = memoize(stacktraceTreeModelSupplier);

        var wrapper = new JPanel(new BorderLayout());

        var timer = new javax.swing.Timer(2_000, e -> {
            wrapper.removeAll();
            wrapper.add(new JScrollPane(createInternalFlameGraphPanel()));
            wrapper.repaint(1_000);
            wrapper.revalidate();
        });
        timer.setRepeats(true);

        var refreshToggle = new JToggleButton("Refresh");
        refreshToggle.addActionListener(e -> {
            if (timer.isRunning()) {
                timer.stop();
            } else {

                timer.start();
            }
        });

        add(refreshToggle, BorderLayout.NORTH);
        add(wrapper, BorderLayout.CENTER);

        wrapper.add(new JScrollPane(createInternalFlameGraphPanel()));
    }

    private Component createInternalFlameGraphPanel() {
        return new FlameGraph(stacktraceTreeModelSupplier);
    }

    private static class FlameGraph extends JPanel {
        public static final Color UNDEFINED_COLOR = new Color(108, 163, 189);
        public static final Color JIT_COMPILED_COLOR = new Color(21, 110, 64);
        public static final Color INLINED_COLOR = Color.pink;
        public static final Color INTERPRETED_COLOR = Color.orange;
        private final StacktraceTreeModel stacktraceTreeModel;
        private int depth;

        public FlameGraph(Supplier<StacktraceTreeModel> stacktraceTreeModelSupplier) {
            this.stacktraceTreeModel = stacktraceTreeModelSupplier.get();
//            this.setDoubleBuffered(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            super.paintComponent(g2);     // paint parent's background

//            g2.setFont(new Font("Monospaced", Font.PLAIN, 12));

            var currentWidth = getWidth();
            var currentHeight = getHeight();
            var fontHeight = g2.getFontMetrics().getAscent();
            var textBorder = 2;
            var frameBoxHeight = fontHeight + (textBorder * 2);

            // all root node
            var root = stacktraceTreeModel.getRoot();
            var rootRect = new Rectangle2D.Double(1, 1, currentWidth - 1, frameBoxHeight);
            g2.setPaint(INTERPRETED_COLOR.brighter());
            g2.setColor(INTERPRETED_COLOR.darker());
            g2.fill(rootRect);

            g2.setColor(Color.darkGray);
            var events = stacktraceTreeModel.getItems()
                                            .stream()
                                            .map(iItems -> iItems.getType().getIdentifier())
                                            .collect(joining(", "));
            g2.drawString("all (" + events + ")", textBorder, frameBoxHeight - textBorder);


            depth = 0;


            var children = root.getChildren();
            var globalWeight = children.stream().mapToDouble(Node::getCumulativeWeight).sum();
            var scaleYRatio = currentWidth / globalWeight;

//            var myTransform = AffineTransform.getScaleInstance(1.0, currentWidth / globalWeight);
//            g2.transform(myTransform);

            // for hidpi ?
//            AffineTransform transform = g2.getTransform();
//            double scaleX = 1d / transform.getScaleX();
//            double scaleY = 1d / transform.getScaleY();

            double prevChildBoxX = 1;
            double prevChildBoxY = frameBoxHeight + 1;

            dfsPaint(g2, root.getChildren(), prevChildBoxX, prevChildBoxY, frameBoxHeight, textBorder, scaleYRatio);

            // timestamp
            var s = LocalTime.now().toString();
            var nowWidth = g2.getFontMetrics().stringWidth(s);
            g2.setColor(Color.darkGray);
            g2.fillRect(currentWidth - nowWidth - textBorder * 2, currentHeight - frameBoxHeight, nowWidth + textBorder * 2, frameBoxHeight);
            g2.setColor(Color.yellow);
            g2.drawString(s, currentWidth - nowWidth - textBorder, currentHeight - textBorder);
        }

        private void dfsPaint(Graphics2D g2, java.util.List<Node> children, double prevChildBoxX, double prevChildBoxY, int frameBoxHeight, int textBorder, double scaleYRatio) {
            for (var child : children) {
                if (child.getParent().isRoot()) {
                    depth = Math.max(depth, child.getFrame().getFrameLineNumber());
                }
                // FIXME: If the cumulative weight is too small the rectangle is not quite visible


                double rectWidth = paintFrameRectangle(
                        g2,
                        textBorder,
                        frameBoxHeight,
                        scaleYRatio,
                        prevChildBoxX,
                        prevChildBoxY,
                        child
                );

                dfsPaint(g2, child.getChildren(),
                         prevChildBoxX,
                         prevChildBoxY + frameBoxHeight,
                         frameBoxHeight,
                         textBorder,
                         scaleYRatio);
                prevChildBoxX = prevChildBoxX + rectWidth;
            }
        }

        private double paintFrameRectangle(Graphics2D g2, int textBorder, int frameRectHeight, double scaleYRatio, double prevFrameX, double prevFrameY, Node childFrame) {
            var rectWidth = childFrame.getCumulativeWeight() * scaleYRatio;
            var outerRect = new Rectangle2D.Double(prevFrameX, prevFrameY, rectWidth, frameRectHeight);
            var innerRectSurface = new Rectangle2D.Double(prevFrameX + 1, prevFrameY + 1, rectWidth - 1, frameRectHeight - 1);

            g2.setColor(Color.gray);
            g2.draw(outerRect);

            switch (childFrame.getFrame().getType()) {
                case INTERPRETED:
                    g2.setColor(INTERPRETED_COLOR);
                    break;
                case INLINED:
                    g2.setColor(INLINED_COLOR);
                    break;
                case JIT_COMPILED:
                    g2.setColor(JIT_COMPILED_COLOR);
                    break;
                case UNKNOWN:
                    g2.setColor(UNDEFINED_COLOR);
                    break;
            }

            g2.fill(innerRectSurface);

            g2.setColor(Color.darkGray);
            g2.drawString(adaptFrameText(childFrame.getFrame(), g2, rectWidth),
                          (float) (prevFrameX + textBorder),
                          (float) (prevFrameY + frameRectHeight - textBorder));
            return rectWidth;
        }
    }

    private static String adaptFrameText(AggregatableFrame frame, Graphics2D g2, double targetWidth) {
        var metrics = g2.getFontMetrics();

        var adaptedText = frame.getHumanReadableShortString();
        var textBounds = metrics.getStringBounds(adaptedText, g2);

        if (!(textBounds.getWidth() > targetWidth)) {
            return adaptedText;
        }
        adaptedText = frame.getMethod().getMethodName();
        textBounds = metrics.getStringBounds(adaptedText, g2);
        if (!(textBounds.getWidth() > targetWidth)) {
            return adaptedText;
        }
        return "...";


//        String shortText = text;
//        int activeIndex = text.length() - 1;
//
//        Rectangle2D textBounds = metrics.getStringBounds(shortText, g2);
//        while (textBounds.getWidth() > targetWidth) {
//            shortText = text.substring(0, activeIndex--);
//            textBounds = metrics.getStringBounds(shortText + "...", g2);
//        }
//        return activeIndex != text.length() - 1 ? shortText + "..." : text;
    }


    // non thread safe
    public static <T> Supplier<T> memoize(final Supplier<T> valueSupplier) {
        return new Supplier<T>() {
            private T cachedValue;

            @Override
            public T get() {
                if (cachedValue == null) {
                    cachedValue = valueSupplier.get();
                }
                return cachedValue;
            }
        };
    }

}
