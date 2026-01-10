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
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * Basic tests for {@link FlamegraphView} covering construction, mode, frame gap, constants, and enums.
 */
@DisplayName("FlamegraphView - Basic")
class FlamegraphViewBasicTest {

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
}