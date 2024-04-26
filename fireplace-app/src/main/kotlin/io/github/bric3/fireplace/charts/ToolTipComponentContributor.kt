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

import java.awt.Point
import java.awt.geom.Rectangle2D
import javax.swing.*

interface ToolTipComponentContributor {
    fun createToolTipComponent(chart: ChartSpecification, plotArea: Rectangle2D, mousePosition: Point?): JComponent?
}