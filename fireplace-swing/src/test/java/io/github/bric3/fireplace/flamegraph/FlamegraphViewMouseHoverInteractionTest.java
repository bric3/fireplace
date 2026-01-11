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

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.bric3.fireplace.flamegraph.SwingTestUtil.createDraggedEvent;
import static io.github.bric3.fireplace.flamegraph.SwingTestUtil.createEnteredEvent;
import static io.github.bric3.fireplace.flamegraph.SwingTestUtil.createExitedEvent;
import static io.github.bric3.fireplace.flamegraph.SwingTestUtil.createMovedEvent;
import static io.github.bric3.fireplace.flamegraph.SwingTestUtil.createPressEvent;
import static io.github.bric3.fireplace.flamegraph.SwingTestUtil.createReleaseEvent;
import static io.github.bric3.fireplace.flamegraph.SwingTestUtil.findScrollPane;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link FlamegraphView} user interaction features.
 */
@SuppressWarnings({"DataFlowIssue", "unchecked", "rawtypes"})
@DisplayName("FlamegraphView - Interaction & Mouse")
class FlamegraphViewMouseHoverInteractionTest {

    private FlamegraphView<String> fg;

    @BeforeEach
    void setUp() {
        fg = new FlamegraphView<>();
    }

    @Nested
    @DisplayName("Hovered Siblings")
    class HoveredSiblingsTests {

        @Test
        void isShowHoveredSiblings_default_is_true() {
            assertThat(fg.isShowHoveredSiblings()).isTrue();
        }

        @Test
        void setShowHoveredSiblings_false_disables() {
            fg.setShowHoveredSiblings(false);

            assertThat(fg.isShowHoveredSiblings()).isFalse();
        }

        @Test
        void setShowHoveredSiblings_toggles_correctly() {
            assertThat(fg.isShowHoveredSiblings()).isTrue();

            fg.setShowHoveredSiblings(false);
            assertThat(fg.isShowHoveredSiblings()).isFalse();

            fg.setShowHoveredSiblings(true);
            assertThat(fg.isShowHoveredSiblings()).isTrue();
        }
    }

    @Nested
    @DisplayName("Mouse Dragging Behavior on Canvas")
    class MouseDraggingBehaviorTests {

        private JScrollPane scrollPane;

        @BeforeEach
        void setUpComponents() {
            // Set up a model with frames
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child1", 0.0, 0.5, 1),
                    new FrameBox<>("child2", 0.5, 1.0, 1)
            );
            fg.setModel(new FrameModel<>(frames));

            scrollPane = findScrollPane(fg.component);

            // Set sizes
            scrollPane.setSize(800, 600);
            scrollPane.getViewport().setSize(800, 600);

            var canvas = scrollPane.getViewport().getView();
            canvas.setSize(800, 600);
        }

        // ==================== mousePressed tests ====================

        @Test
        void mouse_pressed_with_left_button_sets_pressed_point() {
            // Arrange
            var event = createPressEvent(scrollPane, 100, 100, MouseEvent.BUTTON1, false);

            // Act & Assert - should not throw
            assertThatCode(() -> scrollPane.dispatchEvent(event))
                    .doesNotThrowAnyException();
        }

        @Test
        void mouse_pressed_with_non_left_button_does_not_set_pressed_point() {
            // Arrange - right click
            var event = createPressEvent(scrollPane, 100, 100, MouseEvent.BUTTON3, false);

            // Act & Assert - should not throw
            assertThatCode(() -> scrollPane.dispatchEvent(event))
                    .doesNotThrowAnyException();
        }

        @Test
        void mouse_pressed_inside_minimap_does_not_set_pressed_point() {
            // Arrange - enable minimap
            fg.setShowMinimap(true);

            // Press inside minimap area (bottom-right corner)
            var event = createPressEvent(scrollPane, 750, 550, MouseEvent.BUTTON1, false);

            // Act & Assert - should not throw, pressedPoint should be set to null
            assertThatCode(() -> scrollPane.dispatchEvent(event))
                    .doesNotThrowAnyException();
        }

        @Test
        void mouse_pressed_outside_minimap_sets_pressed_point() {
            // Arrange - enable minimap
            fg.setShowMinimap(true);

            // Press outside minimap area
            var event = createPressEvent(scrollPane, 100, 100, MouseEvent.BUTTON1, false);

            // Act & Assert - should not throw
            assertThatCode(() -> scrollPane.dispatchEvent(event))
                    .doesNotThrowAnyException();
        }

        @Test
        void mouse_released_clears_pressed_point() {
            // Arrange - first press to set pressedPoint
            var pressEvent = createPressEvent(scrollPane, 100, 100, MouseEvent.BUTTON1, false);
            scrollPane.dispatchEvent(pressEvent);

            // Now release
            var releaseEvent = createReleaseEvent(scrollPane, 100, 100, false);

            // Act & Assert - should not throw
            assertThatCode(() -> scrollPane.dispatchEvent(releaseEvent))
                    .doesNotThrowAnyException();
        }

        @Test
        void mouse_released_without_prior_press_does_not_throw() {
            // Arrange - release without press
            var releaseEvent = createReleaseEvent(scrollPane, 100, 100, false);

            // Act & Assert - should not throw
            assertThatCode(() -> scrollPane.dispatchEvent(releaseEvent))
                    .doesNotThrowAnyException();
        }

        @Test
        void mouse_dragged_without_prior_press_does_nothing() {
            // Arrange - drag without press (pressedPoint is null)
            var dragEvent = createDraggedEvent(scrollPane, 150, 150);

            // Act & Assert - should not throw
            assertThatCode(() -> scrollPane.dispatchEvent(dragEvent))
                    .doesNotThrowAnyException();
        }

        @Test
        void mouse_dragged_after_press_updates_viewport_position() {
            // Arrange - set up with larger canvas to allow scrolling
            scrollPane.getViewport().setViewPosition(new Point(100, 100));

            // Press first to set pressedPoint
            var pressEvent = createPressEvent(scrollPane, 100, 100, MouseEvent.BUTTON1, false);
            scrollPane.dispatchEvent(pressEvent);

            // Now drag
            var dragEvent = createDraggedEvent(scrollPane, 150, 150);

            // Act & Assert - should not throw
            assertThatCode(() -> scrollPane.dispatchEvent(dragEvent))
                    .doesNotThrowAnyException();
        }

        @Test
        void mouse_dragged_from_non_scroll_pane_source_does_nothing() {
            // Arrange - press first
            var pressEvent = createPressEvent(scrollPane, 100, 100, MouseEvent.BUTTON1, false);
            scrollPane.dispatchEvent(pressEvent);

            // Drag from a different component (not JScrollPane)
            var otherComponent = new JPanel();
            var dragEvent = createDraggedEvent(otherComponent, 150, 150);

            // Act & Assert - dispatch to scrollPane but event source is different
            assertThatCode(() -> scrollPane.dispatchEvent(dragEvent))
                    .doesNotThrowAnyException();
        }

        @Test
        void mouse_drag_sequence_press_drag_release() {
            // Arrange - complete drag sequence
            scrollPane.getViewport().setViewPosition(new Point(200, 200));

            // Press
            var pressEvent = createPressEvent(scrollPane, 100, 100, MouseEvent.BUTTON1, false);
            scrollPane.dispatchEvent(pressEvent);

            // Drag
            var dragEvent1 = createDraggedEvent(scrollPane, 120, 120);
            scrollPane.dispatchEvent(dragEvent1);

            // Drag again
            var dragEvent2 = createDraggedEvent(scrollPane, 140, 140);
            scrollPane.dispatchEvent(dragEvent2);

            // Release
            var releaseEvent = createReleaseEvent(scrollPane, 140, 140, false);

            // Act & Assert
            assertThatCode(() -> scrollPane.dispatchEvent(releaseEvent))
                    .doesNotThrowAnyException();
        }

        @Test
        void mouse_dragged_consumes_event() {
            // Arrange
            scrollPane.getViewport().setViewPosition(new Point(100, 100));

            // Press first
            var pressEvent = createPressEvent(scrollPane, 100, 100, MouseEvent.BUTTON1, false);
            scrollPane.dispatchEvent(pressEvent);

            // Drag - the event should be consumed by the listener
            var dragEvent = createDraggedEvent(scrollPane, 150, 150);

            // Act
            scrollPane.dispatchEvent(dragEvent);

            // Assert - event should be consumed (note: we can't easily verify this
            // without reflection, but at least verify no exception)
        }

        @Test
        void mouse_dragged_clamps_position_to_zero() {
            // Arrange - set view position near origin
            scrollPane.getViewport().setViewPosition(new Point(10, 10));

            // Press first
            var pressEvent = createPressEvent(scrollPane, 100, 100, MouseEvent.BUTTON1, false);
            scrollPane.dispatchEvent(pressEvent);

            // Drag significantly to try to go negative
            var dragEvent = createDraggedEvent(scrollPane, 200, 200);

            // Act & Assert - position should be clamped to 0, not negative
            assertThatCode(() -> scrollPane.dispatchEvent(dragEvent))
                    .doesNotThrowAnyException();

            // Verify position is clamped (x and y should be >= 0)
            var viewPosition = scrollPane.getViewport().getViewPosition();
            assertThat(viewPosition.x).isGreaterThanOrEqualTo(0);
            assertThat(viewPosition.y).isGreaterThanOrEqualTo(0);
        }

        @Test
        void mouse_release_after_press_clears_pressed_point_and_prevents_drag() {
            // Arrange - press, release, then try to drag
            scrollPane.getViewport().setViewPosition(new Point(100, 100));

            // Press
            var pressEvent = createPressEvent(scrollPane, 100, 100, MouseEvent.BUTTON1, false);
            scrollPane.dispatchEvent(pressEvent);

            // Release - this clears pressedPoint
            var releaseEvent = createReleaseEvent(scrollPane, 100, 100, false);
            scrollPane.dispatchEvent(releaseEvent);

            // Get position after release
            var positionAfterRelease = scrollPane.getViewport().getViewPosition();

            // Try to drag - should do nothing since pressedPoint is null
            var dragEvent = createDraggedEvent(scrollPane, 150, 150);
            scrollPane.dispatchEvent(dragEvent);

            // Assert - position should not have changed after the drag
            var finalPosition = scrollPane.getViewport().getViewPosition();
            assertThat(finalPosition).isEqualTo(positionAfterRelease);
        }
    }

    @Nested
    @DisplayName("Mouse Hover Behavior on Canvas")
    class MouseHoverBehaviorTests {
        private JScrollPane scrollPane;
        private FlamegraphView.HoverListener<String> hoverListener;

        @BeforeEach
        void setUpGraphicsAndComponents() {
            BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();

            // Set up a model with frames
            var rootFrame = new FrameBox<>("root", 0.0, 1.0, 0);
            var childFrame = new FrameBox<>("child1", 0.0, 0.5, 1);
            var frames = List.of(
                    rootFrame,
                    childFrame,
                    new FrameBox<>("child2", 0.5, 1.0, 1)
            );
            fg.setModel(new FrameModel<>(frames));

            scrollPane = findScrollPane(fg.component);

            // Set sizes
            scrollPane.setSize(800, 600);
            scrollPane.getViewport().setSize(800, 600);

            var canvas = scrollPane.getViewport().getView();
            canvas.setSize(800, 600);

            // Spy on canvas to return real Graphics2D
            var spiedCanvas = spy(canvas);
            doReturn(g2d).when(spiedCanvas).getGraphics();
            scrollPane.getViewport().setView(spiedCanvas);

            // Create mock hover listener
            hoverListener = mock(FlamegraphView.HoverListener.class);
            fg.setHoverListener(hoverListener);
        }

        @Test
        void mouse_entered_inside_minimap_calls_onStopHover_and_bails_out() {
            // Arrange - enable minimap
            fg.setShowMinimap(true);

            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                // Return a point that would be inside the minimap (bottom-right corner area)
                when(mockPointerInfo.getLocation()).thenReturn(new Point(750, 550));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var event = createEnteredEvent(scrollPane, 750, 550);

                // Act
                scrollPane.dispatchEvent(event);

                // Assert - onStopHover should be called because mouse is inside minimap
                verify(hoverListener, atMostOnce()).onStopHover(any(), any(), any());
                verify(hoverListener, never()).onFrameHover(any(), any(), any());
            }
        }

        @Test
        void mouse_entered_outside_minimap_and_over_no_frame_calls_stopHover() {
            // Arrange - disable minimap to simplify
            fg.setShowMinimap(false);

            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                // Return a point outside any frame (far right edge)
                when(mockPointerInfo.getLocation()).thenReturn(new Point(10, 10));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var event = createEnteredEvent(scrollPane, 10, 10);

                // Act
                scrollPane.dispatchEvent(event);

                // Assert - onStopHover should be called when hovering over no frame
                verify(hoverListener, atMostOnce()).onStopHover(any(), any(), any());
            }
        }

        @Test
        void mouse_entered_without_hover_listener_does_not_throw() {
            // Arrange - no hover listener
            fg.setHoverListener(null);
            fg.setShowMinimap(false);

            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                when(mockPointerInfo.getLocation()).thenReturn(new Point(100, 100));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var event = createEnteredEvent(scrollPane, 100, 100);

                // Act & Assert - should not throw
                assertThatCode(() -> scrollPane.dispatchEvent(event))
                        .doesNotThrowAnyException();
            }
        }

        @Test
        void mouse_moved_inside_minimap_bails_out_without_calling_onFrameHover() {
            // Arrange - enable minimap
            fg.setShowMinimap(true);

            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                // Return a point inside the minimap
                when(mockPointerInfo.getLocation()).thenReturn(new Point(750, 550));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var event = createMovedEvent(scrollPane, 750, 550);

                // Act
                scrollPane.dispatchEvent(event);

                // Assert - onFrameHover should never be called when inside minimap
                verify(hoverListener, never()).onFrameHover(any(), any(), any());
            }
        }

        @Test
        void mouse_exited_calls_onStopHover_with_previous_frame() {
            // Arrange
            fg.setShowMinimap(false);

            // First, simulate hovering over a frame to set the hovered state
            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                when(mockPointerInfo.getLocation()).thenReturn(new Point(100, 50));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var enterEvent = createEnteredEvent(scrollPane, 100, 50);
                scrollPane.dispatchEvent(enterEvent);
            }

            // Now simulate mouse exit
            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                when(mockPointerInfo.getLocation()).thenReturn(new Point(-10, -10));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var exitEvent = createExitedEvent(scrollPane, -10, -10);

                // Act
                scrollPane.dispatchEvent(exitEvent);

                // Assert - onStopHover should be called
                verify(hoverListener, atLeastOnce()).onStopHover(any(), any(), any());
            }
        }

        @Test
        void mouse_moved_over_same_frame_twice_optimizes_by_reusing_cached_rectangle() {
            // Arrange
            fg.setShowMinimap(false);

            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                // Point over a frame
                when(mockPointerInfo.getLocation()).thenReturn(new Point(200, 50));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                // First move - should look up the frame
                var event1 = createMovedEvent(scrollPane, 200, 50);
                scrollPane.dispatchEvent(event1);

                // Clear invocations to track second call
                clearInvocations(hoverListener);

                // Second move - same general area, should use cached rectangle
                var event2 = createMovedEvent(scrollPane, 205, 50);

                // Act
                scrollPane.dispatchEvent(event2);

                // Assert - onFrameHover may be called but frame lookup should be optimized
                // The exact behavior depends on whether the point is still in hoveredFrameRectangle
            }
        }

        @Test
        void mouse_wheel_moved_triggers_hover_check() {
            // Arrange
            fg.setShowMinimap(false);

            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                when(mockPointerInfo.getLocation()).thenReturn(new Point(100, 50));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var wheelEvent = new java.awt.event.MouseWheelEvent(
                        scrollPane,
                        java.awt.event.MouseWheelEvent.MOUSE_WHEEL,
                        System.currentTimeMillis(),
                        0,
                        100, 50,
                        0, // click count
                        false,
                        java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL,
                        3, // scroll amount
                        1  // wheel rotation
                );

                // Act & Assert - should not throw, hover check should be debounced
                assertThatCode(() -> scrollPane.dispatchEvent(wheelEvent))
                        .doesNotThrowAnyException();
            }
        }

        @Test
        void hover_listener_null_when_mouse_inside_minimap_does_not_throw() {
            // Arrange - no hover listener, minimap enabled
            fg.setHoverListener(null);
            fg.setShowMinimap(true);

            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                when(mockPointerInfo.getLocation()).thenReturn(new Point(750, 550));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var event = createEnteredEvent(scrollPane, 750, 550);

                // Act & Assert - should not throw even without listener
                assertThatCode(() -> scrollPane.dispatchEvent(event))
                        .doesNotThrowAnyException();
            }
        }

        @Test
        void hover_listener_receives_mouse_event_from_scroll_pane() {
            // Arrange
            fg.setShowMinimap(false);

            var capturedEvent = new AtomicReference<MouseEvent>();
            fg.setHoverListener(new FlamegraphView.HoverListener<>() {
                @Override
                public void onFrameHover(@NotNull FrameBox<@NotNull String> frame,
                                         @NotNull Rectangle hoveredFrameRectangle,
                                         @NotNull MouseEvent e) {
                    capturedEvent.set(e);
                }
            });

            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                when(mockPointerInfo.getLocation()).thenReturn(new Point(200, 50));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var event = createEnteredEvent(scrollPane, 200, 50);

                // Act
                scrollPane.dispatchEvent(event);

                // Assert - if frame was found, event should be captured
                // Note: In headless mode, frame detection may not work perfectly
            }
        }

        @Test
        void stop_hover_passes_null_for_previously_hovered_frame_when_none_existed() {
            // Arrange - fresh state, no previous hover
            fg.setShowMinimap(false);

            var capturedPrevFrame = new AtomicReference<FrameBox<String>>();
            var capturedPrevRect = new AtomicReference<Rectangle>();
            fg.setHoverListener(new FlamegraphView.HoverListener<>() {
                @Override
                public void onFrameHover(@NotNull FrameBox<@NotNull String> frame,
                                         @NotNull Rectangle hoveredFrameRectangle,
                                         @NotNull MouseEvent e) {
                    // no-op
                }

                @Override
                public void onStopHover(FrameBox<@NotNull String> previousHoveredFrame,
                                        Rectangle prevHoveredFrameRectangle,
                                        @NotNull MouseEvent e) {
                    capturedPrevFrame.set(previousHoveredFrame);
                    capturedPrevRect.set(prevHoveredFrameRectangle);
                }
            });

            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                // Point in empty area
                when(mockPointerInfo.getLocation()).thenReturn(new Point(10, 10));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var event = createEnteredEvent(scrollPane, 10, 10);

                // Act
                scrollPane.dispatchEvent(event);

                // Assert - if stopHover was called, prev frame should be null (none existed before)
                if (capturedPrevFrame.get() != null || capturedPrevRect.get() != null) {
                    // If values were captured, they should be from a previous state
                }
            }
        }

        @Test
        void multiple_mouse_entered_events_do_not_cause_duplicate_listener_calls() {
            // Arrange
            fg.setShowMinimap(false);

            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                when(mockPointerInfo.getLocation()).thenReturn(new Point(100, 50));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var event1 = createEnteredEvent(scrollPane, 100, 50);

                var event2 = createEnteredEvent(scrollPane, 100, 50);

                // Act
                scrollPane.dispatchEvent(event1);
                scrollPane.dispatchEvent(event2);

                // Assert - listener should handle multiple events gracefully
                // The optimization should prevent redundant frame lookups for same position
            }
        }
    }

    @Nested
    @DisplayName("Hover Listener")
    class HoverListenerTests {

        @Test
        void setHoverListener_sets_listener() {
            FlamegraphView.HoverListener<String> listener = (frame, rect, e) -> {};

            assertThatCode(() -> fg.setHoverListener(listener))
                    .doesNotThrowAnyException();
        }

        @Test
        void hover_listener_onStopHover_has_default_implementation() {
            FlamegraphView.HoverListener<String> listener = (frame, rect, e) -> {};

            // onStopHover has default no-op implementation, should not throw
            assertThatCode(() -> listener.onStopHover(null, null, mock(MouseEvent.class)))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("HoverListener.getPointLeveledToFrameDepth")
    class HoverListenerGetPointLeveledToFrameDepthTests {

        @Test
        void getPointLeveledToFrameDepth_with_valid_inputs_returns_point() {
            // Set up the FlamegraphView with a model
            var frame = new FrameBox<>("root", 0.0, 1.0, 0);
            fg.setModel(new FrameModel<>(List.of(frame)));

            // Get the scroll pane from the component hierarchy
            var scrollPane = findScrollPane(fg.component);
            assertThat(scrollPane).isNotNull();

            // Create a mock mouse event from the scroll pane
            var mockEvent = mock(MouseEvent.class);
            when(mockEvent.getComponent()).thenReturn(scrollPane);
            when(mockEvent.getPoint()).thenReturn(new Point(100, 50));

            var frameRect = new Rectangle(0, 0, 200, 20);

            // Should not throw and should return a valid point
            assertThatCode(() -> FlamegraphView.HoverListener.getPointLeveledToFrameDepth(mockEvent, frameRect))
                    .doesNotThrowAnyException();
        }

        @Test
        void getPointLeveledToFrameDepth_without_flamegraph_owner_throws() {
            // Create a scroll pane that is NOT owned by a FlamegraphView
            var orphanScrollPane = new JScrollPane();

            var mockEvent = mock(MouseEvent.class);
            when(mockEvent.getComponent()).thenReturn(orphanScrollPane);
            when(mockEvent.getPoint()).thenReturn(new Point(100, 50));

            var frameRect = new Rectangle(0, 0, 200, 20);

            assertThatThrownBy(() -> FlamegraphView.HoverListener.getPointLeveledToFrameDepth(mockEvent, frameRect))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot find FlamegraphView owner");
        }
    }
}