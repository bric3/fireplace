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

import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isFocusing;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isHighlightedFrame;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isHighlighting;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isHovered;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isHoveredSibling;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isInFocusedFlame;
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
    private final ConcurrentHashMap<Color, Color> halfDimmedColorCache = new ConcurrentHashMap<>();

    private Color rootBackGroundColor = ROOT_BACKGROUND_COLOR;
    private Color dimmedTextColor = DIMMED_TEXT_COLOR;
    private boolean dimmedNonFocusedFlames = true;

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

        var shouldDimFocusedFlame = shouldDimFocusedFlame(flags);
        if (!rootNode && shouldDim(flags) && !shouldDimFocusedFlame) {
            backgroundColor = dimmedBackground(baseBackgroundColor);
            foreground = dimmedTextColor;
        } else if (!rootNode && shouldDimFocusedFlame) {
            backgroundColor = halfDimmedBackground(baseBackgroundColor);
            if (isHighlighting(flags) && !isHighlightedFrame(flags)) {
                foreground = dimmedTextColor;
            } else {
                foreground = Colors.withAlpha(Colors.foregroundColor(backgroundColor), Colors.isDarkMode() ? 0.61f : 0.74f);
            }
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
               Colors.darker(backgroundColor, 1.15f);
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
        return dimmedColorCache.computeIfAbsent(backgroundColor, Colors::dim);
    }

    private @NotNull Color halfDimmedBackground(Color backgroundColor) {
        return halfDimmedColorCache.computeIfAbsent(backgroundColor, Colors::halfDim);
    }

    /**
     * Should dim the frame if it's in the focused flame.
     *
     * @param flags
     * @return
     */
    private boolean shouldDimFocusedFlame(int flags) {
        return dimmedNonFocusedFlames
               && isFocusing(flags)
               && isInFocusedFlame(flags)
               && !isHighlightedFrame(flags);
    }

    /**
     * Dim only if not highlighted or not focused
     * <p>
     * <ul>
     *     <li>highlighting and not highlighted frames => dim</li>
     *     <li>focusing and not in focused flames => dim</li>
     *     <li>highlighting and focusing and in focused flame => dim</li>
     *     <li>highlighting and focusing and not focused flame => nope</li>
     *     <li>highlighted => nope</li>
     *     <li>focusing => nope</li>
     * </ul>
     */
    private boolean shouldDim(int flags) {
        var highlighting = isHighlighting(flags);
        var highlightedFrame = isHighlightedFrame(flags);
        var focusing = isFocusing(flags);
        var inFocusedFlame = isInFocusedFlame(flags);

        var dimmedForHighlighting = highlighting && !highlightedFrame;
        var dimmedForFocus = dimmedNonFocusedFlames && focusing && !inFocusedFlame;
        var dimmedInFocusedFlame = dimmedNonFocusedFlames && focusing && inFocusedFlame;

        return (dimmedForHighlighting || dimmedForFocus)
               && !dimmedInFocusedFlame // don't dim frames that are in focused flame
               // && !(highlighting && highlightedFrame) // this dim highlighted that are not in focused flame
                ;
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

    @NotNull
    public DimmingFrameColorProvider<T> withDimNonFocusedFlame(boolean dimmedNonFocusedFlames) {
        this.dimmedNonFocusedFlames = dimmedNonFocusedFlames;
        return this;
    }
}
