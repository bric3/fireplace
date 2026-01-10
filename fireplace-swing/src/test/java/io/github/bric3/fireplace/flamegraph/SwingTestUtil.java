/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.flamegraph;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;

import static org.mockito.Mockito.*;

/**
 * Test utilities for Swing component testing.
 */
final class SwingTestUtil {

    private SwingTestUtil() {
        // utility class
    }

    /**
     * Recursively searches for a {@link JScrollPane} within the given container hierarchy.
     *
     * @param container the container to search within
     * @return the first JScrollPane found, or null if none exists
     */
    static JScrollPane findScrollPane(Container container) {
        for (var component : container.getComponents()) {
            if (component instanceof JScrollPane) {
                return (JScrollPane) component;
            }
            if (component instanceof Container) {
                var found = findScrollPane((Container) component);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Gets the canvas from a FlamegraphView component hierarchy.
     *
     * @param flamegraphComponent the FlamegraphView component
     * @return the canvas component
     * @throws AssertionError if the canvas cannot be found
     */
    static JComponent getCanvas(Container flamegraphComponent) {
        var scrollPane = findScrollPane(flamegraphComponent);
        if (scrollPane == null) {
            throw new AssertionError("ScrollPane not found in component hierarchy");
        }
        var viewport = scrollPane.getViewport();
        if (viewport == null || viewport.getView() == null) {
            throw new AssertionError("Canvas not found in viewport");
        }
        return (JComponent) viewport.getView();
    }

    /**
     * Sets up flamegraph dimensions for testing via reflection.
     *
     * @param canvas the canvas component
     * @param width the desired width
     * @param height the desired height
     * @throws Exception if reflection fails
     */
    static void setupFlamegraphDimensions(JComponent canvas, int width, int height) throws Exception {
        Field flamegraphDimensionField = canvas.getClass().getDeclaredField("flamegraphDimension");
        flamegraphDimensionField.setAccessible(true);
        Dimension flamegraphDimension = (Dimension) flamegraphDimensionField.get(canvas);
        flamegraphDimension.width = width;
        flamegraphDimension.height = height;
    }

    /**
     * Gets the minimap bounds from the canvas via reflection.
     *
     * @param canvas the canvas component
     * @return the minimap bounds rectangle
     * @throws Exception if reflection fails
     */
    static Rectangle getMinimapBounds(JComponent canvas) throws Exception {
        Field minimapBoundsField = canvas.getClass().getDeclaredField("minimapBounds");
        minimapBoundsField.setAccessible(true);
        return (Rectangle) minimapBoundsField.get(canvas);
    }

    /**
     * Makes the FlamegraphView displayable for testing by adding it to a hidden frame.
     * This ensures components have proper visible rectangles calculated.
     *
     * @param flamegraphView the FlamegraphView to make displayable
     */
    static void makeDisplayable(Container flamegraphView) {
        var frame = new JFrame();
        frame.add(flamegraphView);
        frame.setSize(1000, 800);
        frame.pack();
        // Don't show the frame, but make it displayable
        frame.addNotify();
    }

    /**
     * Creates a spy of the canvas and stubs isInsideMinimap to return true.
     * This allows tests to reach processMinimapMouseEvent even in headless environments.
     *
     * @param flamegraphComponent the FlamegraphView component
     * @return the spy canvas with isInsideMinimap stubbed
     */
    static JComponent spyCanvasWithMinimapEnabled(Container flamegraphComponent) {
        var scrollPane = findScrollPane(flamegraphComponent);
        if (scrollPane == null) {
            throw new AssertionError("ScrollPane not found in component hierarchy");
        }
        var viewport = scrollPane.getViewport();
        var realCanvas = viewport.getView();

        // Create spy with custom answer that stubs isInsideMinimap
        var canvasSpy = mock(realCanvas.getClass(), withSettings()
                .spiedInstance(realCanvas)
                .defaultAnswer(invocation -> {
                    if ("isInsideMinimap".equals(invocation.getMethod().getName())) {
                        return true;
                    }
                    return invocation.callRealMethod();
                }));

        // Replace canvas in viewport with spy
        viewport.setView(canvasSpy);

        // Update the canvas reference in mouse adapters to point to the spy
        if (realCanvas instanceof JComponent) {
            var realJComponent = (JComponent) realCanvas;
            for (var listener : realJComponent.getMouseListeners()) {
                // Check if this is a FlamegraphCanvasMouseInputAdapter
                if (listener.getClass().getSimpleName().contains("MouseInputAdapter")) {
                    try {
                        // Update the canvas field in the adapter to point to the spy
                        Field canvasField = listener.getClass().getDeclaredField("canvas");
                        canvasField.setAccessible(true);
                        canvasField.set(listener, canvasSpy);
                    } catch (Exception e) {
                        // Ignore if field doesn't exist or can't be set
                    }
                }
            }
        }

        return (JComponent) canvasSpy;
    }

    /**
     * Captures the current scrollbar positions from a FlamegraphView component.
     *
     * @param flamegraphComponent the FlamegraphView component
     * @return the current scrollbar positions
     */
    static ScrollbarPositions getScrollbarPositions(Container flamegraphComponent) {
        var scrollPane = findScrollPane(flamegraphComponent);
        if (scrollPane == null) {
            throw new AssertionError("ScrollPane not found in component hierarchy");
        }
        return new ScrollbarPositions(
                scrollPane.getHorizontalScrollBar().getValue(),
                scrollPane.getVerticalScrollBar().getValue()
        );
    }

    /**
     * Creates a MouseEvent with the specified parameters.
     *
     * @param source the component that is the source of the event
     * @param eventType the event type (e.g., MouseEvent.MOUSE_CLICKED, MouseEvent.MOUSE_PRESSED)
     * @param x the x coordinate of the mouse position
     * @param y the y coordinate of the mouse position
     * @param button the mouse button (e.g., MouseEvent.BUTTON1, MouseEvent.BUTTON3)
     * @param clickCount the number of clicks (e.g., 1 for single click, 2 for double click)
     * @param isPopupTrigger whether this event is a popup trigger
     * @return a new MouseEvent with the specified parameters
     */
    static MouseEvent createMouseEvent(Component source, int eventType, int x, int y, int button, int clickCount, boolean isPopupTrigger) {
        int modifiers;
        if (button == MouseEvent.BUTTON1) {
            modifiers = MouseEvent.BUTTON1_DOWN_MASK;
        } else if (button == MouseEvent.BUTTON2) {
            modifiers = MouseEvent.BUTTON2_DOWN_MASK;
        } else if (button == MouseEvent.BUTTON3) {
            modifiers = MouseEvent.BUTTON3_DOWN_MASK;
        } else {
            modifiers = 0;
        }
        return new MouseEvent(source, eventType, System.currentTimeMillis(), modifiers, x, y, clickCount, isPopupTrigger, button);
    }

    /**
     * Creates a MOUSE_CLICKED event.
     *
     * @param source the component that is the source of the event
     * @param x the x coordinate of the mouse position
     * @param y the y coordinate of the mouse position
     * @param button the mouse button (e.g., MouseEvent.BUTTON1, MouseEvent.BUTTON3)
     * @param clickCount the number of clicks (e.g., 1 for single click, 2 for double click)
     * @return a new MOUSE_CLICKED event
     */
    static MouseEvent createClickEvent(Component source, int x, int y, int button, int clickCount) {
        return createMouseEvent(source, MouseEvent.MOUSE_CLICKED, x, y, button, clickCount, false);
    }

    /**
     * Creates a MOUSE_PRESSED event.
     *
     * @param source the component that is the source of the event
     * @param x the x coordinate of the mouse position
     * @param y the y coordinate of the mouse position
     * @param button the mouse button (e.g., MouseEvent.BUTTON1, MouseEvent.BUTTON3)
     * @param isPopupTrigger whether this event is a popup trigger
     * @return a new MOUSE_PRESSED event
     */
    static MouseEvent createPressEvent(Component source, int x, int y, int button, boolean isPopupTrigger) {
        return createMouseEvent(source, MouseEvent.MOUSE_PRESSED, x, y, button, 1, isPopupTrigger);
    }

    /**
     * Creates a MOUSE_RELEASED event with BUTTON1.
     *
     * @param source the component that is the source of the event
     * @param x the x coordinate of the mouse position
     * @param y the y coordinate of the mouse position
     * @param isPopupTrigger whether this event is a popup trigger
     * @return a new MOUSE_RELEASED event
     */
    static MouseEvent createReleaseEvent(Component source, int x, int y, boolean isPopupTrigger) {
        return createMouseEvent(source, MouseEvent.MOUSE_RELEASED, x, y, MouseEvent.BUTTON1, 1, isPopupTrigger);
    }

    /**
     * Creates a MOUSE_DRAGGED event with BUTTON1.
     *
     * @param source the component that is the source of the event
     * @param x the x coordinate of the mouse position
     * @param y the y coordinate of the mouse position
     * @return a new MOUSE_DRAGGED event
     */
    static MouseEvent createDraggedEvent(Component source, int x, int y) {
        return createMouseEvent(source, MouseEvent.MOUSE_DRAGGED, x, y, MouseEvent.BUTTON1, 1, false);
    }

    /**
     * Creates a MOUSE_ENTERED event.
     *
     * @param source the component that is the source of the event
     * @param x the x coordinate of the mouse position
     * @param y the y coordinate of the mouse position
     * @return a new MOUSE_ENTERED event
     */
    static MouseEvent createEnteredEvent(Component source, int x, int y) {
        return new MouseEvent(source, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, x, y, 0, false, MouseEvent.NOBUTTON);
    }

    /**
     * Creates a MOUSE_EXITED event.
     *
     * @param source the component that is the source of the event
     * @param x the x coordinate of the mouse position
     * @param y the y coordinate of the mouse position
     * @return a new MOUSE_EXITED event
     */
    static MouseEvent createExitedEvent(Component source, int x, int y) {
        return new MouseEvent(source, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, x, y, 0, false, MouseEvent.NOBUTTON);
    }

    /**
     * Creates a MOUSE_MOVED event.
     *
     * @param source the component that is the source of the event
     * @param x the x coordinate of the mouse position
     * @param y the y coordinate of the mouse position
     * @return a new MOUSE_MOVED event
     */
    static MouseEvent createMovedEvent(Component source, int x, int y) {
        return new MouseEvent(source, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, x, y, 0, false, MouseEvent.NOBUTTON);
    }

    /**
     * Class to hold scrollbar positions for comparison in tests.
     */
    static final class ScrollbarPositions {
        private final int horizontal;
        private final int vertical;

        ScrollbarPositions(int horizontal, int vertical) {
            this.horizontal = horizontal;
            this.vertical = vertical;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ScrollbarPositions)) return false;
            ScrollbarPositions other = (ScrollbarPositions) obj;
            return horizontal == other.horizontal && vertical == other.vertical;
        }

        @Override
        public int hashCode() {
            return 31 * horizontal + vertical;
        }

        @Override
        public String toString() {
            return "ScrollbarPositions{horizontal=" + horizontal + ", vertical=" + vertical + '}';
        }
    }
}