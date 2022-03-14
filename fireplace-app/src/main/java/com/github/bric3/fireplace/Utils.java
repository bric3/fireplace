/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.github.bric3.fireplace;

import java.util.function.Supplier;

public class Utils {
    /**
     * Returns a supplier that caches the computation of the given value supplier.
     *
     * @param valueSupplier the value supplier
     * @param <T>           the type of the value
     * @return a memoized version of the value supplier
     * @see <a href="https://stackoverflow.com/a/35335467/48136">Holger's answer on StackOverflow</a>
     */
    public static <T> Supplier<T> memoize(final Supplier<T> valueSupplier) {
        return new Supplier<T>() {
            Supplier<T> delegate = this::firstTime;
            boolean initialized;

            public T get() {
                return delegate.get();
            }

            private synchronized T firstTime() {
                if (!initialized) {
                    T value = valueSupplier.get();
                    delegate = () -> value;
                    initialized = true;
                }
                return delegate.get();
            }
        };
    }
}
