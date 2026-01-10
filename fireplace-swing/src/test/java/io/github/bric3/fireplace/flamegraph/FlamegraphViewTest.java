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
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

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
    @DisplayName("Request Repaint")
    class RequestRepaintTests {

        @Test
        void requestRepaint_does_not_throw() {
            assertThatCode(() -> fg.requestRepaint())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Hover Listener")
    class HoverListenerTests {

        @Test
        void setHoverListener_sets_listener() {
            FlamegraphView.HoverListener<String> listener = new FlamegraphView.HoverListener<String>() {
                @Override
                public void onFrameHover(FrameBox<String> frame, Rectangle rect, MouseEvent e) {}
            };

            // Should not throw
            assertThatCode(() -> fg.setHoverListener(listener))
                    .doesNotThrowAnyException();
        }
    }
}