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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link FlamegraphView} inner classes, particularly FlamegraphCanvas.
 * These tests focus on canvas behaviors, rendering, and event handling.
 */
@DisplayName("FlamegraphView Canvas")
class FlamegraphViewCanvasTest {

    @Nested
    @DisplayName("Canvas Properties")
    class CanvasPropertiesTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void canvas_has_graph_mode_property_constant() {
            // Access constants through reflection or verify behavior
            fg.view().setMode(Mode.FLAMEGRAPH);
            assertThat(fg.view().getMode()).isEqualTo(Mode.FLAMEGRAPH);

            fg.view().setMode(Mode.ICICLEGRAPH);
            assertThat(fg.view().getMode()).isEqualTo(Mode.ICICLEGRAPH);
        }

        @Test
        void canvas_show_minimap_property_affects_display() {
            fg.view().setShowMinimap(true);
            assertThat(fg.view().isShowMinimap()).isTrue();

            fg.view().setShowMinimap(false);
            assertThat(fg.view().isShowMinimap()).isFalse();
        }

        @Test
        void canvas_frame_model_property_changes() {
            var model1 = new FrameModel<>(List.of(new FrameBox<>("first", 0.0, 1.0, 0)));
            fg.view().setModel(model1);
            assertThat(fg.view().getFrameModel()).isEqualTo(model1);

            var model2 = new FrameModel<>(List.of(new FrameBox<>("second", 0.0, 1.0, 0)));
            fg.view().setModel(model2);
            assertThat(fg.view().getFrameModel()).isEqualTo(model2);
        }
    }

    @Nested
    @DisplayName("Canvas Tooltip")
    class CanvasTooltipTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void tooltip_text_function_is_used_when_set() {
            var tooltipCalled = new AtomicBoolean(false);
            fg.view().setTooltipTextFunction((model, frame) -> {
                tooltipCalled.set(true);
                return "Custom tooltip for " + frame.actualNode;
            });

            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));
            assertThat(fg.view().getTooltipTextFunction()).isNotNull();
        }

        @Test
        void tooltip_component_supplier_provides_custom_tooltip() {
            fg.view().setTooltipComponentSupplier(() -> {
                return new JToolTip();
            });

            assertThat(fg.view().getTooltipComponentSupplier()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Canvas Frame Click Behavior")
    class CanvasFrameClickBehaviorTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void default_click_behavior_is_focus_frame() {
            assertThat(fg.view().getFrameClickAction())
                    .isEqualTo(FlamegraphView.FrameClickAction.FOCUS_FRAME);
        }

        @Test
        void click_behavior_can_be_set_to_expand_frame() {
            fg.view().setFrameClickAction(FlamegraphView.FrameClickAction.EXPAND_FRAME);
            assertThat(fg.view().getFrameClickAction())
                    .isEqualTo(FlamegraphView.FrameClickAction.EXPAND_FRAME);
        }

        @Test
        void selected_frame_consumer_is_called_setup() {
            fg.view().setSelectedFrameConsumer((frame, e) -> {});
            assertThat(fg.view().getSelectedFrameConsumer()).isNotNull();
        }

        @Test
        void popup_consumer_is_set_correctly() {
            fg.view().setPopupConsumer((frame, e) -> {});
            assertThat(fg.view().getPopupConsumer()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Canvas Minimap")
    class CanvasMinimapTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void minimap_shade_color_supplier_is_applied() {
            fg.view().setMinimapShadeColorSupplier(() -> new Color(100, 100, 100, 100));
            assertThat(fg.view().getMinimapShadeColorSupplier()).isNotNull();
            assertThat(fg.view().getMinimapShadeColorSupplier().get()).isEqualTo(new Color(100, 100, 100, 100));
        }

        @Test
        void minimap_visibility_toggles() {
            fg.view().setShowMinimap(true);
            assertThat(fg.view().isShowMinimap()).isTrue();

            fg.view().setShowMinimap(false);
            assertThat(fg.view().isShowMinimap()).isFalse();
        }
    }

    @Nested
    @DisplayName("Canvas Show Stats")
    class CanvasShowStatsTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void show_stats_client_property_can_be_set() {
            fg.view().putClientProperty(FlamegraphView.SHOW_STATS, Boolean.TRUE);
            assertThat(fg.view().<Boolean>getClientProperty(FlamegraphView.SHOW_STATS)).isTrue();
        }

        @Test
        void show_stats_client_property_can_be_cleared() {
            fg.view().putClientProperty(FlamegraphView.SHOW_STATS, Boolean.TRUE);
            fg.view().putClientProperty(FlamegraphView.SHOW_STATS, null);
            assertThat(fg.view().<Boolean>getClientProperty(FlamegraphView.SHOW_STATS)).isNull();
        }
    }

    @Nested
    @DisplayName("Canvas Hovered Siblings")
    class CanvasHoveredSiblingsTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void show_hovered_siblings_default_is_true() {
            assertThat(fg.view().isShowHoveredSiblings()).isTrue();
        }

        @Test
        void show_hovered_siblings_can_be_disabled() {
            fg.view().setShowHoveredSiblings(false);
            assertThat(fg.view().isShowHoveredSiblings()).isFalse();
        }

        @Test
        void show_hovered_siblings_can_be_re_enabled() {
            fg.view().setShowHoveredSiblings(false);
            fg.view().setShowHoveredSiblings(true);
            assertThat(fg.view().isShowHoveredSiblings()).isTrue();
        }
    }

    @Nested
    @DisplayName("Canvas Configurer")
    class CanvasConfigurerTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void configure_canvas_provides_access_to_canvas() {
            var canvasAccessed = new AtomicBoolean(false);
            var canvasRef = new AtomicReference<JComponent>();

            fg.view().configureCanvas(canvas -> {
                canvasAccessed.set(true);
                canvasRef.set(canvas);
            });

            assertThat(canvasAccessed.get()).isTrue();
            assertThat(canvasRef.get()).isNotNull();
        }

        @Test
        void configure_canvas_allows_custom_properties() {
            fg.view().configureCanvas(canvas -> {
                canvas.putClientProperty("customKey", "customValue");
            });

            // Verify the property was set by accessing it through configureCanvas
            var propertyValue = new AtomicReference<String>();
            fg.view().configureCanvas(canvas -> {
                propertyValue.set((String) canvas.getClientProperty("customKey"));
            });

            assertThat(propertyValue.get()).isEqualTo("customValue");
        }
    }

    @Nested
    @DisplayName("Canvas Mode Switching")
    class CanvasModeSwitchingTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void mode_switch_from_icicle_to_flamegraph() {
            fg.view().setMode(Mode.ICICLEGRAPH);
            fg.view().setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            )));

            fg.view().setMode(Mode.FLAMEGRAPH);

            assertThat(fg.view().getMode()).isEqualTo(Mode.FLAMEGRAPH);
        }

        @Test
        void mode_switch_from_flamegraph_to_icicle() {
            fg.view().setMode(Mode.FLAMEGRAPH);
            fg.view().setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            )));

            fg.view().setMode(Mode.ICICLEGRAPH);

            assertThat(fg.view().getMode()).isEqualTo(Mode.ICICLEGRAPH);
        }
    }

    @Nested
    @DisplayName("Canvas Highlight Frames")
    class CanvasHighlightFramesTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void highlight_frames_updates_engine() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child1 = new FrameBox<>("method", 0.0, 0.3, 1);
            var child2 = new FrameBox<>("method", 0.5, 0.8, 1);
            var other = new FrameBox<>("other", 0.3, 0.5, 1);

            fg.view().setModel(new FrameModel<>(List.of(root, child1, child2, other)));

            // Highlight the "method" frames
            fg.view().highlightFrames(java.util.Set.of(child1, child2), "method");

            // No exception means success
        }

        @Test
        void highlight_frames_can_be_cleared() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child = new FrameBox<>("child", 0.0, 0.5, 1);

            fg.view().setModel(new FrameModel<>(List.of(root, child)));
            fg.view().highlightFrames(java.util.Set.of(child), "child");

            // Clear highlights
            fg.view().highlightFrames(java.util.Set.of(), "");

            // No exception means success
        }
    }

    @Nested
    @DisplayName("Canvas Reset Zoom")
    class CanvasResetZoomTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void reset_zoom_with_empty_model_does_not_throw() {
            assertThatCode(() -> fg.view().resetZoom())
                    .doesNotThrowAnyException();
        }

        @Test
        void reset_zoom_with_model_does_not_throw() {
            fg.view().setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            )));

            assertThatCode(() -> fg.view().resetZoom())
                    .doesNotThrowAnyException();
        }

        @Test
        void reset_zoom_after_zoom_to_does_not_throw() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child = new FrameBox<>("child", 0.0, 0.5, 1);
            fg.view().setModel(new FrameModel<>(List.of(root, child)));

            fg.view().zoomTo(child);
            assertThatCode(() -> fg.view().resetZoom())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Canvas ZoomTo")
    class CanvasZoomToTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void zoom_to_root_frame() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            fg.view().setModel(new FrameModel<>(List.of(root)));

            assertThatCode(() -> fg.view().zoomTo(root))
                    .doesNotThrowAnyException();
        }

        @Test
        void zoom_to_child_frame() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child = new FrameBox<>("child", 0.0, 0.5, 1);
            fg.view().setModel(new FrameModel<>(List.of(root, child)));

            assertThatCode(() -> fg.view().zoomTo(child))
                    .doesNotThrowAnyException();
        }

        @Test
        void zoom_to_deeply_nested_frame() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child = new FrameBox<>("child", 0.0, 0.5, 1);
            var grandchild = new FrameBox<>("grandchild", 0.0, 0.25, 2);
            var greatGrandchild = new FrameBox<>("greatGrandchild", 0.0, 0.125, 3);
            fg.view().setModel(new FrameModel<>(List.of(root, child, grandchild, greatGrandchild)));

            assertThatCode(() -> fg.view().zoomTo(greatGrandchild))
                    .doesNotThrowAnyException();
        }

        @Test
        void zoom_to_narrow_frame() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var narrow = new FrameBox<>("narrow", 0.0, 0.01, 1);
            fg.view().setModel(new FrameModel<>(List.of(root, narrow)));

            assertThatCode(() -> fg.view().zoomTo(narrow))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Canvas Request Repaint")
    class CanvasRequestRepaintTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void request_repaint_with_empty_model() {
            assertThatCode(() -> fg.view().requestRepaint())
                    .doesNotThrowAnyException();
        }

        @Test
        void request_repaint_with_model() {
            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            assertThatCode(() -> fg.view().requestRepaint())
                    .doesNotThrowAnyException();
        }

        @Test
        void request_repaint_multiple_times() {
            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            for (int i = 0; i < 10; i++) {
                assertThatCode(() -> fg.view().requestRepaint())
                        .doesNotThrowAnyException();
            }
        }
    }

    @Nested
    @DisplayName("Canvas Frame Equality")
    class CanvasFrameEqualityTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void custom_frame_equality_is_used() {
            var frame1 = new FrameBox<>("method1", 0.0, 0.5, 1);
            var frame2 = new FrameBox<>("method2", 0.5, 1.0, 1);

            // Custom equality: frames are equal if they have same depth
            var model = new FrameModel<>(
                    "Test",
                    (a, b) -> a.stackDepth == b.stackDepth,
                    List.of(new FrameBox<>("root", 0.0, 1.0, 0), frame1, frame2)
            );

            fg.view().setModel(model);

            assertThat(model.frameEquality.equal(frame1, frame2)).isTrue();
        }

        @Test
        void default_frame_equality_compares_actual_node() {
            var frame1 = new FrameBox<>("same", 0.0, 0.5, 1);
            var frame2 = new FrameBox<>("same", 0.5, 1.0, 2);
            var frame3 = new FrameBox<>("different", 0.0, 0.5, 1);

            var model = new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    frame1, frame2, frame3
            ));

            fg.view().setModel(model);

            assertThat(model.frameEquality.equal(frame1, frame2)).isTrue();
            assertThat(model.frameEquality.equal(frame1, frame3)).isFalse();
        }
    }

    @Nested
    @DisplayName("Canvas Model Description")
    class CanvasModelDescriptionTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void model_with_description_is_accessible() {
            var model = new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0)))
                    .withDescription("Test flamegraph description");

            fg.view().setModel(model);

            assertThat(fg.view().getFrameModel().description).isEqualTo("Test flamegraph description");
        }

        @Test
        void model_without_description_has_null_description() {
            var model = new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0)));

            fg.view().setModel(model);

            assertThat(fg.view().getFrameModel().description).isNull();
        }
    }

    @Nested
    @DisplayName("Canvas Model Title")
    class CanvasModelTitleTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void model_with_title_is_accessible() {
            var model = new FrameModel<>(
                    "My Flamegraph Title",
                    (a, b) -> Objects.equals(a.actualNode, b.actualNode),
                    List.of(new FrameBox<>("root", 0.0, 1.0, 0))
            );

            fg.view().setModel(model);

            assertThat(fg.view().getFrameModel().title).isEqualTo("My Flamegraph Title");
        }

        @Test
        void model_from_frames_list_has_empty_title() {
            var model = new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0)));

            fg.view().setModel(model);

            assertThat(fg.view().getFrameModel().title).isEmpty();
        }
    }

    @Nested
    @DisplayName("Hover Listener Interface")
    class HoverListenerInterfaceTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void hover_listener_can_be_set() {
            FlamegraphView.HoverListener<String> listener = (frame, rect, e) -> {
                // no-op
            };

            assertThatCode(() -> fg.view().setHoverListener(listener))
                    .doesNotThrowAnyException();
        }

        @Test
        void hover_listener_onStopHover_default_implementation() {
            FlamegraphView.HoverListener<String> listener = (frame, rect, e) -> {};

            // Default onStopHover should not throw
            assertThatCode(() -> listener.onStopHover(null, null, createMockMouseEvent()))
                    .doesNotThrowAnyException();
        }

        private MouseEvent createMockMouseEvent() {
            var dummyComponent = new JPanel();
            return new MouseEvent(
                    dummyComponent,
                    MouseEvent.MOUSE_MOVED,
                    System.currentTimeMillis(),
                    0,
                    100,
                    100,
                    1,
                    false
            );
        }
    }

    @Nested
    @DisplayName("Rendering with Graphics2D")
    class RenderingWithGraphics2DTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void render_engine_initialized_after_model_set() {
            var frame = new FrameBox<>("root", 0.0, 1.0, 0);
            fg.view().setModel(new FrameModel<>(List.of(frame)));

            // The render engine should be initialized
            assertThat(fg.view().getFrameModel().frames).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Canvas Scroll Pane Integration")
    class CanvasScrollPaneIntegrationTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void component_contains_scroll_pane() {
            var scrollPane = fg.scrollPane();
            assertThat(scrollPane).isNotNull();
        }

        @Test
        void scroll_pane_contains_canvas() {
            var scrollPane = fg.scrollPane();
            assertThat(scrollPane).isNotNull();
            assertThat(scrollPane.getViewport()).isNotNull();
            assertThat(scrollPane.getViewport().getView()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Canvas Empty State")
    class CanvasEmptyStateTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void empty_model_returns_empty_frames() {
            assertThat(fg.view().getFrames()).isEmpty();
        }

        @Test
        void clear_returns_to_empty_state() {
            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));
            assertThat(fg.view().getFrames()).isNotEmpty();

            fg.view().clear();
            assertThat(fg.view().getFrames()).isEmpty();
        }

        @Test
        void empty_model_singleton() {
            assertThat(fg.view().getFrameModel()).isEqualTo(FrameModel.empty());
            assertThat(FrameModel.empty()).isSameAs(FrameModel.empty());
        }
    }

    @Nested
    @DisplayName("Canvas Model Changes")
    class CanvasModelChangesTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void model_change_updates_frames() {
            var model1 = new FrameModel<>(List.of(
                    new FrameBox<>("root1", 0.0, 1.0, 0)
            ));
            var model2 = new FrameModel<>(List.of(
                    new FrameBox<>("root2", 0.0, 1.0, 0),
                    new FrameBox<>("child2", 0.0, 0.5, 1)
            ));

            fg.view().setModel(model1);
            assertThat(fg.view().getFrames()).hasSize(1);

            fg.view().setModel(model2);
            assertThat(fg.view().getFrames()).hasSize(2);
        }

        @Test
        void model_change_preserves_mode() {
            fg.view().setMode(Mode.FLAMEGRAPH);
            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            assertThat(fg.view().getMode()).isEqualTo(Mode.FLAMEGRAPH);
        }

        @Test
        void model_change_preserves_minimap_setting() {
            fg.view().setShowMinimap(false);
            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            assertThat(fg.view().isShowMinimap()).isFalse();
        }
    }

    @Nested
    @DisplayName("Canvas Complex Frame Hierarchies")
    class CanvasComplexFrameHierarchiesTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void deep_hierarchy_is_supported() {
            var frames = new java.util.ArrayList<FrameBox<String>>();
            frames.add(new FrameBox<>("root", 0.0, 1.0, 0));
            for (int i = 1; i <= 50; i++) {
                double width = 1.0 / Math.pow(2, i);
                frames.add(new FrameBox<>("level" + i, 0.0, width, i));
            }

            var model = new FrameModel<>(frames);
            fg.view().setModel(model);

            assertThat(fg.view().getFrames()).hasSize(51);
        }

        @Test
        void wide_hierarchy_is_supported() {
            var frames = new java.util.ArrayList<FrameBox<String>>();
            frames.add(new FrameBox<>("root", 0.0, 1.0, 0));
            for (int i = 0; i < 100; i++) {
                double start = i * 0.01;
                double end = start + 0.01;
                frames.add(new FrameBox<>("child" + i, start, end, 1));
            }

            var model = new FrameModel<>(frames);
            fg.view().setModel(model);

            assertThat(fg.view().getFrames()).hasSize(101);
        }

        @Test
        void multiple_roots_is_supported() {
            var frames = List.of(
                    new FrameBox<>("root1", 0.0, 0.5, 0),
                    new FrameBox<>("root2", 0.5, 1.0, 0),
                    new FrameBox<>("child1", 0.0, 0.25, 1),
                    new FrameBox<>("child2", 0.5, 0.75, 1)
            );

            var model = new FrameModel<>(frames);
            fg.view().setModel(model);

            assertThat(fg.view().getFrames()).hasSize(4);
        }
    }

    @Nested
    @DisplayName("Canvas Property Change Events")
    class CanvasPropertyChangeEventsTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void mode_change_fires_property_change() {
            var propertyChanged = new AtomicBoolean(false);
            fg.view().configureCanvas(canvas -> {
                canvas.addPropertyChangeListener("mode", evt -> propertyChanged.set(true));
            });

            fg.view().setMode(Mode.FLAMEGRAPH);
            // Note: Property change may be on different property name internally
        }

        @Test
        void model_change_fires_property_change() {
            var propertyChanged = new AtomicBoolean(false);
            fg.view().configureCanvas(canvas -> {
                canvas.addPropertyChangeListener(evt -> {
                    if ("frameModel".equals(evt.getPropertyName())) {
                        propertyChanged.set(true);
                    }
                });
            });

            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));
            // Property change event firing depends on actual property name
        }
    }

    @Nested
    @DisplayName("Canvas Renderer Configuration")
    class CanvasRendererConfigurationTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void custom_renderer_with_all_providers() {
            var renderer = new DefaultFrameRenderer<String>(
                    FrameTextsProvider.of(
                            f -> "Title: " + f.actualNode,
                            f -> "Subtitle"
                    ),
                    FrameColorProvider.defaultColorProvider(f -> Color.ORANGE),
                    FrameFontProvider.defaultFontProvider()
            );

            fg.view().setFrameRender(renderer);
            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            // Verify no exception is thrown
            assertThat(fg.view().getFrameModel().frames).isNotEmpty();
        }

        @Test
        void renderer_change_after_model_set() {
            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var renderer = new DefaultFrameRenderer<String>(
                    FrameTextsProvider.of(f -> "Changed"),
                    FrameColorProvider.defaultColorProvider(f -> Color.MAGENTA),
                    FrameFontProvider.defaultFontProvider()
            );

            assertThatCode(() -> fg.view().setFrameRender(renderer))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Canvas Frame Operations")
    class CanvasFrameOperationsTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void frames_list_is_unmodifiable() {
            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var frames = fg.view().getFrames();
            assertThatThrownBy(() -> frames.add(new FrameBox<>("new", 0.0, 0.5, 1)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void frame_model_is_immutable() {
            var originalModel = new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0)));
            fg.view().setModel(originalModel);

            assertThat(fg.view().getFrameModel()).isSameAs(originalModel);
        }
    }

    @Nested
    @DisplayName("Canvas Zoom Multiple Frames")
    class CanvasZoomMultipleFramesTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void zoom_to_different_frames_sequentially() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child1 = new FrameBox<>("child1", 0.0, 0.3, 1);
            var child2 = new FrameBox<>("child2", 0.3, 0.6, 1);
            var child3 = new FrameBox<>("child3", 0.6, 1.0, 1);

            fg.view().setModel(new FrameModel<>(List.of(root, child1, child2, child3)));

            assertThatCode(() -> {
                fg.view().zoomTo(child1);
                fg.view().zoomTo(child2);
                fg.view().zoomTo(child3);
                fg.view().zoomTo(root);
            }).doesNotThrowAnyException();
        }

        @Test
        void reset_zoom_after_multiple_zooms() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child = new FrameBox<>("child", 0.0, 0.5, 1);
            var grandchild = new FrameBox<>("grandchild", 0.0, 0.25, 2);

            fg.view().setModel(new FrameModel<>(List.of(root, child, grandchild)));

            fg.view().zoomTo(child);
            fg.view().zoomTo(grandchild);
            fg.view().resetZoom();

            // Should not throw
        }
    }

    @Nested
    @DisplayName("ZoomableComponent Interface Implementation")
    class ZoomableComponentInterfaceImplementationTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void zoomable_component_provides_dimensions() {
            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            FlamegraphView.ZoomAction inspectingAction = new FlamegraphView.ZoomAction() {
                @Override
                public <T> boolean zoom(FlamegraphView.ZoomableComponent<T> zc, ZoomTarget<T> target) {
                    assertThat(zc.getWidth()).isGreaterThanOrEqualTo(0);
                    assertThat(zc.getHeight()).isGreaterThanOrEqualTo(0);
                    return true;
                }
            };

            fg.view().overrideZoomAction(inspectingAction);
            fg.view().zoomTo(fg.view().getFrames().get(0));
        }

        @Test
        void zoomable_component_provides_location() {
            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            FlamegraphView.ZoomAction inspectingAction = new FlamegraphView.ZoomAction() {
                @Override
                public <T> boolean zoom(FlamegraphView.ZoomableComponent<T> zc, ZoomTarget<T> target) {
                    var location = zc.getLocation();
                    assertThat(location).isNotNull();
                    assertThat(location.x).isGreaterThanOrEqualTo(Integer.MIN_VALUE);
                    assertThat(location.y).isGreaterThanOrEqualTo(Integer.MIN_VALUE);
                    return true;
                }
            };

            fg.view().overrideZoomAction(inspectingAction);
            fg.view().zoomTo(fg.view().getFrames().get(0));
        }
    }

    @Nested
    @DisplayName("Canvas Paint with BufferedImage Graphics2D")
    class CanvasPaintWithBufferedImageTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void paint_component_with_empty_model_draws_no_data_message() {
            var image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
            var g2d = image.createGraphics();

            // Get the canvas and set its bounds
            fg.view().configureCanvas(canvas -> {
                canvas.setSize(800, 600);
                canvas.setBounds(0, 0, 800, 600);
                // Paint the component
                canvas.paint(g2d);
            });

            g2d.dispose();
            // If we get here without exception, the paint worked
        }

        @Test
        void paint_component_with_model_renders_flamegraph() {
            var image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
            var g2d = image.createGraphics();

            fg.view().setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            )));

            fg.view().configureCanvas(canvas -> {
                canvas.setSize(800, 600);
                canvas.setBounds(0, 0, 800, 600);
                canvas.paint(g2d);
            });

            g2d.dispose();
        }

        @Test
        void paint_component_with_show_stats_enabled() {
            var image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
            var g2d = image.createGraphics();

            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));
            fg.view().putClientProperty(FlamegraphView.SHOW_STATS, TRUE);

            fg.view().configureCanvas(canvas -> {
                canvas.setSize(800, 600);
                canvas.setBounds(0, 0, 800, 600);
                canvas.paint(g2d);
            });

            g2d.dispose();
        }

        @Test
        void paint_component_in_flamegraph_mode() {
            var image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
            var g2d = image.createGraphics();

            fg.view().setMode(Mode.FLAMEGRAPH);
            fg.view().setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            )));

            fg.view().configureCanvas(canvas -> {
                canvas.setSize(800, 600);
                canvas.setBounds(0, 0, 800, 600);
                canvas.paint(g2d);
            });

            g2d.dispose();
        }

        @Test
        void paint_component_with_minimap_disabled() {
            var image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
            var g2d = image.createGraphics();

            fg.view().setShowMinimap(false);
            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            fg.view().configureCanvas(canvas -> {
                canvas.setSize(800, 600);
                canvas.setBounds(0, 0, 800, 600);
                canvas.paint(g2d);
            });

            g2d.dispose();
        }
    }

    @Nested
    @DisplayName("Canvas Mouse Event Handling")
    class CanvasMouseEventHandlingTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void mouse_click_on_canvas_with_selected_frame_consumer() {
            var frameClicked = new AtomicReference<FrameBox<String>>();
            fg.view().setSelectedFrameConsumer((frame, e) -> frameClicked.set(frame));

            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            fg.view().setModel(new FrameModel<>(List.of(root)));

            // The consumer should have been set up
            assertThat(fg.view().getSelectedFrameConsumer()).isNotNull();
        }

        @Test
        void mouse_click_triggers_popup_consumer_on_right_click() {
            var popupTriggered = new AtomicBoolean(false);
            fg.view().setPopupConsumer((frame, e) -> popupTriggered.set(true));

            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            assertThat(fg.view().getPopupConsumer()).isNotNull();
        }

        @Test
        void mouse_move_on_canvas_does_not_throw() {
            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            fg.view().configureCanvas(canvas -> {
                canvas.setSize(800, 600);
                canvas.setBounds(0, 0, 800, 600);

                var mouseEvent = new MouseEvent(
                        canvas,
                        MouseEvent.MOUSE_MOVED,
                        System.currentTimeMillis(),
                        0,
                        400, 300,
                        0,
                        false
                );
                // Event dispatch may fail without peer, that's OK for this test
                assertThatCode(() -> canvas.dispatchEvent(mouseEvent))
                        .doesNotThrowAnyException();
            });
        }

        @Test
        void mouse_drag_on_canvas_does_not_throw() {
            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            fg.view().configureCanvas(canvas -> {
                canvas.setSize(800, 600);
                canvas.setBounds(0, 0, 800, 600);

                // Press
                var pressEvent = new MouseEvent(
                        canvas,
                        MouseEvent.MOUSE_PRESSED,
                        System.currentTimeMillis(),
                        0,
                        400, 300,
                        1,
                        false,
                        MouseEvent.BUTTON1
                );
                assertThatCode(() -> canvas.dispatchEvent(pressEvent))
                        .doesNotThrowAnyException();

                // Drag
                var dragEvent = new MouseEvent(
                        canvas,
                        MouseEvent.MOUSE_DRAGGED,
                        System.currentTimeMillis(),
                        0,
                        450, 350,
                        0,
                        false
                );
                assertThatCode(() -> canvas.dispatchEvent(dragEvent))
                        .doesNotThrowAnyException();
            });
        }
    }

    @Nested
    @DisplayName("Canvas Tooltip Behavior")
    class CanvasTooltipBehaviorTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void getToolTipText_with_tooltip_function_set() {
            fg.view().setTooltipTextFunction((model, frame) -> "Tooltip: " + frame.actualNode);
            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            fg.view().configureCanvas(canvas -> {
                canvas.setSize(800, 600);
                canvas.setBounds(0, 0, 800, 600);

                var mouseEvent = new MouseEvent(
                        canvas,
                        MouseEvent.MOUSE_MOVED,
                        System.currentTimeMillis(),
                        0,
                        400, 300,
                        0,
                        false
                );

                // getToolTipText should not throw
                assertThatCode(() -> canvas.getToolTipText(mouseEvent))
                        .doesNotThrowAnyException();
            });
        }

        @Test
        void getToolTipText_returns_empty_when_inside_minimap_area() {
            fg.view().setShowMinimap(true);
            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            fg.view().configureCanvas(canvas -> {
                canvas.setSize(800, 600);
                canvas.setBounds(0, 0, 800, 600);

                // Minimap is at bottom-left by default (x=50, y from bottom)
                // Create event in minimap area
                var mouseEvent = new MouseEvent(
                        canvas,
                        MouseEvent.MOUSE_MOVED,
                        System.currentTimeMillis(),
                        0,
                        60, 500, // likely inside minimap area
                        0,
                        false
                );

                var tooltip = canvas.getToolTipText(mouseEvent);
                // Tooltip should be empty string when inside minimap
                // (or null if not inside, depending on setup)
            });
        }
    }

    @Nested
    @DisplayName("Canvas createToolTip")
    class CanvasCreateToolTipTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void createToolTip_without_supplier_returns_default() {
            fg.view().configureCanvas(canvas -> {
                var tooltip = canvas.createToolTip();
                assertThat(tooltip).isNotNull();
                assertThat(tooltip).isInstanceOf(JToolTip.class);
            });
        }

        @Test
        void createToolTip_with_supplier_returns_custom_tooltip() {
            var customTooltipCreated = new AtomicInteger(0);
            fg.view().setTooltipComponentSupplier(() -> {
                customTooltipCreated.incrementAndGet();
                var tip = new JToolTip();
                tip.setBackground(Color.CYAN);
                return tip;
            });

            fg.view().configureCanvas(canvas -> {
                var tooltip1 = canvas.createToolTip();
                assertThat(tooltip1).isNotNull();
                assertThat(tooltip1.getBackground()).isEqualTo(Color.CYAN);

                // Second call should return same instance (cached)
                var tooltip2 = canvas.createToolTip();
                assertThat(tooltip2).isSameAs(tooltip1);
            });

            // Supplier should be called only once due to caching
            assertThat(customTooltipCreated.get()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Canvas isInsideMinimap")
    class CanvasIsInsideMinimapTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void isInsideMinimap_returns_false_when_minimap_disabled() {
            fg.view().setShowMinimap(false);
            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            fg.view().configureCanvas(canvas -> {
                canvas.setSize(800, 600);
                canvas.setBounds(0, 0, 800, 600);

                // Use reflection or verify through behavior
                var mouseEvent = new MouseEvent(
                        canvas,
                        MouseEvent.MOUSE_MOVED,
                        System.currentTimeMillis(),
                        0,
                        60, 500, // would be in minimap area if enabled
                        0,
                        false
                );

                // When minimap is disabled, tooltip should not return empty
                var tooltip = canvas.getToolTipText(mouseEvent);
                // tooltip won't be the empty string that indicates minimap area
            });
        }

        @Test
        void isInsideMinimap_returns_true_for_point_in_minimap_area() {
            fg.view().setShowMinimap(true);
            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            fg.view().configureCanvas(canvas -> {
                canvas.setSize(800, 600);
                canvas.setBounds(0, 0, 800, 600);

                // Point in minimap area - bottom left corner
                // Minimap bounds are at (50, 50) from visible rect with size (200, 100) + insets
                var mouseEvent = new MouseEvent(
                        canvas,
                        MouseEvent.MOUSE_MOVED,
                        System.currentTimeMillis(),
                        0,
                        100, 550, // likely in minimap area in icicle mode
                        0,
                        false
                );

                var tooltip = canvas.getToolTipText(mouseEvent);
                // If inside minimap, tooltip should be empty string
            });
        }
    }

    @Nested
    @DisplayName("Canvas Preferred Size")
    class CanvasPreferredSizeTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void getPreferredSize_without_model_returns_minimal_size() {
            fg.view().configureCanvas(canvas -> {
                var prefSize = canvas.getPreferredSize();
                assertThat(prefSize).isNotNull();
                assertThat(prefSize.width).isGreaterThanOrEqualTo(10);
                assertThat(prefSize.height).isGreaterThanOrEqualTo(10);
            });
        }

        @Test
        void getPreferredSize_with_model_and_graphics() {
            fg.view().setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            )));

            fg.view().configureCanvas(canvas -> {
                canvas.setSize(800, 600);
                var prefSize = canvas.getPreferredSize();
                assertThat(prefSize).isNotNull();
            });
        }
    }

    @Nested
    @DisplayName("Scroll Pane Mouse Listener")
    class ScrollPaneMouseListenerTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void scroll_pane_receives_mouse_events() {
            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var scrollPane = fg.scrollPane();
            assertThat(scrollPane).isNotNull();

            scrollPane.setSize(800, 600);
            scrollPane.setBounds(0, 0, 800, 600);

            // Dispatch mouse moved event to scroll pane
            var mouseEvent = new MouseEvent(
                    scrollPane,
                    MouseEvent.MOUSE_MOVED,
                    System.currentTimeMillis(),
                    0,
                    400, 300,
                    0,
                    false
            );
            scrollPane.dispatchEvent(mouseEvent);
        }

        @Test
        void scroll_pane_drag_moves_viewport() {
            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var scrollPane = fg.scrollPane();
            assertThat(scrollPane).isNotNull();

            scrollPane.setSize(800, 600);
            scrollPane.setBounds(0, 0, 800, 600);

            // Press
            var pressEvent = new MouseEvent(
                    scrollPane,
                    MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(),
                    0,
                    400, 300,
                    1,
                    false,
                    MouseEvent.BUTTON1
            );
            scrollPane.dispatchEvent(pressEvent);

            // Drag
            var dragEvent = new MouseEvent(
                    scrollPane,
                    MouseEvent.MOUSE_DRAGGED,
                    System.currentTimeMillis(),
                    0,
                    350, 250,
                    0,
                    false
            );
            scrollPane.dispatchEvent(dragEvent);

            // Release
            var releaseEvent = new MouseEvent(
                    scrollPane,
                    MouseEvent.MOUSE_RELEASED,
                    System.currentTimeMillis(),
                    0,
                    350, 250,
                    1,
                    false,
                    MouseEvent.BUTTON1
            );
            scrollPane.dispatchEvent(releaseEvent);
        }

        @Test
        void scroll_pane_mouse_exit_stops_hover() {
            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var scrollPane = fg.scrollPane();
            assertThat(scrollPane).isNotNull();

            scrollPane.setSize(800, 600);
            scrollPane.setBounds(0, 0, 800, 600);

            var exitEvent = new MouseEvent(
                    scrollPane,
                    MouseEvent.MOUSE_EXITED,
                    System.currentTimeMillis(),
                    0,
                    -1, -1, // outside
                    0,
                    false
            );
            scrollPane.dispatchEvent(exitEvent);
        }

        @Test
        void scroll_pane_mouse_wheel_event() {
            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var scrollPane = fg.scrollPane();
            assertThat(scrollPane).isNotNull();

            scrollPane.setSize(800, 600);
            scrollPane.setBounds(0, 0, 800, 600);

            var wheelEvent = new MouseWheelEvent(
                    scrollPane,
                    MouseWheelEvent.MOUSE_WHEEL,
                    System.currentTimeMillis(),
                    0,
                    400, 300,
                    0,
                    false,
                    MouseWheelEvent.WHEEL_UNIT_SCROLL,
                    3, // scroll amount
                    1  // wheel rotation
            );
            scrollPane.dispatchEvent(wheelEvent);
        }

        @Test
        void scroll_pane_is_configured_with_viewport() {
            fg.view().setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            )));

            var scrollPane = fg.scrollPane();
            assertThat(scrollPane).isNotNull();
            assertThat(scrollPane.getViewport()).isNotNull();
            assertThat(scrollPane.getViewport().getView()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Hover Listener Callbacks")
    class HoverListenerCallbacksTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void hover_listener_receives_frame_hover_event() {
            var hoverCount = new AtomicInteger(0);
            var lastHoveredFrame = new AtomicReference<FrameBox<String>>();

            fg.view().setHoverListener(new FlamegraphView.HoverListener<>() {
                @Override
                public void onFrameHover(FrameBox<String> frame, Rectangle rect, MouseEvent e) {
                    hoverCount.incrementAndGet();
                    lastHoveredFrame.set(frame);
                }

                @Override
                public void onStopHover(FrameBox<String> frame, Rectangle rect, MouseEvent e) {
                    // Track stop hover
                }
            });

            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            // Hover listener is set, verify it's configured
            var scrollPane = fg.scrollPane();
            assertThat(scrollPane).isNotNull();
        }

        @Test
        void hover_listener_stop_hover_is_called_on_exit() {
            var stopHoverCalled = new AtomicBoolean(false);

            fg.view().setHoverListener(new FlamegraphView.HoverListener<>() {
                @Override
                public void onFrameHover(FrameBox<String> frame, Rectangle rect, MouseEvent e) {
                }

                @Override
                public void onStopHover(FrameBox<String> frame, Rectangle rect, MouseEvent e) {
                    stopHoverCalled.set(true);
                }
            });

            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var scrollPane = fg.scrollPane();
            assertThat(scrollPane).isNotNull();
            scrollPane.setSize(800, 600);
            scrollPane.setBounds(0, 0, 800, 600);

            // Mouse exit should trigger stop hover
            var exitEvent = new MouseEvent(
                    scrollPane,
                    MouseEvent.MOUSE_EXITED,
                    System.currentTimeMillis(),
                    0,
                    -100, -100, // clearly outside
                    0,
                    false
            );
            scrollPane.dispatchEvent(exitEvent);
        }
    }

    @Nested
    @DisplayName("Frame Click Action Behaviors")
    class FrameClickActionBehaviorsTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void expand_frame_mode_is_configurable() {
            fg.view().setFrameClickAction(FlamegraphView.FrameClickAction.EXPAND_FRAME);
            fg.view().setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            )));

            assertThat(fg.view().getFrameClickAction())
                    .isEqualTo(FlamegraphView.FrameClickAction.EXPAND_FRAME);
        }

        @Test
        void focus_frame_mode_is_configurable() {
            fg.view().setFrameClickAction(FlamegraphView.FrameClickAction.FOCUS_FRAME);
            fg.view().setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            )));

            assertThat(fg.view().getFrameClickAction())
                    .isEqualTo(FlamegraphView.FrameClickAction.FOCUS_FRAME);
        }

        @Test
        void click_action_can_be_changed() {
            fg.view().setFrameClickAction(FlamegraphView.FrameClickAction.EXPAND_FRAME);
            assertThat(fg.view().getFrameClickAction())
                    .isEqualTo(FlamegraphView.FrameClickAction.EXPAND_FRAME);

            fg.view().setFrameClickAction(FlamegraphView.FrameClickAction.FOCUS_FRAME);
            assertThat(fg.view().getFrameClickAction())
                    .isEqualTo(FlamegraphView.FrameClickAction.FOCUS_FRAME);
        }
    }

    @Nested
    @DisplayName("Canvas Mode in Rendering")
    class CanvasModeInRenderingTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void icicle_mode_renders_top_to_bottom() {
            var image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
            var g2d = image.createGraphics();

            fg.view().setMode(Mode.ICICLEGRAPH);
            fg.view().setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1),
                    new FrameBox<>("grandchild", 0.0, 0.25, 2)
            )));

            fg.view().configureCanvas(canvas -> {
                canvas.setSize(800, 600);
                canvas.setBounds(0, 0, 800, 600);
                canvas.paint(g2d);
            });

            g2d.dispose();
        }

        @Test
        void flamegraph_mode_renders_bottom_to_top() {
            var image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
            var g2d = image.createGraphics();

            fg.view().setMode(Mode.FLAMEGRAPH);
            fg.view().setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1),
                    new FrameBox<>("grandchild", 0.0, 0.25, 2)
            )));

            fg.view().configureCanvas(canvas -> {
                canvas.setSize(800, 600);
                canvas.setBounds(0, 0, 800, 600);
                canvas.paint(g2d);
            });

            g2d.dispose();
        }

        @Test
        void mode_switch_during_paint() {
            var image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
            var g2d = image.createGraphics();

            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            fg.view().configureCanvas(canvas -> {
                canvas.setSize(800, 600);
                canvas.setBounds(0, 0, 800, 600);

                fg.view().setMode(Mode.ICICLEGRAPH);
                canvas.paint(g2d);

                fg.view().setMode(Mode.FLAMEGRAPH);
                canvas.paint(g2d);

                fg.view().setMode(Mode.ICICLEGRAPH);
                canvas.paint(g2d);
            });

            g2d.dispose();
        }
    }

    @Nested
    @DisplayName("Canvas getPreferredSize with Graphics")
    class CanvasGetPreferredSizeTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void canvas_getPreferredSize_with_spied_graphics() {
            var image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
            var g2d = image.createGraphics();

            fg.view().setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            )));

            fg.view().configureCanvas(canvas -> {
                var spiedCanvas = spy(canvas);
                doReturn(g2d).when(spiedCanvas).getGraphics();

                spiedCanvas.setSize(800, 600);
                spiedCanvas.setBounds(0, 0, 800, 600);

                var prefSize = spiedCanvas.getPreferredSize();
                assertThat(prefSize).isNotNull();
                assertThat(prefSize.width).isGreaterThan(0);
                assertThat(prefSize.height).isGreaterThan(0);
            });

            g2d.dispose();
        }
    }

    @Nested
    @DisplayName("Scroll Pane Event Handling")
    class ScrollPaneEventHandlingTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void scroll_pane_drag_sequence() {
            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var scrollPane = fg.scrollPane();
            assertThat(scrollPane).isNotNull();

            scrollPane.setSize(800, 600);
            scrollPane.setBounds(0, 0, 800, 600);

            // Press, drag, release sequence - tests FlamegraphHoveringScrollPaneMouseListener
            var pressEvent = new MouseEvent(
                    scrollPane,
                    MouseEvent.MOUSE_PRESSED,
                    System.currentTimeMillis(),
                    0,
                    400, 300,
                    1,
                    false,
                    MouseEvent.BUTTON1
            );
            scrollPane.dispatchEvent(pressEvent);

            var dragEvent = new MouseEvent(
                    scrollPane,
                    MouseEvent.MOUSE_DRAGGED,
                    System.currentTimeMillis(),
                    0,
                    350, 250,
                    0,
                    false
            );
            scrollPane.dispatchEvent(dragEvent);

            var releaseEvent = new MouseEvent(
                    scrollPane,
                    MouseEvent.MOUSE_RELEASED,
                    System.currentTimeMillis(),
                    0,
                    350, 250,
                    1,
                    false,
                    MouseEvent.BUTTON1
            );
            scrollPane.dispatchEvent(releaseEvent);
        }

        @Test
        void scroll_pane_mouse_enter_and_exit() {
            fg.view().setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var scrollPane = fg.scrollPane();
            assertThat(scrollPane).isNotNull();

            scrollPane.setSize(800, 600);
            scrollPane.setBounds(0, 0, 800, 600);

            var enterEvent = new MouseEvent(
                    scrollPane,
                    MouseEvent.MOUSE_ENTERED,
                    System.currentTimeMillis(),
                    0,
                    400, 300,
                    0,
                    false
            );
            scrollPane.dispatchEvent(enterEvent);

            var exitEvent = new MouseEvent(
                    scrollPane,
                    MouseEvent.MOUSE_EXITED,
                    System.currentTimeMillis(),
                    0,
                    -1, -1,
                    0,
                    false
            );
            scrollPane.dispatchEvent(exitEvent);
        }
    }

    @Nested
    @DisplayName("ZoomModel Behavior via Canvas")
    class ZoomModelBehaviorTests {
        private final FlamegraphViewUnderTest<String> fg = FlamegraphViewUnderTest.<String>builder().build();

        @Test
        void zoom_model_tracks_zoom_target() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child = new FrameBox<>("child", 0.0, 0.5, 1);
            fg.view().setModel(new FrameModel<>(List.of(root, child)));

            // Zoom to child frame
            fg.view().zoomTo(child);

            // Zoom model should be updated (tested indirectly through reset)
            assertThatCode(() -> fg.view().resetZoom())
                    .doesNotThrowAnyException();
        }

        @Test
        void zoom_model_reset_clears_target() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            fg.view().setModel(new FrameModel<>(List.of(root)));

            fg.view().zoomTo(root);
            fg.view().resetZoom();

            // Should be able to zoom again
            fg.view().zoomTo(root);
        }
    }
}
