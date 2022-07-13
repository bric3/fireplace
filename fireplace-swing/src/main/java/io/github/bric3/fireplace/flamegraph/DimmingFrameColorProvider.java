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

import io.github.bric3.fireplace.core.ui.Colors;
import io.github.bric3.fireplace.core.ui.DarkLightColor;

import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isFocusedFrame;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isFocusing;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isHighlightedFrame;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isHighlighting;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isHovered;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isHoveredSibling;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isMinimapMode;

public class DimmingFrameColorProvider<T> implements FrameColorProvider<T> {
    public static final Color DIMMED_TEXT = new DarkLightColor(
            Colors.rgba(28, 43, 52, 0.68f),
            Colors.rgba(255, 255, 255, 0.51f)
    );

    public static final Color HOVERED_NODE = new DarkLightColor(
            new Color(0xFFE0C268, true),
            new Color(0xD0E0C268, true)
    );

    public static final Color ROOT_NODE = new DarkLightColor(
            new Color(0xFFEAF6FC),
            new Color(0xFF091222)
    );
    private final Function<FrameBox<T>, Color> baseColorFunction;
    private final ColorModel reusedColorModelForMainCanvas = new ColorModel(null, null);
    private final ColorModel reusedColorModelForMinimap = new ColorModel(null, null);

    private final ConcurrentHashMap<Color, Color> dimmedColorCache = new ConcurrentHashMap<>();

    public DimmingFrameColorProvider(Function<FrameBox<T>, Color> baseColorFunction) {
        this.baseColorFunction = baseColorFunction;
    }


    @Override
    public ColorModel getColors(FrameBox<T> frame, int flags) {
        Color backgroundColor;
        Color foreground;

        var rootNode = frame.isRoot();
        if (rootNode) {
            backgroundColor = ROOT_NODE;
        } else {
            backgroundColor = baseColorFunction.apply(frame);
        }

        if (isMinimapMode(flags)) {
            // Since minimap rendering can happen in a separate thread, we need to use a separate instance
            return reusedColorModelForMinimap.set(backgroundColor, null);
        }

        if (!rootNode && shouldDim(flags)) {
            backgroundColor = cachedDim(backgroundColor);
            foreground = DIMMED_TEXT;
        } else {
            foreground = Colors.foregroundColor(backgroundColor);
        }

        if (isHovered(flags) || isHoveredSibling(flags)) {
            backgroundColor = Colors.blend(backgroundColor, HOVERED_NODE);
            foreground = Colors.foregroundColor(backgroundColor);
        }

        return reusedColorModelForMainCanvas.set(
                backgroundColor,
                foreground
        );
    }

    /**
     * Dim only if not highlighted or not focused
     * <p>
     * - highlighting and not highlighted => dim
     * - focusing and not focused => dim
     * - highlighting and focusing
     * - highlighted => nope
     * - focusing => nope
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
}
