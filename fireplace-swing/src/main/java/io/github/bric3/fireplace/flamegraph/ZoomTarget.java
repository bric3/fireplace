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
    /** The 1:1 scale */
    public static final double SCALE_1 = 1d;

    /** The scale factor for this zoom target */
    public final double scaleFactor;

    /** The bounds required to draw the whole content. */
    public final Dimension canvasBounds;

    /** The coordinates of the top-left corner of the frame that is the target of the zoom. */
    public final Point viewOffset;

    /**
     * Creates a new zoom target.
     *
     * @param scaleFactor
     * @param canvasBounds      the bounds.
     * @param viewOffset  the view offset.
     */
    public ZoomTarget(double scaleFactor, Dimension canvasBounds, Point viewOffset) {
        this.scaleFactor = scaleFactor;
        Objects.requireNonNull(canvasBounds);
        Objects.requireNonNull(viewOffset);
        this.canvasBounds = canvasBounds;
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
               "bounds=" + canvasBounds +
               ", viewOffset=" + viewOffset +
               '}';
    }
}
