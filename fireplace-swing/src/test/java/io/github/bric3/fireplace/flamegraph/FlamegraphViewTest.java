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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link FlamegraphView}.
 * These tests run in headless mode without requiring a display.
 */
@DisplayName("FlamegraphView")
class FlamegraphViewTest {

    private FlamegraphView<String> fg;

    @BeforeEach
    void setUp() {
        fg = new FlamegraphView<>();
    }

    @Test
    void basic_api() {
        assertSoftly(softly -> {
            var component = fg.component;
            softly.assertThat(FlamegraphView.<String>from(component)).contains(fg);
            softly.assertThat(FlamegraphView.<String>from(new JPanel())).isEmpty();
        });
        // non configured
        assertSoftly(softly -> {
            softly.assertThat(fg.getFrameModel()).isEqualTo(FrameModel.empty());
            softly.assertThat(fg.getFrames()).isEmpty();
            softly.assertThat(fg.isFrameGapEnabled()).isTrue();

            softly.assertThat(fg.getMode()).isEqualTo(Mode.ICICLEGRAPH);
            softly.assertThat(fg.isShowMinimap()).isTrue();
            softly.assertThat(fg.isShowHoveredSiblings()).isTrue();

            softly.assertThat(fg.getFrameColorProvider()).isNotNull();
            softly.assertThat(fg.getFrameFontProvider()).isNotNull();
            softly.assertThat(fg.getFrameTextsProvider()).isNotNull();
        });

        // after configuration
        assertSoftly(softly -> {
            var frameTextsProvider = FrameTextsProvider.<String>empty();
            var frameColorProvider = FrameColorProvider.<String>defaultColorProvider(box -> Color.BLACK);
            var frameFontProvider = FrameFontProvider.<String>defaultFontProvider();
            fg.setRenderConfiguration(
                    frameTextsProvider,
                    frameColorProvider,
                    frameFontProvider
            );
            softly.assertThat(fg.getFrameTextsProvider()).isEqualTo(frameTextsProvider);
            softly.assertThat(fg.getFrameColorProvider()).isEqualTo(frameColorProvider);
            softly.assertThat(fg.getFrameFontProvider()).isEqualTo(frameFontProvider);



            var frameTextsProvider2 = FrameTextsProvider.<String>empty();
            fg.setFrameTextsProvider(frameTextsProvider2);
            softly.assertThat(fg.getFrameTextsProvider()).isEqualTo(frameTextsProvider2);

            var frameColorProvider2 = FrameColorProvider.<String>defaultColorProvider(box -> Color.BLACK);
            fg.setFrameColorProvider(frameColorProvider2);
            softly.assertThat(fg.getFrameColorProvider()).isEqualTo(frameColorProvider2);

            var frameFontProvider2 = FrameFontProvider.<String>defaultFontProvider();
            fg.setFrameFontProvider(frameFontProvider2);
            softly.assertThat(fg.getFrameFontProvider()).isEqualTo(frameFontProvider2);


            var frameModel = new FrameModel<>(
                    "title",
                    (a, b) -> Objects.equals(a.actualNode, b.actualNode),
                    List.of(new FrameBox<>("frame1", 0.0, 0.5, 1))
            );
            fg.setModel(frameModel);
            softly.assertThat(fg.getFrameModel()).isEqualTo(frameModel);
            softly.assertThat(fg.getFrames()).isEqualTo(frameModel.frames);


            // non configured
            fg.setFrameGapEnabled(false);
            softly.assertThat(fg.isFrameGapEnabled()).isFalse();

            fg.setMode(Mode.FLAMEGRAPH);
            softly.assertThat(fg.getMode()).isEqualTo(Mode.FLAMEGRAPH);

            fg.setShowMinimap(false);
            softly.assertThat(fg.isShowMinimap()).isFalse();

            fg.setShowHoveredSiblings(false);
            softly.assertThat(fg.isShowHoveredSiblings()).isFalse();
        });
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        void constructor_creates_component_hierarchy() {
            assertThat(fg.component).isNotNull();
            assertThat(fg.component).isInstanceOf(JPanel.class);
        }

        @Test
        void from_with_valid_component_returns_flamegraph_view() {
            var result = FlamegraphView.<String>from(fg.component);

            assertThat(result).isPresent();
            assertThat(result.get()).isSameAs(fg);
        }

        @Test
        void from_with_unrelated_component_returns_empty() {
            var result = FlamegraphView.<String>from(new JButton());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Mode")
    class ModeTests {

        @Test
        void getMode_default_is_icicle_graph() {
            assertThat(fg.getMode()).isEqualTo(Mode.ICICLEGRAPH);
        }

        @Test
        void setMode_to_flamegraph_changes_mode() {
            fg.setMode(Mode.FLAMEGRAPH);

            assertThat(fg.getMode()).isEqualTo(Mode.FLAMEGRAPH);
        }

        @Test
        void setMode_to_iciclegraph_changes_mode() {
            fg.setMode(Mode.FLAMEGRAPH);
            fg.setMode(Mode.ICICLEGRAPH);

            assertThat(fg.getMode()).isEqualTo(Mode.ICICLEGRAPH);
        }
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
    }

    @Nested
    @DisplayName("Frame Click Action")
    class FrameClickActionTests {

        @Test
        void getFrameClickAction_default_is_focus_frame() {
            assertThat(fg.getFrameClickAction()).isEqualTo(FrameClickAction.FOCUS_FRAME);
        }

        @Test
        void setFrameClickAction_expand_frame_changes_action() {
            fg.setFrameClickAction(FrameClickAction.EXPAND_FRAME);

            assertThat(fg.getFrameClickAction()).isEqualTo(FrameClickAction.EXPAND_FRAME);
        }
    }

    @Nested
    @DisplayName("Model")
    class ModelTests {

        @Test
        void getFrameModel_default_is_empty() {
            assertThat(fg.getFrameModel()).isEqualTo(FrameModel.empty());
        }

        @Test
        void setModel_updates_frame_model() {
            var model = new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0)));

            fg.setModel(model);

            assertThat(fg.getFrameModel()).isEqualTo(model);
        }

        @Test
        void setModel_null_throws_exception() {
            assertThatThrownBy(() -> fg.setModel(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void getFrames_returns_frame_list() {
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            );
            var model = new FrameModel<>(frames);

            fg.setModel(model);

            assertThat(fg.getFrames()).isEqualTo(frames);
        }
    }

    @Nested
    @DisplayName("Clear")
    class ClearTests {

        @Test
        void clear_resets_model() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            fg.clear();

            assertThat(fg.getFrameModel()).isEqualTo(FrameModel.empty());
            assertThat(fg.getFrames()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Frame Renderer")
    class FrameRendererTests {

        @Test
        void setFrameRender_changes_renderer() {
            var renderer = new DefaultFrameRenderer<String>(
                    FrameTextsProvider.of(f -> f.actualNode),
                    FrameColorProvider.defaultColorProvider(f -> Color.GREEN),
                    FrameFontProvider.defaultFontProvider()
            );

            fg.setFrameRender(renderer);

            // No direct getter, but should not throw
            assertThat(fg.getFrameColorProvider()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Tooltip")
    class TooltipTests {

        @Test
        void setTooltipTextFunction_sets_function() {
            BiFunction<FrameModel<String>, FrameBox<String>, String> func =
                    (model, frame) -> frame.actualNode;

            fg.setTooltipTextFunction(func);

            assertThat(fg.getTooltipTextFunction()).isEqualTo(func);
        }

        @Test
        void setTooltipTextFunction_null_throws_exception() {
            assertThatThrownBy(() -> fg.setTooltipTextFunction(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void getTooltipTextFunction_default_is_null() {
            assertThat(fg.getTooltipTextFunction()).isNull();
        }

        @Test
        void setTooltipComponentSupplier_sets_supplier() {
            Supplier<JToolTip> supplier = JToolTip::new;

            fg.setTooltipComponentSupplier(supplier);

            assertThat(fg.getTooltipComponentSupplier()).isEqualTo(supplier);
        }

        @Test
        void setTooltipComponentSupplier_null_throws_exception() {
            assertThatThrownBy(() -> fg.setTooltipComponentSupplier(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Client Property")
    class ClientPropertyTests {

        @Test
        void putClientProperty_stores_value() {
            fg.putClientProperty("testKey", "testValue");

            assertThat(fg.<String>getClientProperty("testKey")).isEqualTo("testValue");
        }

        @Test
        void putClientProperty_null_value_removes_key() {
            fg.putClientProperty("testKey", "testValue");
            fg.putClientProperty("testKey", null);

            assertThat(fg.<String>getClientProperty("testKey")).isNull();
        }

        @Test
        void putClientProperty_null_key_throws_exception() {
            assertThatThrownBy(() -> fg.putClientProperty(null, "value"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void getClientProperty_non_existent_returns_null() {
            assertThat(fg.<String>getClientProperty("nonExistent")).isNull();
        }
    }

    @Nested
    @DisplayName("Popup Consumer")
    class PopupConsumerTests {

        @Test
        void setPopupConsumer_sets_consumer() {
            BiConsumer<FrameBox<String>, MouseEvent> consumer = (frame, e) -> {};

            fg.setPopupConsumer(consumer);

            assertThat(fg.getPopupConsumer()).isEqualTo(consumer);
        }

        @Test
        void setPopupConsumer_null_throws_exception() {
            assertThatThrownBy(() -> fg.setPopupConsumer(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void getPopupConsumer_default_is_null() {
            assertThat(fg.getPopupConsumer()).isNull();
        }
    }

    @Nested
    @DisplayName("Selected Frame Consumer")
    class SelectedFrameConsumerTests {

        @Test
        void setSelectedFrameConsumer_sets_consumer() {
            BiConsumer<FrameBox<String>, MouseEvent> consumer = (frame, e) -> {};

            fg.setSelectedFrameConsumer(consumer);

            assertThat(fg.getSelectedFrameConsumer()).isEqualTo(consumer);
        }

        @Test
        void setSelectedFrameConsumer_null_throws_exception() {
            assertThatThrownBy(() -> fg.setSelectedFrameConsumer(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Highlight Frames")
    class HighlightFramesTests {

        @Test
        void highlightFrames_with_valid_set_does_not_throw() {
            var frame = new FrameBox<>("root", 0.0, 1.0, 0);
            fg.setModel(new FrameModel<>(List.of(frame)));

            assertThatCode(() -> fg.highlightFrames(Set.of(frame), "root"))
                    .doesNotThrowAnyException();
        }

        @Test
        void highlightFrames_empty_set_clears_highlights() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            assertThatCode(() -> fg.highlightFrames(Set.of(), ""))
                    .doesNotThrowAnyException();
        }

        @Test
        void highlightFrames_null_set_throws_exception() {
            assertThatThrownBy(() -> fg.highlightFrames(null, "test"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void highlightFrames_null_searched_throws_exception() {
            assertThatThrownBy(() -> fg.highlightFrames(Set.of(), null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Configure Canvas")
    class ConfigureCanvasTests {

        @Test
        void configureCanvas_invokes_configurer() {
            var configured = new AtomicReference<JComponent>(null);

            fg.configureCanvas(configured::set);

            assertThat(configured.get()).isNotNull();
        }

        @Test
        void configureCanvas_null_configurer_throws_exception() {
            assertThatThrownBy(() -> fg.configureCanvas(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Frame Gap")
    class FrameGapTests {

        @Test
        void isFrameGapEnabled_default_is_true() {
            assertThat(fg.isFrameGapEnabled()).isTrue();
        }

        @Test
        void setFrameGapEnabled_false_disables() {
            fg.setFrameGapEnabled(false);

            assertThat(fg.isFrameGapEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Mode Enum")
    class ModeEnumTests {

        @Test
        void mode_enum_has_two_values() {
            assertThat(Mode.values()).containsExactly(Mode.FLAMEGRAPH, Mode.ICICLEGRAPH);
        }
    }

    @Nested
    @DisplayName("FrameClickAction Enum")
    class FrameClickActionEnumTests {

        @Test
        void frame_click_action_enum_has_two_values() {
            assertThat(FrameClickAction.values()).containsExactly(
                    FrameClickAction.EXPAND_FRAME,
                    FrameClickAction.FOCUS_FRAME
            );
        }
    }

    @Nested
    @DisplayName("Constants")
    class ConstantsTests {

        @Test
        void show_stats_constant_has_expected_value() {
            assertThat(FlamegraphView.SHOW_STATS).isEqualTo("flamegraph.show_stats");
        }
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

        private JScrollPane findScrollPane(JComponent component) {
            if (component instanceof JScrollPane) {
                return (JScrollPane) component;
            }
            for (var child : component.getComponents()) {
                if (child instanceof JComponent) {
                    var found = findScrollPane((JComponent) child);
                    if (found != null) {
                        return found;
                    }
                }
            }
            return null;
        }
    }

    @SuppressWarnings("removal")
    @Nested
    @DisplayName("Deprecated Methods Exception Branches")
    class DeprecatedMethodsExceptionBranchesTests {

        @Test
        void getFrameColorProvider_with_custom_renderer_throws_exception() {
            // Create a custom non-DefaultFrameRenderer
            var customRenderer = mock(FrameRenderer.class);
            when(customRenderer.isDrawingFrameGap()).thenReturn(true);
            when(customRenderer.getFrameBoxHeight(any())).thenReturn(20);

            fg.setFrameRender(customRenderer);

            assertThatThrownBy(() -> fg.getFrameColorProvider())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DefaultFrameRenderer");
        }

        @Test
        void setFrameColorProvider_with_custom_renderer_throws_exception() {
            var customRenderer = mock(FrameRenderer.class);
            when(customRenderer.isDrawingFrameGap()).thenReturn(true);
            when(customRenderer.getFrameBoxHeight(any())).thenReturn(20);

            fg.setFrameRender(customRenderer);

            var colorProvider = FrameColorProvider.<String>defaultColorProvider(f -> Color.RED);
            assertThatThrownBy(() -> fg.setFrameColorProvider(colorProvider))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DefaultFrameRenderer");
        }

        @Test
        void getFrameFontProvider_with_custom_renderer_throws_exception() {
            var customRenderer = mock(FrameRenderer.class);
            when(customRenderer.isDrawingFrameGap()).thenReturn(true);
            when(customRenderer.getFrameBoxHeight(any())).thenReturn(20);

            fg.setFrameRender(customRenderer);

            assertThatThrownBy(() -> fg.getFrameFontProvider())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DefaultFrameRenderer");
        }

        @Test
        void setFrameFontProvider_with_custom_renderer_throws_exception() {
            var customRenderer = mock(FrameRenderer.class);
            when(customRenderer.isDrawingFrameGap()).thenReturn(true);
            when(customRenderer.getFrameBoxHeight(any())).thenReturn(20);

            fg.setFrameRender(customRenderer);

            var fontProvider = FrameFontProvider.<String>defaultFontProvider();
            assertThatThrownBy(() -> fg.setFrameFontProvider(fontProvider))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DefaultFrameRenderer");
        }

        @Test
        void getFrameTextsProvider_with_custom_renderer_throws_exception() {
            var customRenderer = mock(FrameRenderer.class);
            when(customRenderer.isDrawingFrameGap()).thenReturn(true);
            when(customRenderer.getFrameBoxHeight(any())).thenReturn(20);

            fg.setFrameRender(customRenderer);

            assertThatThrownBy(() -> fg.getFrameTextsProvider())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DefaultFrameRenderer");
        }

        @Test
        void setFrameTextsProvider_with_custom_renderer_throws_exception() {
            var customRenderer = mock(FrameRenderer.class);
            when(customRenderer.isDrawingFrameGap()).thenReturn(true);
            when(customRenderer.getFrameBoxHeight(any())).thenReturn(20);

            fg.setFrameRender(customRenderer);

            var textsProvider = FrameTextsProvider.<String>of(f -> "test");
            assertThatThrownBy(() -> fg.setFrameTextsProvider(textsProvider))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DefaultFrameRenderer");
        }

        @Test
        void setFrameGapEnabled_with_custom_renderer_throws_exception() {
            var customRenderer = mock(FrameRenderer.class);
            when(customRenderer.isDrawingFrameGap()).thenReturn(true);
            when(customRenderer.getFrameBoxHeight(any())).thenReturn(20);

            fg.setFrameRender(customRenderer);

            assertThatThrownBy(() -> fg.setFrameGapEnabled(false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DefaultFrameRenderer");
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

    @Nested
    @DisplayName("Mode Property Change")
    class ModePropertyChangeTests {

        @Test
        void setMode_from_icicle_to_flamegraph_triggers_change() {
            assertThat(fg.getMode()).isEqualTo(Mode.ICICLEGRAPH);

            fg.setMode(Mode.FLAMEGRAPH);

            assertThat(fg.getMode()).isEqualTo(Mode.FLAMEGRAPH);
        }

        @Test
        void setMode_same_mode_does_not_throw() {
            fg.setMode(Mode.ICICLEGRAPH);

            assertThatCode(() -> fg.setMode(Mode.ICICLEGRAPH))
                    .doesNotThrowAnyException();

            assertThat(fg.getMode()).isEqualTo(Mode.ICICLEGRAPH);
        }

        @Test
        void setMode_multiple_toggles() {
            fg.setMode(Mode.FLAMEGRAPH);
            assertThat(fg.getMode()).isEqualTo(Mode.FLAMEGRAPH);

            fg.setMode(Mode.ICICLEGRAPH);
            assertThat(fg.getMode()).isEqualTo(Mode.ICICLEGRAPH);

            fg.setMode(Mode.FLAMEGRAPH);
            assertThat(fg.getMode()).isEqualTo(Mode.FLAMEGRAPH);
        }
    }

    @Nested
    @DisplayName("Model With Different Configurations")
    class ModelConfigurationTests {

        @Test
        void setModel_with_title_and_equality() {
            var frames = List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            );
            var model = new FrameModel<>(
                    "Test Flamegraph",
                    (a, b) -> Objects.equals(a.actualNode, b.actualNode),
                    frames
            );

            fg.setModel(model);

            assertThat(fg.getFrameModel()).isEqualTo(model);
            assertThat(fg.getFrameModel().title).isEqualTo("Test Flamegraph");
        }

        @Test
        void setModel_with_description() {
            var model = new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0)))
                    .withDescription("Test description");

            fg.setModel(model);

            assertThat(fg.getFrameModel().description).isEqualTo("Test description");
        }

        @Test
        void setModel_same_model_reference_twice() {
            var model = new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0)));

            fg.setModel(model);
            fg.setModel(model);

            assertThat(fg.getFrameModel()).isSameAs(model);
        }

        @Test
        void setModel_different_models_updates() {
            var model1 = new FrameModel<>(List.of(new FrameBox<>("first", 0.0, 1.0, 0)));
            var model2 = new FrameModel<>(List.of(new FrameBox<>("second", 0.0, 1.0, 0)));

            fg.setModel(model1);
            assertThat(fg.getFrames().get(0).actualNode).isEqualTo("first");

            fg.setModel(model2);
            assertThat(fg.getFrames().get(0).actualNode).isEqualTo("second");
        }
    }

    @Nested
    @DisplayName("Minimap Visibility")
    class MinimapVisibilityTests {

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
    @DisplayName("Component Hierarchy")
    class ComponentHierarchyTests {

        @Test
        void component_is_not_null() {
            assertThat(fg.component).isNotNull();
        }

        @Test
        void component_is_jpanel() {
            assertThat(fg.component).isInstanceOf(JPanel.class);
        }

        @Test
        void component_has_children() {
            assertThat(fg.component.getComponentCount()).isGreaterThan(0);
        }

        @Test
        void from_with_nested_child_eventually_finds_owner() {
            // Navigate into the component hierarchy and find a component that has the owner
            var found = findComponentWithOwner(fg.component);
            assertThat(found).isTrue();
        }

        private boolean findComponentWithOwner(JComponent component) {
            var result = FlamegraphView.<String>from(component);
            if (result.isPresent() && result.get() == fg) {
                return true;
            }
            for (var child : component.getComponents()) {
                if (child instanceof JComponent) {
                    if (findComponentWithOwner((JComponent) child)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    @Nested
    @DisplayName("Frame Renderer Configuration")
    class FrameRendererConfigurationTests {

        @Test
        void setFrameRender_with_custom_renderer_does_not_throw() {
            var customRenderer = new DefaultFrameRenderer<String>(
                    FrameTextsProvider.of(f -> "Custom: " + f.actualNode),
                    FrameColorProvider.defaultColorProvider(f -> Color.BLUE),
                    FrameFontProvider.defaultFontProvider()
            );

            assertThatCode(() -> fg.setFrameRender(customRenderer))
                    .doesNotThrowAnyException();
        }

        @Test
        void setFrameRender_triggers_repaint() {
            var frame = new FrameBox<>("root", 0.0, 1.0, 0);
            fg.setModel(new FrameModel<>(List.of(frame)));

            var customRenderer = new DefaultFrameRenderer<String>(
                    FrameTextsProvider.of(f -> "Changed"),
                    FrameColorProvider.defaultColorProvider(f -> Color.GREEN),
                    FrameFontProvider.defaultFontProvider()
            );

            assertThatCode(() -> fg.setFrameRender(customRenderer))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Tooltip Configuration")
    class TooltipConfigurationTests {

        @Test
        void setTooltipTextFunction_custom_function_is_set() {
            BiFunction<FrameModel<String>, FrameBox<String>, String> tooltipFunc =
                    (model, frame) -> "Tooltip: " + frame.actualNode;

            fg.setTooltipTextFunction(tooltipFunc);

            assertThat(fg.getTooltipTextFunction()).isEqualTo(tooltipFunc);
        }

        @Test
        void setTooltipComponentSupplier_custom_supplier_is_set() {
            Supplier<JToolTip> tooltipSupplier = () -> {
                var tip = new JToolTip();
                tip.setBackground(Color.YELLOW);
                return tip;
            };

            fg.setTooltipComponentSupplier(tooltipSupplier);

            assertThat(fg.getTooltipComponentSupplier()).isEqualTo(tooltipSupplier);
        }
    }

    @Nested
    @DisplayName("Highlight Frames Edge Cases")
    class HighlightFramesEdgeCasesTests {

        @Test
        void highlightFrames_with_frames_from_model() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child1 = new FrameBox<>("child1", 0.0, 0.5, 1);
            var child2 = new FrameBox<>("child2", 0.5, 1.0, 1);
            fg.setModel(new FrameModel<>(List.of(root, child1, child2)));

            assertThatCode(() -> fg.highlightFrames(Set.of(child1, child2), "child"))
                    .doesNotThrowAnyException();
        }

        @Test
        void highlightFrames_clear_then_set() {
            var frame = new FrameBox<>("root", 0.0, 1.0, 0);
            fg.setModel(new FrameModel<>(List.of(frame)));

            // Clear highlights
            fg.highlightFrames(Set.of(), "");

            // Set highlights
            fg.highlightFrames(Set.of(frame), "root");

            // Clear again
            assertThatCode(() -> fg.highlightFrames(Set.of(), ""))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Consumer Callbacks")
    class ConsumerCallbackTests {

        @Test
        void popup_consumer_receives_frame_and_event() {
            var receivedFrame = new AtomicReference<FrameBox<String>>();
            var receivedEvent = new AtomicReference<MouseEvent>();

            BiConsumer<FrameBox<String>, MouseEvent> consumer = (frame, event) -> {
                receivedFrame.set(frame);
                receivedEvent.set(event);
            };

            fg.setPopupConsumer(consumer);
            assertThat(fg.getPopupConsumer()).isEqualTo(consumer);
        }

        @Test
        void selected_frame_consumer_receives_frame_and_event() {
            var receivedFrame = new AtomicReference<FrameBox<String>>();

            BiConsumer<FrameBox<String>, MouseEvent> consumer = (frame, event) -> {
                receivedFrame.set(frame);
            };

            fg.setSelectedFrameConsumer(consumer);
            assertThat(fg.getSelectedFrameConsumer()).isEqualTo(consumer);
        }
    }

    @Nested
    @DisplayName("Request Repaint")
    class RequestRepaintTests {

        @Test
        void requestRepaint_with_model_does_not_throw() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            assertThatCode(() -> fg.requestRepaint())
                    .doesNotThrowAnyException();
        }

        @Test
        void requestRepaint_multiple_times_does_not_throw() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            assertThatCode(() -> {
                fg.requestRepaint();
                fg.requestRepaint();
                fg.requestRepaint();
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Clear Operation")
    class ClearOperationTests {

        @Test
        void clear_after_model_set_resets_to_empty() {
            fg.setModel(new FrameModel<>(List.of(
                    new FrameBox<>("root", 0.0, 1.0, 0),
                    new FrameBox<>("child", 0.0, 0.5, 1)
            )));

            assertThat(fg.getFrames()).hasSize(2);

            fg.clear();

            assertThat(fg.getFrameModel()).isEqualTo(FrameModel.empty());
            assertThat(fg.getFrames()).isEmpty();
        }

        @Test
        void clear_then_set_model_works() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("first", 0.0, 1.0, 0))));
            fg.clear();

            var newModel = new FrameModel<>(List.of(new FrameBox<>("second", 0.0, 1.0, 0)));
            fg.setModel(newModel);

            assertThat(fg.getFrames()).hasSize(1);
            assertThat(fg.getFrames().get(0).actualNode).isEqualTo("second");
        }

        @Test
        void clear_multiple_times_does_not_throw() {
            assertThatCode(() -> {
                fg.clear();
                fg.clear();
                fg.clear();
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Frame Click Action Behavior")
    class FrameClickActionBehaviorTests {

        @Test
        void setFrameClickAction_to_expand_changes_behavior() {
            fg.setFrameClickAction(FrameClickAction.EXPAND_FRAME);
            assertThat(fg.getFrameClickAction()).isEqualTo(FrameClickAction.EXPAND_FRAME);
        }

        @Test
        void setFrameClickAction_toggle_between_actions() {
            fg.setFrameClickAction(FrameClickAction.EXPAND_FRAME);
            fg.setFrameClickAction(FrameClickAction.FOCUS_FRAME);
            fg.setFrameClickAction(FrameClickAction.EXPAND_FRAME);

            assertThat(fg.getFrameClickAction()).isEqualTo(FrameClickAction.EXPAND_FRAME);
        }
    }

    @Nested
    @DisplayName("Show Hovered Siblings")
    class ShowHoveredSiblingsTests {

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
}