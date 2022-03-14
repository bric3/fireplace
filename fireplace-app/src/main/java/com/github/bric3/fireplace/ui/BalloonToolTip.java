/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.github.bric3.fireplace.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

public class BalloonToolTip extends JToolTip {
    private transient HierarchyListener listener;

    @Override
    public void updateUI() {
        removeHierarchyListener(listener);
        super.updateUI();
        listener = e -> {
            Component c = e.getComponent();
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && c.isShowing()) {

                // This makes all parent container non-opaque
                // it seems that some LaF change that by default, so it's overridden for this tooltip.
                Component container = c.getParent();
                while (container != null) {
                    if (!(container instanceof JFrame) || ((JFrame) container).isUndecorated()) {
                        container.setBackground(new Color(0x0, true));
                    }

                    if (container instanceof JComponent) {
                        ((JComponent) container).setOpaque(false);
                    }
                    container = container.getParent();
                }

                var window = SwingUtilities.windowForComponent(this);
                window.setBackground(new Color(0x0, true));
            }
        };
        addHierarchyListener(listener);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 5, 1, 5));
    }


    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return d;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Shape s = makeBalloonShape();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fill(s);
        g2.setColor(getForeground());
        g2.draw(s);
        g2.dispose();
        super.paintComponent(g);
    }

    private Shape makeBalloonShape() {
        Insets insets = getInsets();
        float w = getWidth() - 1f;
        float h = getHeight() - 1f;
        float triangleHeight = insets.top * .8f;
        Path2D triangle = new Path2D.Float();
        triangle.moveTo(insets.left + triangleHeight * 2, 0f);
        triangle.lineTo(insets.left + triangleHeight, triangleHeight);
        triangle.lineTo(insets.left + triangleHeight * 3, triangleHeight);
        Area area = new Area(new RoundRectangle2D.Float(0f, triangleHeight, w, h - insets.bottom - triangleHeight, insets.top, insets.top));
        area.add(new Area(triangle));
        return area;
    }
}
