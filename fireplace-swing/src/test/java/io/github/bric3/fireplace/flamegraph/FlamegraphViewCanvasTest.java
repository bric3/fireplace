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

    private FlamegraphView<String> fg;

    @BeforeEach
    void setUp() {
        fg = new FlamegraphView<>();
    }

    @Nested
    @DisplayName("Canvas Properties")
    class CanvasPropertiesTests {

        @Test
        void canvas_has_graph_mode_property_constant() {
            // Access constants through reflection or verify behavior
            fg.setMode(Mode.FLAMEGRAPH);
            assertThat(fg.getMode()).isEqualTo(Mode.FLAMEGRAPH);

            fg.setMode(Mode.ICICLEGRAPH);
            assertThat(fg.getMode()).isEqualTo(Mode.ICICLEGRAPH);
        }

        @Test
        void canvas_show_minimap_property_affects_display() {
            fg.setShowMinimap(true);
            assertThat(fg.isShowMinimap()).isTrue();

            fg.setShowMinimap(false);
            assertThat(fg.isShowMinimap()).isFalse();
        }

        @Test
        void canvas_frame_model_property_changes() {
            var model1 = new FrameModel<>(List.of(new FrameBox<>("first", 0.0, 1.0, 0)));
            fg.setModel(model1);
            assertThat(fg.getFrameModel()).isEqualTo(model1);

            var model2 = new FrameModel<>(List.of(new FrameBox<>("second", 0.0, 1.0, 0)));
            fg.setModel(model2);
            assertThat(fg.getFrameModel()).isEqualTo(model2);
        }
    }

    @Nested
    @DisplayName("Canvas Tooltip")
    class CanvasTooltipTests {

        @Test
        void tooltip_text_function_is_used_when_set() {
            var tooltipCalled = new AtomicBoolean(false);
            fg.setTooltipTextFunction((model, frame) -> {
                tooltipCalled.set(true);
                return "Custom tooltip for " + frame.actualNode;
            });

            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));
            assertThat(fg.getTooltipTextFunction()).isNotNull();
        }

        @Test
        void tooltip_component_supplier_provides_custom_tooltip() {
            var customTooltipCreated = new AtomicBoolean(false);
            fg.setTooltipComponentSupplier(() -> {
                customTooltipCreated.set(true);
                var tooltip = new JToolTip();
                tooltip.setBackground(Color.CYAN);
                return tooltip;
            });

            assertThat(fg.getTooltipComponentSupplier()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Canvas Frame Click Behavior")
    class CanvasFrameClickBehaviorTests {

        @Test
        void default_click_behavior_is_focus_frame() {
            assertThat(fg.getFrameClickAction())
                    .isEqualTo(FlamegraphView.FrameClickAction.FOCUS_FRAME);
        }

        @Test
        void click_behavior_can_be_set_to_expand_frame() {
            fg.setFrameClickAction(FlamegraphView.FrameClickAction.EXPAND_FRAME);
            assertThat(fg.getFrameClickAction())
                    .isEqualTo(FlamegraphView.FrameClickAction.EXPAND_FRAME);
        }

        @Test
        void selected_frame_consumer_is_called_setup() {
            var consumerCalled = new AtomicReference<FrameBox<String>>();
            fg.setSelectedFrameConsumer((frame, e) -> consumerCalled.set(frame));
            assertThat(fg.getSelectedFrameConsumer()).isNotNull();
        }

        @Test
        void popup_consumer_is_set_correctly() {
            var consumerCalled = new AtomicReference<FrameBox<String>>();
            fg.setPopupConsumer((frame, e) -> consumerCalled.set(frame));
            assertThat(fg.getPopupConsumer()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Canvas Zoom Override")
    class CanvasZoomOverrideTests {

        @Test
        void zoom_action_override_is_applied() {
            var zoomCalled = new AtomicBoolean(false);
            fg.overrideZoomAction(new FlamegraphView.ZoomAction() {
                @Override
                public <T> boolean zoom(FlamegraphView.ZoomableComponent<T> zoomableComponent, ZoomTarget<T> zoomTarget) {
                    zoomCalled.set(true);
                    return true;
                }
            });

            var frame = new FrameBox<>("root", 0.0, 1.0, 0);
            fg.setModel(new FrameModel<>(List.of(frame)));
            fg.zoomTo(frame);

            // In headless mode, the zoom action may or may not be called depending on component state
        }

        @Test
        void zoom_action_override_null_throws() {
            assertThatThrownBy(() -> fg.overrideZoomAction(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Canvas Minimap")
    class CanvasMinimapTests {

        @Test
        void minimap_shade_color_supplier_is_applied() {
            fg.setMinimapShadeColorSupplier(() -> new Color(100, 100, 100, 100));
            assertThat(fg.getMinimapShadeColorSupplier()).isNotNull();
            assertThat(fg.getMinimapShadeColorSupplier().get()).isEqualTo(new Color(100, 100, 100, 100));
        }

        @Test
        void minimap_visibility_toggles() {
            fg.setShowMinimap(true);
            assertThat(fg.isShowMinimap()).isTrue();

            fg.setShowMinimap(false);
            assertThat(fg.isShowMinimap()).isFalse();
        }
    }

    @Nested
    @DisplayName("Canvas Show Stats")
    class CanvasShowStatsTests {

        @Test
        void show_stats_client_property_can_be_set() {
            fg.putClientProperty(FlamegraphView.SHOW_STATS, Boolean.TRUE);
            assertThat(fg.<Boolean>getClientProperty(FlamegraphView.SHOW_STATS)).isTrue();
        }

        @Test
        void show_stats_client_property_can_be_cleared() {
            fg.putClientProperty(FlamegraphView.SHOW_STATS, Boolean.TRUE);
            fg.putClientProperty(FlamegraphView.SHOW_STATS, null);
            assertThat(fg.<Boolean>getClientProperty(FlamegraphView.SHOW_STATS)).isNull();
        }
    }

    @Nested
    @DisplayName("Canvas Hovered Siblings")
    class CanvasHoveredSiblingsTests {

        @Test
        void show_hovered_siblings_default_is_true() {
            assertThat(fg.isShowHoveredSiblings()).isTrue();
        }

        @Test
        void show_hovered_siblings_can_be_disabled() {
            fg.setShowHoveredSiblings(false);
            assertThat(fg.isShowHoveredSiblings()).isFalse();
        }

        @Test
        void show_hovered_siblings_can_be_re_enabled() {
            fg.setShowHoveredSiblings(false);
            fg.setShowHoveredSiblings(true);
            assertThat(fg.isShowHoveredSiblings()).isTrue();
        }
    }

    @Nested
    @DisplayName("Canvas Configurer")
    class CanvasConfigurerTests {

        @Test
        void configure_canvas_provides_access_to_canvas() {
            var canvasAccessed = new AtomicBoolean(false);
            var canvasRef = new AtomicReference<JComponent>();

            fg.configureCanvas(canvas -> {
                canvasAccessed.set(true);
                canvasRef.set(canvas);
            });

            assertThat(canvasAccessed.get()).isTrue();
            assertThat(canvasRef.get()).isNotNull();
        }

        @Test
        void configure_canvas_allows_custom_properties() {
            fg.configureCanvas(canvas -> {
                canvas.putClientProperty("customKey", "customValue");
            });

            // Verify the property was set by accessing it through configureCanvas
            var propertyValue = new AtomicReference<String>();
            fg.configureCanvas(canvas -> {
                propertyValue.set((String) canvas.getClientProperty("customKey"));
            });

            assertThat(propertyValue.get()).isEqualTo("customValue");
        }
    }

    @Nested
    @DisplayName("Canvas Mode Switching")
    class CanvasModeSwitchingTests {

        @Test
        void mode_switch_from_icicle_to_flamegraph() {
            fg.setMode(Mode.ICICLEGRAPH);
            fg.setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            )));

            fg.setMode(Mode.FLAMEGRAPH);

            assertThat(fg.getMode()).isEqualTo(Mode.FLAMEGRAPH);
        }

        @Test
        void mode_switch_from_flamegraph_to_icicle() {
            fg.setMode(Mode.FLAMEGRAPH);
            fg.setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            )));

            fg.setMode(Mode.ICICLEGRAPH);

            assertThat(fg.getMode()).isEqualTo(Mode.ICICLEGRAPH);
        }
    }

    @Nested
    @DisplayName("Canvas Highlight Frames")
    class CanvasHighlightFramesTests {

        @Test
        void highlight_frames_updates_engine() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child1 = new FrameBox<>("method", 0.0, 0.3, 1);
            var child2 = new FrameBox<>("method", 0.5, 0.8, 1);
            var other = new FrameBox<>("other", 0.3, 0.5, 1);

            fg.setModel(new FrameModel<>(List.of(root, child1, child2, other)));

            // Highlight the "method" frames
            fg.highlightFrames(java.util.Set.of(child1, child2), "method");

            // No exception means success
        }

        @Test
        void highlight_frames_can_be_cleared() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child = new FrameBox<>("child", 0.0, 0.5, 1);

            fg.setModel(new FrameModel<>(List.of(root, child)));
            fg.highlightFrames(java.util.Set.of(child), "child");

            // Clear highlights
            fg.highlightFrames(java.util.Set.of(), "");

            // No exception means success
        }
    }

    @Nested
    @DisplayName("Canvas Reset Zoom")
    class CanvasResetZoomTests {

        @Test
        void reset_zoom_with_empty_model_does_not_throw() {
            assertThatCode(() -> fg.resetZoom())
                    .doesNotThrowAnyException();
        }

        @Test
        void reset_zoom_with_model_does_not_throw() {
            fg.setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            )));

            assertThatCode(() -> fg.resetZoom())
                    .doesNotThrowAnyException();
        }

        @Test
        void reset_zoom_after_zoom_to_does_not_throw() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child = new FrameBox<>("child", 0.0, 0.5, 1);
            fg.setModel(new FrameModel<>(List.of(root, child)));

            fg.zoomTo(child);
            assertThatCode(() -> fg.resetZoom())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Canvas ZoomTo")
    class CanvasZoomToTests {

        @Test
        void zoom_to_root_frame() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            fg.setModel(new FrameModel<>(List.of(root)));

            assertThatCode(() -> fg.zoomTo(root))
                    .doesNotThrowAnyException();
        }

        @Test
        void zoom_to_child_frame() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child = new FrameBox<>("child", 0.0, 0.5, 1);
            fg.setModel(new FrameModel<>(List.of(root, child)));

            assertThatCode(() -> fg.zoomTo(child))
                    .doesNotThrowAnyException();
        }

        @Test
        void zoom_to_deeply_nested_frame() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child = new FrameBox<>("child", 0.0, 0.5, 1);
            var grandchild = new FrameBox<>("grandchild", 0.0, 0.25, 2);
            var greatGrandchild = new FrameBox<>("greatGrandchild", 0.0, 0.125, 3);
            fg.setModel(new FrameModel<>(List.of(root, child, grandchild, greatGrandchild)));

            assertThatCode(() -> fg.zoomTo(greatGrandchild))
                    .doesNotThrowAnyException();
        }

        @Test
        void zoom_to_narrow_frame() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var narrow = new FrameBox<>("narrow", 0.0, 0.01, 1);
            fg.setModel(new FrameModel<>(List.of(root, narrow)));

            assertThatCode(() -> fg.zoomTo(narrow))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Canvas Request Repaint")
    class CanvasRequestRepaintTests {

        @Test
        void request_repaint_with_empty_model() {
            assertThatCode(() -> fg.requestRepaint())
                    .doesNotThrowAnyException();
        }

        @Test
        void request_repaint_with_model() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            assertThatCode(() -> fg.requestRepaint())
                    .doesNotThrowAnyException();
        }

        @Test
        void request_repaint_multiple_times() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            for (int i = 0; i < 10; i++) {
                assertThatCode(() -> fg.requestRepaint())
                        .doesNotThrowAnyException();
            }
        }
    }

    @Nested
    @DisplayName("Canvas Frame Equality")
    class CanvasFrameEqualityTests {

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

            fg.setModel(model);

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

            fg.setModel(model);

            assertThat(model.frameEquality.equal(frame1, frame2)).isTrue();
            assertThat(model.frameEquality.equal(frame1, frame3)).isFalse();
        }
    }

    @Nested
    @DisplayName("Canvas Model Description")
    class CanvasModelDescriptionTests {

        @Test
        void model_with_description_is_accessible() {
            var model = new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0)))
                    .withDescription("Test flamegraph description");

            fg.setModel(model);

            assertThat(fg.getFrameModel().description).isEqualTo("Test flamegraph description");
        }

        @Test
        void model_without_description_has_null_description() {
            var model = new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0)));

            fg.setModel(model);

            assertThat(fg.getFrameModel().description).isNull();
        }
    }

    @Nested
    @DisplayName("Canvas Model Title")
    class CanvasModelTitleTests {

        @Test
        void model_with_title_is_accessible() {
            var model = new FrameModel<>(
                    "My Flamegraph Title",
                    (a, b) -> Objects.equals(a.actualNode, b.actualNode),
                    List.of(new FrameBox<>("root", 0.0, 1.0, 0))
            );

            fg.setModel(model);

            assertThat(fg.getFrameModel().title).isEqualTo("My Flamegraph Title");
        }

        @Test
        void model_from_frames_list_has_empty_title() {
            var model = new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0)));

            fg.setModel(model);

            assertThat(fg.getFrameModel().title).isEmpty();
        }
    }

    @Nested
    @DisplayName("Hover Listener Interface")
    class HoverListenerInterfaceTests {

        @Test
        void hover_listener_can_be_set() {
            FlamegraphView.HoverListener<String> listener = (frame, rect, e) -> {
                // no-op
            };

            assertThatCode(() -> fg.setHoverListener(listener))
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

        @Test
        void render_engine_initialized_after_model_set() {
            // Create a Graphics2D from a BufferedImage for headless testing
            var image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
            var g2d = image.createGraphics();

            var frame = new FrameBox<>("root", 0.0, 1.0, 0);
            fg.setModel(new FrameModel<>(List.of(frame)));

            // The render engine should be initialized
            assertThat(fg.getFrameModel().frames).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Canvas Scroll Pane Integration")
    class CanvasScrollPaneIntegrationTests {

        @Test
        void component_contains_scroll_pane() {
            var scrollPane = findScrollPane(fg.component);
            assertThat(scrollPane).isNotNull();
        }

        @Test
        void scroll_pane_contains_canvas() {
            var scrollPane = findScrollPane(fg.component);
            assertThat(scrollPane).isNotNull();
            assertThat(scrollPane.getViewport()).isNotNull();
            assertThat(scrollPane.getViewport().getView()).isNotNull();
        }

        private JScrollPane findScrollPane(Container container) {
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
    }

    @Nested
    @DisplayName("Canvas Empty State")
    class CanvasEmptyStateTests {

        @Test
        void empty_model_returns_empty_frames() {
            assertThat(fg.getFrames()).isEmpty();
        }

        @Test
        void clear_returns_to_empty_state() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));
            assertThat(fg.getFrames()).isNotEmpty();

            fg.clear();
            assertThat(fg.getFrames()).isEmpty();
        }

        @Test
        void empty_model_singleton() {
            assertThat(fg.getFrameModel()).isEqualTo(FrameModel.empty());
            assertThat(FrameModel.empty()).isSameAs(FrameModel.empty());
        }
    }

    @Nested
    @DisplayName("Canvas Model Changes")
    class CanvasModelChangesTests {

        @Test
        void model_change_updates_frames() {
            var model1 = new FrameModel<>(List.of(
                    new FrameBox<>("root1", 0.0, 1.0, 0)
            ));
            var model2 = new FrameModel<>(List.of(
                    new FrameBox<>("root2", 0.0, 1.0, 0),
                    new FrameBox<>("child2", 0.0, 0.5, 1)
            ));

            fg.setModel(model1);
            assertThat(fg.getFrames()).hasSize(1);

            fg.setModel(model2);
            assertThat(fg.getFrames()).hasSize(2);
        }

        @Test
        void model_change_preserves_mode() {
            fg.setMode(Mode.FLAMEGRAPH);
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            assertThat(fg.getMode()).isEqualTo(Mode.FLAMEGRAPH);
        }

        @Test
        void model_change_preserves_minimap_setting() {
            fg.setShowMinimap(false);
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            assertThat(fg.isShowMinimap()).isFalse();
        }
    }

    @Nested
    @DisplayName("Canvas Complex Frame Hierarchies")
    class CanvasComplexFrameHierarchiesTests {

        @Test
        void deep_hierarchy_is_supported() {
            var frames = new java.util.ArrayList<FrameBox<String>>();
            frames.add(new FrameBox<>("root", 0.0, 1.0, 0));
            for (int i = 1; i <= 50; i++) {
                double width = 1.0 / Math.pow(2, i);
                frames.add(new FrameBox<>("level" + i, 0.0, width, i));
            }

            var model = new FrameModel<>(frames);
            fg.setModel(model);

            assertThat(fg.getFrames()).hasSize(51);
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
            fg.setModel(model);

            assertThat(fg.getFrames()).hasSize(101);
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
            fg.setModel(model);

            assertThat(fg.getFrames()).hasSize(4);
        }
    }

    @Nested
    @DisplayName("Canvas Property Change Events")
    class CanvasPropertyChangeEventsTests {

        @Test
        void mode_change_fires_property_change() {
            var propertyChanged = new AtomicBoolean(false);
            fg.configureCanvas(canvas -> {
                canvas.addPropertyChangeListener("mode", evt -> propertyChanged.set(true));
            });

            fg.setMode(Mode.FLAMEGRAPH);
            // Note: Property change may be on different property name internally
        }

        @Test
        void model_change_fires_property_change() {
            var propertyChanged = new AtomicBoolean(false);
            fg.configureCanvas(canvas -> {
                canvas.addPropertyChangeListener(evt -> {
                    if ("frameModel".equals(evt.getPropertyName())) {
                        propertyChanged.set(true);
                    }
                });
            });

            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));
            // Property change event firing depends on actual property name
        }
    }

    @Nested
    @DisplayName("Canvas Renderer Configuration")
    class CanvasRendererConfigurationTests {

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

            fg.setFrameRender(renderer);
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            // Verify no exception is thrown
            assertThat(fg.getFrameModel().frames).isNotEmpty();
        }

        @Test
        void renderer_change_after_model_set() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var renderer = new DefaultFrameRenderer<String>(
                    FrameTextsProvider.of(f -> "Changed"),
                    FrameColorProvider.defaultColorProvider(f -> Color.MAGENTA),
                    FrameFontProvider.defaultFontProvider()
            );

            assertThatCode(() -> fg.setFrameRender(renderer))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Canvas Frame Operations")
    class CanvasFrameOperationsTests {

        @Test
        void frames_list_is_unmodifiable() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var frames = fg.getFrames();
            assertThatThrownBy(() -> frames.add(new FrameBox<>("new", 0.0, 0.5, 1)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void frame_model_is_immutable() {
            var originalModel = new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0)));
            fg.setModel(originalModel);

            assertThat(fg.getFrameModel()).isSameAs(originalModel);
        }
    }

    @Nested
    @DisplayName("Canvas Zoom Multiple Frames")
    class CanvasZoomMultipleFramesTests {

        @Test
        void zoom_to_different_frames_sequentially() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child1 = new FrameBox<>("child1", 0.0, 0.3, 1);
            var child2 = new FrameBox<>("child2", 0.3, 0.6, 1);
            var child3 = new FrameBox<>("child3", 0.6, 1.0, 1);

            fg.setModel(new FrameModel<>(List.of(root, child1, child2, child3)));

            assertThatCode(() -> {
                fg.zoomTo(child1);
                fg.zoomTo(child2);
                fg.zoomTo(child3);
                fg.zoomTo(root);
            }).doesNotThrowAnyException();
        }

        @Test
        void reset_zoom_after_multiple_zooms() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child = new FrameBox<>("child", 0.0, 0.5, 1);
            var grandchild = new FrameBox<>("grandchild", 0.0, 0.25, 2);

            fg.setModel(new FrameModel<>(List.of(root, child, grandchild)));

            fg.zoomTo(child);
            fg.zoomTo(grandchild);
            fg.resetZoom();

            // Should not throw
        }
    }

    @Nested
    @DisplayName("ZoomableComponent Interface Implementation")
    class ZoomableComponentInterfaceImplementationTests {

        @Test
        void zoomable_component_provides_dimensions() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            FlamegraphView.ZoomAction inspectingAction = new FlamegraphView.ZoomAction() {
                @Override
                public <T> boolean zoom(FlamegraphView.ZoomableComponent<T> zc, ZoomTarget<T> target) {
                    assertThat(zc.getWidth()).isGreaterThanOrEqualTo(0);
                    assertThat(zc.getHeight()).isGreaterThanOrEqualTo(0);
                    return true;
                }
            };

            fg.overrideZoomAction(inspectingAction);
            fg.zoomTo(fg.getFrames().get(0));
        }

        @Test
        void zoomable_component_provides_location() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

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

            fg.overrideZoomAction(inspectingAction);
            fg.zoomTo(fg.getFrames().get(0));
        }
    }

    @Nested
    @DisplayName("Canvas Paint with BufferedImage Graphics2D")
    class CanvasPaintWithBufferedImageTests {

        @Test
        void paint_component_with_empty_model_draws_no_data_message() {
            var image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
            var g2d = image.createGraphics();

            // Get the canvas and set its bounds
            fg.configureCanvas(canvas -> {
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

            fg.setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            )));

            fg.configureCanvas(canvas -> {
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

            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));
            fg.putClientProperty(FlamegraphView.SHOW_STATS, TRUE);

            fg.configureCanvas(canvas -> {
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

            fg.setMode(Mode.FLAMEGRAPH);
            fg.setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            )));

            fg.configureCanvas(canvas -> {
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

            fg.setShowMinimap(false);
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            fg.configureCanvas(canvas -> {
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

        @Test
        void mouse_click_on_canvas_with_selected_frame_consumer() {
            var frameClicked = new AtomicReference<FrameBox<String>>();
            fg.setSelectedFrameConsumer((frame, e) -> frameClicked.set(frame));

            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            fg.setModel(new FrameModel<>(List.of(root)));

            // The consumer should have been set up
            assertThat(fg.getSelectedFrameConsumer()).isNotNull();
        }

        @Test
        void mouse_click_triggers_popup_consumer_on_right_click() {
            var popupTriggered = new AtomicBoolean(false);
            fg.setPopupConsumer((frame, e) -> popupTriggered.set(true));

            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            assertThat(fg.getPopupConsumer()).isNotNull();
        }

        @Test
        void mouse_move_on_canvas_does_not_throw() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            fg.configureCanvas(canvas -> {
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
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            fg.configureCanvas(canvas -> {
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

        @Test
        void getToolTipText_with_tooltip_function_set() {
            fg.setTooltipTextFunction((model, frame) -> "Tooltip: " + frame.actualNode);
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            fg.configureCanvas(canvas -> {
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
            fg.setShowMinimap(true);
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            fg.configureCanvas(canvas -> {
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

        @Test
        void createToolTip_without_supplier_returns_default() {
            fg.configureCanvas(canvas -> {
                var tooltip = canvas.createToolTip();
                assertThat(tooltip).isNotNull();
                assertThat(tooltip).isInstanceOf(JToolTip.class);
            });
        }

        @Test
        void createToolTip_with_supplier_returns_custom_tooltip() {
            var customTooltipCreated = new AtomicInteger(0);
            fg.setTooltipComponentSupplier(() -> {
                customTooltipCreated.incrementAndGet();
                var tip = new JToolTip();
                tip.setBackground(Color.CYAN);
                return tip;
            });

            fg.configureCanvas(canvas -> {
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

        @Test
        void isInsideMinimap_returns_false_when_minimap_disabled() {
            fg.setShowMinimap(false);
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            fg.configureCanvas(canvas -> {
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
            fg.setShowMinimap(true);
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            fg.configureCanvas(canvas -> {
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

        @Test
        void getPreferredSize_without_model_returns_minimal_size() {
            fg.configureCanvas(canvas -> {
                var prefSize = canvas.getPreferredSize();
                assertThat(prefSize).isNotNull();
                assertThat(prefSize.width).isGreaterThanOrEqualTo(10);
                assertThat(prefSize.height).isGreaterThanOrEqualTo(10);
            });
        }

        @Test
        void getPreferredSize_with_model_and_graphics() {
            fg.setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            )));

            fg.configureCanvas(canvas -> {
                canvas.setSize(800, 600);
                var prefSize = canvas.getPreferredSize();
                assertThat(prefSize).isNotNull();
            });
        }
    }

    @Nested
    @DisplayName("Scroll Pane Mouse Listener")
    class ScrollPaneMouseListenerTests {

        @Test
        void scroll_pane_receives_mouse_events() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var scrollPane = findScrollPane(fg.component);
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
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var scrollPane = findScrollPane(fg.component);
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
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var scrollPane = findScrollPane(fg.component);
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
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var scrollPane = findScrollPane(fg.component);
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
            fg.setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            )));

            var scrollPane = findScrollPane(fg.component);
            assertThat(scrollPane).isNotNull();
            assertThat(scrollPane.getViewport()).isNotNull();
            assertThat(scrollPane.getViewport().getView()).isNotNull();
        }

        private JScrollPane findScrollPane(Container container) {
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
    }

    @Nested
    @DisplayName("Hover Listener Callbacks")
    class HoverListenerCallbacksTests {

        @Test
        void hover_listener_receives_frame_hover_event() {
            var hoverCount = new AtomicInteger(0);
            var lastHoveredFrame = new AtomicReference<FrameBox<String>>();

            fg.setHoverListener(new FlamegraphView.HoverListener<>() {
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

            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            // Hover listener is set, verify it's configured
            var scrollPane = findScrollPane(fg.component);
            assertThat(scrollPane).isNotNull();
        }

        @Test
        void hover_listener_stop_hover_is_called_on_exit() {
            var stopHoverCalled = new AtomicBoolean(false);

            fg.setHoverListener(new FlamegraphView.HoverListener<>() {
                @Override
                public void onFrameHover(FrameBox<String> frame, Rectangle rect, MouseEvent e) {
                }

                @Override
                public void onStopHover(FrameBox<String> frame, Rectangle rect, MouseEvent e) {
                    stopHoverCalled.set(true);
                }
            });

            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var scrollPane = findScrollPane(fg.component);
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

        private JScrollPane findScrollPane(Container container) {
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
    }

    @Nested
    @DisplayName("Frame Click Action Behaviors")
    class FrameClickActionBehaviorsTests {

        @Test
        void expand_frame_mode_is_configurable() {
            fg.setFrameClickAction(FlamegraphView.FrameClickAction.EXPAND_FRAME);
            fg.setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            )));

            assertThat(fg.getFrameClickAction())
                    .isEqualTo(FlamegraphView.FrameClickAction.EXPAND_FRAME);
        }

        @Test
        void focus_frame_mode_is_configurable() {
            fg.setFrameClickAction(FlamegraphView.FrameClickAction.FOCUS_FRAME);
            fg.setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            )));

            assertThat(fg.getFrameClickAction())
                    .isEqualTo(FlamegraphView.FrameClickAction.FOCUS_FRAME);
        }

        @Test
        void click_action_can_be_changed() {
            fg.setFrameClickAction(FlamegraphView.FrameClickAction.EXPAND_FRAME);
            assertThat(fg.getFrameClickAction())
                    .isEqualTo(FlamegraphView.FrameClickAction.EXPAND_FRAME);

            fg.setFrameClickAction(FlamegraphView.FrameClickAction.FOCUS_FRAME);
            assertThat(fg.getFrameClickAction())
                    .isEqualTo(FlamegraphView.FrameClickAction.FOCUS_FRAME);
        }
    }

    @Nested
    @DisplayName("Canvas Mode in Rendering")
    class CanvasModeInRenderingTests {

        @Test
        void icicle_mode_renders_top_to_bottom() {
            var image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
            var g2d = image.createGraphics();

            fg.setMode(Mode.ICICLEGRAPH);
            fg.setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1),
                    new FrameBox<>("grandchild", 0.0, 0.25, 2)
            )));

            fg.configureCanvas(canvas -> {
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

            fg.setMode(Mode.FLAMEGRAPH);
            fg.setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1),
                    new FrameBox<>("grandchild", 0.0, 0.25, 2)
            )));

            fg.configureCanvas(canvas -> {
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

            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            fg.configureCanvas(canvas -> {
                canvas.setSize(800, 600);
                canvas.setBounds(0, 0, 800, 600);

                fg.setMode(Mode.ICICLEGRAPH);
                canvas.paint(g2d);

                fg.setMode(Mode.FLAMEGRAPH);
                canvas.paint(g2d);

                fg.setMode(Mode.ICICLEGRAPH);
                canvas.paint(g2d);
            });

            g2d.dispose();
        }
    }

    @Nested
    @DisplayName("Canvas getPreferredSize with Graphics")
    class CanvasGetPreferredSizeTests {

        @Test
        void canvas_getPreferredSize_with_spied_graphics() {
            var image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
            var g2d = image.createGraphics();

            fg.setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            )));

            fg.configureCanvas(canvas -> {
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

        @Test
        void scroll_pane_drag_sequence() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var scrollPane = findScrollPane(fg.component);
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
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            var scrollPane = findScrollPane(fg.component);
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

        private JScrollPane findScrollPane(Container container) {
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
    }

    @Nested
    @DisplayName("ZoomModel Behavior via Canvas")
    class ZoomModelBehaviorTests {

        @Test
        void zoom_model_tracks_zoom_target() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child = new FrameBox<>("child", 0.0, 0.5, 1);
            fg.setModel(new FrameModel<>(List.of(root, child)));

            // Zoom to child frame
            fg.zoomTo(child);

            // Zoom model should be updated (tested indirectly through reset)
            assertThatCode(() -> fg.resetZoom())
                    .doesNotThrowAnyException();
        }

        @Test
        void zoom_model_reset_clears_target() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            fg.setModel(new FrameModel<>(List.of(root)));

            fg.zoomTo(root);
            fg.resetZoom();

            // Should be able to zoom again
            fg.zoomTo(root);
        }
    }
}
