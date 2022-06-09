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

package io.github.bric3.fireplace;

import io.github.bric3.fireplace.core.ui.Colors;
import io.github.bric3.fireplace.flamegraph.FrameBox;
import io.github.bric3.fireplace.flamegraph.FrameColorProvider;

import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isHighlightedFrame;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isHighlighting;

class DimmingFrameColorProvider<T> implements FrameColorProvider<T> {
    public static final Color DIMMED_TEXT_DARK = Colors.rgba(255, 255, 255, 0.51f);
    public static final Color DIMMED_TEXT_LIGHT = Colors.rgba(28, 43, 52, 0.68f);
    public static final Color ROOT_NODE_LIGHT = new Color(0xffeaf6fc);
    public static final Color ROOT_NODE_DARK = new Color(0xff091222);
    private final Function<FrameBox<T>, Color> baseColorFunction;
    private final ColorModel reusableDataStructure = new ColorModel(null, null);

    private final ConcurrentHashMap<Color, Color> dimmedColorCache = new ConcurrentHashMap<>();

    public DimmingFrameColorProvider(Function<FrameBox<T>, Color> baseColorFunction) {
        this.baseColorFunction = baseColorFunction;
    }


    @Override
    public ColorModel getColors(FrameBox<T> frame, int flags) {
        Color backgroundColor;
        Color foreground;

        if (isRootNode(frame)) {
            backgroundColor = Colors.isDarkMode() ?
                              ROOT_NODE_DARK :
                              ROOT_NODE_LIGHT;
        } else {
            backgroundColor = baseColorFunction.apply(frame);
        }

        if (isDimmed(frame, flags)) {
            backgroundColor = cachedDim(backgroundColor);
            foreground = Colors.isDarkMode() ?
                         DIMMED_TEXT_DARK :
                         DIMMED_TEXT_LIGHT;
        } else {
            foreground = Colors.foregroundColor(backgroundColor);
        }

        return reusableDataStructure.set(
                backgroundColor,
                foreground
        );
    }

    private Color cachedDim(Color color) {
        return dimmedColorCache.computeIfAbsent(color, Colors::dim);
    }

    private boolean isDimmed(FrameBox<T> frame, int flags) {
        return isHighlighting(flags) && !isHighlightedFrame(flags);
    }

    private boolean isRootNode(FrameBox<T> frame) {
        return frame.stackDepth == 0;
    }
}
