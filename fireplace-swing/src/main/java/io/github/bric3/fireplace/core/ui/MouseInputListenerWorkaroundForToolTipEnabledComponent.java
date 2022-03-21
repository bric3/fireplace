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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * Workaround for container such as JScrollPane not receiving mouse events
 * when it is view has display tooltip.
 * <p>
 * This listener's job is to propagate the mouse input events to the
 * target container.
 * </p>
 * <p>
 * This happens on {@link JScrollPane}, when the component is presented, eg on
 * the official javadoc:
 * <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/javax/swing/JScrollPane.html">
 * <img src="https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/javax/swing/doc-files/JScrollPane-1.gif" alt="JScrollPane javadoc image">
 * </a>
 * The image suggests that content of the scroll pane is behind. This is a
 * conceptual view, in practice the content is actually on the top of the scroll
 * pane (its coordinate and its visible size will just be adjusted to match
 * the scroll pane) and as it is over the scroll pane it will be the one that
 * gets the mouse events.
 * </p>
 * <p>
 * When a component does not have mouse listeners they are propagated to the
 * parent component that is behind. But when a component has mouse listeners
 * the events are <em>trapped</em> by the top component's listener.
 * <strong>That means that if the parent is also interested by mouse events
 * they need to be propagated, that is the goal of this class</strong>.
 * </p>
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
