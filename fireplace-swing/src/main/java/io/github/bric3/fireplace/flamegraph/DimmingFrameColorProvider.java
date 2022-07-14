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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isFocusedFrame;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isFocusing;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isHighlightedFrame;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isHighlighting;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isHovered;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isHoveredSibling;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isMinimapMode;

/**
 * Frame color provider that supports frame dimming and hovering.
 *
 * <p>
 * This frame color provider is responsible for computing the color of a frame.
 * It uses the actual frame and passes it to a basic <em>color function</em>
 * that will apply the actual base background color to the frame.
 * </p>
 *
 * <p>
 * Then this base color can be altered or changed depending on the frame's
 * flags.
 * </p>
 *
 * @param <T> The actual type of frame.
 */
public class DimmingFrameColorProvider<T> implements FrameColorProvider<T> {
    public static final Color DIMMED_TEXT_COLOR = new DarkLightColor(
            Colors.rgba(28, 43, 52, 0.68f),
            Colors.rgba(255, 255, 255, 0.51f)
    );

    public static final Color HOVERED_BACKGROUND_COLOR = new DarkLightColor(
            new Color(0xFFE0C268, true),
            new Color(0xD0E0C268, true)
    );

    public static final Color ROOT_BACKGROUND_COLOR = new DarkLightColor(
            new Color(0xFFEAF6FC),
            new Color(0xFF091222)
    );
    private final Function<FrameBox<T>, Color> baseColorFunction;

    /**
     * Single instance to avoid too many allocations, only for the main canvas.
     */
    private final ColorModel reusedColorModelForMainCanvas = new ColorModel(null, null);

    /**
     * Single instance to avoid too many allocations, only for the minimap.
     * Since the minimap generation happens on a different thread, it is necessary
     * to have a separate instance.
     */
    private final ColorModel reusedColorModelForMinimap = new ColorModel(null, null);

    private final ConcurrentHashMap<Color, Color> dimmedColorCache = new ConcurrentHashMap<>();

    private Color rootBackGroundColor = ROOT_BACKGROUND_COLOR;
    private Color dimmedTextColor = DIMMED_TEXT_COLOR;
    private Color hoveredBackgroundColor = HOVERED_BACKGROUND_COLOR;

    /**
     * Builds a basic frame color provider.
     *
     * @param baseColorFunction The color function that provides the frame's color.
     * @see #withRootBackgroundColor(Color)
     * @see #withDimmedTextColor(Color)
     * @see #withHoveredBackgroundColor(Color)
     */
    public DimmingFrameColorProvider(Function<FrameBox<T>, Color> baseColorFunction) {
        this.baseColorFunction = baseColorFunction;
    }

    @Override
    public ColorModel getColors(FrameBox<T> frame, int flags) {
        Color backgroundColor;
        Color foreground;

        var rootNode = frame.isRoot();
        if (rootNode) {
            backgroundColor = rootBackGroundColor;
        } else {
            backgroundColor = baseColorFunction.apply(frame);
        }

        if (isMinimapMode(flags)) {
            // Since minimap rendering can happen in a separate thread, we need to use a separate instance
            return reusedColorModelForMinimap.set(backgroundColor, null);
        }

        if (!rootNode && shouldDim(flags)) {
            backgroundColor = dimmedBackground(backgroundColor);
            foreground = dimmedTextColor;
        } else {
            foreground = Colors.foregroundColor(backgroundColor);
        }

        if (isHovered(flags)) {
            backgroundColor = hoverBackground(backgroundColor);
            foreground = Colors.foregroundColor(backgroundColor);
        }

        if (isHoveredSibling(flags)) {
            backgroundColor = hoverSiblingBackground(backgroundColor);
            foreground = Colors.foregroundColor(backgroundColor);
        }

        return reusedColorModelForMainCanvas.set(
                backgroundColor,
                foreground
        );
    }

    /**
     * Compute the background color for a hovered frame.
     *
     * @param backgroundColor The background color of the frame to alter.
     * @return The hovered background color.
     */
    private Color hoverBackground(Color backgroundColor) {
        return Colors.blend(backgroundColor, hoveredBackgroundColor);
    }

    /**
     * Compute the background color for a hovered sibling frame.
     *
     * @param backgroundColor The background color of the frame to alter.
     * @return The hovered background color.
     */
    private Color hoverSiblingBackground(Color backgroundColor) {
        return Colors.blend(backgroundColor, hoveredBackgroundColor);
    }

    /**
     * Dims the background color.
     *
     * @param backgroundColor The background color to dim.
     * @return The dimmed color.
     */
    protected Color dimmedBackground(Color backgroundColor) {
        return cachedDim(backgroundColor);
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

    public DimmingFrameColorProvider<T> withRootBackgroundColor(Color rootBackgroundColor) {
        this.rootBackGroundColor = Objects.requireNonNull(rootBackgroundColor);
        return this;
    }

    public DimmingFrameColorProvider<T> withDimmedTextColor(Color dimmedTextColor) {
        this.dimmedTextColor = Objects.requireNonNull(dimmedTextColor);
        return this;
    }

    public DimmingFrameColorProvider<T> withHoveredBackgroundColor(Color hoveredBackgroundColor) {
        this.hoveredBackgroundColor = Objects.requireNonNull(hoveredBackgroundColor);
        return this;
    }
}
