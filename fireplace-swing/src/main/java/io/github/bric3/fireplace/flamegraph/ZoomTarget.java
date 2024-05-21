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
public class ZoomTarget<T> {
    private final Rectangle targetBounds = new Rectangle();

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
        this.targetBounds.setBounds(x, y, width, height);
        this.targetFrame = targetFrame;
    }

    /**
     * Creates a new zoom target.
     *
     * @param bounds The target canvas bounds.
     */
    public ZoomTarget(@NotNull Rectangle bounds, @Nullable FrameBox<@NotNull T> targetFrame) {
        this.targetBounds.setBounds(bounds);
        this.targetFrame = targetFrame;
    }

    /**
     * Returns the target bounds.
     *
     * @return The target bounds.
     */
    public Rectangle getTargetBounds() {
        return targetBounds.getBounds();
    }

    /**
     * Returns the target frame.
     *
     * @param rect The target bounds.
     * @return The target frame.
     */
    public Rectangle getTargetBounds(@NotNull Rectangle rect) {
        rect.setBounds(targetBounds);
        return rect;
    }

    public double getWidth() {
        return targetBounds.width;
    }

    public double getHeight() {
        return targetBounds.height;
    }

    public double getX() {
        return targetBounds.x;
    }

    public double getY() {
        return targetBounds.y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ZoomTarget<?> that = (ZoomTarget<?>) o;
        return Objects.equals(targetBounds, that.targetBounds) && Objects.equals(targetFrame, that.targetFrame);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetBounds, targetFrame);
    }
}
