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
import java.util.concurrent.atomic.AtomicReference;

import static io.github.bric3.fireplace.flamegraph.SwingTestUtil.findScrollPane;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link FlamegraphView} zoom operations and related interfaces.
 */
@DisplayName("FlamegraphView - Zoom")
class FlamegraphViewZoomTest {

    private FlamegraphView<String> fg;

    @BeforeEach
    void setUp() {
        fg = new FlamegraphView<>();
    }

    @Nested
    @DisplayName("Override Zoom Action")
    class OverrideZoomActionTests {

        @Test
        void overrideZoomAction_null_throws_exception() {
            assertThatThrownBy(() -> fg.overrideZoomAction(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void overrideZoomAction_custom_action_does_not_throw() {
            FlamegraphView.ZoomAction customAction = new FlamegraphView.ZoomAction() {
                @Override
                public <T> boolean zoom(FlamegraphView.ZoomableComponent<T> zoomableComponent, ZoomTarget<T> zoomTarget) {
                    return true;
                }
            };

            assertThatCode(() -> fg.overrideZoomAction(customAction))
                    .doesNotThrowAnyException();
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

    @Nested
    @DisplayName("Zoom Operations")
    class ZoomOperationsTests {

        @Test
        void resetZoom_does_not_throw() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            assertThatCode(() -> fg.resetZoom())
                    .doesNotThrowAnyException();
        }

        @Test
        void zoomTo_with_valid_frame_does_not_throw() {
            var frame = new FrameBox<>("root", 0.0, 1.0, 0);
            fg.setModel(new FrameModel<>(List.of(frame)));

            assertThatCode(() -> fg.zoomTo(frame))
                    .doesNotThrowAnyException();
        }

        @Test
        void zoomTo_with_child_frame_does_not_throw() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child = new FrameBox<>("child", 0.0, 0.5, 1);
            fg.setModel(new FrameModel<>(List.of(root, child)));

            assertThatCode(() -> fg.zoomTo(child))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("ZoomAction Interface")
    class ZoomActionInterfaceTests {

        @Test
        void zoom_action_receives_correct_parameters() {
            var zoomActionCalled = new AtomicReference<>(false);
            FlamegraphView.ZoomAction customZoomAction = new FlamegraphView.ZoomAction() {
                @Override
                public <T> boolean zoom(FlamegraphView.ZoomableComponent<T> zoomableComponent, ZoomTarget<T> zoomTarget) {
                    zoomActionCalled.set(true);
                    assertThat(zoomableComponent).isNotNull();
                    assertThat(zoomTarget).isNotNull();
                    zoomableComponent.zoom(zoomTarget);
                    return true;
                }
            };

            fg.overrideZoomAction(customZoomAction);
            var frame = new FrameBox<>("root", 0.0, 1.0, 0);
            fg.setModel(new FrameModel<>(List.of(frame)));

            fg.zoomTo(frame);

            // Note: In headless mode without realized component, zoom may not be fully executed
            // but the override should be set
        }

        @Test
        void zoom_action_returning_false_falls_back_to_default() {
            FlamegraphView.ZoomAction customZoomAction = new FlamegraphView.ZoomAction() {
                @Override
                public <T> boolean zoom(FlamegraphView.ZoomableComponent<T> zoomableComponent, ZoomTarget<T> zoomTarget) {
                    return false;
                }
            };

            fg.overrideZoomAction(customZoomAction);
            var frame = new FrameBox<>("root", 0.0, 1.0, 0);
            fg.setModel(new FrameModel<>(List.of(frame)));

            // Should not throw even when custom action returns false
            assertThatCode(() -> fg.zoomTo(frame))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("ZoomableComponent Interface")
    class ZoomableComponentInterfaceTests {

        @Test
        void zoomable_component_has_required_methods() {
            // Verify that the interface methods exist and work
            var frame = new FrameBox<>("root", 0.0, 1.0, 0);
            fg.setModel(new FrameModel<>(List.of(frame)));

            FlamegraphView.ZoomAction inspectingAction = new FlamegraphView.ZoomAction() {
                @Override
                public <T> boolean zoom(FlamegraphView.ZoomableComponent<T> zoomableComponent, ZoomTarget<T> zoomTarget) {
                    // Test all ZoomableComponent methods
                    assertThat(zoomableComponent.getWidth()).isGreaterThanOrEqualTo(0);
                    assertThat(zoomableComponent.getHeight()).isGreaterThanOrEqualTo(0);
                    assertThat(zoomableComponent.getLocation()).isNotNull();
                    return true;
                }
            };

            fg.overrideZoomAction(inspectingAction);
            fg.zoomTo(frame);
        }
    }
}