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
public class ZoomTarget extends Rectangle {
    public ZoomTarget(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    /**
     * Creates a new zoom target.
     *
     * @param bounds The target canvas bounds.
     */
    public ZoomTarget(Rectangle bounds) {
        super(Objects.requireNonNull(bounds));
    }
}
