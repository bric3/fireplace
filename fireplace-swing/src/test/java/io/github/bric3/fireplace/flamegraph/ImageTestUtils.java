/*
 * Copyright 2021 Datadog, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.bric3.fireplace.flamegraph;/*
 * Copyright 2021 Datadog, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;

public class ImageTestUtils {
    public static void dumpPng(RenderedImage image, Path outputFile) {
        try (var os = new BufferedOutputStream(Files.newOutputStream(outputFile))) {
            var written = ImageIO.write(image, "png", os);
            if (!written) {
                throw new IllegalStateException("Expected to write image to: " + outputFile);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void assertImageEquals(String testDisplayName, RenderedImage expected, RenderedImage actual) {
        var differences = new ArrayList<String>();

        if (expected.getWidth() != actual.getWidth()) {
            differences.add("Expected width: " + expected.getWidth() + " while actual width is: " + actual.getWidth());
        }

        if (expected.getHeight() != actual.getHeight()) {
            differences.add("Expected height: " + expected.getHeight() + " while actual height is: " + actual.getHeight());
        }

        // if (expected.getColorModel() != actual.getColorModel()) {
        //     differences.add("Expected color model: '" + expected.getColorModel() + "' while actual color model is: '" + actual.getColorModel() + "'");
        // }

        // if (expected.getType() != actual.getType()) {
        //     differences.add("Expected type: " + imageTypeToString(expected.getType()) + " while actual type is: " + (imageTypeToString(actual.getType())));
        // }

        var widthDifference = Math.max(expected.getWidth(), actual.getWidth());
        var heightDifference = Math.max(expected.getHeight(), actual.getHeight());
        var colorDifferenceImage = new BufferedImage(widthDifference, heightDifference, BufferedImage.TYPE_INT_ARGB);
        {
            var g2d = colorDifferenceImage.createGraphics();
            g2d.setComposite(AlphaComposite.Clear);
            g2d.fillRect(0, 0, widthDifference, heightDifference);
            g2d.setComposite(AlphaComposite.Src);
            g2d.dispose();
        }
        var alphaDifferenceImage = new BufferedImage(widthDifference, heightDifference, BufferedImage.TYPE_INT_ARGB);
        {
            var g2d = alphaDifferenceImage.createGraphics();
            g2d.setComposite(AlphaComposite.Clear);
            g2d.fillRect(0, 0, widthDifference, heightDifference);
            g2d.setComposite(AlphaComposite.Src);
            g2d.dispose();
        }

        class GetRGB {
            private final ColorModel colorModel;
            private final Raster raster;

            public GetRGB(RenderedImage ri) {
                colorModel = ri.getColorModel();
                raster = ri.getData(); // actually a copy
            }

            int getRGB(int x, int y) {
                return colorModel.getRGB(raster.getDataElements(x, y, null));
            }
        }
        var expectedRGB = new GetRGB(expected);
        var actualRGB = new GetRGB(actual);

        int color;
        var colorDifferenceArea = new Rectangle(0, 0, 0, 0);
        var alphaDifferenceArea = new Rectangle(0, 0, 0, 0);
        for (int x = 0; x < widthDifference; x++) {
            for (int y = 0; y < heightDifference; y++) {

                var expectedRgb = (x < expected.getWidth() && y < expected.getHeight()) ?
                                  expectedRGB.getRGB(x, y) :
                                  0;
                var actualRgb = (x < actual.getWidth() && y < actual.getHeight()) ?
                                actualRGB.getRGB(x, y) :
                                0;

                var expectedAlpha = (expectedRgb >> 24) & 0xFF;
                var expectedRed = (expectedRgb >> 16) & 0xFF;
                var expectedGreen = (expectedRgb >> 8) & 0xFF;
                var expectedBlue = expectedRgb & 0xFF;

                var actualAlpha = (actualRgb >> 24) & 0xFF;
                var actualRed = (actualRgb >> 16) & 0xFF;
                var actualGreen = (actualRgb >> 8) & 0xFF;
                var actualBlue = actualRgb & 0xFF;

                var diffAlpha = Math.abs(actualAlpha - expectedAlpha);
                var diffRed = Math.abs(actualRed - expectedRed);
                var diffGreen = Math.abs(actualGreen - expectedGreen);
                var diffBlue = Math.abs(actualBlue - expectedBlue);

                color = (0xFF << 24) |
                        (diffRed << 16) | (diffGreen << 8) | diffBlue;

                if ((color & 0x00FFFFFF) > 0) {
                    colorDifferenceImage.setRGB(x, y, color);

                    if (colorDifferenceArea.x == 0) {
                        colorDifferenceArea.x = x;
                    }
                    if (colorDifferenceArea.y == 0) {
                        colorDifferenceArea.y = y;
                    }
                    colorDifferenceArea.width = x - colorDifferenceArea.x + 1;
                    colorDifferenceArea.height = y - colorDifferenceArea.y + 1;
                }
                if (diffAlpha > 0) {
                    alphaDifferenceImage.setRGB(x, y, (0xFF << 24) | 75 << 16 | (diffAlpha) /* use alpha as blue channel value */);

                    if (alphaDifferenceArea.x == 0) {
                        alphaDifferenceArea.x = x;
                    }
                    if (alphaDifferenceArea.y == 0) {
                        alphaDifferenceArea.y = y;
                    }
                    alphaDifferenceArea.width = x - alphaDifferenceArea.x + 1;
                    alphaDifferenceArea.height = y - alphaDifferenceArea.y + 1;
                }
            }
        }
        if (colorDifferenceArea.x != 0 || colorDifferenceArea.y != 0) {
            var diffPath = testReportDir().resolve(testDisplayName + "-difference-color.png");
            differences.add("Color differences found in this area: " + colorDifferenceArea + ", \ncolor difference image: " + diffPath);

            dumpPng(colorDifferenceImage, diffPath);
        }
        if (alphaDifferenceArea.x != 0 || alphaDifferenceArea.y != 0) {
            var diffPath = testReportDir().resolve(testDisplayName + "-difference-alpha.png");
            differences.add("Alpha differences found in this area: " + alphaDifferenceArea + ", \nalpha difference image: " + diffPath);
            dumpPng(alphaDifferenceImage, diffPath);
        }
        if (!differences.isEmpty()) {
            throw new AssertionError("Image differences were spotted (" +
                                     differences.size() + "):\n * " +
                                     String.join("\n * ", differences));
        }
    }

    public static Path projectDir() {
        return Path.of(System.getProperty("user.dir"));
    }

    public static Path testReportDir() {
        return projectDir().resolve(System.getProperty("gradle.test.suite.report.location"));
    }

    public static String imageTypeToString(int imageType) {
        switch (imageType) {
            case BufferedImage.TYPE_3BYTE_BGR:
                return "TYPE_3BYTE_BGR (" + imageType + ')';
            case BufferedImage.TYPE_4BYTE_ABGR:
                return "TYPE_4BYTE_ABGR (" + imageType + ')';
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
                return "TYPE_4BYTE_ABGR_PRE (" + imageType + ')';
            case BufferedImage.TYPE_BYTE_BINARY:
                return "TYPE_BYTE_BINARY (" + imageType + ')';
            case BufferedImage.TYPE_BYTE_GRAY:
                return "TYPE_BYTE_GRAY (" + imageType + ')';
            case BufferedImage.TYPE_BYTE_INDEXED:
                return "TYPE_BYTE_INDEXED (" + imageType + ')';
            case BufferedImage.TYPE_CUSTOM:
                return "TYPE_CUSTOM (" + imageType + ')';
            case BufferedImage.TYPE_INT_ARGB:
                return "TYPE_INT_ARGB (" + imageType + ')';
            case BufferedImage.TYPE_INT_ARGB_PRE:
                return "TYPE_INT_ARGB_PRE (" + imageType + ')';
            case BufferedImage.TYPE_INT_BGR:
                return "TYPE_INT_BGR (" + imageType + ')';
            case BufferedImage.TYPE_INT_RGB:
                return "TYPE_INT_RGB (" + imageType + ')';
            case BufferedImage.TYPE_USHORT_555_RGB:
                return "TYPE_USHORT_555_RGB (" + imageType + ')';
            case BufferedImage.TYPE_USHORT_565_RGB:
                return "TYPE_USHORT_565_RGB (" + imageType + ')';
            case BufferedImage.TYPE_USHORT_GRAY:
                return "TYPE_USHORT_GRAY (" + imageType + ')';

            default:
                return "UNKNOWN (" + imageType + ')';
        }
    }

    public static BufferedImage readImage(String name) {
        try {
            return ImageIO.read(Objects.requireNonNull(
                    ImageTestUtils.class.getResource(name),
                    "Image not found: " + name
            ));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
