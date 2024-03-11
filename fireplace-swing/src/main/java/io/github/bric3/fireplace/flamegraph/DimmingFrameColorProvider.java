/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.flamegraph;

import io.github.bric3.fireplace.core.ui.Colors;
import io.github.bric3.fireplace.core.ui.LightDarkColor;
import org.jetbrains.annotations.NotNull;

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
public class DimmingFrameColorProvider<T> implements FrameColorProvider<@NotNull T> {
    public static final Color DIMMED_TEXT_COLOR = new LightDarkColor(
            Colors.rgba(28, 43, 52, 0.68f),
            Colors.rgba(255, 255, 255, 0.51f)
    );

    public static final Color ROOT_BACKGROUND_COLOR = new LightDarkColor(
            new Color(0xFFEAF6FC),
            new Color(0xFF091222)
    );
    @NotNull
    private final Function<@NotNull FrameBox<@NotNull T>, @NotNull Color> baseColorFunction;

    /**
     * Single instance to avoid too many allocations, only for the main canvas.
     */
    private final ColorModel reusedColorModelForMainCanvas = new ColorModel();

    /**
     * Single instance to avoid too many allocations, only for the minimap.
     * Since the minimap generation happens on a different thread, it is necessary
     * to have a separate instance.
     */
    private final ColorModel reusedColorModelForMinimap = new ColorModel();

    private final ConcurrentHashMap<Color, Color> dimmedColorCache = new ConcurrentHashMap<>();

    private Color rootBackGroundColor = ROOT_BACKGROUND_COLOR;
    private Color dimmedTextColor = DIMMED_TEXT_COLOR;

    /**
     * Builds a basic frame color provider.
     *
     * @param baseColorFunction The color function that provides the frame's color.
     * @see #withRootBackgroundColor(Color)
     * @see #withDimmedTextColor(Color)
     */
    public DimmingFrameColorProvider(@NotNull Function<@NotNull FrameBox<@NotNull T>, @NotNull Color> baseColorFunction) {
        this.baseColorFunction = baseColorFunction;
    }

    @Override
    @NotNull
    public ColorModel getColors(@NotNull FrameBox<@NotNull T> frame, int flags) {
        Color backgroundColor;
        Color baseBackgroundColor;
        Color foreground;

        var rootNode = frame.isRoot();
        if (rootNode) {
            baseBackgroundColor = backgroundColor = rootBackGroundColor;
        } else {
            baseBackgroundColor = backgroundColor = baseColorFunction.apply(frame);
        }

        if (isMinimapMode(flags)) {
            // Since minimap rendering can happen in a separate thread, we need to use a separate instance
            return reusedColorModelForMinimap.set(
                    backgroundColor,
                    ColorModel.DEFAULT_FRAME_FOREGROUND_COLOR // fg is unused when rendering minimap
            );
        }

        if (!rootNode && shouldDim(flags)) {
            backgroundColor = dimmedBackground(backgroundColor);
            foreground = dimmedTextColor;
        } else {
            foreground = Colors.foregroundColor(backgroundColor);
        }

        if (isHovered(flags)) {
            backgroundColor = hoverBackground(baseBackgroundColor);
            foreground = Colors.foregroundColor(backgroundColor);
        }

        if (isHoveredSibling(flags)) {
            backgroundColor = hoverSiblingBackground(baseBackgroundColor);
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
    private @NotNull Color hoverBackground(@NotNull Color backgroundColor) {
        return Colors.isDarkMode() ?
               Colors.brighter(backgroundColor, 1.1f, 0.95f) :
               Colors.darker(backgroundColor, 1.25f);
    }

    /**
     * Compute the background color for a hovered sibling frame.
     *
     * @param backgroundColor The background color of the frame to alter.
     * @return The hovered background color.
     */
    private @NotNull Color hoverSiblingBackground(@NotNull Color backgroundColor) {
        return hoverBackground(backgroundColor);
    }

    /**
     * Dims the background color.
     *
     * @param backgroundColor The background color to dim.
     * @return The dimmed color.
     */
    protected @NotNull Color dimmedBackground(@NotNull Color backgroundColor) {
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

    private @NotNull Color cachedDim(@NotNull Color color) {
        return dimmedColorCache.computeIfAbsent(color, Colors::dim);
    }

    @NotNull
    public DimmingFrameColorProvider<T> withRootBackgroundColor(@NotNull Color rootBackgroundColor) {
        this.rootBackGroundColor = Objects.requireNonNull(rootBackgroundColor);
        return this;
    }

    @NotNull
    public DimmingFrameColorProvider<T> withDimmedTextColor(@NotNull Color dimmedTextColor) {
        this.dimmedTextColor = Objects.requireNonNull(dimmedTextColor);
        return this;
    }
}
