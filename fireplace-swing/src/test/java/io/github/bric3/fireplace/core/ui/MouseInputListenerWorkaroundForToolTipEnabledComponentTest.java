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

import io.github.bric3.fireplace.core.ui.fixtures.SwingEdtExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MouseInputListenerWorkaroundForToolTipEnabledComponent}.
 * These tests verify that mouse events are properly propagated from a source
 * component to a destination component.
 */
@DisplayName("MouseInputListenerWorkaroundForToolTipEnabledComponent")
@ExtendWith(SwingEdtExtension.class)
class MouseInputListenerWorkaroundForToolTipEnabledComponentTest {

    private JPanel sourceComponent;
    private JPanel destinationComponent;
    private MouseInputListenerWorkaroundForToolTipEnabledComponent workaround;
    private List<MouseEvent> receivedEvents;

    @BeforeEach
    void setUp() {
        sourceComponent = new JPanel();
        sourceComponent.setBounds(50, 70, 200, 200);

        destinationComponent = new JPanel();
        destinationComponent.setBounds(10, 20, 400, 400);
        var container = new JPanel(null);
        container.add(sourceComponent);
        container.add(destinationComponent);

        receivedEvents = new ArrayList<>();
        workaround = new MouseInputListenerWorkaroundForToolTipEnabledComponent(destinationComponent);
    }

    @Nested
    @DisplayName("Installation")
    class InstallationTests {

        @Test
        void install_adds_mouse_listener_to_source() {
            int initialMouseListeners = sourceComponent.getMouseListeners().length;

            workaround.install(sourceComponent);

            assertThat(sourceComponent.getMouseListeners()).hasSize(initialMouseListeners + 1);
            assertThat(sourceComponent.getMouseListeners()).contains(workaround);
        }

        @Test
        void install_adds_mouse_motion_listener_to_source() {
            int initialMotionListeners = sourceComponent.getMouseMotionListeners().length;

            workaround.install(sourceComponent);

            assertThat(sourceComponent.getMouseMotionListeners()).hasSize(initialMotionListeners + 1);
            assertThat(sourceComponent.getMouseMotionListeners()).contains(workaround);
        }
    }

    @Nested
    @DisplayName("Mouse Click Events")
    class MouseClickEventTests {

        @BeforeEach
        void setUp() {
            destinationComponent.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    receivedEvents.add(e);
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    receivedEvents.add(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    receivedEvents.add(e);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    receivedEvents.add(e);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    receivedEvents.add(e);
                }
            });
            workaround.install(sourceComponent);
        }

        @Test
        void mouseClicked_dispatches_to_destination() {
            var event = mouseEvent(MouseEvent.MOUSE_CLICKED, 50, 50);

            sourceComponent.dispatchEvent(event);

            assertThat(receivedEvents).hasSize(1);
            assertThat(receivedEvents.get(0).getID()).isEqualTo(MouseEvent.MOUSE_CLICKED);
            assertThat(receivedEvents.get(0).getSource()).isEqualTo(destinationComponent);
        }

        @Test
        void mousePressed_dispatches_to_destination() {
            var event = mouseEvent(MouseEvent.MOUSE_PRESSED, 50, 50);

            sourceComponent.dispatchEvent(event);

            assertThat(receivedEvents).hasSize(1);
            assertThat(receivedEvents.get(0).getID()).isEqualTo(MouseEvent.MOUSE_PRESSED);
            assertThat(receivedEvents.get(0).getSource()).isEqualTo(destinationComponent);
        }

        @Test
        void mouseReleased_dispatches_to_destination() {
            var event = mouseEvent(MouseEvent.MOUSE_RELEASED, 50, 50);

            sourceComponent.dispatchEvent(event);

            assertThat(receivedEvents).hasSize(1);
            assertThat(receivedEvents.get(0).getID()).isEqualTo(MouseEvent.MOUSE_RELEASED);
            assertThat(receivedEvents.get(0).getSource()).isEqualTo(destinationComponent);
        }

        @Test
        void mouseEntered_dispatches_to_destination() {
            var event = mouseEvent(MouseEvent.MOUSE_ENTERED, 50, 50);

            sourceComponent.dispatchEvent(event);

            assertThat(receivedEvents).hasSize(1);
            assertThat(receivedEvents.get(0).getID()).isEqualTo(MouseEvent.MOUSE_ENTERED);
            assertThat(receivedEvents.get(0).getSource()).isEqualTo(destinationComponent);
        }

        @Test
        void mouseExited_dispatches_to_destination() {
            var event = mouseEvent(MouseEvent.MOUSE_EXITED, -10, -10);

            sourceComponent.dispatchEvent(event);

            assertThat(receivedEvents).hasSize(1);
            assertThat(receivedEvents.get(0).getID()).isEqualTo(MouseEvent.MOUSE_EXITED);
            assertThat(receivedEvents.get(0).getSource()).isEqualTo(destinationComponent);
        }
    }

    @Nested
    @DisplayName("Mouse Motion Events")
    class MouseMotionEventTests {

        @BeforeEach
        void setUp() {
            destinationComponent.addMouseMotionListener(new MouseMotionListener() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    receivedEvents.add(e);
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    receivedEvents.add(e);
                }
            });
            workaround.install(sourceComponent);
        }

        @Test
        void mouseMoved_dispatches_to_destination() {
            var event = mouseEvent(MouseEvent.MOUSE_MOVED, 75, 100);

            sourceComponent.dispatchEvent(event);

            assertThat(receivedEvents).hasSize(1);
            assertThat(receivedEvents.get(0).getID()).isEqualTo(MouseEvent.MOUSE_MOVED);
            assertThat(receivedEvents.get(0).getSource()).isEqualTo(destinationComponent);
        }

        @Test
        void mouseDragged_dispatches_to_destination() {
            var event = mouseEvent(MouseEvent.MOUSE_DRAGGED, 75, 100);

            sourceComponent.dispatchEvent(event);

            assertThat(receivedEvents).hasSize(1);
            assertThat(receivedEvents.get(0).getID()).isEqualTo(MouseEvent.MOUSE_DRAGGED);
            assertThat(receivedEvents.get(0).getSource()).isEqualTo(destinationComponent);
        }
    }

    @Nested
    @DisplayName("Mouse Wheel Events")
    class MouseWheelEventTests {

        private List<MouseWheelEvent> receivedWheelEvents;

        @BeforeEach
        void setUp() {
            receivedWheelEvents = new ArrayList<>();
            destinationComponent.addMouseWheelListener(new MouseWheelListener() {
                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    receivedWheelEvents.add(e);
                }
            });
            workaround.install(sourceComponent);
        }

        @Test
        void explicit_wheel_callback_forwards_coordinates_and_scroll_metadata() {
            var event = new MouseWheelEvent(sourceComponent, MouseEvent.MOUSE_WHEEL,
                    123456, MouseEvent.CTRL_DOWN_MASK, 50, 50, 300, 400, 0, false,
                    MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, -1, -1.25);

            workaround.mouseWheelMoved(event);

            assertThat(receivedWheelEvents).hasSize(1);
            var forwarded = receivedWheelEvents.get(0);
            assertThat(forwarded.getID()).isEqualTo(MouseEvent.MOUSE_WHEEL);
            assertThat(forwarded.getSource()).isSameAs(destinationComponent);
            assertThat(forwarded.getPoint()).isEqualTo(new Point(90, 100));
            assertThat(forwarded.getLocationOnScreen()).isEqualTo(new Point(300, 400));
            assertThat(forwarded.getWhen()).isEqualTo(123456);
            assertThat(forwarded.getModifiersEx()).isEqualTo(MouseEvent.CTRL_DOWN_MASK);
            assertThat(forwarded.getScrollType()).isEqualTo(MouseWheelEvent.WHEEL_UNIT_SCROLL);
            assertThat(forwarded.getScrollAmount()).isEqualTo(3);
            assertThat(forwarded.getWheelRotation()).isEqualTo(-1);
            assertThat(forwarded.getPreciseWheelRotation()).isEqualTo(-1.25);
        }
    }

    @Nested
    @DisplayName("Coordinate Conversion")
    class CoordinateConversionTests {

        @BeforeEach
        void setUp() {
            // Set up a container to properly test coordinate conversion
            JPanel container = new JPanel();
            container.setLayout(null);
            container.setBounds(0, 0, 500, 500);

            sourceComponent.setBounds(50, 50, 200, 200);
            destinationComponent.setBounds(10, 10, 400, 400);

            container.add(sourceComponent);
            container.add(destinationComponent);

            destinationComponent.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    receivedEvents.add(e);
                }

                @Override
                public void mousePressed(MouseEvent e) {}

                @Override
                public void mouseReleased(MouseEvent e) {}

                @Override
                public void mouseEntered(MouseEvent e) {}

                @Override
                public void mouseExited(MouseEvent e) {}
            });
            workaround.install(sourceComponent);
        }

        @Test
        void coordinates_are_converted_and_mouse_metadata_is_preserved() {
            var event = new MouseEvent(sourceComponent, MouseEvent.MOUSE_CLICKED,
                    123456, MouseEvent.SHIFT_DOWN_MASK | MouseEvent.BUTTON1_DOWN_MASK,
                    100, 100, 300, 400, 2, true, MouseEvent.BUTTON1);

            sourceComponent.dispatchEvent(event);

            assertThat(receivedEvents).hasSize(1);
            var dispatchedEvent = receivedEvents.get(0);

            // Verify the event was dispatched
            assertThat(dispatchedEvent.getSource()).isEqualTo(destinationComponent);

            // Source (50, 50) to destination (10, 10) adds exactly (40, 40).
            assertThat(dispatchedEvent.getPoint()).isEqualTo(new Point(140, 140));
            assertThat(dispatchedEvent.getLocationOnScreen()).isEqualTo(new Point(300, 400));
            assertThat(dispatchedEvent.getWhen()).isEqualTo(event.getWhen());
            assertThat(dispatchedEvent.getModifiersEx()).isEqualTo(event.getModifiersEx());
            assertThat(dispatchedEvent.getClickCount()).isEqualTo(2);
            assertThat(dispatchedEvent.getButton()).isEqualTo(MouseEvent.BUTTON1);
            assertThat(dispatchedEvent.isPopupTrigger()).isTrue();
            assertThat(event.getSource()).isSameAs(sourceComponent);
            assertThat(event.getPoint()).isEqualTo(new Point(100, 100));
        }
    }

    @Nested
    @DisplayName("Multiple Event Sequence")
    class MultipleEventSequenceTests {

        private List<Integer> eventSequence;

        @BeforeEach
        void setUp() {
            eventSequence = new ArrayList<>();

            destinationComponent.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {}

                @Override
                public void mousePressed(MouseEvent e) {
                    eventSequence.add(MouseEvent.MOUSE_PRESSED);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    eventSequence.add(MouseEvent.MOUSE_RELEASED);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    eventSequence.add(MouseEvent.MOUSE_ENTERED);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    eventSequence.add(MouseEvent.MOUSE_EXITED);
                }
            });

            destinationComponent.addMouseMotionListener(new MouseMotionListener() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    eventSequence.add(MouseEvent.MOUSE_DRAGGED);
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    eventSequence.add(MouseEvent.MOUSE_MOVED);
                }
            });

            workaround.install(sourceComponent);
        }

        @Test
        void typical_interaction_sequence_is_preserved() {
            // Simulate: enter -> move -> press -> drag -> release -> exit

            sourceComponent.dispatchEvent(mouseEvent(MouseEvent.MOUSE_ENTERED, 50, 50));
            sourceComponent.dispatchEvent(mouseEvent(MouseEvent.MOUSE_MOVED, 60, 60));
            sourceComponent.dispatchEvent(mouseEvent(MouseEvent.MOUSE_PRESSED, 60, 60));
            sourceComponent.dispatchEvent(mouseEvent(MouseEvent.MOUSE_DRAGGED, 70, 70));
            sourceComponent.dispatchEvent(mouseEvent(MouseEvent.MOUSE_DRAGGED, 80, 80));
            sourceComponent.dispatchEvent(mouseEvent(MouseEvent.MOUSE_RELEASED, 80, 80));
            sourceComponent.dispatchEvent(mouseEvent(MouseEvent.MOUSE_EXITED, -10, -10));

            assertThat(eventSequence).containsExactly(
                    MouseEvent.MOUSE_ENTERED,
                    MouseEvent.MOUSE_MOVED,
                    MouseEvent.MOUSE_PRESSED,
                    MouseEvent.MOUSE_DRAGGED,
                    MouseEvent.MOUSE_DRAGGED,
                    MouseEvent.MOUSE_RELEASED,
                    MouseEvent.MOUSE_EXITED
            );
        }
    }

    private MouseEvent mouseEvent(int id, int x, int y) {
        boolean leftButton = id == MouseEvent.MOUSE_CLICKED || id == MouseEvent.MOUSE_PRESSED
                             || id == MouseEvent.MOUSE_RELEASED || id == MouseEvent.MOUSE_DRAGGED;
        return new MouseEvent(sourceComponent, id, System.currentTimeMillis(),
                leftButton ? MouseEvent.BUTTON1_DOWN_MASK : 0, x, y, leftButton ? 1 : 0,
                false, leftButton ? MouseEvent.BUTTON1 : MouseEvent.NOBUTTON);
    }
}
