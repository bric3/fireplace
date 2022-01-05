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

import java.util.function.Supplier;

public class Utils {
    // non thread safe
    public static <T> Supplier<T> memoize(final Supplier<T> valueSupplier) {
        return new Supplier<T>() {
            private T cachedValue;

            @Override
            public T get() {
                if (cachedValue == null) {
                    cachedValue = valueSupplier.get();
                }
                return cachedValue;
            }
        };
    }
}
