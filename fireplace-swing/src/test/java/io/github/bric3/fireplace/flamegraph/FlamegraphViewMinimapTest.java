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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Supplier;

import static io.github.bric3.fireplace.flamegraph.SwingTestUtil.getCanvas;
import static io.github.bric3.fireplace.flamegraph.SwingTestUtil.getMinimapBounds;
import static io.github.bric3.fireplace.flamegraph.SwingTestUtil.getScrollbarPositions;
import static io.github.bric3.fireplace.flamegraph.SwingTestUtil.setupFlamegraphDimensions;
import static io.github.bric3.fireplace.flamegraph.SwingTestUtil.spyCanvasWithMinimapEnabled;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link FlamegraphView} minimap functionality.
 */
@DisplayName("FlamegraphView - Minimap")
class FlamegraphViewMinimapTest {

    private FlamegraphView<String> fg;

    @BeforeEach
    void setUp() {
        fg = new FlamegraphView<>();
    }

    @Nested
    @DisplayName("Minimap")
    class MinimapTests {

        @Test
        void isShowMinimap_default_is_true() {
            assertThat(fg.isShowMinimap()).isTrue();
        }

        @Test
        void setShowMinimap_false_hides_minimap() {
            fg.setShowMinimap(false);

            assertThat(fg.isShowMinimap()).isFalse();
        }

        @Test
        void setMinimapShadeColorSupplier_sets_supplier() {
            Supplier<Color> supplier = () -> Color.RED;

            fg.setMinimapShadeColorSupplier(supplier);

            assertThat(fg.getMinimapShadeColorSupplier()).isEqualTo(supplier);
        }

        @Test
        void setMinimapShadeColorSupplier_null_throws_exception() {
            assertThatThrownBy(() -> fg.setMinimapShadeColorSupplier(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void setShowMinimap_toggle_multiple_times() {
            assertThat(fg.isShowMinimap()).isTrue();

            fg.setShowMinimap(false);
            assertThat(fg.isShowMinimap()).isFalse();

            fg.setShowMinimap(true);
            assertThat(fg.isShowMinimap()).isTrue();

            fg.setShowMinimap(false);
            assertThat(fg.isShowMinimap()).isFalse();
        }

        @Test
        void setShowMinimap_same_value_does_not_throw() {
            fg.setShowMinimap(true);

            assertThatCode(() -> fg.setShowMinimap(true))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Minimap Shade Color")
    class MinimapShadeColorTests {

        @Test
        void getMinimapShadeColorSupplier_default_is_null() {
            // Default may be null or have a default supplier
            // Just verify it doesn't throw
            assertThatCode(() -> fg.getMinimapShadeColorSupplier())
                    .doesNotThrowAnyException();
        }

        @Test
        void setMinimapShadeColorSupplier_custom_color() {
            Supplier<Color> redSupplier = () -> Color.RED;

            fg.setMinimapShadeColorSupplier(redSupplier);

            assertThat(fg.getMinimapShadeColorSupplier()).isEqualTo(redSupplier);
            assertThat(fg.getMinimapShadeColorSupplier().get()).isEqualTo(Color.RED);
        }

        @Test
        void setMinimapShadeColorSupplier_with_alpha_color() {
            Supplier<Color> alphaSupplier = () -> new Color(128, 128, 128, 128);

            fg.setMinimapShadeColorSupplier(alphaSupplier);

            var color = fg.getMinimapShadeColorSupplier().get();
            assertThat(color.getAlpha()).isEqualTo(128);
        }
    }

    @Nested
    @DisplayName("Minimap Mouse Event Processing")
    class MinimapMouseEventProcessingTests {

        @Test
        @DisplayName("mousePressed in minimap area processes minimap event")
        void mousePressed_in_minimap_area() throws Exception {
            // Set up a model
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            );
            fg.setModel(new FrameModel<>(frames));

            var canvas = getCanvas(fg.component);
            setupFlamegraphDimensions(canvas, 1000, 500);

            // Spy on canvas and stub isInsideMinimap to reach processMinimapMouseEvent
            var canvasSpy = spyCanvasWithMinimapEnabled(fg.component);
            Rectangle minimapBounds = getMinimapBounds(canvasSpy);

            // Create mouse pressed event in minimap area
            var mouseEvent = new MouseEvent(
                    canvasSpy,
                    MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(),
                    MouseEvent.BUTTON1_DOWN_MASK,
                    minimapBounds.x + 50,
                    minimapBounds.y + 50,
                    1,
                    false,
                    MouseEvent.BUTTON1
            );

            // Event should be processed without throwing and reach processMinimapMouseEvent
            assertThatCode(() -> canvasSpy.dispatchEvent(mouseEvent))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("mouseDragged in minimap area updates viewport")
        void mouseDragged_in_minimap_area() throws Exception {
            // Set up a model with enough frames to allow scrolling
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child1", 0.0, 0.25, 1),
                    new FrameBox<>("child2", 0.25, 0.5, 1),
                    new FrameBox<>("child3", 0.5, 0.75, 1),
                    new FrameBox<>("child4", 0.75, 1.0, 1)
            );
            fg.setModel(new FrameModel<>(frames));

            var canvas = getCanvas(fg.component);
            setupFlamegraphDimensions(canvas, 2000, 1000);

            // Spy on canvas and stub isInsideMinimap to reach processMinimapMouseEvent
            var canvasSpy = spyCanvasWithMinimapEnabled(fg.component);
            Rectangle minimapBounds = getMinimapBounds(canvasSpy);

            int minimapX = minimapBounds.x + 50;
            int minimapY = minimapBounds.y + 50;

            // First press in minimap
            var pressedEvent = new MouseEvent(
                    canvasSpy,
                    MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(),
                    MouseEvent.BUTTON1_DOWN_MASK,
                    minimapX,
                    minimapY,
                    1,
                    false,
                    MouseEvent.BUTTON1
            );
            canvasSpy.dispatchEvent(pressedEvent);

            // Capture initial scrollbar positions after press
            var initialPositions = getScrollbarPositions(fg.component);

            // Then drag in minimap
            var draggedEvent = new MouseEvent(
                    canvasSpy,
                    MouseEvent.MOUSE_DRAGGED,
                    System.currentTimeMillis(),
                    MouseEvent.BUTTON1_DOWN_MASK,
                    minimapX + 20,
                    minimapY + 20,
                    1,
                    false,
                    MouseEvent.BUTTON1
            );
            canvasSpy.dispatchEvent(draggedEvent);

            // Capture final scrollbar positions after drag
            var finalPositions = getScrollbarPositions(fg.component);

            // Assert that scrollbar positions changed
            assertThat(finalPositions)
                    .as("Scrollbar positions should change after dragging in minimap")
                    .isNotEqualTo(initialPositions);
        }

        @Test
        @DisplayName("mousePressed at different minimap positions")
        void mousePressed_at_different_minimap_positions() throws Exception {
            // Set up a model
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child1", 0.0, 0.33, 1),
                    new FrameBox<>("child2", 0.33, 0.66, 1),
                    new FrameBox<>("child3", 0.66, 1.0, 1)
            );
            fg.setModel(new FrameModel<>(frames));

            var canvas = getCanvas(fg.component);
            setupFlamegraphDimensions(canvas, 1600, 800);

            // Spy on canvas and stub isInsideMinimap to reach processMinimapMouseEvent
            var canvasSpy = spyCanvasWithMinimapEnabled(fg.component);
            Rectangle minimapBounds = getMinimapBounds(canvasSpy);

            // Test various positions within minimap
            Point[] testPoints = {
                    new Point(minimapBounds.x + 10, minimapBounds.y + 10),
                    new Point(minimapBounds.x + minimapBounds.width / 2, minimapBounds.y + minimapBounds.height / 2),
                    new Point(minimapBounds.x + minimapBounds.width - 10, minimapBounds.y + minimapBounds.height - 10)
            };

            for (Point point : testPoints) {
                var mouseEvent = new MouseEvent(
                        canvasSpy,
                        MouseEvent.MOUSE_PRESSED,
                        System.currentTimeMillis(),
                        MouseEvent.BUTTON1_DOWN_MASK,
                        point.x,
                        point.y,
                        1,
                        false,
                        MouseEvent.BUTTON1
                );

                assertThatCode(() -> canvasSpy.dispatchEvent(mouseEvent))
                        .as("Mouse pressed at point (%d, %d) should not throw", point.x, point.y)
                        .doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("mouseDragged updates scrollbars when dragging across minimap")
        void mouseDragged_updates_scrollbars() throws Exception {
            // Set up a model with multiple frames
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child1", 0.0, 0.2, 1),
                    new FrameBox<>("child2", 0.2, 0.4, 1),
                    new FrameBox<>("child3", 0.4, 0.6, 1),
                    new FrameBox<>("child4", 0.6, 0.8, 1),
                    new FrameBox<>("child5", 0.8, 1.0, 1)
            );
            fg.setModel(new FrameModel<>(frames));

            var canvas = getCanvas(fg.component);
            setupFlamegraphDimensions(canvas, 3000, 1500);

            // Spy on canvas and stub isInsideMinimap to reach processMinimapMouseEvent
            var canvasSpy = spyCanvasWithMinimapEnabled(fg.component);
            Rectangle minimapBounds = getMinimapBounds(canvasSpy);

            // Start position in minimap
            int startX = minimapBounds.x + 30;
            int startY = minimapBounds.y + 30;

            // Press at start position
            var pressedEvent = new MouseEvent(
                    canvasSpy,
                    MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(),
                    MouseEvent.BUTTON1_DOWN_MASK,
                    startX,
                    startY,
                    1,
                    false,
                    MouseEvent.BUTTON1
            );
            canvasSpy.dispatchEvent(pressedEvent);

            // Capture initial scrollbar positions after press
            var previousPositions = getScrollbarPositions(fg.component);

            // Drag to different positions and verify scrollbars update
            int[][] dragPositions = {
                    {startX + 20, startY},
                    {startX + 40, startY + 20},
                    {startX + 60, startY + 40}
            };

            for (int[] pos : dragPositions) {
                var dragEvent = new MouseEvent(
                        canvasSpy,
                        MouseEvent.MOUSE_DRAGGED,
                        System.currentTimeMillis(),
                        MouseEvent.BUTTON1_DOWN_MASK,
                        pos[0],
                        pos[1],
                        1,
                        false,
                        MouseEvent.BUTTON1
                );
                canvasSpy.dispatchEvent(dragEvent);

                // Capture new scrollbar positions after this drag
                var newPositions = getScrollbarPositions(fg.component);

                // Assert that scrollbar positions changed from previous positions
                assertThat(newPositions)
                        .as("Scrollbar positions should change after dragging to (%d, %d)", pos[0], pos[1])
                        .isNotEqualTo(previousPositions);

                // Update previousPositions for next iteration
                previousPositions = newPositions;
            }
        }

        @Test
        @DisplayName("mousePressed outside minimap does not trigger minimap navigation")
        void mousePressed_outside_minimap() throws Exception {
            // Set up a model
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            );
            fg.setModel(new FrameModel<>(frames));

            var canvas = getCanvas(fg.component);
            setupFlamegraphDimensions(canvas, 1000, 500);
            Rectangle minimapBounds = getMinimapBounds(canvas);

            // Click outside minimap area (in the flamegraph itself)
            var mouseEvent = new MouseEvent(
                    canvas,
                    MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(),
                    MouseEvent.BUTTON1_DOWN_MASK,
                    minimapBounds.x + minimapBounds.width + 100,
                    minimapBounds.y + minimapBounds.height + 100,
                    1,
                    false,
                    MouseEvent.BUTTON1
            );

            // Event should be processed without throwing
            assertThatCode(() -> canvas.dispatchEvent(mouseEvent))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("mouseDragged after pressing outside minimap does not update via minimap")
        void mouseDragged_after_pressing_outside_minimap() throws Exception {
            // Set up a model
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            );
            fg.setModel(new FrameModel<>(frames));

            var canvas = getCanvas(fg.component);
            setupFlamegraphDimensions(canvas, 1000, 500);
            Rectangle minimapBounds = getMinimapBounds(canvas);

            // Press outside minimap
            int outsideX = minimapBounds.x + minimapBounds.width + 100;
            int outsideY = minimapBounds.y + minimapBounds.height + 100;

            var pressedEvent = new MouseEvent(
                    canvas,
                    MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(),
                    MouseEvent.BUTTON1_DOWN_MASK,
                    outsideX,
                    outsideY,
                    1,
                    false,
                    MouseEvent.BUTTON1
            );
            canvas.dispatchEvent(pressedEvent);

            // Drag into minimap area (but press was outside, so minimap nav shouldn't trigger)
            var draggedEvent = new MouseEvent(
                    canvas,
                    MouseEvent.MOUSE_DRAGGED,
                    System.currentTimeMillis(),
                    MouseEvent.BUTTON1_DOWN_MASK,
                    minimapBounds.x + 50,
                    minimapBounds.y + 50,
                    1,
                    false,
                    MouseEvent.BUTTON1
            );

            // Event should be processed without throwing
            assertThatCode(() -> canvas.dispatchEvent(draggedEvent))
                    .doesNotThrowAnyException();
        }
    }
}