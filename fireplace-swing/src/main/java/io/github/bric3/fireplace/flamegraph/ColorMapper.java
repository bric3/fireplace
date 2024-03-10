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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Objects;
import java.util.function.Function;

/**
 * Named function to map a value to a color.
 *
 * @param <T> The type of the object to map to a color.
 */
public interface ColorMapper<T> extends Function<@Nullable T, @NotNull Color> {
    /**
     * Returns a {@code ColorMapper} instance that maps objects (using the hashCode) to colors
     * in the supplied palette.
     *
     * @param palette the palette.
     *
     * @return A color.
     */
    @NotNull
    static <T> ColorMapper<@Nullable T> ofObjectHashUsing(@NotNull Color... palette) {
        return o -> o == null ?
                        palette[0] :
                        palette[Math.abs(Objects.hashCode(o)) % palette.length];
    }

    /**
     * Returns the color that is mapped to the specified object.
     *
     * @param o the object ({@code null} permitted).
     *
     * @return A color.
     */
    @NotNull
    default Color apply(@Nullable T o) {
        return mapToColor(o);
    }

    /**
     * Returns the color that is mapped to the specified object.  This is the same as the {@link #apply(Object)}
     * method but has a more descriptive name.
     *
     * @param o the object ({@code null} permitted).
     *
     * @return The color.
     */
    @NotNull
    Color mapToColor(@Nullable T o);
}
