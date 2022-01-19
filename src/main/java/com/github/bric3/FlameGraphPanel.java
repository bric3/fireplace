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

import com.github.bric3.flamegraph.ColorMode;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.AggregatableFrame;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.stream.Collectors.joining;

public class FlameGraphPanel extends JPanel {
    private final Supplier<StacktraceTreeModel> stacktraceTreeModelSupplier;
    private FlameGraphPainter flameGraph;


    public FlameGraphPanel(Supplier<StacktraceTreeModel> stacktraceTreeModelSupplier) {
        super(new BorderLayout());
        this.stacktraceTreeModelSupplier = Utils.memoize(stacktraceTreeModelSupplier);

        var wrapper = new JPanel(new BorderLayout());
        wrapper.add(createInternalFlameGraphPanel());

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

        var colorPaletteJComboBox = new JComboBox<ColorPalette>(ColorPalette.values());
        colorPaletteJComboBox.addActionListener(e -> {
            flameGraph.colorPalette = (ColorPalette) colorPaletteJComboBox.getSelectedItem();
            wrapper.repaint();
        });
        colorPaletteJComboBox.setSelectedItem(flameGraph.colorPalette);

        var colorModeJComboBox = new JComboBox<ColorMode>(ColorMode.values());
        colorModeJComboBox.addActionListener(e -> {
            flameGraph.colorMode = (ColorMode) colorModeJComboBox.getSelectedItem();
            wrapper.repaint();
        });
        colorModeJComboBox.setSelectedItem(flameGraph.colorMode);

        var borderToggle = new JCheckBox("Border");
        borderToggle.addActionListener(e -> {
            flameGraph.paintFrameBorder = borderToggle.isSelected();
            wrapper.repaint();
        });
        borderToggle.setSelected(flameGraph.paintFrameBorder);

        var controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(colorPaletteJComboBox);
        controlPanel.add(colorModeJComboBox);
        controlPanel.add(borderToggle);
        controlPanel.add(refreshToggle);


        add(controlPanel, BorderLayout.NORTH);
        add(wrapper, BorderLayout.CENTER);
    }

    private JComponent createInternalFlameGraphPanel() {
        flameGraph = new FlameGraphPainter(stacktraceTreeModelSupplier);

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

        var scrollPaneMouseListener = new ScrollPaneMouseListener(flameGraph);

        return JScrollPaneWithButton.create(
                flameGraphCanvas,
                sp -> {
                    sp.getVerticalScrollBar().setUnitIncrement(16);
                    sp.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
                    scrollPaneMouseListener.install(sp);
                }
        );
    }


    static class ScrollPaneMouseListener implements java.awt.event.MouseListener, MouseMotionListener {
        private Point pressedPoint;
        private FlameGraphPainter flameGraph;

        public ScrollPaneMouseListener(FlameGraphPainter flameGraph) {
            this.flameGraph = flameGraph;
        }

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
                pressedPoint = e.getPoint();
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                this.pressedPoint = e.getPoint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            pressedPoint = null;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e)) {
                return;
            }

            if ((e.getSource() instanceof JScrollPane)) {
                var scrollPane = (JScrollPane) e.getComponent();
                var viewPort = scrollPane.getViewport();

                var point = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), viewPort.getView());

                flameGraph.toggleSelectedFrameAt(
                        (Graphics2D) viewPort.getView().getGraphics(),
                        point,
                        viewPort.getVisibleRect()
                );

                scrollPane.repaint();
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {
            if ((e.getSource() instanceof JScrollPane)) {
                var scrollPane = (JScrollPane) e.getComponent();
                flameGraph.stopHighlight();
                scrollPane.repaint();
            }

        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if ((e.getSource() instanceof JScrollPane)) {
                var scrollPane = (JScrollPane) e.getComponent();
                var viewPort = scrollPane.getViewport();

                var point = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), viewPort.getView());
                flameGraph.highlightFrameAt(
                        (Graphics2D) viewPort.getView().getGraphics(),
                        point,
                        viewPort.getVisibleRect()
                );

                scrollPane.repaint();
            }
        }

        public void install(JScrollPane sp) {
            sp.addMouseListener(this);
            sp.addMouseMotionListener(this);
        }

    }

    private static class FlameGraphPainter {
        public ColorMode colorMode = ColorMode.BY_PACKAGE;
        public ColorPalette colorPalette = ColorPalette.DARK_CUSTOM;
        public Color selectionColor = Color.yellow;
        public Color frameBorderColor = Color.gray;
        public boolean paintFrameBorder = true;
        private final StacktraceTreeModel stacktraceTreeModel;
        private final java.util.List<FrameBox<Node>> nodes;
        private final int depth;
        private final int textBorder = 2;

        private FrameBox<Node> highlightedFrame;
        private FrameBox<Node> selectedFrame;

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

        private float getFrameBoxTextOffset(Graphics2D g2) {
            return getFrameBoxHeight(g2) - (g2.getFontMetrics().getDescent() / 2f) - textBorder;
        }

        public void paint(Graphics2D g2, int canvasWidth, int canvasHeight, Rectangle visibleRect) {
            long start = System.currentTimeMillis();
            var frameBoxHeight = getFrameBoxHeight(g2);
            var rect = new Rectangle();

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
                           frameRectSurface.width,
                           text -> {
                               g2.setColor(ColorPalette.foregroundColor(bgColor));
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
                           frameRectSurface.width,
                           text -> {
                               g2.setColor(ColorPalette.foregroundColor(ColorMode.rootNodeColor));
                               g2.drawString(text, textBorder, (float) frameRectSurface.y + getFrameBoxTextOffset(g2));
                           });
        }

        private Rectangle2D.Double paintFrameRectangle(Graphics2D g2, Color bgColor, boolean highlighted, boolean selected) {
            var borderWidth = paintFrameBorder ? 1 : 0;
            var outerRect = g2.getClipBounds();

            if (paintFrameBorder) {
                g2.setColor(frameBorderColor);
                g2.draw(outerRect);
            }

            g2.setColor(bgColor);
            var frameRectSurface = new Rectangle2D.Double(outerRect.x + borderWidth,
                                                          outerRect.y + borderWidth,
                                                          outerRect.width - borderWidth,
                                                          outerRect.height - borderWidth);
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
}
