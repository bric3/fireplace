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


import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

import java.awt.*;
import java.nio.file.Path;
import java.util.List;

import static io.github.bric3.fireplace.flamegraph.ImageTestUtils.assertImageEquals;
import static io.github.bric3.fireplace.flamegraph.ImageTestUtils.dumpPng;
import static io.github.bric3.fireplace.flamegraph.ImageTestUtils.projectDir;
import static io.github.bric3.fireplace.flamegraph.ImageTestUtils.readImage;

@Tag("public-api")
class FlamegraphViewTest {
    @Test
    void exercise_saving_graph_to_image(@TempDir Path tempDir, TestInfo testInfo) {
        var flamegraphView = new FlamegraphView<String>();
        flamegraphView.setRenderConfiguration(
                FrameTextsProvider.of(f -> f.actualNode),
                FrameColorProvider.defaultColorProvider(__ -> Color.ORANGE),
                FrameFontProvider.defaultFontProvider()
        );

        flamegraphView.setModel(new FrameModel<>(List.of(
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
        )));


        var image = flamegraphView.saveGraphToImage(200, FlamegraphView.Mode.ICICLEGRAPH);

        dumpPng(image, projectDir().resolve("flamegraph.png"));
        assertImageEquals(
                testInfo.getDisplayName(),
                readImage("/fg-ak-200x72.png"),
                image
        );
    }
}