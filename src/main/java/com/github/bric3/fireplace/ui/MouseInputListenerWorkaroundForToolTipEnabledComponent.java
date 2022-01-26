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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * Workaround for container such as JScrollPane not receiving mouse events
 * when it is view has display tooltip.
 * <p>
 * This listener's job is to propagate the mouse input events to the
 * target container.
 */
public class MouseInputListenerWorkaroundForToolTipEnabledComponent extends MouseAdapter {
    private final JComponent destination;

    public MouseInputListenerWorkaroundForToolTipEnabledComponent(JComponent destination) {
        this.destination = destination;
    }

    public void install(JComponent jComponentWithToolTip) {
        jComponentWithToolTip.addMouseMotionListener(this);
        jComponentWithToolTip.addMouseListener(this);
    }

    private void dispatch(MouseEvent e) {
        destination.dispatchEvent(
                SwingUtilities.convertMouseEvent(e.getComponent(), e, destination)
        );
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        dispatch(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        dispatch(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dispatch(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        dispatch(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        dispatch(e);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        dispatch(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        dispatch(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        dispatch(e);
    }
}
