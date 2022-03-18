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

/**
 * Represents a target for zooming.
 */
public class ZoomTarget {

    public Dimension bounds;

    public Point viewOffset;

    public ZoomTarget(Dimension bounds, Point viewOffset) {
        this.bounds = bounds;
        this.viewOffset = viewOffset;
    }

    public String toString() {
        return "[bounds=" + bounds + ",offset=" + viewOffset + "]";
    }
}
