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
import java.util.function.Function;

/**
 * Named function to map a value to a color.
 */
public interface ColorMapper<T> extends Function<T, Color> {
    static ColorMapper<Object> ofObjectHashUsing(Color... palette) {
        return value -> value == null ?
                        palette[0] :
                        palette[Math.abs(Objects.hashCode(value)) % palette.length];
    }

    default Color apply(T o) {
        return mapToColor(o);
    }

    Color mapToColor(T o);
}
