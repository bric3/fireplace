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

import io.github.bric3.fireplace.core.ui.fixtures.SwingEdtExtension;
import io.github.bric3.fireplace.flamegraph.FlamegraphView.FrameClickAction;
import io.github.bric3.fireplace.flamegraph.FlamegraphView.Mode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * Basic tests for {@link FlamegraphView} covering construction, mode, frame gap, constants, and enums.
 */
@DisplayName("FlamegraphView - Basic")
@org.junit.jupiter.api.extension.ExtendWith(SwingEdtExtension.class)
class FlamegraphView_BasicApiTest {

    private FlamegraphView<String> fg;

    @BeforeEach
    void setUp() {
        fg = new FlamegraphView<>();
    }

    @Test
    void rejects_a_null_zoom_override() {
        assertThatThrownBy(() -> fg.overrideZoomAction(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_null_configuration_arguments() {
        assertThatThrownBy(() -> fg.configureCanvas(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> fg.setSelectedFrameConsumer(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> fg.setPopupConsumer(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> fg.setMinimapShadeColorSupplier(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> fg.highlightFrames(null, "test")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> fg.highlightFrames(Set.of(), null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void defaults_and_optional_configuration_remain_available() {
        assertThat(fg.getFrameClickAction()).isEqualTo(FrameClickAction.FOCUS_FRAME);
        assertThat(fg.getSelectedFrameConsumer()).isNull();
        assertThat(fg.getPopupConsumer()).isNull();
        assertThat(fg.getMinimapShadeColorSupplier()).isNull();
        Supplier<Color> shade = () -> new Color(100, 100, 100, 100);
        fg.setMinimapShadeColorSupplier(shade);
        assertThat(fg.getMinimapShadeColorSupplier()).isSameAs(shade);
        fg.putClientProperty(FlamegraphView.SHOW_STATS, Boolean.TRUE);
        assertThat(fg.<Boolean>getClientProperty(FlamegraphView.SHOW_STATS)).isTrue();
        fg.putClientProperty(FlamegraphView.SHOW_STATS, null);
        assertThat(fg.<Boolean>getClientProperty(FlamegraphView.SHOW_STATS)).isNull();
    }

    @Test
    void minimap_and_hovered_siblings_are_enabled_by_default_and_can_be_disabled() {
        assertThat(fg.isShowMinimap()).isTrue();
        assertThat(fg.isShowHoveredSiblings()).isTrue();
        fg.setShowMinimap(false);
        fg.setShowHoveredSiblings(false);
        assertThat(fg.isShowMinimap()).isFalse();
        assertThat(fg.isShowHoveredSiblings()).isFalse();
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

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
        void mode_defaults_to_icicle_and_can_switch_both_ways() {
            assertThat(fg.getMode()).isEqualTo(Mode.ICICLEGRAPH);
            fg.setMode(Mode.FLAMEGRAPH);
            assertThat(fg.getMode()).isEqualTo(Mode.FLAMEGRAPH);
            fg.setMode(Mode.FLAMEGRAPH);
            assertThat(fg.getMode()).isEqualTo(Mode.FLAMEGRAPH);
            fg.setMode(Mode.ICICLEGRAPH);
            assertThat(fg.getMode()).isEqualTo(Mode.ICICLEGRAPH);
        }
    }

    @Nested
    @DisplayName("Deprecated API")
    @SuppressWarnings({"deprecation", "removal"})
    class DeprecatedApiTests {

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
        @DisplayName("Frame Providers")
        class FrameProvidersTests {

            @Test
            void frame_providers_not_null_by_default() {
                assertSoftly(softly -> {
                    softly.assertThat(fg.getFrameColorProvider()).isNotNull();
                    softly.assertThat(fg.getFrameFontProvider()).isNotNull();
                    softly.assertThat(fg.getFrameTextsProvider()).isNotNull();
                });
            }

            @Test
            void setRenderConfiguration_configures_all_providers() {
                var frameTextsProvider = FrameTextsProvider.<String>empty();
                var frameColorProvider = FrameColorProvider.<String>defaultColorProvider(box -> Color.BLACK);
                var frameFontProvider = FrameFontProvider.<String>defaultFontProvider();

                fg.setRenderConfiguration(
                        frameTextsProvider,
                        frameColorProvider,
                        frameFontProvider
                );

                assertSoftly(softly -> {
                    softly.assertThat(fg.getFrameTextsProvider()).isEqualTo(frameTextsProvider);
                    softly.assertThat(fg.getFrameColorProvider()).isEqualTo(frameColorProvider);
                    softly.assertThat(fg.getFrameFontProvider()).isEqualTo(frameFontProvider);
                });
            }

            @Test
            void setFrameTextsProvider_updates_provider() {
                var frameTextsProvider = FrameTextsProvider.<String>empty();

                fg.setFrameTextsProvider(frameTextsProvider);

                assertThat(fg.getFrameTextsProvider()).isEqualTo(frameTextsProvider);
            }

            @Test
            void setFrameColorProvider_updates_provider() {
                var frameColorProvider = FrameColorProvider.<String>defaultColorProvider(box -> Color.BLACK);

                fg.setFrameColorProvider(frameColorProvider);

                assertThat(fg.getFrameColorProvider()).isEqualTo(frameColorProvider);
            }

            @Test
            void setFrameFontProvider_updates_provider() {
                var frameFontProvider = FrameFontProvider.<String>defaultFontProvider();

                fg.setFrameFontProvider(frameFontProvider);

                assertThat(fg.getFrameFontProvider()).isEqualTo(frameFontProvider);
            }
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
    @DisplayName("Component Hierarchy")
    class ComponentHierarchyTests {

        @Test
        void from_with_nested_child_eventually_finds_owner() {
            var configured = new ArrayList<JComponent>();
            fg.configureCanvas(canvas -> {
                configured.add(canvas);
                canvas.setBackground(Color.PINK);
            });

            assertThat(configured).hasSize(1);
            var canvas = configured.get(0);
            assertThat(canvas).isNotSameAs(fg.component);
            assertThat(SwingUtilities.isDescendingFrom(canvas, fg.component)).isTrue();
            assertThat(FlamegraphView.<String>from(canvas)).contains(fg);
            assertThat(canvas.getBackground()).isEqualTo(Color.PINK);
        }

        @Test
        void leveling_a_hover_point_requires_a_flamegraph_owner() {
            var orphan = new JScrollPane();
            var event = new MouseEvent(orphan, MouseEvent.MOUSE_MOVED, 0, 0, 10, 20, 0, false);
            assertThatThrownBy(() -> FlamegraphView.HoverListener.getPointLeveledToFrameDepth(
                    event, new Rectangle(0, 0, 200, 20)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot find FlamegraphView owner");
        }
    }
}
