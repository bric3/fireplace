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

import io.github.bric3.fireplace.core.ui.StringClipper;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

/**
 * Provides a list of functions to convert a frame to a text.
 *
 * <p>
 * Those are used to render the frame labels. It is up to the renderer to choose which function
 * will be used. Implementors can assume the renderer will try the text providers,
 * and choose the text that fits the best. The renderer is likely to clip the text,
 * according to {@link #clipStrategy()}, if it is too long.
 *
 * @param <T> The type of the frame node (depends on the source of profiling data).
 */
@FunctionalInterface
public interface FrameTextsProvider<T> {
    /**
     * Returns a list of functions to convert a frame to a text.
     *
     * @return a list of functions to convert a frame to a text.
     */
    @NotNull
    List<@NotNull Function<@NotNull FrameBox<@NotNull T>, @NotNull String>> frameToTextCandidates();

    /**
     * Factory method to create a {@link FrameTextsProvider} from a list of functions.
     * @param frameToTextCandidates a list of functions to convert a frame to a text.
     * @return a new {@link FrameTextsProvider} instance.
     * @param <T> The type of the frame node (depends on the source of profiling data).
     */
    @NotNull
    static <T> FrameTextsProvider<@NotNull T> of(@NotNull List<@NotNull Function<@NotNull FrameBox<@NotNull T>, @NotNull String>> frameToTextCandidates) {
        return () -> frameToTextCandidates;
    }

    /**
     * Factory method to create a {@link FrameTextsProvider} from a list of functions.
     * @param frameToTextCandidates a list of functions to convert a frame to a text.
     * @return a new {@link FrameTextsProvider} instance.
     * @param <T> The type of the frame node (depends on the source of profiling data).
     */
    @NotNull
    @SafeVarargs
    static <T> FrameTextsProvider<@NotNull T> of(@NotNull Function<@NotNull FrameBox<@NotNull T>, @NotNull String>... frameToTextCandidates) {
        return of(List.of(frameToTextCandidates));
    }

    /**
     * Factory method to create an empty {@link FrameTextsProvider}.
     * @return a new empty {@link FrameTextsProvider} instance.
     * @param <T> The type of the frame node (depends on the source of profiling data).
     */
    @NotNull
    static <T> FrameTextsProvider<@NotNull T> empty() {
        return of(List.of());
    }

    /**
     * Returns the strategy to use to clip the text.
     * @return the strategy to use to clip the text.
     */
    @NotNull
    default StringClipper clipStrategy() {
        return StringClipper.RIGHT;
    }
}
