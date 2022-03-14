/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.github.bric3.fireplace.flamegraph;

public class FrameBox<T> {
    public final T actualNode;
    public final double startX;
    public final double endX;
    public final int stackDepth;

    public FrameBox(T actualNode, double startX, double endX, int stackDepth) {
        this.actualNode = actualNode;
        this.startX = startX;
        this.endX = endX;
        this.stackDepth = stackDepth;
    }

    @Override
    public String toString() {
        return "FlameNode{" +
               "startX=" + startX +
               ", endX=" + endX +
               ", depth=" + stackDepth +
               '}';
    }
}
