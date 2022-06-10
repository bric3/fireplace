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

import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isFocusedFrame;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isFocusing;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isHighlightedFrame;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isHighlighting;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isMinimapMode;

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

        var rootNode = isRootNode(frame);
        if (rootNode) {
            backgroundColor = Colors.isDarkMode() ?
                              ROOT_NODE_DARK :
                              ROOT_NODE_LIGHT;
        } else {
            backgroundColor = baseColorFunction.apply(frame);
        }

        if (!rootNode && shouldDim(flags) && !isMinimapMode(flags)) {
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

    /**
     * Dim only if not highlighted or not focused
     *
     * - highlighting and not highlighted => dim
     * - focusing and not focused => dim
     * - highlighting and focusing
     *    - highlighted => nope
     *    - focusing => nope
     */
    private boolean shouldDim(int flags) {
        var highlighting = isHighlighting(flags);
        var highlightedFrame = isHighlightedFrame(flags);
        var focusing = isFocusing(flags);
        var focusedFrame = isFocusedFrame(flags);


        var dimmedForHighlighting = highlighting && !highlightedFrame;
        var dimmedForFocus = focusing && !focusedFrame;


        return (dimmedForHighlighting || dimmedForFocus)
               && !(highlighting
                    && focusing
                    && (highlightedFrame || focusedFrame));
    }

    private Color cachedDim(Color color) {
        return dimmedColorCache.computeIfAbsent(color, Colors::dim);
    }
    
    private boolean isRootNode(FrameBox<T> frame) {
        return frame.stackDepth == 0;
    }
}
