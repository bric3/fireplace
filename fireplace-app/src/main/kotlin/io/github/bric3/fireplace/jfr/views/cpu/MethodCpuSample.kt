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
            // val chart = Chart(
            //     null,
            //     LineChartRenderer().apply {
            //         fillColors = arrayOf(Color.RED, Color.YELLOW)
            //     }
            // ).apply {
            //     background = RectangleContent.blankCanvas { Colors.panelBackground }
            // }
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
                        listOf(
                            ChartSpecification(
                                XYPercentageDataset(cpuUserLoadValues, ""),
                                "User CPU load",
                                LineRendererDescriptor(
                                    lineColor = Color.BLACK,
                                    fillColors = listOf(Color.RED, Color.YELLOW),
                                )
                            )
                        )
                    )

                    // buildList {
                    //     if (cpuUserLoadValues.isNotEmpty()) {
                    //         add(
                    //             XYPercentageDataset(
                    //                 cpuUserLoadValues,
                    //                 "User CPU load"
                    //             )
                    //         )
                    //     }
                    //     if (cpuSystemLoadValues.isNotEmpty()) {
                    //         add(
                    //             XYPercentageDataset(
                    //                 cpuSystemLoadValues,
                    //                 "System CPU load"
                    //             )
                    //         )
                    //     }
                    //     if (cpuTotalLoadValues.isNotEmpty()) {
                    //         add(
                    //             XYPercentageDataset(
                    //                 cpuTotalLoadValues,
                    //                 "Total CPU load"
                    //             )
                    //         )
                    //     }
                    // }
                },
                componentUpdate = {
                    // if (it.isEmpty()) {
                    //     chart.dataset = null
                    // } else {
                    //     it.stream().limit(1).forEach { chart.dataset = it }
                    // }
                    chartComponent.chart = it
                } // JdkFilters.THREAD_CPU_LOAD
            )

            return JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)

                add(chartComponent)
            }
        }
}
// java.util.concurrent.CompletionException: java.lang.IllegalArgumentException: min must be less than max, min is 9223372036854775807, max is -9223372036854775808
// 	at java.base/java.util.concurrent.CompletableFuture.encodeThrowable(CompletableFuture.java:315)
// 	at java.base/java.util.concurrent.CompletableFuture.completeThrowable(CompletableFuture.java:320)
// 	at java.base/java.util.concurrent.CompletableFuture$AsyncSupply.run(CompletableFuture.java:1770)
// 	at java.base/java.util.concurrent.CompletableFuture$AsyncSupply.exec(CompletableFuture.java:1760)
// 	at java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:387)
// 	at java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1312)
// 	at java.base/java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1843)
// 	at java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1808)
// 	at java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:188)
// Caused by: java.lang.IllegalArgumentException: min must be less than max, min is 9223372036854775807, max is -9223372036854775808
// 	at io.github.bric3.fireplace.charts.Range.check(Range.java:30)
// 	at io.github.bric3.fireplace.charts.Range.<init>(Range.java:16)
// 	at io.github.bric3.fireplace.charts.XYDataset.<init>(XYDataset.java:41)
// 	at io.github.bric3.fireplace.charts.XYPercentageDataset.<init>(XYPercentageDataset.java:17)
// 	at io.github.bric3.fireplace.jfr.views.cpu.MethodCpuSample._get_bottomCharts_$lambda$6(MethodCpuSample.kt:88)
// 	at io.github.bric3.fireplace.jfr.support.JFRLoaderBinder$bindEvents$eventBinder$1.invoke$lambda$0(JFRLoaderBinder.kt:41)
// 	at java.base/java.util.concurrent.CompletableFuture$AsyncSupply.run(CompletableFuture.java:1768)
// 	... 6 more