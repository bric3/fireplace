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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link StringClipper}.
 * Uses BufferedImage to obtain FontMetrics in headless mode,
 * similar to OpenJDK's approach for headless Swing testing.
 */
@DisplayName("StringClipper")
class StringClipperTest {

    private Font font;
    private FontMetrics metrics;
    private Graphics2D g2d;

    @BeforeEach
    void setUp() {
        // Create a BufferedImage to get a Graphics2D context
        // This works in headless mode
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        g2d = image.createGraphics();

        // Use a standard monospace font for predictable measurements
        font = new Font(Font.MONOSPACED, Font.PLAIN, 12);
        g2d.setFont(font);
        metrics = g2d.getFontMetrics(font);
    }

    @AfterEach
    void disposeGraphics() {
        g2d.dispose();
    }

    @Nested
    @DisplayName("NONE clipper")
    class NoneClipper {

        @Test
        void ignores_width_and_clip_marker() {
            String text = "This is a very long string that would normally be clipped";

            // Even with very small width, NONE should return original
            String result = StringClipper.NONE.clipString(font, metrics, 1, text, "[CLIPPED]");

            assertThat(result).isEqualTo(text);
        }

        @Test
        void handles_empty_string() {
            String result = StringClipper.NONE.clipString(font, metrics, 100, "", "...");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("RIGHT clipper")
    class RightClipper {

        @Test
        void appends_marker_when_the_entire_candidate_fits() {
            String text = "Hi";
            double width = metrics.stringWidth(text) + 100; // Plenty of room

            String result = StringClipper.RIGHT.clipString(font, metrics, width, text, "...");

            assertThat(result).isEqualTo(text + "...");
        }

        @Test
        void long_text_clips_from_right() {
            String text = "Hello World Test String";
            double clipWidth = font.getStringBounds("...", metrics.getFontRenderContext()).getWidth();
            double availableWidth = clipWidth + metrics.stringWidth("Hello");

            String result = StringClipper.RIGHT.clipString(font, metrics, availableWidth, text, "...");

            assertThat(result).isEqualTo("Hello...");
        }

        @Test
        void very_small_width_returns_only_clip_string() {
            String text = "Hello World";
            // Width smaller than clip string
            double clipWidth = font.getStringBounds("...", metrics.getFontRenderContext()).getWidth();
            double verySmallWidth = clipWidth - 1;

            String result = StringClipper.RIGHT.clipString(font, metrics, verySmallWidth, text, "...");

            assertThat(result).isEqualTo("...");
        }

        @Test
        void zero_width_returns_clip_string() {
            String text = "Test";

            String result = StringClipper.RIGHT.clipString(font, metrics, 0, text, "...");

            assertThat(result).isEqualTo("...");
        }

        @Test
        void negative_width_returns_clip_string() {
            String text = "Test";

            String result = StringClipper.RIGHT.clipString(font, metrics, -10, text, "...");

            assertThat(result).isEqualTo("...");
        }

        @Test
        void custom_clip_string() {
            String text = "Hello World";
            String customClip = "[...]";
            double clipWidth = font.getStringBounds(customClip, metrics.getFontRenderContext()).getWidth();
            double width = clipWidth + metrics.stringWidth("Hel");

            String result = StringClipper.RIGHT.clipString(font, metrics, width, text, customClip);

            assertThat(result).isEqualTo("Hel[...]");
        }

        @Test
        void empty_string_returns_clip_string() {
            String result = StringClipper.RIGHT.clipString(font, metrics, 100, "", "...");

            // Empty string + clip string
            assertThat(result).isEqualTo("...");
        }

        @Test
        void exact_fit_no_extra_clipping() {
            String text = "Test";
            // Calculate exact width needed
            double exactWidth = metrics.stringWidth(text) +
                                font.getStringBounds("...", metrics.getFontRenderContext()).getWidth();

            String result = StringClipper.RIGHT.clipString(font, metrics, exactWidth, text, "...");

            // Should fit the full text
            assertThat(result).isEqualTo(text + "...");
        }

        @Test
        void unicode_text_clips_correctly() {
            String text = "日本語テスト"; // Japanese text
            double clipWidth = font.getStringBounds("...", metrics.getFontRenderContext()).getWidth();
            double width = clipWidth + metrics.stringWidth("日本");

            String result = StringClipper.RIGHT.clipString(font, metrics, width, text, "...");

            assertThat(result).isEqualTo("日本...");
        }

        @Test
        void width_exactly_for_clip_string_returns_clip_string() {
            String text = "Hello";
            double clipWidth = font.getStringBounds("...", metrics.getFontRenderContext()).getWidth();

            String result = StringClipper.RIGHT.clipString(font, metrics, clipWidth, text, "...");

            assertThat(result).isEqualTo("...");
        }
    }

    @Nested
    @DisplayName("Default method")
    class DefaultMethod {

        @Test
        void uses_long_text_placeholder() {
            String text = "Hello World";
            double width = metrics.stringWidth("Hel") + font.getStringBounds("…", metrics.getFontRenderContext()).getWidth();

            String result = StringClipper.RIGHT.clipString(font, metrics, width, text);

            assertThat(result).isEqualTo("Hel…");
        }
    }

    @Nested
    @DisplayName("Different fonts")
    class DifferentFonts {

        @Test
        void works_with_serif_font() {
            Font serifFont = new Font(Font.SERIF, Font.PLAIN, 14);
            g2d.setFont(serifFont);
            FontMetrics serifMetrics = g2d.getFontMetrics(serifFont);

            String text = "Hello World";
            double width = serifMetrics.stringWidth("Hello") +
                           serifFont.getStringBounds("...", serifMetrics.getFontRenderContext()).getWidth();

            String result = StringClipper.RIGHT.clipString(serifFont, serifMetrics, width, text, "...");

            assertThat(result).isEqualTo("Hello...");
        }

        @Test
        void works_with_bold_font() {
            Font boldFont = new Font(Font.SANS_SERIF, Font.BOLD, 12);
            g2d.setFont(boldFont);
            FontMetrics boldMetrics = g2d.getFontMetrics(boldFont);

            String text = "Bold Text";
            double clipWidth = boldFont.getStringBounds("...", boldMetrics.getFontRenderContext()).getWidth();
            double width = clipWidth + boldMetrics.stringWidth("Bold");

            String result = StringClipper.RIGHT.clipString(boldFont, boldMetrics, width, text, "...");

            assertThat(result).isEqualTo("Bold...");
        }
    }
}
