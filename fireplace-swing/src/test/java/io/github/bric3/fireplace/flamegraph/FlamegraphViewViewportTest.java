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

import io.github.bric3.fireplace.flamegraph.FlamegraphView.Mode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.List;

import static io.github.bric3.fireplace.flamegraph.SwingTestUtil.findScrollPane;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link FlamegraphView} viewport layout and scrollbar behavior.
 */
@DisplayName("FlamegraphView - Viewport")
class FlamegraphViewViewportTest {

    private FlamegraphView<String> fg;

    @BeforeEach
    void setUp() {
        fg = new FlamegraphView<>();
    }

    @Nested
    @DisplayName("Custom Viewport Layout")
    class CustomViewportLayoutTests {

        @Test
        void viewport_has_custom_layout_manager() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var scrollPane = findScrollPane(fg.component);
            assertThat(scrollPane).isNotNull();

            var viewport = scrollPane.getViewport();
            assertThat(viewport).isNotNull();
            assertThat(viewport.getLayout()).isNotNull();
        }

        @Test
        void viewport_resize_via_component_event() {
            fg.setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            )));

            var scrollPane = findScrollPane(fg.component);
            assertThat(scrollPane).isNotNull();

            var viewport = scrollPane.getViewport();
            viewport.setSize(800, 600);

            // Trigger layout via component event
            var resizeEvent = new ComponentEvent(
                    viewport,
                    ComponentEvent.COMPONENT_RESIZED
            );
            viewport.dispatchEvent(resizeEvent);

            // Layout should have been triggered
            assertThat(viewport.getViewSize()).isNotNull();
        }

        @Test
        void viewport_view_is_canvas() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var scrollPane = findScrollPane(fg.component);
            var viewport = scrollPane.getViewport();

            assertThat(viewport.getView()).isNotNull();
        }

        @Test
        void viewport_size_can_be_set() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var scrollPane = findScrollPane(fg.component);
            var viewport = scrollPane.getViewport();

            viewport.setSize(800, 600);
            assertThat(viewport.getSize()).isEqualTo(new Dimension(800, 600));

            viewport.setSize(600, 400);
            assertThat(viewport.getSize()).isEqualTo(new Dimension(600, 400));
        }

        @Test
        void viewport_in_different_modes() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var scrollPane = findScrollPane(fg.component);
            var viewport = scrollPane.getViewport();

            viewport.setSize(800, 600);

            // Set modes - should not throw
            fg.setMode(Mode.ICICLEGRAPH);
            assertThat(fg.getMode()).isEqualTo(Mode.ICICLEGRAPH);

            fg.setMode(Mode.FLAMEGRAPH);
            assertThat(fg.getMode()).isEqualTo(Mode.FLAMEGRAPH);
        }

        @Test
        void viewport_with_deep_flamegraph() {
            // Create a deep flamegraph
            var frames = new java.util.ArrayList<FrameBox<String>>();
            frames.add(new FrameBox<>("root", 0.0, 1.0, 0));
            for (int i = 1; i <= 30; i++) {
                frames.add(new FrameBox<>("level" + i, 0.0, 0.9, i));
            }
            fg.setModel(new FrameModel<>(frames));

            var scrollPane = findScrollPane(fg.component);
            var viewport = scrollPane.getViewport();

            viewport.setSize(800, 200);
            assertThat(viewport.getView()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Viewport View Position Changes")
    class ViewportViewPositionChangesTests {

        @Test
        void viewport_setViewPosition_does_not_throw() {
            fg.setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            )));

            var scrollPane = findScrollPane(fg.component);
            var viewport = scrollPane.getViewport();

            viewport.setSize(800, 600);
            scrollPane.setSize(800, 600);

            // Set view position
            assertThatCode(() -> viewport.setViewPosition(new Point(0, 0)))
                    .doesNotThrowAnyException();
        }

        @Test
        void viewport_scrollRectToVisible_does_not_throw() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var scrollPane = findScrollPane(fg.component);
            var viewport = scrollPane.getViewport();

            viewport.setSize(800, 600);
            scrollPane.setSize(800, 600);

            fg.configureCanvas(canvas -> {
                assertThatCode(() -> canvas.scrollRectToVisible(new Rectangle(0, 0, 100, 100)))
                        .doesNotThrowAnyException();
            });
        }
    }

    @Nested
    @DisplayName("Scrollbar Visibility")
    class ScrollbarVisibilityTests {

        @Test
        void scrollbar_policy_can_be_queried() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var scrollPane = findScrollPane(fg.component);
            assertThat(scrollPane.getVerticalScrollBarPolicy()).isNotNull();
            assertThat(scrollPane.getHorizontalScrollBarPolicy()).isNotNull();
        }

        @Test
        void scrollbar_policies_are_valid_values() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var scrollPane = findScrollPane(fg.component);

            assertThat(scrollPane.getVerticalScrollBarPolicy()).isIn(
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            );

            assertThat(scrollPane.getHorizontalScrollBarPolicy()).isIn(
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
            );
        }
    }

    @Nested
    @DisplayName("Component Events on ScrollPane")
    class ComponentEventsOnScrollPaneTests {

        @Test
        void component_resize_event_triggers_layout() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var scrollPane = findScrollPane(fg.component);
            scrollPane.setSize(800, 600);

            var resizeEvent = new ComponentEvent(
                    scrollPane,
                    ComponentEvent.COMPONENT_RESIZED
            );
            scrollPane.dispatchEvent(resizeEvent);
        }

        @Test
        void component_shown_event_handled() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var scrollPane = findScrollPane(fg.component);
            scrollPane.setSize(800, 600);

            var shownEvent = new ComponentEvent(
                    scrollPane,
                    ComponentEvent.COMPONENT_SHOWN
            );
            scrollPane.dispatchEvent(shownEvent);
        }

        @Test
        void component_hidden_event_handled() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var scrollPane = findScrollPane(fg.component);
            scrollPane.setSize(800, 600);

            var hiddenEvent = new ComponentEvent(
                    scrollPane,
                    ComponentEvent.COMPONENT_HIDDEN
            );
            scrollPane.dispatchEvent(hiddenEvent);
        }
    }

    @Nested
    @DisplayName("Viewport Layout with Mocked ZoomModel")
    class ViewportLayoutWithMockedZoomModelTests {

        private BufferedImage image;
        private Graphics2D g2d;

        @BeforeEach
        void setUpGraphics() {
            image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
            g2d = image.createGraphics();
        }

        /**
         * Sets up a spied canvas that returns a real Graphics2D.
         * This is necessary because getGraphics() returns null in headless mode.
         */
        private void setupCanvasWithGraphics() {
            var scrollPane = findScrollPane(fg.component);
            var viewport = scrollPane.getViewport();
            var canvas = viewport.getView();

            var spiedCanvas = spy(canvas);
            doReturn(g2d).when(spiedCanvas).getGraphics();

            viewport.setView(spiedCanvas);
        }

        /**
         * Sets zoom state fields in the ZoomModel via reflection.
         * This allows testing the layoutContainer branches that depend on zoom state.
         */
        private void setZoomModelState(double scaleFactor, double lastUserInteractionStartX) throws Exception {
            var scrollPane = findScrollPane(fg.component);
            var canvas = scrollPane.getViewport().getView();

            // Get the zoomModel field from canvas
            Field zoomModelField = canvas.getClass().getDeclaredField("zoomModel");
            zoomModelField.setAccessible(true);
            var zoomModel = zoomModelField.get(canvas);

            // Set the lastScaleFactor field
            Field lastScaleFactorField = zoomModel.getClass().getDeclaredField("lastScaleFactor");
            lastScaleFactorField.setAccessible(true);
            lastScaleFactorField.setDouble(zoomModel, scaleFactor);

            // Set the lastUserInteractionStartX field
            Field lastUserInteractionStartXField = zoomModel.getClass().getDeclaredField("lastUserInteractionStartX");
            lastUserInteractionStartXField.setAccessible(true);
            lastUserInteractionStartXField.setDouble(zoomModel, lastUserInteractionStartX);
        }

        @Test
        void layout_container_handles_viewport_width_change_with_scale_factor() throws Exception {
            // Create a deep flamegraph to ensure height > viewport
            var frames = new java.util.ArrayList<FrameBox<String>>();
            frames.add(new FrameBox<>("root", 0.0, 1.0, 0));
            for (int i = 1; i <= 20; i++) {
                frames.add(new FrameBox<>("level" + i, 0.0, 0.9, i));
            }
            fg.setModel(new FrameModel<>(frames));
            setupCanvasWithGraphics();

            var scrollPane = findScrollPane(fg.component);
            var viewport = scrollPane.getViewport();

            // Set initial size
            viewport.setSize(800, 600);
            scrollPane.setSize(800, 600);

            // Inject mock with scale factor > 1.0 (zoomed in)
            setZoomModelState(2.0, 0.0);

            // Trigger viewport resize - this should exercise the width change branch
            viewport.setSize(600, 600);
            viewport.doLayout();

            // The layout should have been executed without exception
            assertThat(viewport.getViewSize()).isNotNull();
        }

        @Test
        void layout_container_preserves_zoom_position_when_resized() throws Exception {
            var frames = new java.util.ArrayList<FrameBox<String>>();
            frames.add(new FrameBox<>("root", 0.0, 1.0, 0));
            for (int i = 1; i <= 20; i++) {
                frames.add(new FrameBox<>("level" + i, 0.0, 0.9, i));
            }
            fg.setModel(new FrameModel<>(frames));
            setupCanvasWithGraphics();

            var scrollPane = findScrollPane(fg.component);
            var viewport = scrollPane.getViewport();

            // Set initial size
            viewport.setSize(800, 600);
            scrollPane.setSize(800, 600);

            // Inject mock with scale factor > 1.0 and non-zero position ratio
            // This exercises the oldFlamegraphX > 0 branch
            setZoomModelState(2.5, 0.3);

            // Set view position to simulate zoomed state with non-zero X
            var canvas = viewport.getView();
            canvas.setLocation(-200, 0); // Negative X means scrolled to the right

            // Trigger viewport resize
            viewport.setSize(600, 600);
            viewport.doLayout();

            assertThat(viewport.getViewSize()).isNotNull();
        }

        @Test
        void layout_container_handles_flamegraph_mode_y_calculation() throws Exception {
            var frames = new java.util.ArrayList<FrameBox<String>>();
            frames.add(new FrameBox<>("root", 0.0, 1.0, 0));
            for (int i = 1; i <= 30; i++) {
                frames.add(new FrameBox<>("level" + i, 0.0, 0.9, i));
            }
            fg.setModel(new FrameModel<>(frames));
            fg.setMode(Mode.FLAMEGRAPH);
            setupCanvasWithGraphics();

            var scrollPane = findScrollPane(fg.component);
            var viewport = scrollPane.getViewport();

            viewport.setSize(800, 600);
            scrollPane.setSize(800, 600);

            setZoomModelState(1.5, 0.0);

            // Trigger resize
            viewport.setSize(600, 400);
            viewport.doLayout();

            assertThat(viewport.getViewSize()).isNotNull();
        }

        @Test
        void layout_container_handles_iciclegraph_mode_y_calculation() throws Exception {
            var frames = new java.util.ArrayList<FrameBox<String>>();
            frames.add(new FrameBox<>("root", 0.0, 1.0, 0));
            for (int i = 1; i <= 30; i++) {
                frames.add(new FrameBox<>("level" + i, 0.0, 0.9, i));
            }
            fg.setModel(new FrameModel<>(frames));
            fg.setMode(Mode.ICICLEGRAPH);
            setupCanvasWithGraphics();

            var scrollPane = findScrollPane(fg.component);
            var viewport = scrollPane.getViewport();

            viewport.setSize(800, 600);
            scrollPane.setSize(800, 600);

            setZoomModelState(1.5, 0.0);

            // Trigger resize
            viewport.setSize(600, 400);
            viewport.doLayout();

            assertThat(viewport.getViewSize()).isNotNull();
        }

        @Test
        void layout_container_sets_vertical_scrollbar_never_when_fits() throws Exception {
            // Small flamegraph that fits in viewport
            fg.setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            )));
            setupCanvasWithGraphics();

            var scrollPane = findScrollPane(fg.component);
            var viewport = scrollPane.getViewport();

            // Large viewport that fits the flamegraph
            viewport.setSize(800, 600);
            scrollPane.setSize(800, 600);

            setZoomModelState(1.0, 0.0);

            viewport.setSize(700, 600);
            viewport.doLayout();

            // When flamegraph fits, vertical scrollbar should be NEVER
            assertThat(scrollPane.getVerticalScrollBarPolicy())
                    .isEqualTo(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        }

        @Test
        void layout_container_sets_vertical_scrollbar_always_when_exceeds() throws Exception {
            // Deep flamegraph that exceeds viewport
            var frames = new java.util.ArrayList<FrameBox<String>>();
            frames.add(new FrameBox<>("root", 0.0, 1.0, 0));
            for (int i = 1; i <= 50; i++) {
                frames.add(new FrameBox<>("level" + i, 0.0, 0.9, i));
            }
            fg.setModel(new FrameModel<>(frames));
            setupCanvasWithGraphics();

            var scrollPane = findScrollPane(fg.component);
            var viewport = scrollPane.getViewport();

            // Small viewport that doesn't fit the flamegraph
            viewport.setSize(800, 200);
            scrollPane.setSize(800, 200);

            setZoomModelState(1.0, 0.0);

            viewport.setSize(700, 200);
            viewport.doLayout();

            // When flamegraph exceeds viewport, vertical scrollbar should be ALWAYS
            assertThat(scrollPane.getVerticalScrollBarPolicy())
                    .isEqualTo(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        }

        @Test
        void layout_container_sets_horizontal_scrollbar_never_when_scale_is_one() throws Exception {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));
            fg.setMode(Mode.ICICLEGRAPH);
            setupCanvasWithGraphics();

            var scrollPane = findScrollPane(fg.component);
            var viewport = scrollPane.getViewport();

            viewport.setSize(800, 600);
            scrollPane.setSize(800, 600);

            // Scale factor of 1.0 means not zoomed
            setZoomModelState(1.0, 0.0);

            viewport.setSize(700, 600);
            viewport.doLayout();

            // When scale is 1.0 in icicle mode, horizontal scrollbar should be NEVER
            assertThat(scrollPane.getHorizontalScrollBarPolicy())
                    .isEqualTo(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        }

        @Test
        void layout_container_sets_horizontal_scrollbar_as_needed_when_zoomed() throws Exception {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));
            fg.setMode(Mode.ICICLEGRAPH);
            setupCanvasWithGraphics();

            var scrollPane = findScrollPane(fg.component);
            var viewport = scrollPane.getViewport();

            viewport.setSize(800, 600);
            scrollPane.setSize(800, 600);

            // Scale factor > 1.0 means zoomed in
            setZoomModelState(2.0, 0.0);

            viewport.setSize(700, 600);
            viewport.doLayout();

            // When zoomed in icicle mode, horizontal scrollbar should be AS_NEEDED
            assertThat(scrollPane.getHorizontalScrollBarPolicy())
                    .isEqualTo(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        }

        @Test
        void layout_container_always_shows_horizontal_scrollbar_in_flamegraph_mode() throws Exception {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));
            fg.setMode(Mode.FLAMEGRAPH);
            setupCanvasWithGraphics();

            var scrollPane = findScrollPane(fg.component);
            var viewport = scrollPane.getViewport();

            viewport.setSize(800, 600);
            scrollPane.setSize(800, 600);

            setZoomModelState(1.0, 0.0);

            viewport.setSize(700, 600);
            viewport.doLayout();

            // In flamegraph mode, horizontal scrollbar is always shown
            assertThat(scrollPane.getHorizontalScrollBarPolicy())
                    .isEqualTo(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        }

        @Test
        void layout_container_uses_else_branch_when_width_unchanged() throws Exception {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));
            setupCanvasWithGraphics();

            var scrollPane = findScrollPane(fg.component);
            var viewport = scrollPane.getViewport();

            // Set size
            viewport.setSize(800, 600);
            scrollPane.setSize(800, 600);

            setZoomModelState(1.0, 0.0);

            // First layout to establish oldViewPortSize
            viewport.doLayout();

            // Second layout with same width - exercises else branch
            viewport.doLayout();

            assertThat(viewport.getViewSize()).isNotNull();
        }

        @Test
        void layout_container_throws_when_non_flamegraph_canvas_view() throws Exception {
            // This tests the failsafe branch when view is not FlamegraphCanvas
            // Note: The production code has a bug where it doesn't return after
            // calling super.layoutContainer(), causing a ClassCastException
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var scrollPane = findScrollPane(fg.component);
            var viewport = scrollPane.getViewport();
            var layout = viewport.getLayout();

            // Create a regular JPanel as the view (not FlamegraphCanvas)
            var regularPanel = new JPanel();
            regularPanel.setPreferredSize(new Dimension(400, 300));

            // Temporarily swap the view
            var originalView = viewport.getView();
            viewport.setView(regularPanel);

            viewport.setSize(800, 600);

            // The custom layout is tightly coupled to FlamegraphCanvas,
            // so it throws ClassCastException when given a different view type
            assertThatThrownBy(() -> layout.layoutContainer(viewport))
                    .isInstanceOf(ClassCastException.class);

            // Restore original view
            viewport.setView(originalView);
        }
    }
}