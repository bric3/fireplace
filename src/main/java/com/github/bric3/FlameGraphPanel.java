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

import org.openjdk.jmc.common.IMCFrame.Type;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.AggregatableFrame;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.stream.Collectors.joining;

public class FlameGraphPanel extends JPanel {
    private final Supplier<StacktraceTreeModel> stacktraceTreeModelSupplier;

    public FlameGraphPanel(Supplier<StacktraceTreeModel> stacktraceTreeModelSupplier) {
        super(new BorderLayout());
        this.stacktraceTreeModelSupplier = Utils.memoize(stacktraceTreeModelSupplier);

        var wrapper = new JPanel(new BorderLayout());

        var timer = new Timer(2_000, e -> {
            wrapper.removeAll();
            wrapper.add(JScrollPaneWithButton.create(createInternalFlameGraphPanel(),
                                                     sp -> sp.getVerticalScrollBar().setUnitIncrement(16)));
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

        wrapper.add(JScrollPaneWithButton.create(createInternalFlameGraphPanel(),
                                                 sp -> sp.getVerticalScrollBar().setUnitIncrement(16)));
    }

    private JComponent createInternalFlameGraphPanel() {
        return new FlameGraph(stacktraceTreeModelSupplier);
    }

    private static class FlameGraph extends JPanel {
        public static final Color UNDEFINED_COLOR = new Color(108, 163, 189);
        public static final Color JIT_COMPILED_COLOR = new Color(21, 110, 64);
        public static final Color INLINED_COLOR = Color.pink;
        public static final Color INTERPRETED_COLOR = Color.orange;
        private final StacktraceTreeModel stacktraceTreeModel;
        private final java.util.List<FlameNode<Node>> nodes;
        private final int depth;
        private final int textBorder = 2;

        public FlameGraph(Supplier<StacktraceTreeModel> stacktraceTreeModelSupplier) {
            this.stacktraceTreeModel = stacktraceTreeModelSupplier.get();
            this.nodes = FlameNodeBuilder.buildFlameNodes(this.stacktraceTreeModel);

            this.depth = this.stacktraceTreeModel.getRoot().getChildren().stream().mapToInt(node -> node.getFrame().getFrameLineNumber()).max().orElse(0);
//            this.setDoubleBuffered(true);
        }

        private int getFrameBoxHeight() {
            return getGraphics().getFontMetrics().getAscent() + (textBorder * 2);
        }

        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            d = (d == null) ? new Dimension(400, 400) : d;
            Insets insets = getInsets();


            d.height = Math.max(d.height, depth * getFrameBoxHeight() + insets.top + insets.bottom);
            return d;
        }

        @Override
        protected void paintComponent(Graphics g) {
            long start = System.currentTimeMillis();
            Graphics2D g2 = (Graphics2D) g;
            super.paintComponent(g2);     // paint parent'drawTimeMs background

//            g2.setFont(new Font("Monospaced", Font.PLAIN, 12));

            var currentWidth = getWidth();
            var currentHeight = getHeight();
            var frameBoxHeight = getFrameBoxHeight();
            var visible = this.getVisibleRect();

            var rect = new Rectangle();
            //noinspection ForLoopReplaceableByForEach Faster to not use

            {
                var events = stacktraceTreeModel.getItems()
                                                .stream()
                                                .map(iItems -> iItems.getType().getIdentifier())
                                                .collect(joining(", "));
                var rootNode = nodes.get(0);
                rect.x = (int) (currentWidth * rootNode.startX);
                rect.width = ((int) (currentWidth * rootNode.endX)) - rect.x;

                rect.y = frameBoxHeight * rootNode.stackDepth;
                rect.height = frameBoxHeight;

                if (visible.intersects(rect)) {
                    paintRootFrameRectangle((Graphics2D) g.create(rect.x, rect.y, rect.width, rect.height),
                                            "all (" + events + ")");
                }
            }

            for (int i = 1; i < nodes.size(); i++) {
                var node = nodes.get(i);
                // TODO Can we do cheaper checks like depth is outside range etc

                rect.x = (int) (currentWidth * node.startX);
                rect.width = ((int) (currentWidth * node.endX)) - rect.x;

                rect.y = frameBoxHeight * node.stackDepth;
                rect.height = frameBoxHeight;

                if (visible.intersects(rect)) {
                    paintNodeFrameRectangle((Graphics2D) g.create(rect.x, rect.y, rect.width, rect.height), node.jfrNode);
                }
            }

            // timestamp
            var drawTimeMs = "Draw time: " + (System.currentTimeMillis() - start) + " ms";
            var nowWidth = g2.getFontMetrics().stringWidth(drawTimeMs);
            g2.setColor(Color.darkGray);
            var visibleRect = getVisibleRect();
            g2.fillRect(currentWidth - nowWidth - textBorder * 2, visibleRect.y + visibleRect.height - frameBoxHeight, nowWidth + textBorder * 2, frameBoxHeight);
            g2.setColor(Color.yellow);

            g2.drawString(drawTimeMs, currentWidth - nowWidth - textBorder, visibleRect.y + visibleRect.height - textBorder);
        }

        private void paintNodeFrameRectangle(Graphics2D g2, Node childFrame) {
            Rectangle2D.Double innerRectSurface = paintFrameRectangle2(g2, childFrame.getFrame().getType());
            adaptFrameText(childFrame.getFrame(), g2, innerRectSurface.width, text -> g2.drawString(text, 2, (float) innerRectSurface.height - 2));
        }

        private void paintRootFrameRectangle(Graphics2D g2, String text) {
            Rectangle2D.Double innerRectSurface = paintFrameRectangle2(g2, Type.UNKNOWN);
            adaptFrameText(text, g2, innerRectSurface.width, t -> g2.drawString(t, 2, (float) innerRectSurface.height - 2));
        }

        private Rectangle2D.Double paintFrameRectangle2(Graphics2D g2, Type frameType) {
            var outerRect = g2.getClipBounds();
            var innerRectSurface = new Rectangle2D.Double(outerRect.x + 1, outerRect.y + 1, outerRect.width - 1, outerRect.height - 1);

            g2.setColor(Color.gray);
            g2.draw(outerRect);

            switch (frameType) {
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
            return innerRectSurface;
        }
    }

    private static void adaptFrameText(AggregatableFrame frame, Graphics2D g2, double targetWidth, Consumer<String> textConsumer) {
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

    private static void adaptFrameText(String text, Graphics2D g2, double targetWidth, Consumer<String> textConsumer) {
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
