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

import java.awt.*;
import java.util.Objects;

/**
 * Represents a target for zooming.
 */
public class ZoomTarget {

    /** The bounds required to draw the whole content. */
    public Dimension bounds;

    /** The coordinates of the top-left corner of the frame that is the target of the zoom. */
    public Point viewOffset;

    /**
     * Creates a new zoom target.
     *
     * @param bounds     the bounds.
     * @param viewOffset the view offset.
     */
    public ZoomTarget(Dimension bounds, Point viewOffset) {
        Objects.requireNonNull(bounds);
        Objects.requireNonNull(viewOffset);
        this.bounds = bounds;
        this.viewOffset = viewOffset;
    }

    /**
     * Returns a string representation of this object, primarily for debugging purposes.
     *
     * @return A string representation of this object.
     */
    @Override
    public String toString() {
        return "ZoomTarget{" +
                "bounds=" + bounds +
                ", viewOffset=" + viewOffset +
                '}';
    }
}
