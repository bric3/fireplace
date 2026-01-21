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

import io.github.bric3.fireplace.flamegraph.SwingTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
class MouseInputListenerWorkaroundForToolTipEnabledComponentTest {

    private JPanel sourceComponent;
    private JPanel destinationComponent;
    private MouseInputListenerWorkaroundForToolTipEnabledComponent workaround;
    private List<MouseEvent> receivedEvents;

    @BeforeEach
    void setUp() {
        sourceComponent = new JPanel();
        sourceComponent.setBounds(0, 0, 200, 200);

        destinationComponent = new JPanel();
        destinationComponent.setBounds(0, 0, 400, 400);

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
            var event = SwingTestUtil.createClickEvent(sourceComponent, 50, 50, MouseEvent.BUTTON1, 1);

            workaround.mouseClicked(event);

            assertThat(receivedEvents).hasSize(1);
            assertThat(receivedEvents.get(0).getID()).isEqualTo(MouseEvent.MOUSE_CLICKED);
            assertThat(receivedEvents.get(0).getSource()).isEqualTo(destinationComponent);
        }

        @Test
        void mousePressed_dispatches_to_destination() {
            var event = SwingTestUtil.createPressEvent(sourceComponent, 50, 50, MouseEvent.BUTTON1, false);

            workaround.mousePressed(event);

            assertThat(receivedEvents).hasSize(1);
            assertThat(receivedEvents.get(0).getID()).isEqualTo(MouseEvent.MOUSE_PRESSED);
            assertThat(receivedEvents.get(0).getSource()).isEqualTo(destinationComponent);
        }

        @Test
        void mouseReleased_dispatches_to_destination() {
            var event = SwingTestUtil.createReleaseEvent(sourceComponent, 50, 50, false);

            workaround.mouseReleased(event);

            assertThat(receivedEvents).hasSize(1);
            assertThat(receivedEvents.get(0).getID()).isEqualTo(MouseEvent.MOUSE_RELEASED);
            assertThat(receivedEvents.get(0).getSource()).isEqualTo(destinationComponent);
        }

        @Test
        void mouseEntered_dispatches_to_destination() {
            var event = SwingTestUtil.createEnteredEvent(sourceComponent, 50, 50);

            workaround.mouseEntered(event);

            assertThat(receivedEvents).hasSize(1);
            assertThat(receivedEvents.get(0).getID()).isEqualTo(MouseEvent.MOUSE_ENTERED);
            assertThat(receivedEvents.get(0).getSource()).isEqualTo(destinationComponent);
        }

        @Test
        void mouseExited_dispatches_to_destination() {
            var event = SwingTestUtil.createExitedEvent(sourceComponent, -10, -10);

            workaround.mouseExited(event);

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
            var event = SwingTestUtil.createMovedEvent(sourceComponent, 75, 100);

            workaround.mouseMoved(event);

            assertThat(receivedEvents).hasSize(1);
            assertThat(receivedEvents.get(0).getID()).isEqualTo(MouseEvent.MOUSE_MOVED);
            assertThat(receivedEvents.get(0).getSource()).isEqualTo(destinationComponent);
        }

        @Test
        void mouseDragged_dispatches_to_destination() {
            var event = SwingTestUtil.createDraggedEvent(sourceComponent, 75, 100);

            workaround.mouseDragged(event);

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
        void mouseWheelMoved_dispatches_to_destination() {
            var event = SwingTestUtil.createWheelEvent(sourceComponent, 50, 50, -1);

            workaround.mouseWheelMoved(event);

            assertThat(receivedWheelEvents).hasSize(1);
            assertThat(receivedWheelEvents.get(0).getID()).isEqualTo(MouseEvent.MOUSE_WHEEL);
            assertThat(receivedWheelEvents.get(0).getSource()).isEqualTo(destinationComponent);
            assertThat(receivedWheelEvents.get(0).getWheelRotation()).isEqualTo(-1);
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
        void coordinates_are_converted_between_components() {
            // Click at position (100, 100) in source component
            var event = SwingTestUtil.createClickEvent(sourceComponent, 100, 100, MouseEvent.BUTTON1, 1);

            workaround.mouseClicked(event);

            assertThat(receivedEvents).hasSize(1);
            var dispatchedEvent = receivedEvents.get(0);

            // Verify the event was dispatched
            assertThat(dispatchedEvent.getSource()).isEqualTo(destinationComponent);

            // Coordinates should be converted relative to destination
            // This tests that SwingUtilities.convertMouseEvent is being called
            assertThat(dispatchedEvent.getX()).isNotEqualTo(event.getX())
                    .describedAs("X coordinate should be converted");
            assertThat(dispatchedEvent.getY()).isNotEqualTo(event.getY())
                    .describedAs("Y coordinate should be converted");
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

            workaround.mouseEntered(SwingTestUtil.createEnteredEvent(sourceComponent, 50, 50));
            workaround.mouseMoved(SwingTestUtil.createMovedEvent(sourceComponent, 60, 60));
            workaround.mousePressed(SwingTestUtil.createPressEvent(sourceComponent, 60, 60, MouseEvent.BUTTON1, false));
            workaround.mouseDragged(SwingTestUtil.createDraggedEvent(sourceComponent, 70, 70));
            workaround.mouseDragged(SwingTestUtil.createDraggedEvent(sourceComponent, 80, 80));
            workaround.mouseReleased(SwingTestUtil.createReleaseEvent(sourceComponent, 80, 80, false));
            workaround.mouseExited(SwingTestUtil.createExitedEvent(sourceComponent, -10, -10));

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
}