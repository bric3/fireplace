/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.github.bric3;

public class FrameBox<T> {
    final T jfrNode;
    final double startX;
    final double endX;
    final int stackDepth;

    public FrameBox(T jfrNode, double startX, double endX, int stackDepth) {
        this.jfrNode = jfrNode;
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
