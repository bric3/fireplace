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

package io.github.bric3.fireplace.flamegraph;


import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

import java.awt.*;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static io.github.bric3.fireplace.flamegraph.ImageTestUtils.assertImageEquals;
import static io.github.bric3.fireplace.flamegraph.ImageTestUtils.dumpPng;
import static io.github.bric3.fireplace.flamegraph.ImageTestUtils.projectDir;
import static io.github.bric3.fireplace.flamegraph.ImageTestUtils.readImage;
import static io.github.bric3.fireplace.flamegraph.ImageTestUtils.testReportDir;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("public-api")
class FlamegraphImageTest {
    @Test
    void exercise_saving_graph_to_image(@TempDir Path tempDir, TestInfo testInfo) {
        var flamegraphView = new FlamegraphImage<String>(
                FrameTextsProvider.of(f -> f.actualNode),
                FrameColorProvider.defaultColorProvider(__ -> Color.ORANGE),
                FrameFontProvider.defaultFontProvider()
        );


        var image = flamegraphView.generate(
                simpleFrameModel(),
                FlamegraphView.Mode.ICICLEGRAPH,
                200
        );

        dumpPng(image, testReportDir().resolve(testInfo.getDisplayName() + "-output.png"));
        assertImageEquals(
                testInfo.getDisplayName(),
                readImage(asset_fg_ak_200x72("png")),
                image
        );
    }

    private static FrameModel<String> simpleFrameModel() {
        return new FrameModel<>(List.of(
                new FrameBox<>("root", 0, 1, 0),
                new FrameBox<>("A", 0, 0.2, 1),
                new FrameBox<>("B", 0.20000000001, 0.40, 1),
                new FrameBox<>("C", 0.40000000001, 1, 1),
                new FrameBox<>("D", 0.020001, 0.10, 2),
                new FrameBox<>("E", 0.11, 0.18, 2),
                new FrameBox<>("F", 0.21, 0.30, 2),
                new FrameBox<>("G", 0.41, 0.50, 2),
                new FrameBox<>("H", 0.51, 0.99, 2),
                new FrameBox<>("I", 0.111, 0.15, 3),
                new FrameBox<>("J", 0.43, 0.46, 3),
                new FrameBox<>("K", 0.53, 0.80, 3)
        ));
    }

    @Test
    void exercice_saving_by_passing_custom_graphics_eg_for_SVG_with_batik(TestInfo testInfo) throws IOException {
        var flamegraphView = new FlamegraphImage<String>(
                FrameTextsProvider.of(f -> f.actualNode),
                FrameColorProvider.defaultColorProvider(__ -> Color.ORANGE),
                FrameFontProvider.defaultFontProvider()
        );

        var document = GenericDOMImplementation.getDOMImplementation().createDocument(
                "http://www.w3.org/2000/svg",
                "svg",
                null
        );
        var svgGraphics2D = new SVGGraphics2D(document);

        var wantedWidth = 200;
        flamegraphView.generate(
                simpleFrameModel(),
                FlamegraphView.Mode.FLAMEGRAPH,
                wantedWidth,
                svgGraphics2D,
                height -> {
                    svgGraphics2D.setSVGCanvasSize(new Dimension(wantedWidth, height));
                    // svgGraphics2D.getRoot().setAttributeNS(
                    //         null,
                    //         "viewBox",
                    //         "0 0 " + wantedWidth + " " + height
                    // );
                }
        );

        var stringWriter = new StringWriter();
        svgGraphics2D.stream(stringWriter, true);

        Files.writeString(testReportDir().resolve(testInfo.getDisplayName() + "-output.svg"), stringWriter.getBuffer());
        var type = "svg";
        assertThat(stringWriter.toString()).isEqualTo(content(asset_fg_ak_200x72(type)));
    }

    private static String asset_fg_ak_200x72(String type) {
        return "/fg-ak-200x72" + platform() +
               "." + type;
    }

    private static String platform() {
        var osName = System.getProperty("os.name");
        if (osName.startsWith("Mac")) {
            return "macOs";
        } else if (osName.startsWith("Linux")) {
            return Objects.equals(System.getenv("CI"), "true") ? "-gha-linux" : "linux";
        }
        return "";
    }

    private static String content(String name) {
        try {
            return Files.readString(Path.of(Objects.requireNonNull(ImageTestUtils.class.getResource(name)).toURI()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}