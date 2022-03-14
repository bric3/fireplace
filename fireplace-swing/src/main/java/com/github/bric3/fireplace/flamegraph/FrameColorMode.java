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

import java.awt.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface FrameColorMode<T> extends BiFunction<Function<Object, Color>, T, Color> {
    default Color apply(Function<Object, Color> colorMapper, T frameNode) {
        return getColor(colorMapper, frameNode);
    }

    Color getColor(Function<Object, Color> colorMapper, T frameNode);
}
