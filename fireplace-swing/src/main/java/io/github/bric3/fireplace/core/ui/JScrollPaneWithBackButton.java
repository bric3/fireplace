/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.core.ui;

import javax.swing.*;
import javax.swing.plaf.LayerUI;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.function.Supplier;

/**
 * Builds a JLayer over a JScrollPane that displays a button that go back
 * on top of the scroll pane.
 */
public abstract class JScrollPaneWithBackButton {
    public static final String BACK_TO_DIRECTION = "backToDirection";

    private JScrollPaneWithBackButton() {
        // no need to instantiate this class
    }

    /**
     * Creates a JScrollPane with a button that go back on top of the scroll pane.
     *
     * @param scrollPaneSupplier The supplier of the scroll pane.
     * @return a JLayer over the scroll pane.
     */
    public static JLayer<JScrollPane> create(Supplier<JScrollPane> scrollPaneSupplier) {
        var layer = new JLayer<>(
                scrollPaneSupplier.get(),
                new ScrollBackToTopLayerUI(15, 15)
        );

        layer.addPropertyChangeListener(BACK_TO_DIRECTION, evt -> {
            var direction = (int) evt.getNewValue();
            ((ScrollBackToTopLayerUI) layer.getUI()).setDirection(direction);
            layer.repaint();
        });
        return layer;
    }

    private static class ScrollBackToTopLayerUI extends LayerUI<JScrollPane> {
        public int xGap;
        public int yGap;
        private final JPanel buttonContainer = new JPanel();
        private final Point currentMousePoint = new Point();
        private final UpArrowIcon buttonIcon = new UpArrowIcon(
                new Color(0xAA3D4244, true),
                new Color(0xAA389FD6, true)
        );
        private final JButton button = new JButton(buttonIcon) {

            private final Color ARMED_BUTTON_COLOR = new DarkLightColor(
                    Color.darkGray,
                    Color.lightGray
            );

            @Override
            public void updateUI() {
                super.updateUI();
                setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
                setFocusPainted(false);

                setBorderPainted(true);
                setContentAreaFilled(false);
                setRolloverEnabled(false); // handled by the icon
                setPreferredSize(new Dimension(30, 30));
            }

            @Override
            protected void paintComponent(Graphics g) {
                var g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isArmed()) {
                    g2.setColor(ARMED_BUTTON_COLOR);
                } else {
                    g2.setColor(UIManager.getColor("Button.background"));
                }

                g2.fillOval(1, 1, getWidth() - 1 - 3, getHeight() - 1 - 3);
                super.paintComponent(g2);
            }

            protected void paintBorder(Graphics g) {
                g.setColor(UIManager.getColor("Button.borderColor"));
                g.drawOval(1, 1, getWidth() - 1 - 3, getHeight() - 1 - 3);
            }
        };
        private final Rectangle buttonRect = new Rectangle(button.getPreferredSize());
        private Cursor componentCursor;
        private int direction = SwingConstants.NORTH;

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

                switch (direction) {
                    case SwingConstants.NORTH:
                        if (scrollPane.getViewport().getViewRect().y > 0) {
                            button.getModel().setRollover(buttonRect.contains(currentMousePoint));
                            SwingUtilities.paintComponent(g, button, buttonContainer, buttonRect);
                        }
                        break;
                    case SwingConstants.SOUTH:
                        if (scrollPane.getViewport().getViewRect().y < scrollPane.getViewport().getViewSize().getHeight() - scrollPane.getViewport().getViewRect().height) {
                            button.getModel().setRollover(buttonRect.contains(currentMousePoint));
                            SwingUtilities.paintComponent(g, button, buttonContainer, buttonRect);
                        }
                        break;
                }
            }
        }

        @Override
        public void updateUI(JLayer<? extends JScrollPane> l) {
            super.updateUI(l);
            button.updateUI();
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
            var viewRect = scroll.getViewport().getViewRect();
            var p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), scroll);
            currentMousePoint.setLocation(p);
            int id = e.getID();
            if (id == MouseEvent.MOUSE_CLICKED) {
                if (buttonRect.contains(currentMousePoint)) {
                    scrollBackToTop(layer.getView());
                }
            } else if (id == MouseEvent.MOUSE_PRESSED && viewRect.y > 0 && buttonRect.contains(currentMousePoint)) {
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
                e.getComponent().setCursor(Cursor.getDefaultCursor());
            } else {
                if (componentCursor != null) {
                    e.getComponent().setCursor(componentCursor);
                    componentCursor = null;
                }
            }
        }

        private void scrollBackToTop(JScrollPane scrollPane) {
            var viewport = scrollPane.getViewport();
            var view = (JComponent) viewport.getView();
            var currentViewRect = viewport.getViewRect();

            new Timer(20, e -> {
                Timer animator = (Timer) e.getSource();
                if (direction == SwingConstants.NORTH
                    && 0 < currentViewRect.y
                    && animator.isRunning()
                ) {
                    currentViewRect.y -= Math.max(1, currentViewRect.y / 2);
                    view.scrollRectToVisible(currentViewRect);
                } else if (direction == SwingConstants.SOUTH
                           && (view.getHeight() - currentViewRect.height) > currentViewRect.y
                           && animator.isRunning()
                ) {
                    currentViewRect.y += Math.max(1, currentViewRect.y / 2);
                    view.scrollRectToVisible(currentViewRect);
                } else {
                    animator.stop();
                }
            }).start();
        }

        public void setDirection(int direction) {
            this.direction = direction;
            buttonIcon.setDirection(direction);
        }
    }

    private static class UpArrowIcon implements Icon {
        private final Color arrowColor;
        private final Color rolloverColor;
        private final int size;
        private int direction = SwingConstants.NORTH;
        ;

        public UpArrowIcon(Color arrowColor, Color rolloverColor) {
            this.arrowColor = arrowColor;
            this.rolloverColor = rolloverColor;
            size = 24;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            var g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x - 1, y);

            if (c instanceof AbstractButton && ((AbstractButton) c).getModel().isRollover()) {
                g2.setPaint(rolloverColor);
            } else {
                g2.setPaint(UIManager.getColor("Button.foreground"));
            }

            float w2 = getIconWidth() / 2f;
            float h2 = getIconHeight() / 2f;
            float tw = w2 / 3f;
            float th = h2 / 6f;

            g2.setStroke(new BasicStroke(w2 / 4f));

            Path2D p = new Path2D.Float();
            switch (direction) {
                case SwingConstants.NORTH:
                    p.moveTo(w2 - tw, h2 + th);
                    p.lineTo(w2, h2 - th);
                    p.lineTo(w2 + tw, h2 + th);
                    break;
                case SwingConstants.SOUTH:
                    p.moveTo(w2 - tw, h2 - th);
                    p.lineTo(w2, h2 + th);
                    p.lineTo(w2 + tw, h2 - th);
                    break;
            }

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

        public void setDirection(int direction) {
            this.direction = direction;
        }
    }
}
