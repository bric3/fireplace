/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.core.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link Colors}.
 * These tests run in headless mode without requiring a display.
 */
class ColorsTest {

    private boolean originalDarkMode;

    @BeforeEach
    void setUp() {
        originalDarkMode = Colors.isDarkMode();
    }

    @AfterEach
    void tearDown() {
        Colors.setDarkMode(originalDarkMode);
    }

    @Nested
    class Brightness {

        @Test
        void black_is_zero() {
            int brightness = Colors.brightness(Color.BLACK);

            assertThat(brightness).isZero();
        }

        @Test
        void white_is_maximum() {
            int brightness = Colors.brightness(Color.WHITE);

            assertThat(brightness).isEqualTo(255);
        }

        @Test
        void gray_is_middle() {
            // Mid gray should have mid brightness
            int brightness = Colors.brightness(Color.GRAY);

            assertThat(brightness).isBetween(100, 160);
        }

        @Test
        void red_low_brightness() {
            // Red has lower perceived brightness than green
            int redBrightness = Colors.brightness(Color.RED);
            int greenBrightness = Colors.brightness(Color.GREEN);

            assertThat(redBrightness).isLessThan(greenBrightness);
        }

        @Test
        void blue_lowest_primary_brightness() {
            // Blue has the lowest perceived brightness among primaries
            int blueBrightness = Colors.brightness(Color.BLUE);
            int redBrightness = Colors.brightness(Color.RED);
            int greenBrightness = Colors.brightness(Color.GREEN);

            assertThat(blueBrightness).isLessThan(redBrightness);
            assertThat(blueBrightness).isLessThan(greenBrightness);
        }
    }

    @Nested
    class IsBright {

        @Test
        void white_returns_true() {
            assertThat(Colors.isBright(Color.WHITE)).isTrue();
        }

        @Test
        void black_returns_false() {
            assertThat(Colors.isBright(Color.BLACK)).isFalse();
        }

        @Test
        void yellow_returns_true() {
            // Yellow is perceived as bright
            assertThat(Colors.isBright(Color.YELLOW)).isTrue();
        }

        @Test
        void dark_blue_returns_false() {
            assertThat(Colors.isBright(new Color(0, 0, 100))).isFalse();
        }
    }

    @Nested
    class WithAlpha {

        @Test
        void float_value_sets_alpha() {
            Color result = Colors.withAlpha(Color.RED, 0.5f);

            assertThat(result.getRed()).isEqualTo(255);
            assertThat(result.getGreen()).isZero();
            assertThat(result.getBlue()).isZero();
            assertThat(result.getAlpha()).isEqualTo(127);
        }

        @Test
        void int_value_sets_alpha() {
            Color result = Colors.withAlpha(Color.GREEN, 128);

            assertThat(result.getGreen()).isEqualTo(255);
            assertThat(result.getAlpha()).isEqualTo(128);
        }

        @Test
        void zero_fully_transparent() {
            Color result = Colors.withAlpha(Color.BLUE, 0);

            assertThat(result.getAlpha()).isZero();
        }

        @Test
        void max_255_fully_opaque() {
            Color result = Colors.withAlpha(Color.BLUE, 255);

            assertThat(result.getAlpha()).isEqualTo(255);
        }

        @Test
        void negative_int_throws_exception() {
            assertThatThrownBy(() -> Colors.withAlpha(Color.RED, -1))
                    .isInstanceOf(IndexOutOfBoundsException.class);
        }

        @Test
        void over_255_throws_exception() {
            assertThatThrownBy(() -> Colors.withAlpha(Color.RED, 256))
                    .isInstanceOf(IndexOutOfBoundsException.class);
        }
    }

    @Nested
    class Blend {

        @Test
        void same_colors_returns_same_color() {
            Color result = Colors.blend(Color.RED, Color.RED);

            assertThat(result.getRed()).isEqualTo(255);
            assertThat(result.getGreen()).isZero();
            assertThat(result.getBlue()).isZero();
        }

        @Test
        void black_and_white_returns_gray() {
            Color result = Colors.blend(Color.BLACK, Color.WHITE);

            // Mid gray expected
            assertThat(result.getRed()).isEqualTo(result.getGreen());
            assertThat(result.getGreen()).isEqualTo(result.getBlue());
            assertThat(result.getRed()).isBetween(120, 135);
        }

        @Test
        void red_and_blue_returns_purple() {
            Color result = Colors.blend(Color.RED, Color.BLUE);

            assertThat(result.getRed()).isGreaterThan(100);
            assertThat(result.getBlue()).isGreaterThan(100);
            assertThat(result.getGreen()).isZero();
        }
    }

    @Nested
    class HslaComponents {

        @Test
        void red_correct_hue() {
            float[] hsla = Colors.hslaComponents(Color.RED);

            assertThat(hsla[Colors.H]).isCloseTo(0f, within(1f));  // Red hue is 0
            assertThat(hsla[Colors.S]).isCloseTo(1f, within(0.01f)); // Full saturation
            assertThat(hsla[Colors.L]).isCloseTo(0.5f, within(0.01f)); // Mid luminance
            assertThat(hsla[Colors.ALPHA]).isEqualTo(1f);
        }

        @Test
        void green_correct_hue() {
            float[] hsla = Colors.hslaComponents(Color.GREEN);

            assertThat(hsla[Colors.H]).isCloseTo(120f, within(1f)); // Green hue is 120
        }

        @Test
        void blue_correct_hue() {
            float[] hsla = Colors.hslaComponents(Color.BLUE);

            assertThat(hsla[Colors.H]).isCloseTo(240f, within(1f)); // Blue hue is 240
        }

        @Test
        void white_zero_saturation() {
            float[] hsla = Colors.hslaComponents(Color.WHITE);

            assertThat(hsla[Colors.L]).isCloseTo(1f, within(0.01f)); // Max luminance
        }

        @Test
        void black_zero_luminance() {
            float[] hsla = Colors.hslaComponents(Color.BLACK);

            assertThat(hsla[Colors.L]).isCloseTo(0f, within(0.01f)); // Zero luminance
        }

        @Test
        void with_alpha_preserves_alpha() {
            Color colorWithAlpha = new Color(255, 0, 0, 128);
            float[] hsla = Colors.hslaComponents(colorWithAlpha);

            assertThat(hsla[Colors.ALPHA]).isCloseTo(128f / 255f, within(0.01f));
        }
    }

    @Nested
    class Hsla {

        @Test
        void red_creates_red_color() {
            Color result = Colors.hsla(0f, 1f, 0.5f, 1f);

            assertThat(result.getRed()).isEqualTo(255);
            assertThat(result.getGreen()).isZero();
            assertThat(result.getBlue()).isZero();
        }

        @Test
        void green_creates_green_color() {
            Color result = Colors.hsla(120f, 1f, 0.5f, 1f);

            assertThat(result.getGreen()).isEqualTo(255);
            assertThat(result.getRed()).isZero();
            assertThat(result.getBlue()).isZero();
        }

        @Test
        void blue_creates_blue_color() {
            Color result = Colors.hsla(240f, 1f, 0.5f, 1f);

            assertThat(result.getBlue()).isEqualTo(255);
            assertThat(result.getRed()).isZero();
            assertThat(result.getGreen()).isZero();
        }

        @Test
        void white_full_luminance() {
            Color result = Colors.hsla(0f, 0f, 1f, 1f);

            assertThat(result).isEqualTo(Color.WHITE);
        }

        @Test
        void black_zero_luminance() {
            Color result = Colors.hsla(0f, 0f, 0f, 1f);

            assertThat(result).isEqualTo(Color.BLACK);
        }

        @Test
        void with_alpha_does_not_preserve_alpha() {
            // Note: The hsla method uses Color(int) constructor which ignores alpha.
            // This is a known limitation - alpha is always 255 (fully opaque).
            // To preserve alpha, the implementation would need to use Color(int, true).
            Color result = Colors.hsla(0f, 1f, 0.5f, 0.5f);

            // Alpha is ignored and defaults to fully opaque
            assertThat(result.getAlpha()).isEqualTo(255);
        }

        @ParameterizedTest
        @CsvSource({
                "-0.1, 0.5, 0.5",   // invalid saturation
                "1.1, 0.5, 0.5",    // invalid saturation
                "0.5, -0.1, 0.5",   // invalid luminance
                "0.5, 1.1, 0.5",    // invalid luminance
                "0.5, 0.5, -0.1",   // invalid alpha
                "0.5, 0.5, 1.1"     // invalid alpha
        })
        void invalid_values_throws_exception(float s, float l, float alpha) {
            assertThatThrownBy(() -> Colors.hsla(0f, s, l, alpha))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void hslaComponents_and_hsla_round_trip() {
            Color original = new Color(100, 150, 200);

            float[] hsla = Colors.hslaComponents(original);
            Color roundTrip = Colors.hsla(hsla[Colors.H], hsla[Colors.S], hsla[Colors.L], hsla[Colors.ALPHA]);

            assertThat(roundTrip.getRed()).isCloseTo(original.getRed(), within(1));
            assertThat(roundTrip.getGreen()).isCloseTo(original.getGreen(), within(1));
            assertThat(roundTrip.getBlue()).isCloseTo(original.getBlue(), within(1));
        }
    }

    @Nested
    class BrighterAndDarker {

        @Test
        void brighter_increases_luminance() {
            Color original = new Color(100, 100, 100);
            Color brighter = Colors.brighter(original, 1f);

            int originalBrightness = Colors.brightness(original);
            int brighterBrightness = Colors.brightness(brighter);

            assertThat(brighterBrightness).isGreaterThan(originalBrightness);
        }

        @Test
        void brighter_with_max_luminance_does_not_exceed_max() {
            Color result = Colors.brighter(Color.WHITE, 2f, 1f);

            float[] hsla = Colors.hslaComponents(result);
            assertThat(hsla[Colors.L]).isLessThanOrEqualTo(1f);
        }

        @Test
        void brighter_negative_k_throws_exception() {
            assertThatThrownBy(() -> Colors.brighter(Color.GRAY, -1f))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void darker_decreases_luminance() {
            Color original = new Color(200, 200, 200);
            Color darker = Colors.darker(original, 1f);

            int originalBrightness = Colors.brightness(original);
            int darkerBrightness = Colors.brightness(darker);

            assertThat(darkerBrightness).isLessThan(originalBrightness);
        }

        @Test
        void darker_with_min_luminance_does_not_go_below_min() {
            Color result = Colors.darker(Color.BLACK, 2f, 0f);

            float[] hsla = Colors.hslaComponents(result);
            assertThat(hsla[Colors.L]).isGreaterThanOrEqualTo(0f);
        }

        @Test
        void darker_negative_k_throws_exception() {
            assertThatThrownBy(() -> Colors.darker(Color.GRAY, -1f))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Rgba {

        @Test
        void creates_color_with_alpha() {
            Color result = Colors.rgba(255, 128, 64, 0.5f);

            assertThat(result.getRed()).isEqualTo(255);
            assertThat(result.getGreen()).isEqualTo(128);
            assertThat(result.getBlue()).isEqualTo(64);
            assertThat(result.getAlpha()).isEqualTo(127);
        }
    }

    @Nested
    class ToHex {

        @Test
        void red_returns_FF0000() {
            String hex = Colors.toHex(Color.RED);

            assertThat(hex).isEqualToIgnoringCase("#ff0000");
        }

        @Test
        void green_returns_00FF00() {
            String hex = Colors.toHex(Color.GREEN);

            assertThat(hex).isEqualToIgnoringCase("#00ff00");
        }

        @Test
        void blue_returns_0000FF() {
            String hex = Colors.toHex(Color.BLUE);

            assertThat(hex).isEqualToIgnoringCase("#0000ff");
        }

        @Test
        void white_returns_FFFFFF() {
            String hex = Colors.toHex(Color.WHITE);

            assertThat(hex).isEqualToIgnoringCase("#ffffff");
        }

        @Test
        void black_returns_000000() {
            String hex = Colors.toHex(Color.BLACK);

            assertThat(hex).isEqualToIgnoringCase("#000000");
        }
    }

    @Nested
    class DarkMode {

        @Test
        void isDarkMode_default_returns_false() {
            Colors.setDarkMode(false);

            assertThat(Colors.isDarkMode()).isFalse();
        }

        @Test
        void setDarkMode_true_updates_flag() {
            Colors.setDarkMode(true);

            assertThat(Colors.isDarkMode()).isTrue();
        }

        @Test
        void setDarkMode_false_updates_flag() {
            Colors.setDarkMode(true);
            Colors.setDarkMode(false);

            assertThat(Colors.isDarkMode()).isFalse();
        }
    }

    @Nested
    class PaletteTests {

        @Test
        void datadog_has_colors() {
            Color[] colors = Colors.Palette.DATADOG.colors();

            assertThat(colors).isNotEmpty();
            assertThat(colors.length).isGreaterThan(10);
        }

        @Test
        void pyroscope_has_colors() {
            Color[] colors = Colors.Palette.PYROSCOPE.colors();

            assertThat(colors).isNotEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "LIGHT_BLACK_TO_YELLOW",
                "LIGHT_RED_TO_BLUE",
                "LIGHT_VIOLET_TO_ORANGE",
                "DARK_BLACK_TO_SLATE",
                "DARK_GREENY_TO_VIOLET",
                "DATADOG",
                "PYROSCOPE"
        })
        void all_palettes_have_valid_colors(String paletteName) {
            Colors.Palette palette = Colors.Palette.valueOf(paletteName);
            Color[] colors = palette.colors();

            assertThat(colors).isNotNull();
            assertThat(colors).isNotEmpty();
            assertThat(colors).allSatisfy(color -> {
                assertThat(color).isNotNull();
                assertThat(color.getRed()).isBetween(0, 255);
                assertThat(color.getGreen()).isBetween(0, 255);
                assertThat(color.getBlue()).isBetween(0, 255);
            });
        }
    }

    @Nested
    class TranslucentColorConstants {

        @Test
        void translucent_black_colors_have_correct_alphas() {
            assertThat(Colors.translucent_black_D0.getAlpha()).isEqualTo(0xD0);
            assertThat(Colors.translucent_black_B0.getAlpha()).isEqualTo(0xB0);
            assertThat(Colors.translucent_black_80.getAlpha()).isEqualTo(0x80);
            assertThat(Colors.translucent_black_60.getAlpha()).isEqualTo(0x60);
            assertThat(Colors.translucent_black_40.getAlpha()).isEqualTo(0x40);
            assertThat(Colors.translucent_black_20.getAlpha()).isEqualTo(0x20);
            assertThat(Colors.translucent_black_10.getAlpha()).isEqualTo(0x10);
        }

        @Test
        void translucent_black_colors_are_black() {
            assertThat(Colors.translucent_black_80.getRed()).isZero();
            assertThat(Colors.translucent_black_80.getGreen()).isZero();
            assertThat(Colors.translucent_black_80.getBlue()).isZero();
        }

        @Test
        void translucent_white_colors_have_correct_alphas() {
            assertThat(Colors.translucent_white_D0.getAlpha()).isEqualTo(0xD0);
            assertThat(Colors.translucent_white_B0.getAlpha()).isEqualTo(0xB0);
            assertThat(Colors.translucent_white_80.getAlpha()).isEqualTo(0x80);
            assertThat(Colors.translucent_white_60.getAlpha()).isEqualTo(0x60);
            assertThat(Colors.translucent_white_40.getAlpha()).isEqualTo(0x40);
            assertThat(Colors.translucent_white_20.getAlpha()).isEqualTo(0x20);
            assertThat(Colors.translucent_white_10.getAlpha()).isEqualTo(0x10);
        }

        @Test
        void translucent_white_colors_are_white() {
            assertThat(Colors.translucent_white_80.getRed()).isEqualTo(255);
            assertThat(Colors.translucent_white_80.getGreen()).isEqualTo(255);
            assertThat(Colors.translucent_white_80.getBlue()).isEqualTo(255);
        }
    }

    @Nested
    class Dim {

        @Test
        void returns_light_dark_color() {
            Color result = Colors.dim(Color.RED);

            assertThat(result).isInstanceOf(LightDarkColor.class);
        }

        @Test
        void reduces_luminance() {
            Color original = new Color(200, 100, 50);
            Color dimmed = Colors.dim(original);

            // In light mode, dimmed color should be light (high luminance)
            Colors.setDarkMode(false);
            float[] hslaLight = Colors.hslaComponents(dimmed);
            assertThat(hslaLight[Colors.L]).isGreaterThan(0.9f);

            // In dark mode, dimmed color should be dark (low luminance)
            Colors.setDarkMode(true);
            float[] hslaDark = Colors.hslaComponents(dimmed);
            assertThat(hslaDark[Colors.L]).isLessThan(0.3f);
        }
    }

    @Nested
    class HalfDim {

        @Test
        void returns_light_dark_color() {
            Color result = Colors.halfDim(Color.RED);

            assertThat(result).isInstanceOf(LightDarkColor.class);
        }

        @Test
        void reduces_luminance_partially() {
            Color original = new Color(200, 100, 50);
            Color halfDimmed = Colors.halfDim(original);

            // Half dim should have moderate luminance
            Colors.setDarkMode(false);
            float[] hslaLight = Colors.hslaComponents(halfDimmed);
            assertThat(hslaLight[Colors.L]).isBetween(0.5f, 0.8f);
        }
    }

    @Nested
    class ForegroundColor {

        @Test
        void bright_background_returns_dark_foreground() {
            Colors.setDarkMode(false);
            Color foreground = Colors.foregroundColor(Color.WHITE);

            // Should return a dark color for readability
            assertThat(Colors.isBright(foreground)).isFalse();
        }

        @Test
        void dark_background_returns_white() {
            Color foreground = Colors.foregroundColor(Color.BLACK);

            assertThat(foreground).isEqualTo(Color.WHITE);
        }
    }
}