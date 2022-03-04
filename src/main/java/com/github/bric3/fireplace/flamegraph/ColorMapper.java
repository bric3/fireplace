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
import java.util.function.Function;

public interface ColorMapper extends Function<Object, Color> {
    default Color apply(Object o) {
        return mapToColor(o);
    }

    Color mapToColor(Object o);
}
