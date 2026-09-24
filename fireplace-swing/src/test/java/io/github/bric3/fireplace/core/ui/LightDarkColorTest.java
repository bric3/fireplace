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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.color.ColorSpace;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LightDarkColor}.
 * These tests run in headless mode without requiring a display.
 */
@DisplayName("LightDarkColor")
class LightDarkColorTest {

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
    @DisplayName("Constructor")
    class Constructor {

        @Test
        void with_colors_stores_light_and_dark() {
            Color light = Color.WHITE;
            Color dark = Color.BLACK;

            LightDarkColor color = new LightDarkColor(light, dark);

            Colors.setDarkMode(false);
            assertThat(color.getRGB()).isEqualTo(light.getRGB());

            Colors.setDarkMode(true);
            assertThat(color.getRGB()).isEqualTo(dark.getRGB());
        }

        @Test
        void with_rgba_ints_stores_light_and_dark() {
            int lightRgba = 0x80FF0000;
            int darkRgba = 0x400000FF;

            LightDarkColor color = new LightDarkColor(lightRgba, darkRgba);

            Colors.setDarkMode(false);
            assertThat(color.getRGB()).isEqualTo(lightRgba);

            Colors.setDarkMode(true);
            assertThat(color.getRGB()).isEqualTo(darkRgba);
        }

        @Test
        void with_alpha_in_light_preserves_alpha() {
            Color lightWithAlpha = new Color(255, 0, 0, 100);
            Color dark = Color.BLUE;

            LightDarkColor color = new LightDarkColor(lightWithAlpha, dark);

            Colors.setDarkMode(false);
            assertThat(color.getAlpha()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("getRed")
    class GetRed {

        @Test
        void light_mode_returns_light_red() {
            LightDarkColor color = new LightDarkColor(new Color(100, 50, 25), new Color(200, 150, 75));

            Colors.setDarkMode(false);
            assertThat(color.getRed()).isEqualTo(100);
        }

        @Test
        void dark_mode_returns_dark_red() {
            LightDarkColor color = new LightDarkColor(new Color(100, 50, 25), new Color(200, 150, 75));

            Colors.setDarkMode(true);
            assertThat(color.getRed()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("getGreen")
    class GetGreen {

        @Test
        void light_mode_returns_light_green() {
            LightDarkColor color = new LightDarkColor(new Color(100, 50, 25), new Color(200, 150, 75));

            Colors.setDarkMode(false);
            assertThat(color.getGreen()).isEqualTo(50);
        }

        @Test
        void dark_mode_returns_dark_green() {
            LightDarkColor color = new LightDarkColor(new Color(100, 50, 25), new Color(200, 150, 75));

            Colors.setDarkMode(true);
            assertThat(color.getGreen()).isEqualTo(150);
        }
    }

    @Nested
    @DisplayName("getBlue")
    class GetBlue {

        @Test
        void light_mode_returns_light_blue() {
            LightDarkColor color = new LightDarkColor(new Color(100, 50, 25), new Color(200, 150, 75));

            Colors.setDarkMode(false);
            assertThat(color.getBlue()).isEqualTo(25);
        }

        @Test
        void dark_mode_returns_dark_blue() {
            LightDarkColor color = new LightDarkColor(new Color(100, 50, 25), new Color(200, 150, 75));

            Colors.setDarkMode(true);
            assertThat(color.getBlue()).isEqualTo(75);
        }
    }

    @Nested
    @DisplayName("getAlpha")
    class GetAlpha {

        @Test
        void light_mode_returns_light_alpha() {
            Color light = new Color(255, 0, 0, 128);
            Color dark = new Color(0, 0, 255, 64);
            LightDarkColor color = new LightDarkColor(light, dark);

            Colors.setDarkMode(false);
            assertThat(color.getAlpha()).isEqualTo(128);
        }

        @Test
        void dark_mode_returns_dark_alpha() {
            Color light = new Color(255, 0, 0, 128);
            Color dark = new Color(0, 0, 255, 64);
            LightDarkColor color = new LightDarkColor(light, dark);

            Colors.setDarkMode(true);
            assertThat(color.getAlpha()).isEqualTo(64);
        }
    }

    @Nested
    @DisplayName("brighter")
    class Brighter {

        @Test
        void light_mode_brightens_light_color() {
            LightDarkColor color = new LightDarkColor(new Color(100, 100, 100), new Color(50, 50, 50));

            Colors.setDarkMode(false);
            Color brighter = color.brighter();

            assertThat(brighter).isEqualTo(new Color(100, 100, 100).brighter());
        }

        @Test
        void dark_mode_brightens_dark_color() {
            LightDarkColor color = new LightDarkColor(new Color(100, 100, 100), new Color(50, 50, 50));

            Colors.setDarkMode(true);
            Color brighter = color.brighter();

            assertThat(brighter).isEqualTo(new Color(50, 50, 50).brighter());
        }
    }

    @Nested
    @DisplayName("darker")
    class Darker {

        @Test
        void light_mode_darkens_light_color() {
            LightDarkColor color = new LightDarkColor(new Color(200, 200, 200), new Color(100, 100, 100));

            Colors.setDarkMode(false);
            Color darker = color.darker();

            assertThat(darker).isEqualTo(new Color(200, 200, 200).darker());
        }

        @Test
        void dark_mode_darkens_dark_color() {
            LightDarkColor color = new LightDarkColor(new Color(200, 200, 200), new Color(100, 100, 100));

            Colors.setDarkMode(true);
            Color darker = color.darker();

            assertThat(darker).isEqualTo(new Color(100, 100, 100).darker());
        }
    }

    @Nested
    @DisplayName("getRGBComponents")
    class GetRGBComponents {

        @Test
        void light_mode_returns_light_components() {
            LightDarkColor color = new LightDarkColor(Color.RED, Color.BLUE);

            Colors.setDarkMode(false);
            float[] components = color.getRGBComponents(null);

            assertThat(components).containsExactly(1f, 0f, 0f, 1f);
        }

        @Test
        void dark_mode_returns_dark_components() {
            LightDarkColor color = new LightDarkColor(Color.RED, new Color(0, 0, 255, 64));

            Colors.setDarkMode(true);
            float[] components = color.getRGBComponents(null);

            assertThat(components).containsExactly(0f, 0f, 1f, 64f / 255);
        }
    }

    @Nested
    @DisplayName("getRGBColorComponents")
    class GetRGBColorComponents {

        @Test
        void light_mode_excludes_alpha() {
            LightDarkColor color = new LightDarkColor(Color.RED, Color.BLUE);

            Colors.setDarkMode(false);
            float[] components = color.getRGBColorComponents(null);

            assertThat(components).containsExactly(1f, 0f, 0f);
        }
    }

    @Nested
    @DisplayName("getComponents")
    class GetComponents {

        @Test
        void components_follow_the_active_color_including_alpha() {
            Color light = new Color(13, 57, 91, 123);
            Color dark = new Color(197, 163, 129, 87);
            LightDarkColor color = new LightDarkColor(light, dark);

            Colors.setDarkMode(false);
            assertThat(color.getComponents(null)).containsExactly(light.getComponents(null));

            Colors.setDarkMode(true);
            assertThat(color.getComponents(null)).containsExactly(dark.getComponents(null));
        }
    }

    @Nested
    @DisplayName("getColorSpace")
    class GetColorSpace {

        @Test
        void returns_correct_color_space() {
            Color light = Color.RED;
            Color dark = new Color(ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB), new float[]{.1f, .2f, .3f}, 1f);
            LightDarkColor color = new LightDarkColor(light, dark);

            Colors.setDarkMode(false);
            assertThat(color.getColorSpace()).isSameAs(light.getColorSpace());

            Colors.setDarkMode(true);
            assertThat(color.getColorSpace()).isSameAs(dark.getColorSpace());
        }
    }

    @Nested
    @DisplayName("getTransparency")
    class GetTransparency {

        @Test
        void light_mode_returns_light_transparency() {
            Color light = new Color(255, 0, 0, 128);
            Color dark = new Color(0, 0, 255, 255);
            LightDarkColor color = new LightDarkColor(light, dark);

            Colors.setDarkMode(false);
            assertThat(color.getTransparency()).isEqualTo(Transparency.TRANSLUCENT);
        }

        @Test
        void dark_mode_returns_dark_transparency() {
            Color light = new Color(255, 0, 0, 128);
            Color dark = new Color(0, 0, 255, 255);
            LightDarkColor color = new LightDarkColor(light, dark);

            Colors.setDarkMode(true);
            assertThat(color.getTransparency()).isEqualTo(Transparency.OPAQUE);
        }
    }

    @Nested
    @DisplayName("hashCode")
    class HashCode {

        @Test
        void light_mode_uses_light_color() {
            Color light = Color.RED;
            Color dark = Color.BLUE;
            LightDarkColor color = new LightDarkColor(light, dark);

            Colors.setDarkMode(false);
            assertThat(color.hashCode()).isEqualTo(light.hashCode());
        }

        @Test
        void dark_mode_uses_dark_color() {
            Color light = Color.RED;
            Color dark = Color.BLUE;
            LightDarkColor color = new LightDarkColor(light, dark);

            Colors.setDarkMode(true);
            assertThat(color.hashCode()).isEqualTo(dark.hashCode());
        }
    }

    @Nested
    @DisplayName("equals")
    class Equals {

        @Test
        void light_mode_compares_with_light_color() {
            Color light = Color.RED;
            Color dark = Color.BLUE;
            LightDarkColor color = new LightDarkColor(light, dark);

            Colors.setDarkMode(false);
            assertThat(color.equals(light)).isTrue();
            assertThat(color.equals(dark)).isFalse();
        }

        @Test
        void dark_mode_compares_with_dark_color() {
            Color light = Color.RED;
            Color dark = Color.BLUE;
            LightDarkColor color = new LightDarkColor(light, dark);

            Colors.setDarkMode(true);
            assertThat(color.equals(dark)).isTrue();
            assertThat(color.equals(light)).isFalse();
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        void light_mode_uses_light_color() {
            LightDarkColor color = new LightDarkColor(Color.RED, Color.BLUE);

            Colors.setDarkMode(false);
            String str = color.toString();

            assertThat(str).contains("255").contains("r=255"); // Red component
        }

        @Test
        void dark_mode_uses_dark_color() {
            LightDarkColor color = new LightDarkColor(Color.RED, Color.BLUE);

            Colors.setDarkMode(true);
            String str = color.toString();

            assertThat(str).contains("b=255"); // Blue component
        }
    }

    @Nested
    @DisplayName("Dynamic mode switching")
    class DynamicModeSwitching {

        @Test
        void updates_color_immediately() {
            LightDarkColor color = new LightDarkColor(Color.WHITE, Color.BLACK);

            Colors.setDarkMode(false);
            assertThat(color.getRed()).isEqualTo(255);
            assertThat(color.getGreen()).isEqualTo(255);
            assertThat(color.getBlue()).isEqualTo(255);

            Colors.setDarkMode(true);
            assertThat(color.getRed()).isEqualTo(0);
            assertThat(color.getGreen()).isEqualTo(0);
            assertThat(color.getBlue()).isEqualTo(0);

            Colors.setDarkMode(false);
            assertThat(color.getRed()).isEqualTo(255);
        }
    }
}
