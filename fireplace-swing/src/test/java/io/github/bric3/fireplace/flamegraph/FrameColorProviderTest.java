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

import java.awt.*;

import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link FrameColorProvider} and its {@link FrameColorProvider.ColorModel}.
 * These tests run in headless mode without requiring a display.
 */
@DisplayName("FrameColorProvider")
class FrameColorProviderTest {

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
    @DisplayName("ColorModel")
    class ColorModelTests {

        @Test
        void defaultConstructor_usesDefaultColors() {
            FrameColorProvider.ColorModel model = new FrameColorProvider.ColorModel();

            assertThat(model.background).isEqualTo(FrameColorProvider.ColorModel.DEFAULT_FRAME_BACKGROUND_COLOR);
            assertThat(model.foreground).isEqualTo(FrameColorProvider.ColorModel.DEFAULT_FRAME_FOREGROUND_COLOR);
        }

        @Test
        void constructorWithColors_setsColors() {
            Color bg = Color.RED;
            Color fg = Color.WHITE;

            FrameColorProvider.ColorModel model = new FrameColorProvider.ColorModel(bg, fg);

            assertThat(model.background).isEqualTo(bg);
            assertThat(model.foreground).isEqualTo(fg);
        }

        @Test
        void set_updatesColors() {
            FrameColorProvider.ColorModel model = new FrameColorProvider.ColorModel();

            model.set(Color.BLUE, Color.YELLOW);

            assertThat(model.background).isEqualTo(Color.BLUE);
            assertThat(model.foreground).isEqualTo(Color.YELLOW);
        }

        @Test
        void set_returnsSameInstance() {
            FrameColorProvider.ColorModel model = new FrameColorProvider.ColorModel();

            FrameColorProvider.ColorModel result = model.set(Color.BLUE, Color.YELLOW);

            assertThat(result).isSameAs(model);
        }

        @Test
        void copy_createsNewInstance() {
            FrameColorProvider.ColorModel original = new FrameColorProvider.ColorModel(Color.RED, Color.WHITE);

            FrameColorProvider.ColorModel copy = original.copy();

            assertThat(copy).isNotSameAs(original);
            assertThat(copy.background).isEqualTo(original.background);
            assertThat(copy.foreground).isEqualTo(original.foreground);
        }

        @Test
        void copy_isIndependent() {
            FrameColorProvider.ColorModel original = new FrameColorProvider.ColorModel(Color.RED, Color.WHITE);
            FrameColorProvider.ColorModel copy = original.copy();

            copy.set(Color.BLUE, Color.GREEN);

            // Original should be unchanged
            assertThat(original.background).isEqualTo(Color.RED);
            assertThat(original.foreground).isEqualTo(Color.WHITE);
        }
    }

    @Nested
    @DisplayName("defaultColorProvider factory method")
    class DefaultColorProviderTests {

        @Test
        void nullFunction_throwsException() {
            assertThatThrownBy(() -> FrameColorProvider.defaultColorProvider(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void noFlags_usesBaseColor() {
            FrameColorProvider<String> provider = FrameColorProvider.defaultColorProvider(frame -> Color.ORANGE);
            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 0);

            FrameColorProvider.ColorModel colors = provider.getColors(frame, 0);

            assertThat(colors.background).isEqualTo(Color.ORANGE);
            assertThat(colors.foreground).isNotNull();
        }

        @Test
        void hoveredFlag_darkensBrightBackground() {
            FrameColorProvider<String> provider = FrameColorProvider.defaultColorProvider(frame -> Color.WHITE);
            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 0);

            int flags = toFlags(false, false, false, true, false, false, false, false);
            FrameColorProvider.ColorModel colors = provider.getColors(frame, flags);

            // Hovered should blend with translucent black, making it darker
            assertThat(colors.background).isNotEqualTo(Color.WHITE);
        }

        @Test
        void focusingNotInFocused_blendsDarker() {
            FrameColorProvider<String> provider = FrameColorProvider.defaultColorProvider(frame -> Color.CYAN);
            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 1);

            // FOCUSING flag set, but FOCUSED_FRAME not set
            int flags = toFlags(false, false, false, false, false, true, false, false);
            FrameColorProvider.ColorModel colors = provider.getColors(frame, flags);

            // Should be blended darker
            assertThat(colors.background).isNotEqualTo(Color.CYAN);
        }

        @Test
        void highlighting_blendsBackground() {
            FrameColorProvider<String> provider = FrameColorProvider.defaultColorProvider(frame -> Color.GREEN);
            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 1);

            // HIGHLIGHTING flag set, but not HIGHLIGHTED_FRAME
            int flags = toFlags(false, true, false, false, false, false, false, false);
            FrameColorProvider.ColorModel colors = provider.getColors(frame, flags);

            // Should be blended (with white in light mode)
            assertThat(colors.background).isNotEqualTo(Color.GREEN);
        }

        @Test
        void highlightedFrame_usesBaseColor() {
            FrameColorProvider<String> provider = FrameColorProvider.defaultColorProvider(frame -> Color.MAGENTA);
            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 1);

            // Both HIGHLIGHTING and HIGHLIGHTED_FRAME set
            int flags = toFlags(false, true, true, false, false, false, false, false);
            FrameColorProvider.ColorModel colors = provider.getColors(frame, flags);

            // Highlighted frame should use base color
            assertThat(colors.background).isEqualTo(Color.MAGENTA);
        }

        @Test
        void reusesColorModel() {
            FrameColorProvider<String> provider = FrameColorProvider.defaultColorProvider(frame -> Color.RED);
            FrameBox<String> frame1 = new FrameBox<>("test1", 0.0, 0.5, 0);
            FrameBox<String> frame2 = new FrameBox<>("test2", 0.5, 1.0, 0);

            FrameColorProvider.ColorModel colors1 = provider.getColors(frame1, 0);
            FrameColorProvider.ColorModel colors2 = provider.getColors(frame2, 0);

            // Same instance should be reused for efficiency
            assertThat(colors1).isSameAs(colors2);
        }

        @Test
        void differentBaseColors() {
            FrameColorProvider<String> provider = FrameColorProvider.defaultColorProvider(frame -> {
                if (frame.actualNode.startsWith("A")) return Color.RED;
                if (frame.actualNode.startsWith("B")) return Color.BLUE;
                return Color.GRAY;
            });

            FrameBox<String> frameA = new FrameBox<>("Apple", 0.0, 0.5, 0);
            FrameBox<String> frameB = new FrameBox<>("Banana", 0.5, 1.0, 0);
            FrameBox<String> frameC = new FrameBox<>("Cherry", 0.0, 1.0, 0);

            assertThat(provider.getColors(frameA, 0).background).isEqualTo(Color.RED);
            assertThat(provider.getColors(frameB, 0).background).isEqualTo(Color.BLUE);
            assertThat(provider.getColors(frameC, 0).background).isEqualTo(Color.GRAY);
        }

        @Test
        void foregroundContrastsWithBackground() {
            FrameColorProvider<String> provider = FrameColorProvider.defaultColorProvider(frame -> Color.WHITE);
            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 0);

            FrameColorProvider.ColorModel colors = provider.getColors(frame, 0);

            // Foreground should contrast with white background (should be dark)
            assertThat(Colors.isBright(colors.foreground)).isFalse();
        }

        @Test
        void darkBackground_whiteText() {
            FrameColorProvider<String> provider = FrameColorProvider.defaultColorProvider(frame -> Color.BLACK);
            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 0);

            FrameColorProvider.ColorModel colors = provider.getColors(frame, 0);

            // Foreground should be white on black background
            assertThat(colors.foreground).isEqualTo(Color.WHITE);
        }

        @Test
        void darkMode_blendsDifferently() {
            FrameColorProvider<String> provider = FrameColorProvider.defaultColorProvider(frame -> Color.CYAN);
            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 1);

            // HIGHLIGHTING flag set
            int flags = toFlags(false, true, false, false, false, false, false, false);

            // Copy the background colors before mode switch (ColorModel is reused)
            Colors.setDarkMode(true);
            Color darkModeBackground = provider.getColors(frame, flags).background;

            Colors.setDarkMode(false);
            Color lightModeBackground = provider.getColors(frame, flags).background;

            // Colors should be different between dark and light modes
            assertThat(darkModeBackground).isNotEqualTo(lightModeBackground);
        }
    }

    @Nested
    @DisplayName("functional interface implementation")
    class FunctionalInterfaceTests {

        @Test
        void canBeImplementedAsLambda() {
            FrameColorProvider<String> provider = (frame, flags) ->
                    new FrameColorProvider.ColorModel(Color.PINK, Color.BLACK);

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 0);
            FrameColorProvider.ColorModel colors = provider.getColors(frame, 0);

            assertThat(colors.background).isEqualTo(Color.PINK);
            assertThat(colors.foreground).isEqualTo(Color.BLACK);
        }

        @Test
        void canAccessFlags() {
            FrameColorProvider<String> provider = (frame, flags) -> {
                if (isHovered(flags)) {
                    return new FrameColorProvider.ColorModel(Color.YELLOW, Color.BLACK);
                }
                return new FrameColorProvider.ColorModel(Color.GRAY, Color.WHITE);
            };

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 0);

            int hoveredFlags = toFlags(false, false, false, true, false, false, false, false);

            assertThat(provider.getColors(frame, 0).background).isEqualTo(Color.GRAY);
            assertThat(provider.getColors(frame, hoveredFlags).background).isEqualTo(Color.YELLOW);
        }
    }
}
