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
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class FlameGraphPanel extends JPanel {
    private final Supplier<StacktraceTreeModel> stacktraceTreeModelSupplier;

    public FlameGraphPanel(Supplier<StacktraceTreeModel> stacktraceTreeModelSupplier) {
        super(new BorderLayout());
        this.stacktraceTreeModelSupplier = memoize(stacktraceTreeModelSupplier);

        var wrapper = new JPanel(new BorderLayout());

        var timer = new Timer(2_000, e -> {
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
        private final java.util.List<FlameNode<Node>> nodes;

        public FlameGraph(Supplier<StacktraceTreeModel> stacktraceTreeModelSupplier) {
            this.stacktraceTreeModel = stacktraceTreeModelSupplier.get();
            this.nodes = FlameNodeBuilder.buildFlameNodes(this.stacktraceTreeModel);
//            this.setDoubleBuffered(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            long start = System.nanoTime();
            Graphics2D g2 = (Graphics2D) g;
            super.paintComponent(g2);     // paint parent's background

//            g2.setFont(new Font("Monospaced", Font.PLAIN, 12));

            var currentWidth = getWidth();
            var currentHeight = getHeight();
            var fontHeight = g2.getFontMetrics().getAscent();
            var textBorder = 2;
            var frameBoxHeight = fontHeight + (textBorder * 2);
            var visible = this.getVisibleRect();

            var rect = new Rectangle();
            //noinspection ForLoopReplaceableByForEach Faster to not use
            for (int i = 0; i < nodes.size(); i++) {
                var node = nodes.get(i);
                // TODO Can we do cheaper checks like depth is outside range etc

                rect.x = (int) (currentWidth * node.startX);
                rect.width = ((int) (currentWidth * node.endX)) - rect.x;

                rect.y = frameBoxHeight * node.stackDepth;
                rect.height = frameBoxHeight;

                if (visible.intersects(rect)) {
                    paintFrameRectangle((Graphics2D) g.create(rect.x, rect.y, rect.width, rect.height), node.jfrNode);
                }
            }


            // timestamp
            var s = Long.toString(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            var nowWidth = g2.getFontMetrics().stringWidth(s);
            g2.setColor(Color.darkGray);
            g2.fillRect(currentWidth - nowWidth - textBorder * 2, currentHeight - frameBoxHeight, nowWidth + textBorder * 2, frameBoxHeight);
            g2.setColor(Color.yellow);
            g2.drawString(s, currentWidth - nowWidth - textBorder, currentHeight - textBorder);
        }

        private void paintFrameRectangle(Graphics2D g2, Node childFrame) {
            var outerRect = g2.getClipBounds();
            var innerRectSurface = new Rectangle2D.Double(outerRect.x + 1, outerRect.y + 1, outerRect.width - 1, outerRect.height - 1);

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
            g2.drawString(adaptFrameText(childFrame.getFrame(), g2, innerRectSurface.width), 2,
                          (float) innerRectSurface.height - 2);
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
