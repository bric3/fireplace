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

/**
 * A dataset containing zero, one or many (x, y) data items.
 * This dataset is specifically for datasets where Y is a percentage between 0 and 1.
 * @param sourceItems the source items (`null` not permitted).
 * @param label The label for the dataset.
 */
class XYPercentageDataset(sourceItems: List<XY<Long, Double>>, label: String) : XYDataset(sourceItems, label) {
    override fun tweakYRange(yRange: Range<Double>): Range<Double> {
        return Range(0.0, 1.0)
    }
}
