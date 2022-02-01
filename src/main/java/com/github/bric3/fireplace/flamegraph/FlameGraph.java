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

import com.github.bric3.fireplace.ui.BalloonToolTip;
import com.github.bric3.fireplace.ui.Colors;
import com.github.bric3.fireplace.ui.JScrollPaneWithButton;
import com.github.bric3.fireplace.ui.MouseInputListenerWorkaroundForToolTipEnabledComponent;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class FlameGraph<T> {
    public final FlameGraphPainter<T> flameGraphPainter;
    private final FlameGraphCanvas<T> canvas;
    public final JLayer<JScrollPane> component;

    public FlameGraph(List<FrameBox<T>> framesSupplier,
                      List<Function<T, String>> frameToStringCandidates,
                      Function<T, String> rootFrameToString,
                      Function<T, Color> frameColorFunction,
                      Function<FrameBox<T>, String> extractToolTip,
                      FrameColorMode<T> frameColorMode
    ) {
        flameGraphPainter = new FlameGraphPainter<T>(
                framesSupplier,
                frameToStringCandidates,
                rootFrameToString,
                frameColorFunction,
                frameColorMode
        );
        canvas = new FlameGraphCanvas<>(flameGraphPainter);
        ToolTipManager.sharedInstance().registerComponent(canvas);

        component = JScrollPaneWithButton.create(
                canvas,
                scrollPane -> {
                    scrollPane.getVerticalScrollBar().setUnitIncrement(16);
                    scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
                    new ScrollPaneMouseListener<>(canvas, flameGraphPainter, extractToolTip).install(scrollPane);
                    new MouseInputListenerWorkaroundForToolTipEnabledComponent(scrollPane).install(canvas);
                    canvas.linkListenerTo(scrollPane);
                }
        );
    }


    static class ScrollPaneMouseListener<T> implements MouseInputListener {
        private Point pressedPoint;
        private FlameGraphCanvas<T> canvas;
        private final FlameGraphPainter<T> flameGraph;
        private final Function<FrameBox<T>, String> extractToolTip;

        public ScrollPaneMouseListener(FlameGraphCanvas<T> canvas, FlameGraphPainter<T> flameGraph, Function<FrameBox<T>, String> extractToolTip) {
            this.canvas = canvas;
            this.flameGraph = flameGraph;
            this.extractToolTip = extractToolTip;
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
            var scrollPane = (JScrollPane) e.getComponent();
            var viewPort = scrollPane.getViewport();
            var point = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), viewPort.getView());
            if (canvas.isInsideMinimap(point)) {
                // bail out
                return;
            }

            if (e.getClickCount() == 2) {
                flameGraph.zoomToFrameAt(
                        (Graphics2D) viewPort.getView().getGraphics(),
                        point
                ).ifPresent(zoomPoint -> {
                    scrollPane.revalidate();
                    EventQueue.invokeLater(() -> viewPort.setViewPosition(zoomPoint));
                });

                return;
            }

            if ((e.getSource() instanceof JScrollPane)) {
                flameGraph.toggleSelectedFrameAt(
                        (Graphics2D) viewPort.getView().getGraphics(),
                        point
                );

                scrollPane.repaint();
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {}

        @Override
        public void mouseExited(MouseEvent e) {
            if ((e.getSource() instanceof JScrollPane)) {
                var scrollPane = (JScrollPane) e.getComponent();
                flameGraph.stopHover();
                scrollPane.repaint();
            }

        }

        @Override
        public void mouseMoved(MouseEvent e) {
            var scrollPane = (JScrollPane) e.getComponent();
            var viewPort = scrollPane.getViewport();
            var view = (JComponent) viewPort.getView();
            var point = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), view);

            if (canvas.isInsideMinimap(point)) {
                // bail out
                return;
            }

            flameGraph.hoverFrameAt(
                    (Graphics2D) view.getGraphics(),
                    point,
                    frame -> view.setToolTipText(extractToolTip.apply(frame))
            );

            scrollPane.repaint();
        }

        public void install(JScrollPane sp) {
            sp.addMouseListener(this);
            sp.addMouseMotionListener(this);
        }

    }

    private static class FlameGraphCanvas<T> extends JPanel {
        private Image minimap;
        private BalloonToolTip toolTip;
        private final FlameGraphPainter<T> flameGraphPainter;
        private Dimension flameGraphDimension;
        private int minimapWidth = 200;
        private int minimapHeight = 100;
        private int minimapInset = 10;
        private int minimapRadius = 10;
        private Point minimapLocation = new Point(50, 50);

        public FlameGraphCanvas(FlameGraphPainter<T> flameGraphPainter) {
            this.flameGraphPainter = flameGraphPainter;
        }


        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            d = (d == null) ? new Dimension(400, 400) : d;

            Insets insets = getInsets();
            var flameGraphDimension = flameGraphPainter.computeFlameGraphDimension((Graphics2D) getGraphics(),
                                                                                   getVisibleRect(),
                                                                                   insets
            );
            d.width = Math.max(d.width, flameGraphDimension.width + insets.left + insets.right);
            d.height = Math.max(d.height, flameGraphDimension.height + insets.top + insets.bottom);

            // When the preferred size is discovered, also set the actual
            // canvas size, as it is needed during `viewPort.setViewPosition`
            // if it is not then the viewport will not be able to scroll to the
            // right frame when the flamegraph is zoomed (ie its dimensions
            // change)
            setSize(d);

            // trigger minimap generation
            if (!flameGraphDimension.equals(this.flameGraphDimension)) {
                triggerMinimapGeneration();
            }
            this.flameGraphDimension = flameGraphDimension;
            return d;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            var visibleRect = getVisibleRect();
            flameGraphPainter.paint((Graphics2D) g, visibleRect);

            paintMinimap(g, visibleRect);
        }

        private void paintMinimap(Graphics g, Rectangle visibleRect) {
            if (minimap != null) {
                var g2 = (Graphics2D) g.create(visibleRect.x + minimapLocation.x,
                                               visibleRect.y + visibleRect.height - minimapHeight - minimapLocation.y,
                                               minimapWidth + minimapInset * 2,
                                               minimapHeight + minimapInset * 2);

                g2.setColor(getBackground());
                g2.fillRoundRect(1, 1, minimapWidth + 2 * minimapInset - 1, minimapHeight + 2 * minimapInset - 1, minimapRadius, minimapRadius);
                g2.drawImage(minimap, minimapInset, minimapInset, null);

                // the image is already rendered, so the hints are only for the shapes below
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setColor(getForeground());
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(1, 1, minimapWidth + 2 * minimapInset - 2, minimapHeight + 2 * minimapInset - 2, minimapRadius, minimapRadius);

                {
                    // Zoom zone
                    double zoomZoneScaleX = (double) minimapWidth / flameGraphDimension.width;
                    double zoomZoneScaleY = (double) minimapHeight / flameGraphDimension.height;

                    int x = (int) (visibleRect.x * zoomZoneScaleX);
                    int y = (int) (visibleRect.y * zoomZoneScaleY);
                    int w = (int) (visibleRect.width * zoomZoneScaleX);
                    int h = (int) (visibleRect.height * zoomZoneScaleY);

                    var zoomZone = new Area(new Rectangle(minimapInset, minimapInset, minimapWidth, minimapHeight));
                    zoomZone.subtract(new Area(new Rectangle(x + minimapInset, y + minimapInset, w, h)));

                    g2.setColor(Colors.translucent_white_80);
                    g2.fill(zoomZone);

                    g2.setColor(getForeground());
                    g2.setStroke(new BasicStroke(1));
                    g2.drawRect(x + minimapInset, y + minimapInset, w, h);
                }
            }
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            if (isInsideMinimap(e.getPoint())) {
                return "";
            }

            return super.getToolTipText(e);
        }

        public boolean isInsideMinimap(Point point) {
            var visibleRect = getVisibleRect();
            var rectangle = new Rectangle(visibleRect.x + minimapLocation.y,
                                          visibleRect.y + visibleRect.height - minimapHeight - minimapLocation.y,
                                          minimapWidth + 2 * minimapInset,
                                          minimapHeight + 2 * minimapInset
            );

            return rectangle.contains(point);
        }

        @Override
        public JToolTip createToolTip() {
            if (toolTip == null) {
                toolTip = new BalloonToolTip();
                toolTip.setComponent(this);
            }

            return toolTip;
        }


        private void triggerMinimapGeneration() {
            CompletableFuture.runAsync(() -> {
                var height = flameGraphPainter.computeFlameGraphThumbnailHeight(minimapWidth);

                GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
                GraphicsConfiguration c = e.getDefaultScreenDevice().getDefaultConfiguration();
                BufferedImage minimapImage = c.createCompatibleImage(minimapWidth, height, Transparency.TRANSLUCENT);
                Graphics2D minimapGraphics = minimapImage.createGraphics();
                minimapGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                minimapGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                minimapGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);


                flameGraphPainter.paintMinimap(minimapGraphics, new Rectangle(minimapWidth, height));
                minimapGraphics.dispose();

                SwingUtilities.invokeLater(() -> this.setImage(minimapImage));
            }).handle((__, t) -> {
                t.printStackTrace(); // no thumbnail
                return null;
            });
        }

        public void setImage(BufferedImage i) {
            this.minimap = i.getScaledInstance(minimapWidth, minimapHeight, Image.SCALE_SMOOTH);
            repaint();
        }

        public void linkListenerTo(JScrollPane scrollPane) {
            var mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    processMinimapMouseEvent(e);
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    processMinimapMouseEvent(e);
                }

                private void processMinimapMouseEvent(MouseEvent e) {
                    var pt = e.getPoint();
                    if (!(e.getComponent() instanceof FlameGraph.FlameGraphCanvas)) {
                        return;
                    }

                    var visibleRect = ((FlameGraphCanvas<?>) e.getComponent()).getVisibleRect();

                    double zoomZoneScaleX = (double) minimapWidth / flameGraphDimension.width;
                    double zoomZoneScaleY = (double) minimapHeight / flameGraphDimension.height;

                    var h = (pt.x - (visibleRect.x + minimapLocation.x)) / zoomZoneScaleX;
                    var horizontalBarModel = scrollPane.getHorizontalScrollBar().getModel();
                    horizontalBarModel.setValue((int) h - horizontalBarModel.getExtent());


                    var v = (pt.y - (visibleRect.y + visibleRect.height - minimapHeight - minimapLocation.y)) / zoomZoneScaleY;
                    var verticalBarModel = scrollPane.getVerticalScrollBar().getModel();
                    verticalBarModel.setValue((int) v - verticalBarModel.getExtent());
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    setCursor(isInsideMinimap(e.getPoint()) ?
                              Cursor.getPredefinedCursor(System.getProperty("os.name").startsWith("Mac") ? Cursor.HAND_CURSOR : Cursor.MOVE_CURSOR) :
                              Cursor.getDefaultCursor());
                }
            };
            this.addMouseListener(mouseAdapter);
            this.addMouseMotionListener(mouseAdapter);
        }
    }
}
