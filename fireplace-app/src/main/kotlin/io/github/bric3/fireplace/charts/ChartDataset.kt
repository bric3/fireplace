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

interface ChartDataset {
    val label: String
    val rangeOfX: Range<Long>
    val rangeOfY: Range<Double>
    val itemCount: Int
    fun xAt(index: Int): Long
    fun yAt(index: Int): Double?
}