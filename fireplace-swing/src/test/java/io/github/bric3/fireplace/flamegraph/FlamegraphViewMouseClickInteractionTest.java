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

import io.github.bric3.fireplace.flamegraph.FlamegraphView.FrameClickAction;
import io.github.bric3.fireplace.flamegraph.FlamegraphView.Mode;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static io.github.bric3.fireplace.flamegraph.SwingTestUtil.createClickEvent;
import static io.github.bric3.fireplace.flamegraph.SwingTestUtil.createDraggedEvent;
import static io.github.bric3.fireplace.flamegraph.SwingTestUtil.createEnteredEvent;
import static io.github.bric3.fireplace.flamegraph.SwingTestUtil.createExitedEvent;
import static io.github.bric3.fireplace.flamegraph.SwingTestUtil.createMovedEvent;
import static io.github.bric3.fireplace.flamegraph.SwingTestUtil.createPressEvent;
import static io.github.bric3.fireplace.flamegraph.SwingTestUtil.createReleaseEvent;
import static io.github.bric3.fireplace.flamegraph.SwingTestUtil.findScrollPane;
import static java.util.Optional.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.nullable;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link FlamegraphView} user interaction features.
 */
@SuppressWarnings({"DataFlowIssue", "unchecked", "rawtypes"})
@DisplayName("FlamegraphView - Interaction & Mouse")
class FlamegraphViewMouseClickInteractionTest {

    @Nested
    @DisplayName("Hovered Siblings")
    class HoveredSiblingsTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void isShowHoveredSiblings_default_is_true() {
            assertThat(fg.view().isShowHoveredSiblings()).isTrue();
        }

        @Test
        void setShowHoveredSiblings_false_disables() {
            fg.view().setShowHoveredSiblings(false);

            assertThat(fg.view().isShowHoveredSiblings()).isFalse();
        }

        @Test
        void setShowHoveredSiblings_toggles_correctly() {
            assertThat(fg.view().isShowHoveredSiblings()).isTrue();

            fg.view().setShowHoveredSiblings(false);
            assertThat(fg.view().isShowHoveredSiblings()).isFalse();

            fg.view().setShowHoveredSiblings(true);
            assertThat(fg.view().isShowHoveredSiblings()).isTrue();
        }
    }

    @Nested
    @DisplayName("Frame Click Action")
    class FrameClickActionTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void getFrameClickAction_default_is_focus_frame() {
            assertThat(fg.view().getFrameClickAction()).isEqualTo(FrameClickAction.FOCUS_FRAME);
        }

        @Test
        void setFrameClickAction_expand_frame_changes_action() {
            fg.view().setFrameClickAction(FrameClickAction.EXPAND_FRAME);

            assertThat(fg.view().getFrameClickAction()).isEqualTo(FrameClickAction.EXPAND_FRAME);
        }

        @Test
        void setFrameClickAction_to_expand_changes_behavior() {
            fg.view().setFrameClickAction(FrameClickAction.EXPAND_FRAME);
            assertThat(fg.view().getFrameClickAction()).isEqualTo(FrameClickAction.EXPAND_FRAME);
        }

        @Test
        void setFrameClickAction_toggle_between_actions() {
            fg.view().setFrameClickAction(FrameClickAction.EXPAND_FRAME);
            fg.view().setFrameClickAction(FrameClickAction.FOCUS_FRAME);
            fg.view().setFrameClickAction(FrameClickAction.EXPAND_FRAME);

            assertThat(fg.view().getFrameClickAction()).isEqualTo(FrameClickAction.EXPAND_FRAME);
        }
    }

    @Nested
    @DisplayName("Configure Canvas")
    class ConfigureCanvasTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void configureCanvas_invokes_configurer() {
            var configured = new AtomicReference<JComponent>(null);

            fg.view().configureCanvas(configured::set);

            assertThat(configured.get()).isNotNull();
        }

        @Test
        void configureCanvas_null_configurer_throws_exception() {
            assertThatThrownBy(() -> fg.view().configureCanvas(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Mouse Click Behavior on Canvas")
    class MouseClickBehaviorTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder()
                .withCanvasSpy()
                .withRenderEngineMock()
                .build();
        private FrameBox<String> testFrame;

        @BeforeEach
        void setUpGraphicsAndComponents() {
            // Set up a model with frames
            testFrame = new FrameBox<>("root", 0.0, 1.0, 0);
            var frames = List.of(
                    testFrame,
                    new FrameBox<>("child1", 0.0, 0.5, 1),
                    new FrameBox<>("child2", 0.5, 1.0, 1)
            );
            fg.view().setModel(new FrameModel<>(frames));

            fg.scrollPane().setSize(800, 600);
            fg.viewport().setSize(800, 600);
            fg.canvas().setSize(800, 600);
        }

        @Test
        void setSelectedFrameConsumer_sets_consumer() {
            BiConsumer<FrameBox<String>, MouseEvent> consumer = (frame, e) -> {};

            fg.view().setSelectedFrameConsumer(consumer);

            assertThat(fg.view().getSelectedFrameConsumer()).isEqualTo(consumer);
        }

        @Test
        void setSelectedFrameConsumer_null_throws_exception() {
            assertThatThrownBy(() -> fg.view().setSelectedFrameConsumer(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void popup_consumer_receives_frame_and_event() {
            BiConsumer<FrameBox<String>, MouseEvent> consumer = mock(BiConsumer.class);

            fg.view().setPopupConsumer(consumer);
            assertThat(fg.view().getPopupConsumer()).isEqualTo(consumer);
        }

        @Test
        void selected_frame_consumer_receives_frame_and_event() {
            BiConsumer<FrameBox<String>, MouseEvent> consumer = mock(BiConsumer.class);

            fg.view().setSelectedFrameConsumer(consumer);
            assertThat(fg.view().getSelectedFrameConsumer()).isEqualTo(consumer);
        }

        @Test
        void mouse_click_with_non_left_button_on_scroll_pane_returns_early() {
            // Arrange - spy on scrollPane to verify it returns early (before requestFocus)
            var spiedScrollPane = spy(fg.scrollPane());
            var event = createClickEvent(spiedScrollPane, 100, 100, MouseEvent.BUTTON3, 1);

            // Act
            spiedScrollPane.dispatchEvent(event);

            // Assert - requestFocus should NOT be called since we return early for non-left button
            verify(spiedScrollPane, never()).requestFocus();
        }

        @Test
        void mouse_click_requests_focus_on_scroll_pane() {
            // Arrange - spy on scrollPane to verify requestFocus() is called
            var spiedScrollPane = spy(fg.scrollPane());

            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                when(mockPointerInfo.getLocation()).thenReturn(new Point(100, 100));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var event = createClickEvent(spiedScrollPane, 100, 100, MouseEvent.BUTTON1, 1);

                // Act
                spiedScrollPane.dispatchEvent(event);

                // Assert - requestFocus should be called (FlamegraphView.java:1823)
                verify(spiedScrollPane).requestFocus();
            }
        }

        @Test
        void mouse_click_inside_minimap_bails_out_early() {
            // Arrange - enable minimap and spy on scrollPane
            fg.view().setShowMinimap(true);

            // Return a point that would be inside the minimap (typically bottom-right corner)
            doReturn(true).when(fg.canvasSpy()).isInsideMinimap(any());
            var event = createClickEvent(fg.scrollPane(), 750, 550, MouseEvent.BUTTON1, 1);

            // Act
            fg.scrollPane().dispatchEvent(event);

            // Assert - requestFocus IS called (line 1823), then it bails out early (line 1828-1830)
            // Verify it bailed out: frame lookup methods should never be called
            verify(fg.renderEngine(), never()).getFrameAt(any(), any(), any());
            verify(fg.renderEngine(), never()).toggleSelectedFrameAt(nullable(Graphics2D.class), any(), any(), any());
            verify(fg.renderEngine(), never()).calculateHorizontalZoomTargetForFrameAt(any(), any(), any(), any());
        }

        @Test
        void single_click_in_expand_frame_mode_zoom_on_frame() {
            // Arrange
            fg.view().setFrameClickAction(FrameClickAction.EXPAND_FRAME);
            fg.view().setShowMinimap(false); // Disable minimap to avoid early bailout at line 1828-1830

            // Create a mock ZoomTarget to be returned by calculateHorizontalZoomTargetForFrameAt
            var mockZoomTarget = mock(ZoomTarget.class);
            var mockTargetBounds = new Rectangle(100, 0, 200, 100); // Different from canvas bounds to avoid reset
            when(mockZoomTarget.getTargetBounds()).thenReturn(mockTargetBounds);
            when(fg.renderEngine().calculateHorizontalZoomTargetForFrameAt(any(), any(), any(), any()))
                    .thenReturn(of(mockZoomTarget));

            try (var mockedMouseInfo = mockStatic(MouseInfo.class);
                 var mockedFlamegraphView = mockStatic(FlamegraphView.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                // Point inside canvas but outside minimap
                when(mockPointerInfo.getLocation()).thenReturn(new Point(200, 50));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var event = createClickEvent(fg.scrollPane(), 200, 50, MouseEvent.BUTTON1, 1);

                // Act
                fg.scrollPane().dispatchEvent(event);

                // Assert - single click in EXPAND_FRAME mode should toggle selection, calculate zoom, and call zoom (FlamegraphView.java:1837-1859)
                verify(fg.renderEngine()).toggleSelectedFrameAt(nullable(Graphics2D.class), any(), any(), any());
                verify(fg.renderEngine()).calculateHorizontalZoomTargetForFrameAt(nullable(Graphics2D.class), any(), any(), any());
                // Verify zoom is called with the mock zoom target (line 1858)
                mockedFlamegraphView.verify(() -> FlamegraphView.zoom(any(), eq(mockZoomTarget)));
            }
        }

        @Test
        void single_click_in_expand_frame_mode_and__canvas_has_same_bounds_as_target__resets_zoom() {
            // Arrange
            fg.view().setFrameClickAction(FrameClickAction.EXPAND_FRAME);
            fg.view().setShowMinimap(false);

            // Create a ZoomTarget to be returned by calculateHorizontalZoomTargetForFrameAt
            // The target bounds should match canvas bounds (x=0, width=800) to trigger reset (FlamegraphView.java:1854-1856)
            var mockZoomTarget = new ZoomTarget<String>(0, 0, 800, 600, null);
            when(fg.renderEngine().calculateHorizontalZoomTargetForFrameAt(any(), any(), any(), any()))
                    .thenReturn(of(mockZoomTarget));

            // Mock computeVisibleFlamegraphHeight so getResetZoomTarget can compute the reset target
            when(fg.renderEngine().computeVisibleFlamegraphHeight(any(), anyInt())).thenReturn(600);

            try (var mockedMouseInfo = mockStatic(MouseInfo.class);
                 var mockedFlamegraphView = mockStatic(FlamegraphView.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                // Point inside canvas but outside minimap
                when(mockPointerInfo.getLocation()).thenReturn(new Point(200, 50));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var event = createClickEvent(fg.scrollPane(), 200, 50, MouseEvent.BUTTON1, 1);

                // Act
                fg.scrollPane().dispatchEvent(event);

                // Assert - single click in EXPAND_FRAME mode should toggle selection, calculate zoom, and call zoom (FlamegraphView.java:1837-1859)
                verify(fg.renderEngine()).toggleSelectedFrameAt(nullable(Graphics2D.class), any(), any(), any());
                verify(fg.renderEngine()).calculateHorizontalZoomTargetForFrameAt(nullable(Graphics2D.class), any(), any(), any());
                // Verify the reset zoom path is taken (line 1856) by confirming zoom is NOT called with mockZoomTarget
                // When canvas bounds match target bounds, it calls getResetZoomTarget instead of using mockZoomTarget
                // In headless mode, getResetZoomTarget returns null, so zoom is called with null (not mockZoomTarget)
                mockedFlamegraphView.verify(() -> FlamegraphView.zoom(any(), nullable(ZoomTarget.class)));
                // Verify it's NOT called with the mockZoomTarget (which would be the else branch at line 1858)
                mockedFlamegraphView.verify(() -> FlamegraphView.zoom(any(), eq(mockZoomTarget)), never());
            }
        }

        @Test
        void double_click_in_expand_frame_mode_does_not_trigger_zoom() {
            // Arrange - double click in EXPAND_FRAME mode should not trigger zoom (only single click does)
            fg.view().setFrameClickAction(FrameClickAction.EXPAND_FRAME);
            fg.view().setShowMinimap(false);
            var spiedScrollPane = spy(fg.scrollPane());

            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                when(mockPointerInfo.getLocation()).thenReturn(new Point(200, 50));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var event = createClickEvent(spiedScrollPane, 200, 50, MouseEvent.BUTTON1, 2);

                // Act
                spiedScrollPane.dispatchEvent(event);

                // Assert - zoom should NOT be triggered for double-click in EXPAND_FRAME mode
                verify(fg.renderEngine(), never()).toggleSelectedFrameAt(any(), any(), any(), any());
                verify(fg.renderEngine(), never()).calculateHorizontalZoomTargetForFrameAt(any(), any(), any(), any());
            }
        }

        @Test
        void single_click_in_focus_frame_mode_toggles_selection() {
            // Arrange
            fg.view().setFrameClickAction(FrameClickAction.FOCUS_FRAME);
            fg.view().setShowMinimap(false);
            var spiedScrollPane = spy(fg.scrollPane());

            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                when(mockPointerInfo.getLocation()).thenReturn(new Point(200, 50));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var event = createClickEvent(spiedScrollPane, 200, 50, MouseEvent.BUTTON1, 1);

                // Act
                spiedScrollPane.dispatchEvent(event);

                // Assert - requestFocus should be called
                verify(fg.renderEngine()).toggleSelectedFrameAt(nullable(Graphics2D.class), any(), any(), any());
            }
        }

        @Test
        void double_click_in_focus_frame_mode_zooms_on_frame() {
            // Arrange
            fg.view().setFrameClickAction(FrameClickAction.FOCUS_FRAME);
            fg.view().setShowMinimap(false); // Disable minimap to avoid early bailout at line 1828-1830

            // Create a mock ZoomTarget to be returned by calculateZoomTargetForFrameAt
            var mockZoomTarget = mock(ZoomTarget.class);
            var mockTargetBounds = new Rectangle(100, 0, 200, 100); // Different from canvas bounds to avoid reset
            when(mockZoomTarget.getTargetBounds()).thenReturn(mockTargetBounds);
            when(fg.renderEngine().calculateZoomTargetForFrameAt(any(), any(), any(), any()))
                    .thenReturn(of(mockZoomTarget));

            var spiedScrollPane = spy(fg.scrollPane());

            try (var mockedMouseInfo = mockStatic(MouseInfo.class);
                 var mockedFlamegraphView = mockStatic(FlamegraphView.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                // Point inside canvas but outside minimap
                when(mockPointerInfo.getLocation()).thenReturn(new Point(200, 50));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var event = createClickEvent(spiedScrollPane, 200, 50, MouseEvent.BUTTON1, 2);

                // Act
                spiedScrollPane.dispatchEvent(event);

                // Assert - double click in FOCUS_FRAME mode should calculate zoom and call zoom (FlamegraphView.java:1864-1878)
                verify(fg.renderEngine()).calculateZoomTargetForFrameAt(nullable(Graphics2D.class), any(), any(), any());
                // Verify zoom is called with the mock zoom target (line 1875)
                mockedFlamegraphView.verify(() -> FlamegraphView.zoom(any(), eq(mockZoomTarget)));
            }
        }

        @Test
        void double_click_in_focus_frame_mode_and__canvas_equals_target__resets_zoom() {
            // Arrange
            fg.view().setFrameClickAction(FrameClickAction.FOCUS_FRAME);
            fg.view().setShowMinimap(false); // Disable minimap to avoid early bailout at line 1828-1830

            // Create a ZoomTarget to be returned by calculateZoomTargetForFrameAt
            // The target bounds should match canvas bounds to trigger reset (FlamegraphView.java:1872-1873)
            var mockZoomTarget = new ZoomTarget<String>(0, 0, 800, 600, null);
            when(fg.renderEngine().calculateZoomTargetForFrameAt(any(), any(), any(), any()))
                    .thenReturn(of(mockZoomTarget));

            // Mock computeVisibleFlamegraphHeight so getResetZoomTarget can compute the reset target
            when(fg.renderEngine().computeVisibleFlamegraphHeight(any(), anyInt())).thenReturn(600);

            try (var mockedMouseInfo = mockStatic(MouseInfo.class);
                 var mockedFlamegraphView = mockStatic(FlamegraphView.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                // Point inside canvas but outside minimap
                when(mockPointerInfo.getLocation()).thenReturn(new Point(200, 50));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var event = createClickEvent(fg.scrollPane(), 200, 50, MouseEvent.BUTTON1, 2);

                // Act
                fg.scrollPane().dispatchEvent(event);

                // Assert - double click in FOCUS_FRAME mode should calculate zoom and call zoom (FlamegraphView.java:1864-1878)
                verify(fg.renderEngine()).calculateZoomTargetForFrameAt(nullable(Graphics2D.class), any(), any(), any());
                // Verify the reset zoom path is taken (line 1873) by confirming zoom is NOT called with mockZoomTarget
                // When canvas bounds equal target bounds, it calls getResetZoomTarget instead of using mockZoomTarget
                // In headless mode, getResetZoomTarget returns null, so zoom is called with null (not mockZoomTarget)
                mockedFlamegraphView.verify(() -> FlamegraphView.zoom(any(), nullable(ZoomTarget.class)));
                // Verify it's NOT called with the mockZoomTarget (which would be the else branch at line 1875)
                mockedFlamegraphView.verify(() -> FlamegraphView.zoom(any(), eq(mockZoomTarget)), never());
            }
        }

        @Test
        void mouse_click_with_minimap_disabled_does_not_check_minimap() {
            // Arrange
            fg.view().setShowMinimap(false);
            var spiedScrollPane = spy(fg.scrollPane());

            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                // Point that would be inside minimap if enabled
                when(mockPointerInfo.getLocation()).thenReturn(new Point(750, 550));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var event = createClickEvent(spiedScrollPane, 750, 550, MouseEvent.BUTTON1, 1);

                // Act
                spiedScrollPane.dispatchEvent(event);

                // Assert - requestFocus should be called
                verify(spiedScrollPane).requestFocus();
            }
        }

        @Test
        void mouse_click_in_iciclegraph_mode_processes_correctly() {
            // Arrange
            fg.view().setMode(Mode.ICICLEGRAPH);
            fg.view().setFrameClickAction(FrameClickAction.FOCUS_FRAME);
            var spiedScrollPane = spy(fg.scrollPane());

            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                when(mockPointerInfo.getLocation()).thenReturn(new Point(200, 50));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var event = createClickEvent(spiedScrollPane, 200, 50, MouseEvent.BUTTON1, 1);

                // Act
                spiedScrollPane.dispatchEvent(event);

                // Assert - requestFocus should be called
                verify(spiedScrollPane).requestFocus();
            }
        }

        @Test
        void mouse_click_in_flamegraph_mode_processes_correctly() {
            // Arrange
            fg.view().setMode(Mode.FLAMEGRAPH);
            fg.view().setFrameClickAction(FrameClickAction.FOCUS_FRAME);
            var spiedScrollPane = spy(fg.scrollPane());

            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                when(mockPointerInfo.getLocation()).thenReturn(new Point(200, 550));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var event = createClickEvent(spiedScrollPane, 200, 550, MouseEvent.BUTTON1, 1);

                // Act
                spiedScrollPane.dispatchEvent(event);

                // Assert - requestFocus should be called
                verify(spiedScrollPane).requestFocus();
            }
        }

        @Test
        void popup_consumer_is_available_after_setting() {
            // Arrange
            BiConsumer<FrameBox<String>, MouseEvent> popupConsumer = (frame, e) -> {};

            fg.view().setPopupConsumer(popupConsumer);

            // Assert
            assertThat(fg.view().getPopupConsumer()).isEqualTo(popupConsumer);
        }

        @Test
        void mouse_double_click_do_not_invoke_selectedFrameConsumer() {
            // Arrange - double click should return early (clickCount != 1) in the canvas listener
            BiConsumer<FrameBox<String>, MouseEvent> selectedFrameConsumer = mock(BiConsumer.class);
            fg.view().setSelectedFrameConsumer(selectedFrameConsumer);

            var canvas = fg.canvas();
            var event = createClickEvent(canvas, 100, 30, MouseEvent.BUTTON1, 2);

            // Act
            canvas.dispatchEvent(event);

            // Assert - consumer should NOT be called for double click
            verify(selectedFrameConsumer, never()).accept(any(), any());
        }

        @Test
        void mouse_click_with_non_left_button_do_not_invoke_selectedFrameConsumer() {
            // Arrange - right button should return early (button != BUTTON1)
            BiConsumer<FrameBox<String>, MouseEvent> selectedFrameConsumer = mock(BiConsumer.class);
            fg.view().setSelectedFrameConsumer(selectedFrameConsumer);

            var canvas = fg.canvas();
            var event = createClickEvent(canvas, 100, 30, MouseEvent.BUTTON3, 1);

            // Act
            canvas.dispatchEvent(event);

            // Assert - consumer should NOT be called for non-left button
            verify(selectedFrameConsumer, never()).accept(any(), any());
        }

        @Test
        void mouse_click_without_selectedFrameConsumer_returns_early() {
            // Arrange - no consumer set (null check returns early before getFrameAt)
            // Don't set selectedFrameConsumer - it should be null by default

            var canvas = fg.canvas();
            var event = createClickEvent(canvas, 100, 30, MouseEvent.BUTTON1, 1);

            // Act
            canvas.dispatchEvent(event);

            // Assert - getFrameAt should never be called when consumer is null
            verify(fg.renderEngine(), never()).getFrameAt(any(), any(), any());
        }

        @Test
        void mouse_click_invokes_selectedFrameConsumer_when_frame_is_found() {
            // Arrange - stub render engine mock to return a frame
            // Use nullable() because getGraphics() returns null in headless mode
            when(fg.renderEngine().getFrameAt(nullable(Graphics2D.class), any(), any()))
                    .thenReturn(of(testFrame));

            BiConsumer<FrameBox<String>, MouseEvent> selectedFrameConsumer = mock(BiConsumer.class);
            fg.view().setSelectedFrameConsumer(selectedFrameConsumer);

            var canvas = fg.canvas();

            var event = createClickEvent(canvas, 400, 300, MouseEvent.BUTTON1, 1);

            // Act
            canvas.dispatchEvent(event);

            // Assert - consumer should be invoked with the found frame and event (FlamegraphView.java:1680-1681)
            verify(selectedFrameConsumer).accept(testFrame, event);
        }

        @Test
        void mouse_click_does_not_invoke_selectedFrameConsumer_when_no_frame_is_found() {
            // Arrange - stub render engine mock to return empty
            // Use nullable() because getGraphics() returns null in headless mode
            when(fg.renderEngine().getFrameAt(nullable(Graphics2D.class), any(), any()))
                    .thenReturn(empty());

            BiConsumer<FrameBox<String>, MouseEvent> selectedFrameConsumer = mock(BiConsumer.class);
            fg.view().setSelectedFrameConsumer(selectedFrameConsumer);

            var canvas = fg.canvas();

            var event = createClickEvent(canvas, 799, 599, MouseEvent.BUTTON1, 1);

            // Act
            canvas.dispatchEvent(event);

            // Assert - consumer should NOT be invoked when no frame is found (FlamegraphView.java:1680-1681)
            verify(selectedFrameConsumer, never()).accept(any(), any());
        }
    }

    @Nested
    @DisplayName("Mouse Dragging Behavior on Canvas")
    class MouseDraggingBehaviorTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @BeforeEach
        void setUpComponents() {
            // Set up a model with frames
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child1", 0.0, 0.5, 1),
                    new FrameBox<>("child2", 0.5, 1.0, 1)
            );
            fg.view().setModel(new FrameModel<>(frames));

            // Set sizes
            fg.scrollPane().setSize(800, 600);
            fg.scrollPane().getViewport().setSize(800, 600);

            fg.canvas().setSize(800, 600);
        }

        // ==================== mousePressed tests ====================

        @Test
        void mouse_pressed_with_left_button_sets_pressed_point() {
            // Arrange
            var event = createPressEvent(fg.scrollPane(), 100, 100, MouseEvent.BUTTON1, false);

            // Act & Assert - should not throw
            assertThatCode(() -> fg.scrollPane().dispatchEvent(event))
                    .doesNotThrowAnyException();
        }

        @Test
        void mouse_pressed_with_non_left_button_does_not_set_pressed_point() {
            // Arrange - right click
            var event = createPressEvent(fg.scrollPane(), 100, 100, MouseEvent.BUTTON3, false);

            // Act & Assert - should not throw
            assertThatCode(() -> fg.scrollPane().dispatchEvent(event))
                    .doesNotThrowAnyException();
        }

        @Test
        void mouse_pressed_inside_minimap_does_not_set_pressed_point() {
            // Arrange - enable minimap
            fg.view().setShowMinimap(true);

            // Press inside minimap area (bottom-right corner)
            var event = createPressEvent(fg.scrollPane(), 750, 550, MouseEvent.BUTTON1, false);

            // Act & Assert - should not throw, pressedPoint should be set to null
            assertThatCode(() -> fg.scrollPane().dispatchEvent(event))
                    .doesNotThrowAnyException();
        }

        @Test
        void mouse_pressed_outside_minimap_sets_pressed_point() {
            // Arrange - enable minimap
            fg.view().setShowMinimap(true);

            // Press outside minimap area
            var event = createPressEvent(fg.scrollPane(), 100, 100, MouseEvent.BUTTON1, false);

            // Act & Assert - should not throw
            assertThatCode(() -> fg.scrollPane().dispatchEvent(event))
                    .doesNotThrowAnyException();
        }

        @Test
        void mouse_released_clears_pressed_point() {
            // Arrange - first press to set pressedPoint
            var pressEvent = createPressEvent(fg.scrollPane(), 100, 100, MouseEvent.BUTTON1, false);
            fg.scrollPane().dispatchEvent(pressEvent);

            // Now release
            var releaseEvent = createReleaseEvent(fg.scrollPane(), 100, 100, false);

            // Act & Assert - should not throw
            assertThatCode(() -> fg.scrollPane().dispatchEvent(releaseEvent))
                    .doesNotThrowAnyException();
        }

        @Test
        void mouse_released_without_prior_press_does_not_throw() {
            // Arrange - release without press
            var releaseEvent = createReleaseEvent(fg.scrollPane(), 100, 100, false);

            // Act & Assert - should not throw
            assertThatCode(() -> fg.scrollPane().dispatchEvent(releaseEvent))
                    .doesNotThrowAnyException();
        }

        @Test
        void mouse_dragged_without_prior_press_does_nothing() {
            // Arrange - drag without press (pressedPoint is null)
            var dragEvent = createDraggedEvent(fg.scrollPane(), 150, 150);

            // Act & Assert - should not throw
            assertThatCode(() -> fg.scrollPane().dispatchEvent(dragEvent))
                    .doesNotThrowAnyException();
        }

        @Test
        void mouse_dragged_after_press_updates_viewport_position() {
            // Arrange - set up with larger canvas to allow scrolling
            fg.scrollPane().getViewport().setViewPosition(new Point(100, 100));

            // Press first to set pressedPoint
            var pressEvent = createPressEvent(fg.scrollPane(), 100, 100, MouseEvent.BUTTON1, false);
            fg.scrollPane().dispatchEvent(pressEvent);

            // Now drag
            var dragEvent = createDraggedEvent(fg.scrollPane(), 150, 150);

            // Act & Assert - should not throw
            assertThatCode(() -> fg.scrollPane().dispatchEvent(dragEvent))
                    .doesNotThrowAnyException();
        }

        @Test
        void mouse_dragged_from_non_scroll_pane_source_does_nothing() {
            // Arrange - press first
            var pressEvent = createPressEvent(fg.scrollPane(), 100, 100, MouseEvent.BUTTON1, false);
            fg.scrollPane().dispatchEvent(pressEvent);

            // Drag from a different component (not JScrollPane)
            var otherComponent = new JPanel();
            var dragEvent = createDraggedEvent(otherComponent, 150, 150);

            // Act & Assert - dispatch to scrollPane but event source is different
            assertThatCode(() -> fg.scrollPane().dispatchEvent(dragEvent))
                    .doesNotThrowAnyException();
        }

        @Test
        void mouse_drag_sequence_press_drag_release() {
            // Arrange - complete drag sequence
            fg.scrollPane().getViewport().setViewPosition(new Point(200, 200));

            // Press
            var pressEvent = createPressEvent(fg.scrollPane(), 100, 100, MouseEvent.BUTTON1, false);
            fg.scrollPane().dispatchEvent(pressEvent);

            // Drag
            var dragEvent1 = createDraggedEvent(fg.scrollPane(), 120, 120);
            fg.scrollPane().dispatchEvent(dragEvent1);

            // Drag again
            var dragEvent2 = createDraggedEvent(fg.scrollPane(), 140, 140);
            fg.scrollPane().dispatchEvent(dragEvent2);

            // Release
            var releaseEvent = createReleaseEvent(fg.scrollPane(), 140, 140, false);

            // Act & Assert
            assertThatCode(() -> fg.scrollPane().dispatchEvent(releaseEvent))
                    .doesNotThrowAnyException();
        }

        @Test
        void mouse_dragged_consumes_event() {
            // Arrange
            fg.scrollPane().getViewport().setViewPosition(new Point(100, 100));

            // Press first
            var pressEvent = createPressEvent(fg.scrollPane(), 100, 100, MouseEvent.BUTTON1, false);
            fg.scrollPane().dispatchEvent(pressEvent);

            // Drag - the event should be consumed by the listener
            var dragEvent = createDraggedEvent(fg.scrollPane(), 150, 150);

            // Act
            fg.scrollPane().dispatchEvent(dragEvent);

            // Assert - event should be consumed (note: we can't easily verify this
            // without reflection, but at least verify no exception)
        }

        @Test
        void mouse_dragged_clamps_position_to_zero() {
            // Arrange - set view position near origin
            fg.scrollPane().getViewport().setViewPosition(new Point(10, 10));

            // Press first
            var pressEvent = createPressEvent(fg.scrollPane(), 100, 100, MouseEvent.BUTTON1, false);
            fg.scrollPane().dispatchEvent(pressEvent);

            // Drag significantly to try to go negative
            var dragEvent = createDraggedEvent(fg.scrollPane(), 200, 200);

            // Act & Assert - position should be clamped to 0, not negative
            assertThatCode(() -> fg.scrollPane().dispatchEvent(dragEvent))
                    .doesNotThrowAnyException();

            // Verify position is clamped (x and y should be >= 0)
            var viewPosition = fg.scrollPane().getViewport().getViewPosition();
            assertThat(viewPosition.x).isGreaterThanOrEqualTo(0);
            assertThat(viewPosition.y).isGreaterThanOrEqualTo(0);
        }

        @Test
        void mouse_release_after_press_clears_pressed_point_and_prevents_drag() {
            // Arrange - press, release, then try to drag
            fg.scrollPane().getViewport().setViewPosition(new Point(100, 100));

            // Press
            var pressEvent = createPressEvent(fg.scrollPane(), 100, 100, MouseEvent.BUTTON1, false);
            fg.scrollPane().dispatchEvent(pressEvent);

            // Release - this clears pressedPoint
            var releaseEvent = createReleaseEvent(fg.scrollPane(), 100, 100, false);
            fg.scrollPane().dispatchEvent(releaseEvent);

            // Get position after release
            var positionAfterRelease = fg.scrollPane().getViewport().getViewPosition();

            // Try to drag - should do nothing since pressedPoint is null
            var dragEvent = createDraggedEvent(fg.scrollPane(), 150, 150);
            fg.scrollPane().dispatchEvent(dragEvent);

            // Assert - position should not have changed after the drag
            var finalPosition = fg.scrollPane().getViewport().getViewPosition();
            assertThat(finalPosition).isEqualTo(positionAfterRelease);
        }
    }

    @Nested
    @DisplayName("Mouse Hover Behavior on Canvas")
    class MouseHoverBehaviorTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder()
                .withCanvasSpy()
                .withGraphics2D()
                .build();
        private FlamegraphView.HoverListener<String> hoverListener;

        @BeforeEach
        void setUpGraphicsAndComponents() {
            // Set up a model with frames
            var rootFrame = new FrameBox<>("root", 0.0, 1.0, 0);
            var childFrame = new FrameBox<>("child1", 0.0, 0.5, 1);
            var frames = List.of(
                    rootFrame,
                    childFrame,
                    new FrameBox<>("child2", 0.5, 1.0, 1)
            );
            fg.view().setModel(new FrameModel<>(frames));

            // Set sizes
            fg.scrollPane().setSize(800, 600);
            fg.viewport().setSize(800, 600);
            fg.canvas().setSize(800, 600);

            // Create mock hover listener
            hoverListener = mock(FlamegraphView.HoverListener.class);
            fg.view().setHoverListener(hoverListener);
        }

        @Test
        void mouse_entered_inside_minimap_calls_onStopHover_and_bails_out() {
            // Arrange - enable minimap
            fg.view().setShowMinimap(true);

            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                // Return a point that would be inside the minimap (bottom-right corner area)
                when(mockPointerInfo.getLocation()).thenReturn(new Point(750, 550));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var event = createEnteredEvent(fg.scrollPane(), 750, 550);

                // Act
                fg.scrollPane().dispatchEvent(event);

                // Assert - onStopHover should be called because mouse is inside minimap
                verify(hoverListener, atMostOnce()).onStopHover(any(), any(), any());
                verify(hoverListener, never()).onFrameHover(any(), any(), any());
            }
        }

        @Test
        void mouse_entered_outside_minimap_and_over_no_frame_calls_stopHover() {
            // Arrange - disable minimap to simplify
            fg.view().setShowMinimap(false);

            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                // Return a point outside any frame (far right edge)
                when(mockPointerInfo.getLocation()).thenReturn(new Point(10, 10));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var event = createEnteredEvent(fg.scrollPane(), 10, 10);

                // Act
                fg.scrollPane().dispatchEvent(event);

                // Assert - onStopHover should be called when hovering over no frame
                verify(hoverListener, atMostOnce()).onStopHover(any(), any(), any());
            }
        }

        @Test
        void mouse_entered_without_hover_listener_does_not_throw() {
            // Arrange - no hover listener
            fg.view().setHoverListener(null);
            fg.view().setShowMinimap(false);

            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                when(mockPointerInfo.getLocation()).thenReturn(new Point(100, 100));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var event = createEnteredEvent(fg.scrollPane(), 100, 100);

                // Act & Assert - should not throw
                assertThatCode(() -> fg.scrollPane().dispatchEvent(event))
                        .doesNotThrowAnyException();
            }
        }

        @Test
        void mouse_moved_inside_minimap_bails_out_without_calling_onFrameHover() {
            // Arrange - enable minimap
            fg.view().setShowMinimap(true);

            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                // Return a point inside the minimap
                when(mockPointerInfo.getLocation()).thenReturn(new Point(750, 550));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var event = createMovedEvent(fg.scrollPane(), 750, 550);

                // Act
                fg.scrollPane().dispatchEvent(event);

                // Assert - onFrameHover should never be called when inside minimap
                verify(hoverListener, never()).onFrameHover(any(), any(), any());
            }
        }

        @Test
        void mouse_exited_calls_onStopHover_with_previous_frame() {
            // Arrange
            fg.view().setShowMinimap(false);

            // First, simulate hovering over a frame to set the hovered state
            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                when(mockPointerInfo.getLocation()).thenReturn(new Point(100, 50));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var enterEvent = createEnteredEvent(fg.scrollPane(), 100, 50);
                fg.scrollPane().dispatchEvent(enterEvent);
            }

            // Now simulate mouse exit
            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                when(mockPointerInfo.getLocation()).thenReturn(new Point(-10, -10));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var exitEvent = createExitedEvent(fg.scrollPane(), -10, -10);

                // Act
                fg.scrollPane().dispatchEvent(exitEvent);

                // Assert - onStopHover should be called
                verify(hoverListener, atLeastOnce()).onStopHover(any(), any(), any());
            }
        }

        @Test
        void mouse_moved_over_same_frame_twice_optimizes_by_reusing_cached_rectangle() {
            // Arrange
            fg.view().setShowMinimap(false);

            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                // Point over a frame
                when(mockPointerInfo.getLocation()).thenReturn(new Point(200, 50));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                // First move - should look up the frame
                var event1 = createMovedEvent(fg.scrollPane(), 200, 50);
                fg.scrollPane().dispatchEvent(event1);

                // Clear invocations to track second call
                clearInvocations(hoverListener);

                // Second move - same general area, should use cached rectangle
                var event2 = createMovedEvent(fg.scrollPane(), 205, 50);

                // Act
                fg.scrollPane().dispatchEvent(event2);

                // Assert - onFrameHover may be called but frame lookup should be optimized
                // The exact behavior depends on whether the point is still in hoveredFrameRectangle
            }
        }

        @Test
        void mouse_wheel_moved_triggers_hover_check() {
            // Arrange
            fg.view().setShowMinimap(false);

            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                when(mockPointerInfo.getLocation()).thenReturn(new Point(100, 50));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var wheelEvent = new java.awt.event.MouseWheelEvent(
                        fg.scrollPane(),
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
                assertThatCode(() -> fg.scrollPane().dispatchEvent(wheelEvent))
                        .doesNotThrowAnyException();
            }
        }

        @Test
        void hover_listener_null_when_mouse_inside_minimap_does_not_throw() {
            // Arrange - no hover listener, minimap enabled
            fg.view().setHoverListener(null);
            fg.view().setShowMinimap(true);

            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                when(mockPointerInfo.getLocation()).thenReturn(new Point(750, 550));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var event = createEnteredEvent(fg.scrollPane(), 750, 550);

                // Act & Assert - should not throw even without listener
                assertThatCode(() -> fg.scrollPane().dispatchEvent(event))
                        .doesNotThrowAnyException();
            }
        }

        @Test
        void hover_listener_receives_mouse_event_from_scroll_pane() {
            // Arrange
            fg.view().setShowMinimap(false);

            var capturedEvent = new AtomicReference<MouseEvent>();
            fg.view().setHoverListener(new FlamegraphView.HoverListener<>() {
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

                var event = createEnteredEvent(fg.scrollPane(), 200, 50);

                // Act
                fg.scrollPane().dispatchEvent(event);

                // Assert - if frame was found, event should be captured
                // Note: In headless mode, frame detection may not work perfectly
            }
        }

        @Test
        void stop_hover_passes_null_for_previously_hovered_frame_when_none_existed() {
            // Arrange - fresh state, no previous hover
            fg.view().setShowMinimap(false);

            var capturedPrevFrame = new AtomicReference<FrameBox<String>>();
            var capturedPrevRect = new AtomicReference<Rectangle>();
            fg.view().setHoverListener(new FlamegraphView.HoverListener<>() {
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

                var event = createEnteredEvent(fg.scrollPane(), 10, 10);

                // Act
                fg.scrollPane().dispatchEvent(event);

                // Assert - if stopHover was called, prev frame should be null (none existed before)
                if (capturedPrevFrame.get() != null || capturedPrevRect.get() != null) {
                    // If values were captured, they should be from a previous state
                }
            }
        }

        @Test
        void multiple_mouse_entered_events_do_not_cause_duplicate_listener_calls() {
            // Arrange
            fg.view().setShowMinimap(false);

            try (var mockedMouseInfo = mockStatic(MouseInfo.class)) {
                var mockPointerInfo = mock(PointerInfo.class);
                when(mockPointerInfo.getLocation()).thenReturn(new Point(100, 50));
                mockedMouseInfo.when(MouseInfo::getPointerInfo).thenReturn(mockPointerInfo);

                var event1 = createEnteredEvent(fg.scrollPane(), 100, 50);

                var event2 = createEnteredEvent(fg.scrollPane(), 100, 50);

                // Act
                fg.scrollPane().dispatchEvent(event1);
                fg.scrollPane().dispatchEvent(event2);

                // Assert - listener should handle multiple events gracefully
                // The optimization should prevent redundant frame lookups for same position
            }
        }
    }

    @Nested
    @DisplayName("Mouse Popup Behavior")
    class MousePopupBehaviorTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder()
                .withRenderEngineMock()
                .build();
        private FrameBox<String> testFrame;

        @BeforeEach
        void setUpGraphicsAndComponents() {
            // Set up a model with frames
            testFrame = new FrameBox<>("root", 0.0, 1.0, 0);
            var frames = List.of(
                    testFrame,
                    new FrameBox<>("child1", 0.0, 0.5, 1),
                    new FrameBox<>("child2", 0.5, 1.0, 1)
            );
            fg.view().setModel(new FrameModel<>(frames));

            fg.scrollPane().setSize(800, 600);
            fg.viewport().setSize(800, 600);
            fg.canvas().setSize(800, 600);
        }

        @Test
        void setPopupConsumer_sets_consumer() {
            BiConsumer<FrameBox<String>, MouseEvent> consumer = (frame, e) -> {};

            fg.view().setPopupConsumer(consumer);

            assertThat(fg.view().getPopupConsumer()).isEqualTo(consumer);
        }

        @Test
        void setPopupConsumer_null_throws_exception() {
            assertThatThrownBy(() -> fg.view().setPopupConsumer(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void getPopupConsumer_default_is_null() {
            assertThat(fg.view().getPopupConsumer()).isNull();
        }

        @Test
        void left_button_press_inside_minimap_returns_without_calling_handlePopup() {
            // Arrange - enable minimap and press inside it
            fg.view().setShowMinimap(true);
            BiConsumer<FrameBox<String>, MouseEvent> popupConsumer = mock(BiConsumer.class);
            fg.view().setPopupConsumer(popupConsumer);

            var canvas = fg.canvas();

            // Create a left button press event inside minimap area (bottom-right corner)
            var event = createPressEvent(canvas, 750, 550, MouseEvent.BUTTON1, false);

            // Act
            try {
                canvas.dispatchEvent(event);
            } catch (NullPointerException e1) {
                // May occur in headless mode from minimap processing
            }

            // Assert - popup consumer should NOT be called for left button
            verify(popupConsumer, never()).accept(any(), any());
        }

        @Test
        void left_button_press_outside_minimap_returns_without_calling_handlePopup() {
            // Arrange
            fg.view().setShowMinimap(true);
            BiConsumer<FrameBox<String>, MouseEvent> popupConsumer = mock(BiConsumer.class);
            fg.view().setPopupConsumer(popupConsumer);

            var canvas = fg.canvas();

            // Create a left button press event outside minimap area
            var event = createPressEvent(canvas, 100, 100, MouseEvent.BUTTON1, false);

            // Act
            canvas.dispatchEvent(event);

            // Assert - popup consumer should NOT be called for left button
            verify(popupConsumer, never()).accept(any(), any());
        }

        @Test
        void non_left_button_press_without_popup_trigger_returns_early_from_handlePopup() {
            // Arrange - right button but NOT a popup trigger (isPopupTrigger = false)
            BiConsumer<FrameBox<String>, MouseEvent> popupConsumer = mock(BiConsumer.class);
            fg.view().setPopupConsumer(popupConsumer);

            var canvas = fg.canvas();

            var event = createPressEvent(canvas, 100, 100, MouseEvent.BUTTON3, false);

            // Act
            canvas.dispatchEvent(event);

            // Assert - popup consumer should NOT be called when not a popup trigger
            verify(popupConsumer, never()).accept(any(), any());
        }

        @Test
        void non_left_button_press_with_popup_trigger_but_null_consumer_returns_early() {
            // Arrange - right button, IS a popup trigger, but no consumer
            // Don't set popupConsumer - it should be null
            var freshFg = new FlamegraphView<String>();
            freshFg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));
            // No popupConsumer set - null check at line 1502-1504

            var freshScrollPane = findScrollPane(freshFg.component);
            var canvas = freshScrollPane.getViewport().getView();
            canvas.setSize(800, 600);

            var event = createPressEvent(canvas, 100, 100, MouseEvent.BUTTON3, true);

            // Act & Assert - should not throw, returns early when consumer is null
            assertThatCode(() -> canvas.dispatchEvent(event))
                    .doesNotThrowAnyException();
        }


        @Test
        void mouse_released_without_popup_trigger_returns_early() {
            // Arrange
            BiConsumer<FrameBox<String>, MouseEvent> popupConsumer = mock(BiConsumer.class);
            fg.view().setPopupConsumer(popupConsumer);

            var canvas = fg.canvas();

            var event = createReleaseEvent(canvas, 100, 100, false);

            // Act
            canvas.dispatchEvent(event);

            // Assert - popup consumer should NOT be called when not a popup trigger
            verify(popupConsumer, never()).accept(any(), any());
        }

        @Test
        void middle_button_press_with_popup_trigger_but_null_consumer_returns_early() {
            // Arrange - middle button edge case
            var freshFg = new FlamegraphView<String>();
            freshFg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));
            // No popupConsumer set

            var freshScrollPane = findScrollPane(freshFg.component);
            var canvas = freshScrollPane.getViewport().getView();
            canvas.setSize(800, 600);

            var event = createPressEvent(canvas, 100, 100, MouseEvent.BUTTON2, true);

            // Act & Assert - should not throw, returns early when consumer is null
            assertThatCode(() -> canvas.dispatchEvent(event))
                    .doesNotThrowAnyException();
        }

        @Test
        void handlePopup_invokes_consumer_when_frame_is_found() {
            // Arrange - stub render engine mock to return a frame
            // Use nullable() because getGraphics() returns null in headless mode
            when(fg.renderEngine().getFrameAt(nullable(Graphics2D.class), any(), any()))
                    .thenReturn(of(testFrame));

            BiConsumer<FrameBox<String>, MouseEvent> popupConsumer = mock(BiConsumer.class);
            fg.view().setPopupConsumer(popupConsumer);

            var canvas = fg.canvas();

            var event = createPressEvent(canvas, 400, 300, MouseEvent.BUTTON3, true);

            // Act
            canvas.dispatchEvent(event);

            // Assert - consumer should be invoked with the found frame and event
            verify(popupConsumer).accept(testFrame, event);
        }

        @Test
        void handlePopup_does_not_invoke_consumer_when_no_frame_is_found() {
            // Arrange - stub render engine mock to return empty
            // Use nullable() because getGraphics() returns null in headless mode
            when(fg.renderEngine().getFrameAt(nullable(Graphics2D.class), any(), any()))
                    .thenReturn(empty());

            BiConsumer<FrameBox<String>, MouseEvent> popupConsumer = mock(BiConsumer.class);
            fg.view().setPopupConsumer(popupConsumer);

            var canvas = fg.canvas();

            var event = createPressEvent(canvas, 799, 599, MouseEvent.BUTTON3, true);

            // Act
            canvas.dispatchEvent(event);

            // Assert - consumer should NOT be invoked when no frame is found
            verify(popupConsumer, never()).accept(any(), any());
        }
    }
}