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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Objects;

/**
 * Represents a target for zooming.
 */
public class ZoomTarget<T> extends Rectangle {
    @Nullable
    public final FrameBox<@NotNull T> targetFrame;

    /**
     * Creates a new zoom target.
     *
     * @param x           The x coordinate of the target.
     * @param y           The y coordinate of the target.
     * @param width       The width of the target.
     * @param height      The height of the target.
     * @param targetFrame The target frame.
     */
    public ZoomTarget(int x, int y, int width, int height, @Nullable FrameBox<@NotNull T> targetFrame) {
        super(x, y, width, height);
        this.targetFrame = targetFrame;
    }

    /**
     * Creates a new zoom target.
     *
     * @param bounds The target canvas bounds.
     */
    public ZoomTarget(Rectangle bounds, @Nullable FrameBox<@NotNull T> targetFrame) {
        super(Objects.requireNonNull(bounds));
        this.targetFrame = targetFrame;
    }
}
