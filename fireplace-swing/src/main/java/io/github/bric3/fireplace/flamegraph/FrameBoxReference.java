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
 * A general reference to a FrameBox and the mouse location where the reference has
 * been triggered.
 */
public class FrameBoxReference<T> {

    private final FrameBox<T> frameBox;

    private final Point location;

    public FrameBoxReference(FrameBox<T> frameBox, Point location) {
        this.frameBox = frameBox;
        this.location = location;
    }

    public FrameBox<T> getFrameBox() {
        return this.frameBox;
    }

    public Point getLocation() {
        return this.location;
    }

    @Override
    public String toString() {
        return "FrameBoxReference{" +
                "frameBox=" + frameBox + ":" + frameBox.actualNode +
                ", location=" + location +
                '}';
    }
}
