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

import io.github.bric3.fireplace.charts.XYDataset.XY
import java.awt.Color
import javax.swing.*

/**
 * Specifies the various properties of a chart (dataset, label, how it's rendered).
 */
data class ChartSpecification(
    val dataset: XYDataset,
    val label: String,
    val renderer: RendererDescriptor,
) {
    /**
     * Common interface for specifying how chart is rendered.
     */
    sealed interface RendererDescriptor

    /**
     * Specifies a **line-rendered** chart.
     */
    data class LineRendererDescriptor(
        val lineColor: Color? = null,
        val fillColors: List<Color>? = null,
        val tooltipFunction: (XY<Long, Double>, String) -> JComponent? = { _, _ -> null },
    ) : RendererDescriptor
}
