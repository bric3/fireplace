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
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.MouseEvent
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier
import javax.swing.*
import kotlin.collections.Map.Entry

abstract class ThreadFlamegraphView(protected val jfrBinder: JFRLoaderBinder) : ViewPanel {
    private var events: IItemCollection = ItemCollectionToolkit.EMPTY
    private var threadMapping: Map<String, Supplier<List<IItem>>> = mapOf()
    abstract override val identifier: String
    protected abstract val eventSelector: (IItemCollection) -> IItemCollection
    open val nodeWeightAttribute: IAttribute<IQuantity>? =
        null // e.g. JdkAttributes.SAMPLE_WEIGHT or JdkAttributes.ALLOCATION_SIZE ?
    open val bottomCharts: JComponent? = null

    override val view by lazy {
        val flameGraphPane = FlamegraphPane()

        val threadList = object : JList<Pair<String, Supplier<List<IItem>>>>() {
            override fun processMouseEvent(e: MouseEvent) {
                // Clear selection on clicking in empty space
                val cellBounds = this.getCellBounds(0, this.model.size - 1)
                if ((e.id == MouseEvent.MOUSE_CLICKED || e.id == MouseEvent.MOUSE_PRESSED)
                    && cellBounds != null
                    && !cellBounds.contains(e.point)
                ) {
                    this.clearSelection()
                    e.consume()
                } else {
                    super.processMouseEvent(e)
                }
            }
        }.apply {
            cellRenderer = object : DefaultListCellRenderer() {
                @Suppress("UNCHECKED_CAST")
                override fun getListCellRendererComponent(
                    list: JList<out Any>,
                    value: Any,
                    index: Int,
                    isSelected: Boolean,
                    cellHasFocus: Boolean
                ): Component {
                    val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                    val (name, iItems) = value as Pair<String, Supplier<List<IItem>>>
                    text = name
                    return this
                }
            }

            addListSelectionListener {
                val selectedValue = this.selectedValue

                CompletableFuture.runAsync {
                    flameGraphPane.setStacktraceTreeModelAsync(
                        when (selectedValue.first) {
                            ALL_THREADS_LABEL -> events.stacktraceTreeModel()
                            else -> selectedValuesList.map { it.second.get() }
                                // .mapNotNull { threadMapping[it] }
                                .flatten()
                                .stacktraceTreeModel(nodeWeightAttribute)
                        }
                    )
                }
            }
        }

        val charts = JPanel().apply {
            // layout = BoxLayout(this, BoxLayout.Y_AXIS)
            layout = BorderLayout()
            add(flameGraphPane)
            bottomCharts?.let { add(it, BorderLayout.SOUTH) }
        }

        jfrBinder.bindEvents(
            provider = {
                val eventSelection = eventSelector(it)
                events = eventSelection
                threadMapping = byThreads(
                    events,
                    JfrAttributes.EVENT_THREAD,
                    JdkAttributes.EVENT_THREAD_NAME
                )
                val threadListModel = DefaultListModel<Pair<String, Supplier<List<IItem>>>>().apply {
                    addElement(ALL_THREADS_LABEL to Supplier { emptyList() })
                    addAll(
                        threadMapping.entries.map(Entry<String, Supplier<List<IItem>>>::toPair).sortedWith(
                            Comparator.comparing(
                                Pair<String, Supplier<List<IItem>>>::first,
                                Comparator.nullsLast(Comparator.naturalOrder())
                            )
                        )
                    )
                }
                SwingUtilities.invokeLater {
                    threadList.model = threadListModel
                }
                events.stacktraceTreeModel(nodeWeightAttribute)
            },
            componentUpdate = {
                // don't do any heavy computation there
                flameGraphPane.setStacktraceTreeModelAsync(it)
            }
        )

        JSplitPane(JSplitPane.HORIZONTAL_SPLIT, JScrollPane(threadList), charts).apply {
            autoSize(0.2)
        }
    }


    companion object {
        private const val ALL_THREADS_LABEL = "<html><b>All threads</b></html>"
    }
}