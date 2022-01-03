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

import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.time.LocalTime;
import java.util.function.Supplier;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableList;

public class FlameGraphPanel extends JPanel {
    private final Supplier<StacktraceTreeModel> stacktraceTreeModelSupplier;

    public FlameGraphPanel(Supplier<StacktraceTreeModel> stacktraceTreeModelSupplier) {
        super(new BorderLayout());
        this.stacktraceTreeModelSupplier = memoize(stacktraceTreeModelSupplier);

        setDoubleBuffered(true);

        var wrapper = new JPanel(new BorderLayout());

        var timer = new javax.swing.Timer(2_000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                wrapper.removeAll();
                wrapper.add(createInternalFlameGraphPanel());
                wrapper.repaint(1_000);
                wrapper.revalidate();
            }
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

        wrapper.add(createInternalFlameGraphPanel());
    }

    private Component createInternalFlameGraphPanel() {
        return new FlameGraph(stacktraceTreeModelSupplier);
    }

    private static class FlameGraph extends JPanel {
        private final StacktraceTreeModel stacktraceTreeModel;

        public FlameGraph(Supplier<StacktraceTreeModel> stacktraceTreeModelSupplier) {
            this.stacktraceTreeModel = stacktraceTreeModelSupplier.get();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            super.paintComponent(g2);     // paint parent's background

            g2.setFont(new Font("Monospaced", Font.PLAIN, 12));


            var currentWidth = getWidth();
            var currentHeight = getHeight();
            var fontHeight = g2.getFontMetrics().getAscent();
            var textBorder = 2;
            var frameBoxHeight = fontHeight + (textBorder * 2);

            // timestamp
            var s = LocalTime.now().toString();
            g2.setColor(Color.yellow);
            g2.drawString(s, currentWidth - g2.getFontMetrics().stringWidth(s) - textBorder, currentHeight - textBorder);

            // all root node
            var root = stacktraceTreeModel.getRoot();
            var rootRect = new Rectangle2D.Double(1, 1, currentWidth - 1, frameBoxHeight);
            g2.setPaint(Color.orange.brighter());
            g2.setColor(Color.orange.darker());
            g2.fill(rootRect);

            g2.setColor(Color.darkGray);
            var events = stacktraceTreeModel.getItems()
                                            .stream()
                                            .map(iItems -> iItems.getType().getIdentifier())
                                            .collect(joining(", "));
            g2.drawString("all (" + events + ")", textBorder, frameBoxHeight - textBorder);


            int depth = 0;


            var children = root.getChildren();
            var globalWeight = children.stream().mapToDouble(Node::getCumulativeWeight).sum();
            var scaleYRatio = currentWidth / globalWeight;


//            var myTransform = AffineTransform.getScaleInstance(1.0, currentWidth / globalWeight);
//            g2.transform(myTransform);

            // for hidpi ?
            AffineTransform transform = g2.getTransform();
            double scaleX = 1d / transform.getScaleX();
            double scaleY = 1d / transform.getScaleY();

            double prevChildBoxX = 1;
            double prevChildBoxY = frameBoxHeight + 1;
            Rectangle innerRect = g.getClipBounds();


            var firstNode = root.getChildren().get(0);

            for (var child : children.stream().collect(toUnmodifiableList())) {
                // FIXME: If the cumulative weight is too small the rectangle is not quite visible

                if (child.getParent().isRoot()) {
                    depth = Math.max(depth, firstNode.getFrame().getFrameLineNumber());
                }

                var rectWidth = child.getCumulativeWeight() * scaleYRatio;
                var outerRect = new Rectangle2D.Double(prevChildBoxX, prevChildBoxY, rectWidth, frameBoxHeight);
                var innerRectSurface = new Rectangle2D.Double(prevChildBoxX + 1, prevChildBoxY + 1, rectWidth - 1, frameBoxHeight - 1);


                var humanReadableShortString = child.getFrame().getHumanReadableShortString();

                g2.setColor(Color.gray);
                g2.draw(outerRect);

                g2.setColor(Color.orange);
                g2.fill(innerRectSurface);


                var frameTextWidth = g2.getFontMetrics().stringWidth(humanReadableShortString);
                g2.setColor(Color.darkGray);
                g2.drawString((frameTextWidth + textBorder * 2) <= rectWidth ? humanReadableShortString : "...",
                              (float) (prevChildBoxX + textBorder),
                              (float) (prevChildBoxY + frameBoxHeight - textBorder));

                prevChildBoxX = prevChildBoxX + rectWidth;


            }
        }
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
