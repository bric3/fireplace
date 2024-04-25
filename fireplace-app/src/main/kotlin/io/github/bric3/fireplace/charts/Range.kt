/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.charts

import java.util.*

/**
 * Represents a range of values between min and max.
 * 
 * @param min the minimum value.
 * @param max the maximum value.
 */
@JvmRecord
data class Range<T : Number>(val min: T, val max: T) {
    val isZeroLength: Boolean
        get() = min == max

    /**
     * Returns a range based on this range but extended (if necessary)
     * to include the specified value.
     *
     * @param v the value.
     * @return The range.
     */
    fun include(v: T): Range<T> {
        require(!(v is Double && !java.lang.Double.isFinite(v.toDouble()))) { "only finite values permitted" }
        if (compare(v, min) < 0) {
            return Range(v, max)
        }
        if (compare(v, max) > 0) {
            return Range(min, v)
        }
        return this
    }

    /**
     * Calculates a fractional value indicating where [v] lies along the range.
     * This will return 0.0 if the range has zero-length.
     *
     * @param v the value.
     * @return The fraction.
     */
    fun ratioFor(v: T): Double {
        if (compare(max, min) > 0) {
            if (min is Double) {
                return (v.toDouble() - min.toDouble()) / (max.toDouble() - min.toDouble())
            }
            if (min is Long) {
                return (v.toLong() - min.toLong()).toDouble() / (max.toLong() - min.toLong()).toDouble()
            }
            if (min is Int) {
                return (v.toInt() - min.toInt()).toDouble() / (max.toInt() - min.toInt()).toDouble()
            }
            throw IllegalArgumentException("Unsupported type " + min::class.java)
        }
        return 0.0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val range = other as Range<*>
        if (min.javaClass != range.min.javaClass) {
            return false
        }
        return compare(range.min, min) == 0 && compare(range.max, max) == 0
    }

    override fun hashCode(): Int {
        return Objects.hash(min, max)
    }

    override fun toString(): String {
        return "[Range: $min, $max]"
    }

    init {
        check(min, max)
    }

    companion object {
        private fun <T : Number?> check(min: T?, max: T?) {
            require(!(min == null || max == null)) { "Null 'min' or 'max' argument." }
            require(min.javaClass == max.javaClass) { "min and max must be of the same type, min is " + min.javaClass + ", max is " + max.javaClass }
            require(!(min is Double && min.toDouble() > max.toDouble())) { "min must be less than max, min is $min, max is $max" }
            require(!(min is Long && min.toLong() > max.toLong())) { "min must be less than max, min is $min, max is $max" }
            require(!(min is Int && min.toInt() > max.toInt())) { "min must be less than max, min is $min, max is $max" }
        }

        /**
         * compare two `Number` of the same type.
         *
         * @param a   the first value.
         * @param b   the second value.
         * @param <T> The type of the number.
         * @return the value `0` if `d1` is numerically equal to `d2`; a value less than
         * `0` if `d1` is numerically less than `d2`; and a value greater than `0`
         * if `d1` is numerically greater than `d2`.
        </T> */
        fun <T : Number> compare(a: T, b: T): Int {
            if (a is Double) {
                return (a as Double).compareTo((b as Double))
            }
            if (a is Long) {
                return (a as Long).compareTo((b as Long))
            }
            if (a is Int) {
                return (a as Int).compareTo((b as Int))
            }
            throw IllegalArgumentException("Unsupported type " + a.javaClass)
        }
    }
}
