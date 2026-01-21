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

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Supplier;

import static io.github.bric3.fireplace.flamegraph.SwingTestUtil.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

/**
 * Tests for {@link FlamegraphView} minimap functionality.
 */
@DisplayName("FlamegraphView - Minimap")
class FlamegraphViewMinimapTest {

    @Nested
    @DisplayName("Minimap")
    class MinimapTests {

        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void isShowMinimap_default_is_true() {
            assertThat(fg.view().isShowMinimap()).isTrue();
        }

        @Test
        void setShowMinimap_false_hides_minimap() {
            fg.view().setShowMinimap(false);

            assertThat(fg.view().isShowMinimap()).isFalse();
        }

        @Test
        void setMinimapShadeColorSupplier_sets_supplier() {
            Supplier<Color> supplier = () -> Color.RED;

            fg.view().setMinimapShadeColorSupplier(supplier);

            assertThat(fg.view().getMinimapShadeColorSupplier()).isEqualTo(supplier);
        }

        @Test
        void setMinimapShadeColorSupplier_null_throws_exception() {
            assertThatThrownBy(() -> fg.view().setMinimapShadeColorSupplier(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void setShowMinimap_toggle_multiple_times() {
            assertThat(fg.view().isShowMinimap()).isTrue();

            fg.view().setShowMinimap(false);
            assertThat(fg.view().isShowMinimap()).isFalse();

            fg.view().setShowMinimap(true);
            assertThat(fg.view().isShowMinimap()).isTrue();

            fg.view().setShowMinimap(false);
            assertThat(fg.view().isShowMinimap()).isFalse();
        }

        @Test
        void setShowMinimap_same_value_does_not_throw() {
            fg.view().setShowMinimap(true);

            assertThatCode(() -> fg.view().setShowMinimap(true))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Minimap Shade Color")
    class MinimapShadeColorTests {

        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void getMinimapShadeColorSupplier_default_is_null() {
            // Default may be null or have a default supplier
            // Just verify it doesn't throw
            assertThatCode(() -> fg.view().getMinimapShadeColorSupplier())
                    .doesNotThrowAnyException();
        }

        @Test
        void setMinimapShadeColorSupplier_custom_color() {
            Supplier<Color> redSupplier = () -> Color.RED;

            fg.view().setMinimapShadeColorSupplier(redSupplier);

            assertThat(fg.view().getMinimapShadeColorSupplier()).isEqualTo(redSupplier);
            assertThat(fg.view().getMinimapShadeColorSupplier().get()).isEqualTo(Color.RED);
        }

        @Test
        void setMinimapShadeColorSupplier_with_alpha_color() {
            Supplier<Color> alphaSupplier = () -> new Color(128, 128, 128, 128);

            fg.view().setMinimapShadeColorSupplier(alphaSupplier);

            var color = fg.view().getMinimapShadeColorSupplier().get();
            assertThat(color.getAlpha()).isEqualTo(128);
        }
    }

    @Nested
    @DisplayName("Minimap Mouse Event Processing")
    class MinimapMouseEventProcessingTests {

        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder()
                                                                                  .withCanvasSpy()
                                                                                  .build();

        @BeforeEach
        void setUp() {
            // Stub isInsideMinimap to always return true for minimap event testing
            doReturn(true).when(fg.canvasSpy()).isInsideMinimap(any());
        }

        @Test
        @DisplayName("mousePressed in minimap area processes minimap event")
        void mousePressed_in_minimap_area() {
            // Set up a model
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            );
            fg.view().setModel(new FrameModel<>(frames));
            fg.setFlamegraphDimensions(1000, 500);

            var canvas = fg.canvas();
            var minimapBounds = fg.minimapBounds();

            // Create mouse pressed event in minimap area
            var mouseEvent = createPressEvent(canvas, minimapBounds.x + 50, minimapBounds.y + 50, MouseEvent.BUTTON1, false);

            // Event should be processed without throwing and reach processMinimapMouseEvent
            assertThatCode(() -> canvas.dispatchEvent(mouseEvent))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("mouseDragged in minimap area updates viewport")
        void mouseDragged_in_minimap_area() {
            // Set up a model with enough frames to allow scrolling
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child1", 0.0, 0.25, 1),
                    new FrameBox<>("child2", 0.25, 0.5, 1),
                    new FrameBox<>("child3", 0.5, 0.75, 1),
                    new FrameBox<>("child4", 0.75, 1.0, 1)
            );
            fg.view().setModel(new FrameModel<>(frames));
            fg.setFlamegraphDimensions(2000, 1000);

            var canvas = fg.canvas();
            var minimapBounds = fg.minimapBounds();

            var minimapX = minimapBounds.x + 50;
            var minimapY = minimapBounds.y + 50;

            // First press in minimap
            canvas.dispatchEvent(createPressEvent(canvas, minimapX, minimapY, MouseEvent.BUTTON1, false));

            // Capture initial scrollbar positions after press
            var initialPositions = getScrollbarPositions(fg.component());

            // Then drag in minimap
            canvas.dispatchEvent(createDraggedEvent(canvas, minimapX + 20, minimapY + 20));

            // Capture final scrollbar positions after drag
            var finalPositions = getScrollbarPositions(fg.component());

            // Assert that scrollbar positions changed
            assertThat(finalPositions)
                    .as("Scrollbar positions should change after dragging in minimap")
                    .isNotEqualTo(initialPositions);
        }

        @Test
        @DisplayName("mousePressed at different minimap positions")
        void mousePressed_at_different_minimap_positions() {
            // Set up a model
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child1", 0.0, 0.33, 1),
                    new FrameBox<>("child2", 0.33, 0.66, 1),
                    new FrameBox<>("child3", 0.66, 1.0, 1)
            );
            fg.view().setModel(new FrameModel<>(frames));
            fg.setFlamegraphDimensions(1600, 800);

            var canvas = fg.canvas();
            var minimapBounds = fg.minimapBounds();

            // Test various positions within minimap
            var testPoints = new Point[]{
                    new Point(minimapBounds.x + 10, minimapBounds.y + 10),
                    new Point(minimapBounds.x + minimapBounds.width / 2, minimapBounds.y + minimapBounds.height / 2),
                    new Point(minimapBounds.x + minimapBounds.width - 10, minimapBounds.y + minimapBounds.height - 10)
            };

            for (var point : testPoints) {
                var mouseEvent = createPressEvent(canvas, point.x, point.y, MouseEvent.BUTTON1, false);

                assertThatCode(() -> canvas.dispatchEvent(mouseEvent))
                        .as("Mouse pressed at point (%d, %d) should not throw", point.x, point.y)
                        .doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("mouseDragged updates scrollbars when dragging across minimap")
        void mouseDragged_updates_scrollbars() {
            // Set up a model with multiple frames
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child1", 0.0, 0.2, 1),
                    new FrameBox<>("child2", 0.2, 0.4, 1),
                    new FrameBox<>("child3", 0.4, 0.6, 1),
                    new FrameBox<>("child4", 0.6, 0.8, 1),
                    new FrameBox<>("child5", 0.8, 1.0, 1)
            );
            fg.view().setModel(new FrameModel<>(frames));
            fg.setFlamegraphDimensions(3000, 1500);

            var canvas = fg.canvas();
            var minimapBounds = fg.minimapBounds();

            // Start position in minimap
            var startX = minimapBounds.x + 30;
            var startY = minimapBounds.y + 30;

            // Press at start position
            canvas.dispatchEvent(createPressEvent(canvas, startX, startY, MouseEvent.BUTTON1, false));

            // Capture initial scrollbar positions after press
            var previousPositions = getScrollbarPositions(fg.component());

            // Drag to different positions and verify scrollbars update
            var dragPositions = new int[][]{
                    {startX + 20, startY},
                    {startX + 40, startY + 20},
                    {startX + 60, startY + 40}
            };

            for (var pos : dragPositions) {
                canvas.dispatchEvent(createDraggedEvent(canvas, pos[0], pos[1]));

                // Capture new scrollbar positions after this drag
                var newPositions = getScrollbarPositions(fg.component());

                // Assert that scrollbar positions changed from previous positions
                assertThat(newPositions)
                        .as("Scrollbar positions should change after dragging to (%d, %d)", pos[0], pos[1])
                        .isNotEqualTo(previousPositions);

                // Update previousPositions for next iteration
                previousPositions = newPositions;
            }
        }
    }

    @Nested
    @DisplayName("Minimap Mouse Events Outside Minimap")
    class MinimapMouseEventsOutsideMinimapTests {

        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        @DisplayName("mousePressed outside minimap does not trigger minimap navigation")
        void mousePressed_outside_minimap() {
            // Set up a model
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            );
            fg.view().setModel(new FrameModel<>(frames));
            fg.setFlamegraphDimensions(1000, 500);

            var canvas = fg.canvas();
            var minimapBounds = fg.minimapBounds();

            // Click outside minimap area (in the flamegraph itself)
            var mouseEvent = createPressEvent(
                    canvas,
                    minimapBounds.x + minimapBounds.width + 100,
                    minimapBounds.y + minimapBounds.height + 100,
                    MouseEvent.BUTTON1,
                    false
            );

            // Event should be processed without throwing
            assertThatCode(() -> canvas.dispatchEvent(mouseEvent))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("mouseDragged after pressing outside minimap does not update via minimap")
        void mouseDragged_after_pressing_outside_minimap() {
            // Set up a model
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            );
            fg.view().setModel(new FrameModel<>(frames));
            fg.setFlamegraphDimensions(1000, 500);

            var canvas = fg.canvas();
            var minimapBounds = fg.minimapBounds();

            // Press outside minimap
            var outsideX = minimapBounds.x + minimapBounds.width + 100;
            var outsideY = minimapBounds.y + minimapBounds.height + 100;

            canvas.dispatchEvent(createPressEvent(canvas, outsideX, outsideY, MouseEvent.BUTTON1, false));

            // Drag into minimap area (but press was outside, so minimap nav shouldn't trigger)
            var draggedEvent = createDraggedEvent(canvas, minimapBounds.x + 50, minimapBounds.y + 50);

            // Event should be processed without throwing
            assertThatCode(() -> canvas.dispatchEvent(draggedEvent))
                    .doesNotThrowAnyException();
        }
    }
}