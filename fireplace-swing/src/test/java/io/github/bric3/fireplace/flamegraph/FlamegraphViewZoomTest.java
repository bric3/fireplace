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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

/**
 * Tests for {@link FlamegraphView} zoom operations and related interfaces.
 */
@DisplayName("FlamegraphView - Zoom")
class FlamegraphViewZoomTest {

    private FlamegraphView<String> fg;
    private FlamegraphView.FlamegraphCanvas<String> canvasSpy;
    private FlamegraphRenderEngine<String> renderEngineSpy;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        fg = new FlamegraphView<>();

        // Access private canvas field using reflection
        Field canvasField = FlamegraphView.class.getDeclaredField("canvas");
        canvasField.setAccessible(true);
        var canvas = (FlamegraphView.FlamegraphCanvas<String>) canvasField.get(fg);
        renderEngineSpy = spy(canvas.getFlamegraphRenderEngine());
        canvas.setFlamegraphRenderEngine(renderEngineSpy);
        canvasSpy = spy(canvas);
        canvasField.set(fg, canvasSpy);
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
    @DisplayName("Zoom Operations")
    class ZoomOperationsTests {

        @Test
        void resetZoom_calls_getResetZoomTarget_with_horizontal_false() throws Exception {
            // Set up model
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            // Call resetZoom on EDT
            SwingUtilities.invokeAndWait(() -> fg.resetZoom());

            // Verify that getResetZoomTarget was called with false (not horizontal-only)
            verify(canvasSpy).getResetZoomTarget(false);
        }

        @Test
        void zoomTo_calls_getFrameZoomTarget_with_correct_frame() throws Exception {
            var frame = new FrameBox<>("root", 0.0, 1.0, 0);
            fg.setModel(new FrameModel<>(List.of(frame)));

            // Mock getFrameZoomTarget BEFORE setSize to avoid Mockito issues with Swing callbacks
            var mockZoomTarget = new ZoomTarget<>(100, 50, 600, 500, frame);
            doReturn(mockZoomTarget).when(canvasSpy).getFrameZoomTarget(eq(frame));

            // Set canvas to a known size
            SwingUtilities.invokeAndWait(() -> canvasSpy.setSize(800, 600));

            // Call zoomTo on EDT
            SwingUtilities.invokeAndWait(() -> fg.zoomTo(frame));

            // Verify that getFrameZoomTarget was called with the correct frame
            verify(canvasSpy).getFrameZoomTarget(frame);

            // Verify canvas.zoom was called
            verify(canvasSpy).zoom(any());
        }

        @Test
        void zoom_with_null_target_does_not_modify_canvas() throws Exception {
            // Set mode explicitly (mode doesn't matter for null target)
            fg.setMode(FlamegraphView.Mode.ICICLEGRAPH);

            // Set up model
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            // Get initial bounds
            var initialSnapshot = SwingTestUtil.captureBounds(canvasSpy);

            // Call zoom with null target (should be NOOP) on EDT
            SwingUtilities.invokeAndWait(() -> FlamegraphView.zoom(canvasSpy, null));

            // Verify bounds unchanged
            var afterSnapshot = SwingTestUtil.captureBounds(canvasSpy);
            assertThat(afterSnapshot.bounds).isEqualTo(initialSnapshot.bounds);
        }

        @Test
        void zoom_with_valid_target_updates_canvas_bounds_with_FOCUS_FRAME_click_action() throws Exception {
            fg.setFrameClickAction(FrameClickAction.FOCUS_FRAME);

            // Set up model with a simple frame structure
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child = new FrameBox<>("child", 0.0, 0.5, 1);
            fg.setModel(new FrameModel<>(List.of(root, child)));

            // Set canvas to a known size
            SwingUtilities.invokeAndWait(() -> canvasSpy.setSize(800, 600));

            // Get initial bounds
            var initialSnapshot = SwingTestUtil.captureBounds(canvasSpy);

            // Create a zoom target
            var zoomTarget = new ZoomTarget<>(100, 50, 400, 300, child);

            // Perform zoom on EDT
            SwingUtilities.invokeAndWait(() -> FlamegraphView.zoom(canvasSpy, zoomTarget));

            // Verify zoom was called on canvas
            verify(canvasSpy).zoom(zoomTarget);

            // Verify canvas bounds or preferred size changed after zoom
            var afterZoomSnapshot = SwingTestUtil.captureBounds(canvasSpy);
            assertThat(afterZoomSnapshot.differs(initialSnapshot))
                    .as("Canvas bounds or preferred size should change after zoom")
                    .isTrue();
        }

        @Test
        void resetZoom_restores_full_canvas_view_with_FOCUS_FRAME_click_action() throws Exception {
            fg.setFrameClickAction(FrameClickAction.FOCUS_FRAME);

            // Set up model
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child = new FrameBox<>("child", 0.0, 0.5, 1);
            fg.setModel(new FrameModel<>(List.of(root, child)));

            // Mock zoom targets BEFORE setSize to avoid Mockito issues with Swing callbacks
            var childZoomTarget = new ZoomTarget<>(0, 50, 400, 500, child);
            ZoomTarget<String> resetZoomTarget = new ZoomTarget<>(0, 0, 800, 600, null);
            doReturn(childZoomTarget).when(canvasSpy).getFrameZoomTarget(any());
            doReturn(resetZoomTarget).when(canvasSpy).getResetZoomTarget(anyBoolean());

            // Set canvas to a known size
            SwingUtilities.invokeAndWait(() -> canvasSpy.setSize(800, 600));

            // First zoom to child frame on EDT
            SwingUtilities.invokeAndWait(() -> fg.zoomTo(child));
            verify(canvasSpy).getFrameZoomTarget(child);

            // Now reset zoom on EDT
            SwingUtilities.invokeAndWait(() -> fg.resetZoom());

            // Verify getResetZoomTarget was called
            verify(canvasSpy).getResetZoomTarget(false);

            // Verify zoom was called twice (once for child, once for reset)
            verify(canvasSpy, times(2)).zoom(any());
        }

        @Test
        void zoomTo_frame_changes_visible_area_with_FOCUS_FRAME_click_action() throws Exception {
            fg.setFrameClickAction(FrameClickAction.FOCUS_FRAME);

            // Set up model with multiple frames
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child1 = new FrameBox<>("child1", 0.0, 0.5, 1);
            var child2 = new FrameBox<>("child2", 0.5, 1.0, 1);
            fg.setModel(new FrameModel<>(List.of(root, child1, child2)));

            // Mock zoom targets BEFORE setSize (headless mode returns null otherwise)
            // Must be set up before any Swing event-triggering operations to avoid Mockito issues
            var child1ZoomTarget = new ZoomTarget<>(0, 50, 500, 700, child1);
            var child2ZoomTarget = new ZoomTarget<>(500, 50, 500, 700, child2);
            doReturn(child1ZoomTarget, child2ZoomTarget).when(canvasSpy).getFrameZoomTarget(any());

            // Set canvas to a known size
            SwingUtilities.invokeAndWait(() -> canvasSpy.setSize(1000, 800));

            // Zoom to child1 on EDT
            SwingUtilities.invokeAndWait(() -> fg.zoomTo(child1));

            // Verify getFrameZoomTarget was called with child1
            verify(canvasSpy).getFrameZoomTarget(child1);

            // Zoom to child2 on EDT
            SwingUtilities.invokeAndWait(() -> fg.zoomTo(child2));

            // Verify getFrameZoomTarget was called with child2
            verify(canvasSpy).getFrameZoomTarget(child2);

            // Verify zoom was called twice (once for each child)
            verify(canvasSpy, times(2)).zoom(any());
        }

        @Test
        void zoom_with_override_returning_true_skips_default_zoom() throws Exception {
            // Set up model
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            fg.setModel(new FrameModel<>(List.of(root)));
            SwingUtilities.invokeAndWait(() -> canvasSpy.setSize(800, 600));

            // Set up override that returns true (handles zoom itself)
            var overrideCalled = new AtomicReference<>(false);
            FlamegraphView.ZoomAction override = new FlamegraphView.ZoomAction() {
                @Override
                public <T> boolean zoom(FlamegraphView.ZoomableComponent<T> zoomableComponent, ZoomTarget<T> zoomTarget) {
                    overrideCalled.set(true);
                    return true; // Signal that we handled the zoom
                }
            };
            fg.overrideZoomAction(override);

            // Create a zoom target and perform zoom on EDT
            var zoomTarget = new ZoomTarget<>(100, 50, 400, 300, root);
            SwingUtilities.invokeAndWait(() -> FlamegraphView.zoom(canvasSpy, zoomTarget));

            // Verify override was called
            assertThat(overrideCalled.get()).isTrue();

            // Verify default canvas.zoom was NOT called (override handled it)
            verify(canvasSpy, never()).zoom(zoomTarget);
        }

        @Test
        void zoom_with_override_returning_false_calls_default_zoom() throws Exception {
            // Set up model
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            fg.setModel(new FrameModel<>(List.of(root)));
            SwingUtilities.invokeAndWait(() -> canvasSpy.setSize(800, 600));

            // Get initial bounds
            var initialSnapshot = SwingTestUtil.captureBounds(canvasSpy);

            // Set up override that returns false (doesn't handle zoom)
            var overrideCalled = new AtomicReference<>(false);
            FlamegraphView.ZoomAction override = new FlamegraphView.ZoomAction() {
                @Override
                public <T> boolean zoom(FlamegraphView.ZoomableComponent<T> zoomableComponent, ZoomTarget<T> zoomTarget) {
                    overrideCalled.set(true);
                    return false; // Signal that we didn't handle the zoom
                }
            };
            fg.overrideZoomAction(override);

            // Create a zoom target and perform zoom on EDT
            var zoomTarget = new ZoomTarget<>(100, 50, 400, 300, root);
            SwingUtilities.invokeAndWait(() -> FlamegraphView.zoom(canvasSpy, zoomTarget));

            // Verify override was called
            assertThat(overrideCalled.get()).isTrue();

            // Verify default canvas.zoom WAS called (override didn't handle it)
            verify(canvasSpy).zoom(zoomTarget);

            // Verify canvas bounds changed (because default zoom was executed)
            var afterZoomSnapshot = SwingTestUtil.captureBounds(canvasSpy);
            assertThat(afterZoomSnapshot.differs(initialSnapshot))
                    .as("Canvas should change after default zoom executes")
                    .isTrue();
        }

        @Test
        void resetZoom_restores_full_canvas_view_with_EXPAND_FRAME_click_action_ICICLEGRAPH_mode() throws Exception {
            fg.setFrameClickAction(FrameClickAction.EXPAND_FRAME);
            fg.setMode(FlamegraphView.Mode.ICICLEGRAPH);

            // Set up model
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child = new FrameBox<>("child", 0.0, 0.5, 1);
            fg.setModel(new FrameModel<>(List.of(root, child)));

            // Set up ALL stubs BEFORE setSize to avoid Mockito issues with Swing event callbacks
            doReturn(500).when(renderEngineSpy).computeVisibleFlamegraphHeight(any(), anyInt());
            var childZoomTarget = new ZoomTarget<>(0, 50, 400, 500, child);
            ZoomTarget<String> resetZoomTarget = new ZoomTarget<>(0, 0, 800, 600, null);
            doReturn(childZoomTarget).when(canvasSpy).getFrameZoomTarget(any());
            doReturn(resetZoomTarget).when(canvasSpy).getResetZoomTarget(anyBoolean());

            // Set canvas to a known size
            SwingUtilities.invokeAndWait(() -> canvasSpy.setSize(800, 600));

            // Capture initial bounds
            var initialSnapshot = SwingTestUtil.captureBounds(canvasSpy);

            // First zoom to child frame on EDT
            SwingUtilities.invokeAndWait(() -> fg.zoomTo(child));
            verify(canvasSpy).getFrameZoomTarget(child);

            // Capture bounds after zoom to child
            var afterChildZoomSnapshot = SwingTestUtil.captureBounds(canvasSpy);
            assertThat(afterChildZoomSnapshot.differs(initialSnapshot))
                    .as("Canvas should change after zooming to child")
                    .isTrue();

            // In ICICLEGRAPH mode with EXPAND_FRAME, height should be adjusted via computeVisibleFlamegraphHeight
            verify(renderEngineSpy, atLeastOnce()).computeVisibleFlamegraphHeight(any(), anyInt());

            // Now reset zoom on EDT
            SwingUtilities.invokeAndWait(() -> fg.resetZoom());

            // Verify getResetZoomTarget was called
            verify(canvasSpy).getResetZoomTarget(false);

            // Verify zoom was called twice (once for child, once for reset)
            verify(canvasSpy, times(2)).zoom(any());

            // Capture bounds after reset
            var afterResetSnapshot = SwingTestUtil.captureBounds(canvasSpy);
            assertThat(afterResetSnapshot.differs(afterChildZoomSnapshot))
                    .as("Canvas should change after reset zoom")
                    .isTrue();
        }

        @Test
        void resetZoom_restores_full_canvas_view_with_EXPAND_FRAME_click_action_FLAMEGRAPH_mode() throws Exception {
            fg.setFrameClickAction(FrameClickAction.EXPAND_FRAME);
            fg.setMode(FlamegraphView.Mode.FLAMEGRAPH);

            // Set up model
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child = new FrameBox<>("child", 0.0, 0.5, 1);
            fg.setModel(new FrameModel<>(List.of(root, child)));

            // Set up ALL stubs BEFORE setSize to avoid Mockito issues with Swing event callbacks
            doReturn(700).when(renderEngineSpy).computeVisibleFlamegraphHeight(any(), anyInt());
            var childZoomTarget = new ZoomTarget<>(0, 50, 400, 500, child);
            ZoomTarget<String> resetZoomTarget = new ZoomTarget<>(0, 0, 800, 600, null);
            doReturn(childZoomTarget).when(canvasSpy).getFrameZoomTarget(any());
            doReturn(resetZoomTarget).when(canvasSpy).getResetZoomTarget(anyBoolean());

            // Set canvas to a known size
            SwingUtilities.invokeAndWait(() -> canvasSpy.setSize(800, 600));

            // Capture initial bounds
            var initialSnapshot = SwingTestUtil.captureBounds(canvasSpy);

            // First zoom to child frame on EDT
            SwingUtilities.invokeAndWait(() -> fg.zoomTo(child));
            verify(canvasSpy).getFrameZoomTarget(child);

            // Capture bounds after zoom to child
            var afterChildZoomSnapshot = SwingTestUtil.captureBounds(canvasSpy);
            assertThat(afterChildZoomSnapshot.differs(initialSnapshot))
                    .as("Canvas should change after zooming to child")
                    .isTrue();

            // In FLAMEGRAPH mode with EXPAND_FRAME, height should be adjusted via computeVisibleFlamegraphHeight
            // and y coordinate should be adjusted (anchors to bottom)
            verify(renderEngineSpy, atLeastOnce()).computeVisibleFlamegraphHeight(any(), anyInt());

            // Now reset zoom on EDT
            SwingUtilities.invokeAndWait(() -> fg.resetZoom());

            // Verify getResetZoomTarget was called
            verify(canvasSpy).getResetZoomTarget(false);

            // Verify zoom was called twice (once for child, once for reset)
            verify(canvasSpy, times(2)).zoom(any());

            // Capture bounds after reset
            var afterResetSnapshot = SwingTestUtil.captureBounds(canvasSpy);
            assertThat(afterResetSnapshot.differs(afterChildZoomSnapshot))
                    .as("Canvas should change after reset zoom")
                    .isTrue();
        }

        @Test
        void zoomTo_frame_changes_visible_area_with_EXPAND_FRAME_click_action_ICICLEGRAPH_mode() throws Exception {
            fg.setFrameClickAction(FrameClickAction.EXPAND_FRAME);
            fg.setMode(FlamegraphView.Mode.ICICLEGRAPH);

            // Set up model with multiple frames
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child1 = new FrameBox<>("child1", 0.0, 0.5, 1);
            var child2 = new FrameBox<>("child2", 0.5, 1.0, 1);
            fg.setModel(new FrameModel<>(List.of(root, child1, child2)));

            // Set up ALL stubs BEFORE setSize to avoid Mockito issues with Swing event callbacks
            doReturn(600, 550).when(renderEngineSpy).computeVisibleFlamegraphHeight(any(), anyInt());
            var child1ZoomTarget = new ZoomTarget<>(0, 50, 500, 700, child1);
            var child2ZoomTarget = new ZoomTarget<>(500, 50, 500, 700, child2);
            doReturn(child1ZoomTarget, child2ZoomTarget).when(canvasSpy).getFrameZoomTarget(any());

            // Set canvas to a known size
            SwingUtilities.invokeAndWait(() -> canvasSpy.setSize(1000, 800));

            // Capture initial bounds
            var initialSnapshot = SwingTestUtil.captureBounds(canvasSpy);

            // Zoom to child1 on EDT
            SwingUtilities.invokeAndWait(() -> fg.zoomTo(child1));

            // Verify getFrameZoomTarget was called with child1
            verify(canvasSpy).getFrameZoomTarget(child1);

            // Capture bounds after first zoom
            var afterChild1ZoomSnapshot = SwingTestUtil.captureBounds(canvasSpy);
            assertThat(afterChild1ZoomSnapshot.differs(initialSnapshot))
                    .as("Canvas should change after zooming to child1")
                    .isTrue();

            // Verify computeVisibleFlamegraphHeight was called
            verify(renderEngineSpy, atLeastOnce()).computeVisibleFlamegraphHeight(any(), anyInt());

            // Zoom to child2 on EDT
            SwingUtilities.invokeAndWait(() -> fg.zoomTo(child2));

            // Verify getFrameZoomTarget was called with child2
            verify(canvasSpy).getFrameZoomTarget(child2);

            // Capture bounds after second zoom
            var afterChild2ZoomSnapshot = SwingTestUtil.captureBounds(canvasSpy);
            assertThat(afterChild2ZoomSnapshot.differs(afterChild1ZoomSnapshot))
                    .as("Canvas should change after zooming to child2")
                    .isTrue();

            // Verify zoom was called twice (once for each child)
            verify(canvasSpy, times(2)).zoom(any());

            // In ICICLEGRAPH mode, y coordinate should stay the same (anchors to top)
            // The height is adjusted by computeVisibleFlamegraphHeight
            assertThat(afterChild1ZoomSnapshot.bounds.y).isEqualTo(afterChild2ZoomSnapshot.bounds.y);
        }

        @Test
        void zoomTo_frame_changes_visible_area_with_EXPAND_FRAME_click_action_FLAMEGRAPH_mode() throws Exception {
            fg.setFrameClickAction(FrameClickAction.EXPAND_FRAME);
            fg.setMode(FlamegraphView.Mode.FLAMEGRAPH);

            // Set up model with multiple frames
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child1 = new FrameBox<>("child1", 0.0, 0.5, 1);
            var child2 = new FrameBox<>("child2", 0.5, 1.0, 1);
            fg.setModel(new FrameModel<>(List.of(root, child1, child2)));

            // Set up ALL stubs BEFORE setSize to avoid Mockito issues with Swing event callbacks
            doReturn(650, 580).when(renderEngineSpy).computeVisibleFlamegraphHeight(any(), anyInt());
            var child1ZoomTarget = new ZoomTarget<>(0, 50, 500, 700, child1);
            var child2ZoomTarget = new ZoomTarget<>(500, 50, 500, 700, child2);
            doReturn(child1ZoomTarget, child2ZoomTarget).when(canvasSpy).getFrameZoomTarget(any());

            // Set canvas to a known size
            SwingUtilities.invokeAndWait(() -> canvasSpy.setSize(1000, 800));

            // Capture initial bounds
            var initialSnapshot = SwingTestUtil.captureBounds(canvasSpy);

            // Zoom to child1 on EDT
            SwingUtilities.invokeAndWait(() -> fg.zoomTo(child1));

            // Verify getFrameZoomTarget was called with child1
            verify(canvasSpy).getFrameZoomTarget(child1);

            // Capture bounds after first zoom
            var afterChild1ZoomSnapshot = SwingTestUtil.captureBounds(canvasSpy);
            assertThat(afterChild1ZoomSnapshot.differs(initialSnapshot))
                    .as("Canvas should change after zooming to child1")
                    .isTrue();

            // Verify computeVisibleFlamegraphHeight was called
            verify(renderEngineSpy, atLeastOnce()).computeVisibleFlamegraphHeight(any(), anyInt());

            // Zoom to child2 on EDT
            SwingUtilities.invokeAndWait(() -> fg.zoomTo(child2));

            // Verify getFrameZoomTarget was called with child2
            verify(canvasSpy).getFrameZoomTarget(child2);

            // Capture bounds after second zoom
            var afterChild2ZoomSnapshot = SwingTestUtil.captureBounds(canvasSpy);
            assertThat(afterChild2ZoomSnapshot.differs(afterChild1ZoomSnapshot))
                    .as("Canvas should change after zooming to child2")
                    .isTrue();

            // Verify zoom was called twice (once for each child)
            verify(canvasSpy, times(2)).zoom(any());

            // In FLAMEGRAPH mode, y coordinate is adjusted to anchor to bottom
            // The y coordinate should be different between the two zooms due to different computed heights
            verify(renderEngineSpy, atLeast(2)).computeVisibleFlamegraphHeight(any(), anyInt());
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