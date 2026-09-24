/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.flamegraph.fixtures;

import io.github.bric3.fireplace.flamegraph.FrameBox;
import io.github.bric3.fireplace.flamegraph.FrameModel;

import java.util.ArrayList;
import java.util.List;

/** Fresh, deterministic data shared by component fixtures. */
public final class FrameModelFixture {
    private FrameModelFixture() {}

    public static FrameModel<String> rootWithSiblings() {
        return new FrameModel<>(List.of(
                new FrameBox<>("root", 0, 1, 0),
                new FrameBox<>("left", 0, .5, 1),
                new FrameBox<>("leaf", 0, .25, 2),
                new FrameBox<>("right", .5, 1, 1)
        ));
    }

    public static FrameModel<String> deepChain(int depth) {
        var frames = new ArrayList<FrameBox<String>>();
        frames.add(new FrameBox<>("root", 0, 1, 0));
        for (int level = 1; level <= depth; level++) {
            frames.add(new FrameBox<>("level " + level, .25, .75, level));
        }
        return new FrameModel<>(frames);
    }
}
