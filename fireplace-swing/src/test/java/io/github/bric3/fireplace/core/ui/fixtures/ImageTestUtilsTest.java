/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.core.ui.fixtures;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;

import static io.github.bric3.fireplace.core.ui.fixtures.ImageTestUtils.assertImageEquals;
import static io.github.bric3.fireplace.core.ui.fixtures.ImageTestUtils.testReportDir;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageTestUtilsTest {
    @BeforeEach
    void createReportDirectory() throws IOException {
        Files.createDirectories(testReportDir());
    }

    @ParameterizedTest
    @CsvSource({"0, 0", "0, 2", "2, 0", "4, 2", "2, 4", "2, 2", "4, 4"})
    void detects_each_channel_at_origin_edges_and_interior(int x, int y) throws IOException {
        var expected = new BufferedImage(5, 5, TYPE_INT_ARGB);
        for (int channel : new int[]{0x00010000, 0x00000100, 0x00000001, 0x01000000}) {
            var actual = new BufferedImage(5, 5, TYPE_INT_ARGB);
            actual.setRGB(x, y, channel);
            var alpha = channel == 0x01000000;
            var difference = alpha ? "Alpha" : "Color";
            var name = "ImageTestUtilsTest-pixel-" + x + "-" + y + "-" + channel;

            assertThatThrownBy(() -> assertImageEquals(name, expected, actual))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining(difference + " differences found in this area: " + new Rectangle(x, y, 1, 1))
                    .hasMessageNotContaining((alpha ? "Color" : "Alpha") + " differences");

            var image = ImageIO.read(testReportDir().resolve(name + "-difference-" + difference.toLowerCase() + ".png").toFile());
            assertThat(image.getWidth()).isEqualTo(5);
            assertThat(image.getHeight()).isEqualTo(5);
            assertThat(image.getRGB(x, y)).isEqualTo(alpha ? 0xff4b0001 : 0xff000000 | channel);
            assertThat(image.getRGB((x + 1) % 5, y)).isZero();
        }
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0, 4, 4, 0, 0, 5, 5",
            "1, 4, 3, 1, 1, 1, 3, 4",
            "0, 2, 2, 0, 0, 0, 3, 3"
    })
    void bounds_include_all_mismatching_pixels(int x1, int y1, int x2, int y2,
                                              int x, int y, int width, int height) {
        var expected = new BufferedImage(5, 5, TYPE_INT_ARGB);
        var actual = new BufferedImage(5, 5, TYPE_INT_ARGB);
        actual.setRGB(x1, y1, 0x01010101);
        actual.setRGB(x2, y2, 0x01010101);
        var bounds = new Rectangle(x, y, width, height);

        assertThatThrownBy(() -> assertImageEquals("ImageTestUtilsTest-bounds-" + x1 + "-" + y1, expected, actual))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Color differences found in this area: " + bounds)
                .hasMessageContaining("Alpha differences found in this area: " + bounds);
    }

    @Test
    void identical_pixels_pass() {
        var expected = new BufferedImage(3, 3, TYPE_INT_ARGB);
        var actual = new BufferedImage(3, 3, TYPE_INT_ARGB);
        expected.setRGB(0, 0, 0x01020304);
        actual.setRGB(0, 0, 0x01020304);
        expected.setRGB(2, 2, 0xff123456);
        actual.setRGB(2, 2, 0xff123456);

        assertThatCode(() -> assertImageEquals("ImageTestUtilsTest-identical", expected, actual))
                .doesNotThrowAnyException();
    }

    @Test
    void optional_rgb_tolerance_does_not_relax_alpha_or_default_exact_comparison() {
        var expected = new BufferedImage(1, 1, TYPE_INT_ARGB);
        var actual = new BufferedImage(1, 1, TYPE_INT_ARGB);
        expected.setRGB(0, 0, 0xff202020);
        actual.setRGB(0, 0, 0xff212121);
        assertImageEquals("tolerated-glyph-rounding", expected, actual, 1);
        assertThatThrownBy(() -> assertImageEquals("exact-glyph-rounding", expected, actual))
                .isInstanceOf(AssertionError.class).hasMessageContaining("Color differences");

        actual.setRGB(0, 0, 0xff222020);
        assertThatThrownBy(() -> assertImageEquals("excess-glyph-rounding", expected, actual, 1))
                .isInstanceOf(AssertionError.class).hasMessageContaining("Color differences");
        actual.setRGB(0, 0, 0xfe202020);
        assertThatThrownBy(() -> assertImageEquals("alpha-is-exact", expected, actual, 1))
                .isInstanceOf(AssertionError.class).hasMessageContaining("Alpha differences");
        assertThatThrownBy(() -> assertImageEquals("invalid-tolerance", expected, actual, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> assertImageEquals("invalid-tolerance", expected, actual, 256))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "3, 2, Expected width: 2 while actual width is: 3",
            "2, 3, Expected height: 2 while actual height is: 3",
            "1, 2, Expected width: 2 while actual width is: 1",
            "2, 1, Expected height: 2 while actual height is: 1"
    })
    void different_dimensions_fail_even_with_transparent_pixels(int width, int height, String message) {
        var expected = new BufferedImage(2, 2, TYPE_INT_ARGB);
        var actual = new BufferedImage(width, height, TYPE_INT_ARGB);

        assertThatThrownBy(() -> assertImageEquals("ImageTestUtilsTest-dimensions", expected, actual))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining(message);
    }
}
