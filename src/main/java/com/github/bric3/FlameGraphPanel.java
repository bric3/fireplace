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
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
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
            wrapper.add(createInternalFlameGraphPanel());
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

        wrapper.add(createInternalFlameGraphPanel());
    }

    static class ScrollPaneMouseListener implements java.awt.event.MouseListener, MouseMotionListener {
        private Point pressedPoint;

        @Override
        public void mouseDragged(MouseEvent e) {
            if ((e.getSource() instanceof JScrollPane) && pressedPoint != null) {

                var scrollPane = (JScrollPane) e.getComponent();
                var viewPort = scrollPane.getViewport();
                if (viewPort == null) {
                    return;
                }

                var dx = e.getX() - pressedPoint.x;
                var dy = e.getY() - pressedPoint.y;
                var viewPortViewPosition = viewPort.getViewPosition();
                viewPort.setViewPosition(new Point(Math.max(0, viewPortViewPosition.x - dx),
                                                   Math.max(0, viewPortViewPosition.y - dy)));
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                this.pressedPoint = e.getPoint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            pressedPoint = null;
        }

        @Override
        public void mouseClicked(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }

        @Override
        public void mouseMoved(MouseEvent e) {

        }

        public void install(JScrollPane sp) {
            sp.addMouseListener(this);
            sp.addMouseMotionListener(this);
        }
    }

    private JComponent createInternalFlameGraphPanel() {
        var flameGraph = new FlameGraphPainter(stacktraceTreeModelSupplier);

        var flameGraphCanvas = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d = (d == null) ? new Dimension(400, 400) : d;
                Insets insets = getInsets();

                d.height = Math.max(d.height, flameGraph.getFlameGraphHeight((Graphics2D) getGraphics()) + insets.top + insets.bottom);
                return d;
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                flameGraph.paint((Graphics2D) g, getWidth(), getHeight(), getVisibleRect());
            }
        };

        return JScrollPaneWithButton.create(flameGraphCanvas,
                                            sp -> {
                                                sp.getVerticalScrollBar().setUnitIncrement(16);
                                                sp.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
                                                new ScrollPaneMouseListener().install(sp);
                                            });
    }

    private static class FlameGraphPainter {
        public static final Color UNDEFINED_COLOR = new Color(108, 163, 189);
        public static final Color JIT_COMPILED_COLOR = new Color(21, 110, 64);
        public static final Color INLINED_COLOR = Color.pink;
        public static final Color INTERPRETED_COLOR = Color.orange;
        private final StacktraceTreeModel stacktraceTreeModel;
        private final java.util.List<FlameNode<Node>> nodes;
        private final int depth;
        private final int textBorder = 2;

        public FlameGraphPainter(Supplier<StacktraceTreeModel> stacktraceTreeModelSupplier) {
            this.stacktraceTreeModel = stacktraceTreeModelSupplier.get();
            this.nodes = FlameNodeBuilder.buildFlameNodes(this.stacktraceTreeModel);

            this.depth = this.stacktraceTreeModel.getRoot()
                                                 .getChildren()
                                                 .stream()
                                                 .mapToInt(node -> node.getFrame().getFrameLineNumber())
                                                 .max()
                                                 .orElse(0);
        }

        private int getFrameBoxHeight(Graphics2D g2) {
            return g2.getFontMetrics().getAscent() + (textBorder * 2);
        }

        public int getFlameGraphHeight(Graphics2D g2) {
            return depth * getFrameBoxHeight(g2);
        }

        protected void paint(Graphics2D g2, int canvasWidth, int canvasHeight, Rectangle visibleRect) {
            long start = System.currentTimeMillis();

//            g2.setFont(new Font("Monospaced", Font.PLAIN, 12));

            var currentWidth = canvasWidth;
            var currentHeight = canvasHeight;
            var frameBoxHeight = getFrameBoxHeight(g2);

            var rect = new Rectangle();

            {
                // handle root node
                var events = stacktraceTreeModel.getItems()
                                                .stream()
                                                .map(iItems -> iItems.getType().getIdentifier())
                                                .collect(joining(", "));
                var rootNode = nodes.get(0);
                rect.x = (int) (currentWidth * rootNode.startX);
                rect.width = ((int) (currentWidth * rootNode.endX)) - rect.x;

                rect.y = frameBoxHeight * rootNode.stackDepth;
                rect.height = frameBoxHeight;

                if (visibleRect.intersects(rect)) {
                    paintRootFrameRectangle((Graphics2D) g2.create(rect.x, rect.y, rect.width, rect.height),
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

                if (visibleRect.intersects(rect)) {
                    paintNodeFrameRectangle((Graphics2D) g2.create(rect.x, rect.y, rect.width, rect.height), node.jfrNode);
                }
            }

            // timestamp
            var drawTimeMs = "Draw time: " + (System.currentTimeMillis() - start) + " ms";
            var nowWidth = g2.getFontMetrics().stringWidth(drawTimeMs);
            g2.setColor(Color.darkGray);
            g2.fillRect(currentWidth - nowWidth - textBorder * 2, visibleRect.y + visibleRect.height - frameBoxHeight, nowWidth + textBorder * 2, frameBoxHeight);
            g2.setColor(Color.yellow);

            g2.drawString(drawTimeMs, currentWidth - nowWidth - textBorder, visibleRect.y + visibleRect.height - textBorder);
        }

        private void paintNodeFrameRectangle(Graphics2D g2, Node childFrame) {
            var innerRectSurface = paintFrameRectangle2(g2, childFrame.getFrame().getType());
            adaptFrameText(childFrame.getFrame(),
                           g2,
                           innerRectSurface.width,
                           text -> g2.drawString(text, 2, (float) innerRectSurface.y + getFrameBoxHeight(g2) - textBorder));
        }

        private void paintRootFrameRectangle(Graphics2D g2, String str) {
            var innerRectSurface = paintFrameRectangle2(g2, Type.UNKNOWN);
            adaptFrameText(str,
                           g2,
                           innerRectSurface.width,
                           text -> g2.drawString(text, 2, (float) innerRectSurface.y + getFrameBoxHeight(g2) - textBorder));
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
