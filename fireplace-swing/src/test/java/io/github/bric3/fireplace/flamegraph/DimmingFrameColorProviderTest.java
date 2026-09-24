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

import io.github.bric3.fireplace.core.ui.Colors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.*;

import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DimmingFrameColorProvider}.
 * These tests run in headless mode without requiring a display.
 */
@DisplayName("DimmingFrameColorProvider")
class DimmingFrameColorProviderTest {

    private boolean originalDarkMode;

    @BeforeEach
    void setUp() {
        originalDarkMode = Colors.isDarkMode();
        Colors.setDarkMode(false);
    }

    @AfterEach
    void tearDown() {
        Colors.setDarkMode(originalDarkMode);
    }

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        void with_base_color_function_creates_provider() {
            DimmingFrameColorProvider<String> provider = new DimmingFrameColorProvider<>(frame -> Color.ORANGE);

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 1);
            FrameColorProvider.ColorModel colors = provider.getColors(frame, 0);

            assertThat(colors.background).isEqualTo(Color.ORANGE);
        }
    }

    @Nested
    @DisplayName("Root frame handling")
    class RootFrameHandling {

        @ParameterizedTest
        @ValueSource(ints = {0, HIGHLIGHTING})
        void root_frame_uses_root_colors_without_consulting_base_color_function(int flags) {
            DimmingFrameColorProvider<String> provider = new DimmingFrameColorProvider<>(frame -> {
                throw new RuntimeException("Should not be called for root");
            });

            FrameBox<String> rootFrame = new FrameBox<>("root", 0.0, 1.0, 0);
            FrameColorProvider.ColorModel colors = provider.getColors(rootFrame, flags);

            assertThat(colors.background).isEqualTo(DimmingFrameColorProvider.ROOT_BACKGROUND_COLOR);
            assertThat(colors.foreground).isEqualTo(Colors.foregroundColor(DimmingFrameColorProvider.ROOT_BACKGROUND_COLOR));
        }
    }

    @Nested
    @DisplayName("Minimap mode")
    class MinimapMode {

        @Test
        void minimap_keeps_base_background_despite_hover_focus_and_highlighting() {
            DimmingFrameColorProvider<String> provider = new DimmingFrameColorProvider<>(frame -> Color.BLUE);

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 1);
            int flags = MINIMAP_MODE | HOVERED | HIGHLIGHTING | FOCUSING;

            FrameColorProvider.ColorModel colors = provider.getColors(frame, flags);

            assertThat(colors.background).isEqualTo(Color.BLUE);
            assertThat(colors.foreground).isEqualTo(FrameColorProvider.ColorModel.DEFAULT_FRAME_FOREGROUND_COLOR);
        }

        @Test
        void main_and_minimap_colors_do_not_overwrite_each_other() {
            DimmingFrameColorProvider<Color> provider = new DimmingFrameColorProvider<>(frame -> frame.actualNode);
            var red = new FrameBox<>(Color.RED, 0.0, 0.5, 1);
            var blue = new FrameBox<>(Color.BLUE, 0.5, 1.0, 1);

            var mainColors = provider.getColors(red, HIGHLIGHTING);
            var minimapColors = provider.getColors(blue, MINIMAP_MODE);

            assertThat(mainColors.background).isEqualTo(Colors.dim(Color.RED));
            assertThat(mainColors.foreground).isEqualTo(DimmingFrameColorProvider.DIMMED_TEXT_COLOR);
            provider.getColors(red, 0);
            assertThat(minimapColors.background).isEqualTo(Color.BLUE);
            assertThat(minimapColors.foreground).isEqualTo(FrameColorProvider.ColorModel.DEFAULT_FRAME_FOREGROUND_COLOR);
        }
    }

    @Nested
    @DisplayName("Hover colors")
    class HoveredFrame {

        @ParameterizedTest(name = "dark mode {0}, sibling {1}")
        @CsvSource({"false, false", "false, true", "true, false", "true, true"})
        void hover_uses_theme_adjusted_background_and_matching_foreground(boolean darkMode, boolean sibling) {
            Colors.setDarkMode(darkMode);
            var base = new Color(130, 150, 170);
            DimmingFrameColorProvider<String> provider = new DimmingFrameColorProvider<>(frame -> base);

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 1);
            var colors = provider.getColors(frame, sibling ? HOVERED_SIBLING : HOVERED);
            Color expectedBackground = darkMode ? Colors.brighter(base, 1.1f, 0.95f) : Colors.darker(base, 1.15f);

            assertThat(colors.background).isEqualTo(expectedBackground);
            assertThat(colors.foreground).isEqualTo(Colors.foregroundColor(expectedBackground));
        }
    }

    @Nested
    @DisplayName("Highlighting")
    class Highlighting {

        @Test
        void dims_unhighlighted_frames() {
            DimmingFrameColorProvider<String> provider = new DimmingFrameColorProvider<>(frame -> Color.ORANGE);

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 1);
            int highlightingFlags = toFlags(false, true, false, false, false, false, false, false);

            var colors = provider.getColors(frame, highlightingFlags);

            assertThat(colors.background).isEqualTo(Colors.dim(Color.ORANGE));
            assertThat(colors.foreground).isEqualTo(DimmingFrameColorProvider.DIMMED_TEXT_COLOR);
        }

        @Test
        void highlighted_frame_not_dimmed() {
            DimmingFrameColorProvider<String> provider = new DimmingFrameColorProvider<>(frame -> Color.MAGENTA);

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 1);
            int highlightedFlags = toFlags(false, true, true, false, false, false, false, false);

            FrameColorProvider.ColorModel highlightedColors = provider.getColors(frame, highlightedFlags);

            assertThat(highlightedColors.background).isEqualTo(Color.MAGENTA);
            assertThat(highlightedColors.foreground).isEqualTo(Colors.foregroundColor(Color.MAGENTA));
        }
    }

    @Nested
    @DisplayName("Configuration methods")
    class ConfigurationMethods {

        @Test
        void withRootBackgroundColor_changes_root_color() {
            Color customRootColor = Color.PINK;
            DimmingFrameColorProvider<String> provider = new DimmingFrameColorProvider<String>(frame -> Color.RED);
            provider.withRootBackgroundColor(customRootColor);

            FrameBox<String> rootFrame = new FrameBox<>("root", 0.0, 1.0, 0);
            FrameColorProvider.ColorModel colors = provider.getColors(rootFrame, 0);

            assertThat(colors.background).isEqualTo(customRootColor);
        }

        @Test
        void withDimmedTextColor_changes_text_color_for_dimmed_frames() {
            Color customDimmedText = Color.GRAY;
            DimmingFrameColorProvider<String> provider = new DimmingFrameColorProvider<String>(frame -> Color.RED);
            provider.withDimmedTextColor(customDimmedText);

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 1);
            int highlightingFlags = toFlags(false, true, false, false, false, false, false, false);

            FrameColorProvider.ColorModel colors = provider.getColors(frame, highlightingFlags);

            // Dimmed text color should be the custom one
            assertThat(colors.foreground).isEqualTo(customDimmedText);
        }

        @Test
        void withDimNonFocusedFlame_dims_frames_outside_focus() {
            DimmingFrameColorProvider<String> provider = new DimmingFrameColorProvider<String>(frame -> Color.BLUE);
            provider.withDimNonFocusedFlame(true);

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 1);
            // FOCUSING flag set, but NOT FOCUSED_FRAME (frame is outside focused flame)
            int focusingFlags = toFlags(false, false, false, false, false, true, false, false);

            var beforeFocus = provider.getColors(frame, 0);
            assertThat(beforeFocus.background).isEqualTo(Color.BLUE);
            assertThat(beforeFocus.foreground).isEqualTo(Colors.foregroundColor(Color.BLUE));

            var colors = provider.getColors(frame, focusingFlags);

            assertThat(colors.background).isEqualTo(Colors.dim(Color.BLUE));
            assertThat(colors.foreground).isEqualTo(DimmingFrameColorProvider.DIMMED_TEXT_COLOR);
        }

        @Test
        void withDimNonFocusedFlame_half_dims_unhighlighted_focused_frames() {
            DimmingFrameColorProvider<String> provider = new DimmingFrameColorProvider<String>(frame -> Color.YELLOW);
            provider.withDimNonFocusedFlame(true);

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 1);
            // FOCUSING and FOCUSED_FRAME flags set (frame is inside focused flame)
            int inFocusFlags = toFlags(false, false, false, false, false, true, true, false);

            FrameColorProvider.ColorModel inFocusColors = provider.getColors(frame, inFocusFlags);

            assertThat(inFocusColors.background).isEqualTo(Colors.halfDim(Color.YELLOW));
            assertThat(inFocusColors.foreground).isEqualTo(
                    Colors.withAlpha(Colors.foregroundColor(Colors.halfDim(Color.YELLOW)), 0.74f)
            );
        }

        @Test
        void chained_configuration_returns_this_instance() {
            DimmingFrameColorProvider<String> provider = new DimmingFrameColorProvider<>(frame -> Color.RED);

            DimmingFrameColorProvider<String> result = provider
                    .withRootBackgroundColor(Color.WHITE)
                    .withDimmedTextColor(Color.GRAY)
                    .withDimNonFocusedFlame(true);

            assertThat(result).isSameAs(provider);
        }
    }

    @Nested
    @DisplayName("Complex flag combinations")
    class ComplexFlagCombinations {

        @Test
        void highlighted_focused_frame_keeps_base_colors() {
            DimmingFrameColorProvider<String> provider = new DimmingFrameColorProvider<String>(frame -> Color.CYAN);
            provider.withDimNonFocusedFlame(true);

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 1);

            // Highlighting + Focusing + In Focus + Highlighted
            int flags = toFlags(false, true, true, false, false, true, true, false);
            FrameColorProvider.ColorModel colors = provider.getColors(frame, flags);

            assertThat(colors.background).isEqualTo(Color.CYAN);
            assertThat(colors.foreground).isEqualTo(Colors.foregroundColor(Color.CYAN));
        }
    }
}
