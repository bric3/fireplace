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

import javax.swing.*;
import javax.swing.plaf.LayerUI;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;

public abstract class JScrollPaneWithButton {

    public static JLayer<JScrollPane> create(JComponent content) {
        return new JLayer<JScrollPane>(new JScrollPane(content), new ScrollBackToTopLayerUI(15, 15));
    }

    private static class ScrollBackToTopLayerUI extends LayerUI<JScrollPane> {
        private int xGap;
        private int yGap;
        private final Container buttonContainer = new JPanel();
        private final Point currentMousePoint = new Point();
        private final JButton button = new JButton(new UpArrowIcon(new Color(0xAA_3D_42_44, true),
                                                                   new Color(0xAA_38_9F_D6, true))) {
            @Override
            public void updateUI() {
                super.updateUI();
                setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
                setFocusPainted(false);

                setBorderPainted(true);
                setContentAreaFilled(false);
                setRolloverEnabled(false); // handled by the icon
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isArmed()) {
                    g2.setColor(Color.darkGray);
                } else {
                    g2.setColor(getBackground());
                }

                g2.fillOval(0, 0, getSize().width - 1, getSize().height - 1);
                super.paintComponent(g2);
            }

            protected void paintBorder(Graphics g) {
                g.setColor(Color.darkGray);
                g.drawOval(0, 0, getSize().width - 1, getSize().height - 1);
            }
        };
        private final Rectangle buttonRect = new Rectangle(button.getPreferredSize());
        private Cursor componentCursor;

        public ScrollBackToTopLayerUI() {
            this(10, 10);
        }

        public ScrollBackToTopLayerUI(int xGap, int yGap) {
            this.xGap = xGap;
            this.yGap = yGap;
        }

        private void updateButtonRect(JScrollPane scrollPane) {
            var viewport = scrollPane.getViewport();
            int x = viewport.getX() + viewport.getWidth() - buttonRect.width - xGap;
            int y = viewport.getY() + viewport.getHeight() - buttonRect.height - yGap;
            buttonRect.setLocation(x, y);
        }

        @Override
        public void paint(Graphics g, JComponent layer) {
            super.paint(g, layer);
            if (layer instanceof JLayer) {
                var scrollPane = (JScrollPane) ((JLayer<?>) layer).getView();
                updateButtonRect(scrollPane);
                if (scrollPane.getViewport().getViewRect().y > 0) {
                    button.getModel().setRollover(buttonRect.contains(currentMousePoint));
                    SwingUtilities.paintComponent(g, button, buttonContainer, buttonRect);
                }
            }
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            if (c instanceof JLayer) {
                ((JLayer<?>) c).setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
            }
        }

        @Override
        public void uninstallUI(JComponent c) {
            if (c instanceof JLayer) {
                ((JLayer<?>) c).setLayerEventMask(0);
            }
            super.uninstallUI(c);
        }

        @Override
        protected void processMouseEvent(MouseEvent e, JLayer<? extends JScrollPane> layer) {
            var scroll = layer.getView();
            var r = scroll.getViewport().getViewRect();
            var p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), scroll);
            currentMousePoint.setLocation(p);
            int id = e.getID();
            if (id == MouseEvent.MOUSE_CLICKED) {
                if (buttonRect.contains(currentMousePoint)) {
                    scrollBackToTop(layer.getView());
                }
            } else if (id == MouseEvent.MOUSE_PRESSED && r.y > 0 && buttonRect.contains(currentMousePoint)) {
                e.consume();
            }
        }

        @Override
        protected void processMouseMotionEvent(MouseEvent e, JLayer<? extends JScrollPane> layer) {
            var p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), layer.getView());
            currentMousePoint.setLocation(p);
            layer.repaint(buttonRect);

            if (buttonRect.contains(currentMousePoint)) {
                if (componentCursor == null) {
                    componentCursor = e.getComponent().getCursor();
                }
                e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            } else {
                if (componentCursor != null) {
                    e.getComponent().setCursor(componentCursor);
                    componentCursor = null;
                }
            }
        }

        private void scrollBackToTop(JScrollPane scrollPane) {
            var viewport = scrollPane.getViewport();
            var scrollViewClient = (JComponent) viewport.getView();
            var current = viewport.getViewRect();

            new Timer(20, e -> {
                Timer animator = (Timer) e.getSource();
                if (0 < current.y && animator.isRunning()) {
                    current.y -= Math.max(1, current.y / 2);
                    //                viewport.scrollRectToVisible(current);
                    scrollViewClient.scrollRectToVisible(current);
                } else {
                    animator.stop();
                }
            }).start();
        }

    }

    private static class UpArrowIcon implements Icon {
        private Color arrowColor;
        private Color rolloverColor;
        private int size;

        public UpArrowIcon(Color arrowColor, Color rolloverColor) {
            this.arrowColor = arrowColor;
            this.rolloverColor = rolloverColor;
            size = 24;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            var g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);

            if (c instanceof AbstractButton && ((AbstractButton) c).getModel().isRollover()) {
                g2.setPaint(rolloverColor);
            } else {
                g2.setPaint(arrowColor);
            }

            float w2 = getIconWidth() / 2f;
            float h2 = getIconHeight() / 2f;
            float tw = w2 / 3f;
            float th = h2 / 6f;

            g2.setStroke(new BasicStroke(w2 / 4f));

            Path2D p = new Path2D.Float();
            p.moveTo(w2 - tw, h2 + th);
            p.lineTo(w2, h2 - th);
            p.lineTo(w2 + tw, h2 + th);

            g2.draw(p);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
