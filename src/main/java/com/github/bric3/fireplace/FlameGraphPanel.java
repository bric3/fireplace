/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.github.bric3.fireplace;

import com.github.bric3.fireplace.flamegraph.ColorMode;
import com.github.bric3.fireplace.flamegraph.FlameGraphPainter;
import com.github.bric3.fireplace.ui.Colors.Palette;
import com.github.bric3.fireplace.ui.JScrollPaneWithButton;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.function.Supplier;

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

        var colorPaletteJComboBox = new JComboBox<Palette>(Palette.values());
        colorPaletteJComboBox.addActionListener(e -> {
            flameGraph.colorPalette = (Palette) colorPaletteJComboBox.getSelectedItem();
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

}
