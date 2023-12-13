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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represent the model of the flamegraph.
 *
 * @param <T> The type of the actual node object.
 * @see FrameBox
 */
public class FrameModel<T> {
    public static final FrameModel<?> EMPTY = new FrameModel<>(Collections.emptyList());

    public final String title;
    public final List<FrameBox<T>> frames;
    public final FrameEquality<T> frameEquality;
    public String description;

    /**
     * Creates model of the flamegraph frames.
     *
     * <p>It takes a list of {@link FrameBox} objects that wraps the actual data,
     * which is referred to as <em>node</em>.
     * </p>.
     *
     * <p>
     * The equality applies equals on the actual node object {@link T}.
     * </p>
     *
     * @param frames The list of {@code FrameBox} objects.
     */
    public FrameModel(List<FrameBox<T>> frames) {
        this("", (a, b) -> Objects.equals(a.actualNode, b.actualNode), frames);
    }

    /**
     * Creates model of the flamegraph frames.
     *
     * <p>It takes a list of {@link FrameBox} objects that wraps the actual data,
     * which is referred to as <em>node</em>.
     * </p>.
     *
     * @param title         The title of the flamegraph, used in the root frame.
     * @param frameEquality Custom equality code for the actual node object {@code T}.
     * @param frames        The list of {@code FrameBox} objects.
     */
    public FrameModel(
            String title,
            FrameEquality<T> frameEquality,
            List<FrameBox<T>> frames
    ) {
        this.title = Objects.requireNonNull(title, "title");
        this.frames = Objects.requireNonNull(frames, "frames");
        this.frameEquality = Objects.requireNonNull(frameEquality, "frameEquality");
    }

    @SuppressWarnings("unchecked")
    public static <T> FrameModel<T> empty() {
        return (FrameModel<T>) EMPTY;
    }

    /**
     * Returns whether two frames are considered equal.
     *
     * @param <T> The type of the actual node object.
     */
    public interface FrameEquality<T> {
        boolean equal(FrameBox<T> a, FrameBox<T> b);
    }

    /**
     * Text that describes the flamegraph.
     *
     * <p>Tooltip function could access that to render a specific tooltip
     * on the root node.</p>
     *
     * @param description The text that describes the flamegraph.
     * @return this
     */
    public FrameModel<T> withDescription(String description) {
        this.description = Objects.requireNonNull(description, "description");
        return this;
    }
}
