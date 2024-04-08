/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.jfr.views.cpu

import io.github.bric3.fireplace.charts.Chart
import io.github.bric3.fireplace.charts.ChartComponent
import io.github.bric3.fireplace.charts.ChartSpecification
import io.github.bric3.fireplace.charts.ChartSpecification.LineRendererDescriptor
import io.github.bric3.fireplace.charts.XYDataset.XY
import io.github.bric3.fireplace.charts.XYPercentageDataset
import io.github.bric3.fireplace.charts.withAlpha
import io.github.bric3.fireplace.jfr.support.JFRLoaderBinder
import io.github.bric3.fireplace.jfr.support.JfrAnalyzer
import io.github.bric3.fireplace.jfr.support.getMemberFromEvent
import io.github.bric3.fireplace.ui.CPU_BASE
import io.github.bric3.fireplace.ui.ThreadFlamegraphView
import io.github.bric3.fireplace.ui.ViewPanel.Priority
import org.openjdk.jmc.common.IDisplayable
import org.openjdk.jmc.common.unit.UnitLookup
import org.openjdk.jmc.flightrecorder.JfrAttributes
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters
import java.awt.Color
import java.awt.Dimension
import javax.swing.*

@Priority(CPU_BASE + 1)
class MethodCpuSample(jfrBinder: JFRLoaderBinder) : ThreadFlamegraphView(jfrBinder) {
    override val identifier = "CPU"
    override val eventSelector = JfrAnalyzer::executionSamples
    override val bottomCharts: JComponent
        get() {
            val chartComponent = ChartComponent().apply {
                minimumSize = Dimension(99999, 84)
                size = size.apply { height = 84 }
                preferredSize = Dimension(99999, 84)
            }

            jfrBinder.bindEvents(
                provider = {
                    val cpuLoad = it.apply(JdkFilters.CPU_LOAD)

                    val cpuUserLoadValues = mutableListOf<XY<Long, Double>>()
                    val cpuSystemLoadValues = mutableListOf<XY<Long, Double>>()
                    val cpuTotalLoadValues = mutableListOf<XY<Long, Double>>()

                    println("type count: ${cpuLoad.count()}")

                    cpuLoad.stream().forEach { itemIterable ->
                        val type = itemIterable.type
                        val timestampAccessor = JfrAttributes.START_TIME.getAccessor(type)
                        val userCpuAccessor = JdkAttributes.JVM_USER.getAccessor(type)
                        val systemCpuAccessor = JdkAttributes.JVM_SYSTEM.getAccessor(type)
                        val totalCpuAccessor = JdkAttributes.JVM_TOTAL.getAccessor(type)

                        println("type count: ${itemIterable.itemCount} ${type.identifier}")
                        itemIterable.stream().forEach { item ->
                            val timestamp = timestampAccessor
                                .getMemberFromEvent(item).also {
                                    it.displayUsing(IDisplayable.EXACT)
                                }
                                .clampedLongValueIn(UnitLookup.EPOCH_MS)

                            if (userCpuAccessor != null) {
                                val userCpuLoad = userCpuAccessor
                                    .getMemberFromEvent(item)
                                    .doubleValueIn(UnitLookup.PERCENT_UNITY)
                                cpuUserLoadValues.add(XY(timestamp, userCpuLoad))
                            }

                            if (systemCpuAccessor != null) {
                                val systemCpuLoad = systemCpuAccessor
                                    .getMemberFromEvent(item)
                                    .doubleValueIn(UnitLookup.PERCENT_UNITY)
                                cpuSystemLoadValues.add(XY(timestamp, systemCpuLoad))
                            }

                            if (totalCpuAccessor != null) {
                                val totalCpuLoad = totalCpuAccessor
                                    .getMemberFromEvent(item)
                                    .doubleValueIn(UnitLookup.PERCENT_UNITY)
                                cpuTotalLoadValues.add(XY(timestamp, totalCpuLoad))
                            }
                        }
                    }

                    Chart(
                        buildList {
                            if (cpuUserLoadValues.isNotEmpty()) {
                                add(
                                    ChartSpecification(
                                        XYPercentageDataset(cpuUserLoadValues, ""),
                                        "User CPU load",
                                        LineRendererDescriptor(
                                            lineColor = Color.GREEN,
                                            fillColors = listOf(Color.GREEN.withAlpha(0.4f), Color.GREEN.withAlpha(0.01f)),
                                        )
                                    )
                                )
                            }
                            if (cpuSystemLoadValues.isNotEmpty()) {
                                add(
                                    ChartSpecification(
                                        XYPercentageDataset(cpuSystemLoadValues, ""),
                                        "System CPU load",
                                        LineRendererDescriptor(
                                            lineColor = Color.RED,
                                            fillColors = listOf(Color.RED.withAlpha(0.4f), Color.RED.withAlpha(0.01f)),
                                        )
                                    )
                                )
                            }
                            // Total CPU does not display well in the chart
                            // if (cpuTotalLoadValues.isNotEmpty()) {
                            //     add(
                            //         ChartSpecification(
                            //             XYPercentageDataset(cpuTotalLoadValues, ""),
                            //             "Total CPU load",
                            //             LineRendererDescriptor(
                            //                 lineColor = Color.BLACK,
                            //             )
                            //         )
                            //     )
                            // }
                        }
                    )
                           },
                componentUpdate = {
                    chartComponent.chart = it
                } // JdkFilters.THREAD_CPU_LOAD
            )

            return JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)

                add(chartComponent)
            }
        }
}