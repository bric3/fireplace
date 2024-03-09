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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Objects;
import java.util.function.Function;

import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isFocusedFrame;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isFocusing;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isHighlightedFrame;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isHighlighting;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isHovered;

/**
 * Strategy for choosing the colors of a frame.
 *
 * @param <T> The type of the frame node (depends on the source of profiling data).
 */
@FunctionalInterface
public interface FrameColorProvider<T> {
    class ColorModel {
        @Nullable
        public Color background;
        @Nullable
        public Color foreground;

        /**
         * Data-structure that hold the computed colors for a frame.
         *
         * @param background The background color of the frame.
         * @param foreground The foreground color of the frame.
         */
        public ColorModel(@Nullable Color background, @Nullable Color foreground) {
            this.background = background;
            this.foreground = foreground;
        }

        /**
         * Set the background and foreground colors on this instance.
         *
         * @param background The background color of the frame.
         * @param foreground The foreground color of the frame.
         * @return this
         */
        @NotNull
        public ColorModel set(@Nullable Color background, @Nullable Color foreground) {
            this.background = background;
            this.foreground = foreground;
            return this;
        }

        public ColorModel copy() {
            return new ColorModel(background, foreground);
        }
    }

    /**
     * Returns the color model for the given <code>frame</code> according to the given <code>flags</code>.
     *
     * <p>
     *     An implementation may choose to return the same instance of color model
     *     for all frames to save allocations.
     * </p>
     *
     * @param frame The frame
     * @param flags The flags
     * @return The color model for this frame
     */
    @NotNull
    ColorModel getColors(@NotNull FrameBox<@NotNull T> frame, int flags) ;

    @NotNull
    static <T> FrameColorProvider<@NotNull T> defaultColorProvider(@NotNull Function<@NotNull FrameBox<@NotNull T>, @NotNull Color> frameBaseColorFunction) {
        Objects.requireNonNull(frameBaseColorFunction, "frameColorFunction");
        return new FrameColorProvider<>() {
            /**
             * The color used to draw frames that are highlighted.
             */
            private final Color highlightedColor = new Color(0xFFFFE771, true);

            private final ColorModel reusableDataStructure = new ColorModel(null, null);

            @Override
            @NotNull
            public ColorModel getColors(@NotNull FrameBox<@NotNull T> frame, int flags) {
                Color baseBackgroundColor = frameBaseColorFunction.apply(frame);
                Color backgroundColor = baseBackgroundColor;

                if (isFocusing(flags) && !isFocusedFrame(flags)) {
                    backgroundColor = Colors.blend(baseBackgroundColor, Colors.translucent_black_80);
                }
                if (isHighlighting(flags)) {
                    backgroundColor = Colors.isDarkMode() ?
                            Colors.blend(backgroundColor, Colors.translucent_black_B0) :
                            Colors.blend(backgroundColor, Color.WHITE);
                    if (isHighlightedFrame(flags)) {
                        backgroundColor = baseBackgroundColor;
                    }
                }
                if (isHovered(flags)) {
                    backgroundColor = Colors.blend(backgroundColor, Colors.translucent_black_40);
                }

                return reusableDataStructure.set(
                        backgroundColor,
                        Colors.foregroundColor(backgroundColor)
                );
            }
        };
    }
}
