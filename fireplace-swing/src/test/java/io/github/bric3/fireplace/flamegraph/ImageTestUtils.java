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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class ImageTestUtils {
    public static void dump(OutputStream tmpFile, RenderedImage image) throws IOException {
        try (var output = tmpFile) {
            ImageIO.write(image, "png", output);
        }
    }

    public static void assertImageEquals(RenderedImage expected, RenderedImage actual) throws IOException {
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
        BufferedImage differenceImage = new BufferedImage(widthDifference, heightDifference, BufferedImage.TYPE_INT_ARGB);
        var g2d = differenceImage.createGraphics();
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, widthDifference, heightDifference);
        g2d.setComposite(AlphaComposite.Src);
        g2d.dispose();

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
        int minX = 0, maxX = 0, minY = 0, maxY = 0;
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

                color = (0xFF << 24) | // TODO diff alpha
                        (diffRed << 16) | (diffGreen << 8) | diffBlue;

                if ((color & 0x00FFFFFF) > 0) { // TODO diff alpha
                    differenceImage.setRGB(x, y, color);

                    if (minX == 0) {
                        minX = x;
                    }
                    if (minY == 0) {
                        minY = y;
                    }
                    maxX = x;
                    maxY = y;
                }
            }
        }
        if (minX != 0 || minY != 0) {
            differences.add("Difference found in this area: [" + minX + ',' + minY + " ; " + maxX + ',' + maxY + ']');
        }
        if (!differences.isEmpty()) {
            var diffPath = resolvePath("difference.png");
            try (var os = new BufferedOutputStream(
                    Files.newOutputStream(diffPath))) {

                ImageIO.write(differenceImage, "png", os);
            }
            differences.add("Difference image: " + diffPath);

            throw new AssertionError("differences were spotted (" +
                                     differences.size() + "):\n * " +
                                     String.join("\n * ", differences));
        }
    }

    public static Path resolvePath(String diffFile) {
        return Path.of(System.getProperty("user.dir")).resolve(diffFile);
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
}
