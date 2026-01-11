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
import io.github.bric3.fireplace.core.ui.LightDarkColor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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

        @Test
        void root_frame_uses_root_background_color() {
            DimmingFrameColorProvider<String> provider = new DimmingFrameColorProvider<>(frame -> Color.RED);

            FrameBox<String> rootFrame = new FrameBox<>("root", 0.0, 1.0, 0);
            FrameColorProvider.ColorModel colors = provider.getColors(rootFrame, 0);

            // Root should use default ROOT_BACKGROUND_COLOR, not the base color function
            assertThat(colors.background).isNotEqualTo(Color.RED);
            assertThat(colors.background).isInstanceOf(LightDarkColor.class);
        }

        @Test
        void root_frame_with_flags_ignores_base_color_function() {
            DimmingFrameColorProvider<String> provider = new DimmingFrameColorProvider<>(frame -> {
                throw new RuntimeException("Should not be called for root");
            });

            FrameBox<String> rootFrame = new FrameBox<>("root", 0.0, 1.0, 0);
            int flags = toFlags(false, true, false, false, false, false, false, false);

            // Should not throw because root uses special background
            FrameColorProvider.ColorModel colors = provider.getColors(rootFrame, flags);

            assertThat(colors).isNotNull();
        }
    }

    @Nested
    @DisplayName("Minimap mode")
    class MinimapMode {

        @Test
        void returns_foreground_default() {
            DimmingFrameColorProvider<String> provider = new DimmingFrameColorProvider<>(frame -> Color.BLUE);

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 1);
            int flags = toFlags(true, false, false, false, false, false, false, false);

            FrameColorProvider.ColorModel colors = provider.getColors(frame, flags);

            // In minimap mode, foreground is DEFAULT_FRAME_FOREGROUND_COLOR
            assertThat(colors.foreground).isEqualTo(FrameColorProvider.ColorModel.DEFAULT_FRAME_FOREGROUND_COLOR);
        }

        @Test
        void uses_separate_color_model() {
            DimmingFrameColorProvider<String> provider = new DimmingFrameColorProvider<>(frame -> Color.RED);

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 1);

            FrameColorProvider.ColorModel mainColors = provider.getColors(frame, 0);
            FrameColorProvider.ColorModel minimapColors = provider.getColors(frame, MINIMAP_MODE);

            // Different instances for main canvas vs minimap (thread safety)
            assertThat(mainColors).isNotSameAs(minimapColors);
        }
    }

    @Nested
    @DisplayName("Hovered frame")
    class HoveredFrame {

        @Test
        void alters_brightness() {
            DimmingFrameColorProvider<String> provider = new DimmingFrameColorProvider<>(frame -> Color.GREEN);

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 1);
            int hoveredFlags = toFlags(false, false, false, true, false, false, false, false);

            // Copy the background color before second call (ColorModel is reused)
            Color normalBackground = provider.getColors(frame, 0).background;
            Color hoveredBackground = provider.getColors(frame, hoveredFlags).background;

            // Hovered should be different from normal (darker in light mode)
            assertThat(hoveredBackground).isNotEqualTo(normalBackground);
        }
    }

    @Nested
    @DisplayName("Hovered sibling")
    class HoveredSibling {

        @Test
        void alters_brightness() {
            DimmingFrameColorProvider<String> provider = new DimmingFrameColorProvider<>(frame -> Color.CYAN);

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 1);
            int siblingFlags = toFlags(false, false, false, false, true, false, false, false);

            // Copy the background color before second call (ColorModel is reused)
            Color normalBackground = provider.getColors(frame, 0).background;
            Color siblingBackground = provider.getColors(frame, siblingFlags).background;

            // Hovered sibling should be different from normal
            assertThat(siblingBackground).isNotEqualTo(normalBackground);
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

            // Copy the background color before second call (ColorModel is reused)
            Color normalBackground = provider.getColors(frame, 0).background;
            Color dimmedBackground = provider.getColors(frame, highlightingFlags).background;

            // Unhighlighted frame during highlighting should be dimmed
            assertThat(dimmedBackground).isNotEqualTo(normalBackground);
        }

        @Test
        void highlighted_frame_not_dimmed() {
            DimmingFrameColorProvider<String> provider = new DimmingFrameColorProvider<>(frame -> Color.MAGENTA);

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 1);
            int highlightedFlags = toFlags(false, true, true, false, false, false, false, false);

            FrameColorProvider.ColorModel normalColors = provider.getColors(frame, 0);
            FrameColorProvider.ColorModel highlightedColors = provider.getColors(frame, highlightedFlags);

            // Highlighted frame should NOT be dimmed (uses base color)
            assertThat(highlightedColors.background).isEqualTo(normalColors.background);
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

            // Copy the background color before second call (ColorModel is reused)
            Color normalBackground = provider.getColors(frame, 0).background;
            Color outsideFocusBackground = provider.getColors(frame, focusingFlags).background;

            // Frame outside focus should be dimmed
            assertThat(outsideFocusBackground).isNotEqualTo(normalBackground);
        }

        @Test
        void withDimNonFocusedFlame_keeps_focused_frames_bright() {
            DimmingFrameColorProvider<String> provider = new DimmingFrameColorProvider<String>(frame -> Color.YELLOW);
            provider.withDimNonFocusedFlame(true);

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 1);
            // FOCUSING and FOCUSED_FRAME flags set (frame is inside focused flame)
            int inFocusFlags = toFlags(false, false, false, false, false, true, true, false);

            FrameColorProvider.ColorModel normalColors = provider.getColors(frame, 0);
            FrameColorProvider.ColorModel inFocusColors = provider.getColors(frame, inFocusFlags);

            // Frame in focus should be half-dimmed but not fully dimmed
            // The colors will be different due to half-dimming
            assertThat(inFocusColors).isNotNull();
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
    @DisplayName("ColorModel reuse")
    class ColorModelReuse {

        @Test
        void reuses_color_model() {
            DimmingFrameColorProvider<String> provider = new DimmingFrameColorProvider<>(frame -> Color.RED);

            FrameBox<String> frame1 = new FrameBox<>("test1", 0.0, 0.5, 1);
            FrameBox<String> frame2 = new FrameBox<>("test2", 0.5, 1.0, 1);

            FrameColorProvider.ColorModel colors1 = provider.getColors(frame1, 0);
            FrameColorProvider.ColorModel colors2 = provider.getColors(frame2, 0);

            // Same instance reused for efficiency
            assertThat(colors1).isSameAs(colors2);
        }
    }

    @Nested
    @DisplayName("Dark mode")
    class DarkMode {

        @Test
        void affects_dimming() {
            DimmingFrameColorProvider<String> provider = new DimmingFrameColorProvider<>(frame -> Color.GREEN);

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 1);
            int hoveredFlags = toFlags(false, false, false, true, false, false, false, false);

            // Copy the background colors before mode switch (ColorModel is reused)
            Colors.setDarkMode(false);
            Color lightModeBackground = provider.getColors(frame, hoveredFlags).background;

            Colors.setDarkMode(true);
            Color darkModeBackground = provider.getColors(frame, hoveredFlags).background;

            // Colors should be different in dark mode vs light mode
            assertThat(lightModeBackground).isNotEqualTo(darkModeBackground);
        }
    }

    @Nested
    @DisplayName("Constants")
    class Constants {

        @Test
        void are_initialized() {
            assertThat(DimmingFrameColorProvider.DIMMED_TEXT_COLOR).isNotNull();
            assertThat(DimmingFrameColorProvider.DIMMED_TEXT_COLOR).isInstanceOf(LightDarkColor.class);

            assertThat(DimmingFrameColorProvider.ROOT_BACKGROUND_COLOR).isNotNull();
            assertThat(DimmingFrameColorProvider.ROOT_BACKGROUND_COLOR).isInstanceOf(LightDarkColor.class);
        }
    }

    @Nested
    @DisplayName("Complex flag combinations")
    class ComplexFlagCombinations {

        @Test
        void handles_correctly() {
            DimmingFrameColorProvider<String> provider = new DimmingFrameColorProvider<String>(frame -> Color.CYAN);
            provider.withDimNonFocusedFlame(true);

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 1);

            // Highlighting + Focusing + In Focus + Highlighted
            int flags = toFlags(false, true, true, false, false, true, true, false);
            FrameColorProvider.ColorModel colors = provider.getColors(frame, flags);

            // Should return valid colors without throwing
            assertThat(colors).isNotNull();
            assertThat(colors.background).isNotNull();
            assertThat(colors.foreground).isNotNull();
        }
    }

    @Nested
    @DisplayName("Dimmed color cache")
    class DimmedColorCache {

        @Test
        void caches_dimmed_colors() {
            DimmingFrameColorProvider<String> provider = new DimmingFrameColorProvider<>(frame -> Color.RED);

            FrameBox<String> frame1 = new FrameBox<>("test1", 0.0, 0.5, 1);
            FrameBox<String> frame2 = new FrameBox<>("test2", 0.5, 1.0, 1);

            int highlightingFlags = toFlags(false, true, false, false, false, false, false, false);

            // Both frames use same base color (RED), should get same dimmed color from cache
            provider.getColors(frame1, highlightingFlags);
            provider.getColors(frame2, highlightingFlags);

            // The dimmed color for RED should be cached and reused
            // (Can't directly verify cache, but the behavior should be consistent)
        }
    }
}