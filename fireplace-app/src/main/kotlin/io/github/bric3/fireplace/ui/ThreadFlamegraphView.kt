/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.ui

import io.github.bric3.fireplace.jfr.support.JFRLoaderBinder
import io.github.bric3.fireplace.jfr.support.byThreads
import io.github.bric3.fireplace.jfr.support.stacktraceTreeModel
import io.github.bric3.fireplace.ui.toolkit.autoSize
import org.openjdk.jmc.common.item.IAttribute
import org.openjdk.jmc.common.item.IItem
import org.openjdk.jmc.common.item.IItemCollection
import org.openjdk.jmc.common.item.ItemCollectionToolkit
import org.openjdk.jmc.common.unit.IQuantity
import org.openjdk.jmc.flightrecorder.JfrAttributes
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes
import java.awt.event.MouseEvent
import java.util.concurrent.CompletableFuture
import javax.swing.*

abstract class ThreadFlamegraphView(private val jfrBinder: JFRLoaderBinder) : ViewPanel {
    private var events: IItemCollection = ItemCollectionToolkit.EMPTY
    private var threadMapping: Map<String, List<IItem>> = mapOf()
    abstract override val identifier: String
    protected abstract val eventSelector: (IItemCollection) -> IItemCollection
    open val nodeWeightAttribute : IAttribute<IQuantity>? = null // e.g. JdkAttributes.SAMPLE_WEIGHT or JdkAttributes.ALLOCATION_SIZE ?

    override val view by lazy {
        val flameGraphPane = FlamegraphPane()

        val threadListModel = DefaultListModel<String>()
        val threadList = object : JList<String>(threadListModel) {
            override fun processMouseEvent(e: MouseEvent) {
                // Clear selection on clicking in empty space
                if ((e.id == MouseEvent.MOUSE_CLICKED || e.id == MouseEvent.MOUSE_PRESSED) &&
                    !this.getCellBounds(0, this.model.size - 1).contains(e.point)
                ) {
                    this.clearSelection()
                    e.consume()
                } else {
                    super.processMouseEvent(e)
                }
            }
        }.apply {
            addListSelectionListener {
                val selectedValue = this.selectedValue

                CompletableFuture.runAsync {
                    flameGraphPane.setStacktraceTreeModelAsync(
                        when (selectedValue) {
                            ALL_THREADS_LABEL, null -> events.stacktraceTreeModel()
                            else -> selectedIndices.map { threadListModel[it] }
                                .mapNotNull { threadMapping[it] }
                                .flatten()
                                .stacktraceTreeModel(nodeWeightAttribute)
                        }
                    )
                }
            }
        }


        jfrBinder.bindEvents(
            {
                val eventSelection = eventSelector(it)
                events = eventSelection
                threadMapping = byThreads(
                    events,
                    JfrAttributes.EVENT_THREAD,
                    JdkAttributes.EVENT_THREAD_NAME
                )
                threadListModel.run {
                    clear()
                    addElement(ALL_THREADS_LABEL)
                    addAll(
                        threadMapping.keys.sortedWith(
                            Comparator.nullsLast(
                                Comparator.naturalOrder()
                            )
                        )
                    )
                }

                events.stacktraceTreeModel(nodeWeightAttribute)
            }
        ) {
            // don't do any heavy computation there
            flameGraphPane.setStacktraceTreeModelAsync(it)
        }


        JSplitPane(JSplitPane.HORIZONTAL_SPLIT, JScrollPane(threadList), flameGraphPane).apply {
            autoSize(0.2)
        }
    }


    companion object {
        private const val ALL_THREADS_LABEL = "<html><b>All threads</b></html>"
    }
}