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
import kotlin.math.max
import kotlin.math.min

/**
 * A dataset containing zero, one or many (x, y) data items.
 */
open class XYDataset(
    sourceItems: List<XY<Long, Double>>,
    val label: String
) {
    private val items: List<XY<Long, Double>>
    val rangeOfX: Range<Long>
    val rangeOfY: Range<Double>

    /**
     * Creates a new dataset.
     *
     * @param sourceItems the source items (`null` not permitted).
     * @param label The label for the dataset.
     */
    init {
        // verify that none of the x-values is null or NaN ot INF
        // and while doing that record the mins and maxes
        Objects.requireNonNull(sourceItems, "sourceItems must not be null")
        Objects.requireNonNull(label, "label must not be null")
        require(sourceItems.isNotEmpty()) { "sourceItems must not be empty" }
        this.items = ArrayList(sourceItems)
        var minX = Long.MAX_VALUE
        var maxX = Long.MIN_VALUE
        var minY = Double.POSITIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        for (sourceItem in sourceItems) {
            Objects.requireNonNull(sourceItem, "elements of sourceItems must not be null")
            minX = min(minX.toDouble(), sourceItem.x.toDouble()).toLong()
            maxX = max(maxX.toDouble(), sourceItem.x.toDouble()).toLong()

            minY = min(minY, sourceItem.y)
            maxY = max(maxY, sourceItem.y)
        }
        this.rangeOfX = Range(minX, maxX)
        @Suppress("LeakingThis")
        this.rangeOfY = tweakYRange(Range(minY, maxY))
    }

    protected open fun tweakYRange(yRange: Range<Double>): Range<Double> {
        return yRange
    }

    val itemCount: Int
        get() = items.size

    fun xAt(index: Int): Long {
        return items[index].x
    }

    fun yAt(index: Int): Double {
        return items[index].y
    }

    /**
     * Compare with another instance, ignoring data points.
     *
     * Only compare the ranges and the label.
     *
     * @param other the object to compare with this instance.
     * @return whether two data set are assumed to be equal.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val xyDataset = other as XYDataset
        return rangeOfX == xyDataset.rangeOfX && rangeOfY == xyDataset.rangeOfY && label == xyDataset.label
    }

    /**
     * Hash this instance, ignoring data points.
     *
     * Only hashes the ranges and the label.
     * @return The hash.
     */
    override fun hashCode(): Int {
        return Objects.hash(rangeOfX, rangeOfY, label)
    }

    /**
     * A pair of objects.
     *
     * @param <X> the type of the first object.
     * @param <Y> the type of the second object.
    </Y></X> */
    @JvmRecord
    data class XY<X : Number?, Y : Number?>(val x: X, val y: Y)
}
