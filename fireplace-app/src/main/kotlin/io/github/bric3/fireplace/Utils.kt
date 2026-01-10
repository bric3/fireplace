/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace

import java.util.*
import java.util.function.Supplier

object Utils {
    val isFireplaceDebug: Boolean
        get() = (isFireplaceSwingDebug
                || java.lang.Boolean.getBoolean("fireplace.debug"))
    val isFireplaceSwingDebug: Boolean
        get() = java.lang.Boolean.getBoolean("fireplace.swing.debug")
    val isDebugging: Boolean
        get() = ProcessHandle.current()
            .info()
            .arguments()
            .map { args: Array<String>? ->
                Arrays.stream(args).anyMatch { arg: String -> arg.contains("-agentlib:jdwp") }
            }
            .orElse(false)

    fun ifDebugging(runnable: Runnable) {
        if (isDebugging) {
            runnable.run()
        }
    }

    /**
     * Returns a supplier that caches the computation of the given value supplier.
     *
     * @param valueSupplier the value supplier
     * @param <T>           the type of the value
     * @return a memoized version of the value supplier
     * @see [Holger's answer on StackOverflow](https://stackoverflow.com/a/35335467/48136)
     */
    fun <T> memoize(valueSupplier: Supplier<T>): Supplier<T> {
        return object : Supplier<T> {
            var delegate = Supplier { firstTime() }
            var initialized = false
            override fun get(): T {
                return delegate.get()
            }

            @Synchronized
            private fun firstTime(): T {
                if (!initialized) {
                    val value = valueSupplier.get()
                    delegate = Supplier { value }
                    initialized = true
                }
                return delegate.get()
            }
        }
    }

    inline fun <T> stopWatch(name: String, crossinline block: () -> T): T {
        if (!isFireplaceSwingDebug) {
            return block()
        }

        val start: Long = System.nanoTime()

        try {
            return block()
        } finally {
            val elapsed: Long = (System.nanoTime() - start) / 1_000_000 // ns -> ms
            println("[${Thread.currentThread().name}] $name took $elapsed ms")
        }
    }
}

