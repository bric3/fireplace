/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.jfr.views.events

import io.github.bric3.fireplace.jfr.JFRLoaderBinder
import io.github.bric3.fireplace.ui.ViewPanel
import io.github.bric3.fireplace.ui.toolkit.autoSize
import org.openjdk.jmc.common.item.IItemCollection
import org.openjdk.jmc.common.item.ItemCollectionToolkit
import java.awt.BorderLayout
import java.util.function.Consumer
import java.util.function.Function
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JTabbedPane
import javax.swing.JTable
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel


class EventBrowser(private val jfrBinder: JFRLoaderBinder) : ViewPanel {
    private var events: IItemCollection = ItemCollectionToolkit.EMPTY
    override val identifier: String = "Event Browser"

    override val view by lazy {
        createContent().also {
            jfrBinder.bindEvents(
                Function.identity(),
                eventConsumer,
            )
        }
    }

    private lateinit var eventConsumer: Consumer<IItemCollection>

    private fun createContent(): JPanel {

        val eventPropertiesTableModel = SingleEventAttributesTableModel(null)
        val eventPropertiesTable = JTable(eventPropertiesTableModel).apply {
            autoCreateRowSorter = true
            columnModel.getColumn(0).headerValue = "Field Name"
            columnModel.getColumn(1).headerValue = "Value"
        }

        val stackTraceTableModel = StackTraceTableModel()
        val stackTraceTable = JTable(stackTraceTableModel)

        val eventDetailsTabs = JTabbedPane().apply {
            add("Properties", JPanel(BorderLayout()).apply {
                add(JScrollPane(eventPropertiesTable))
            })
            add("Stack Trace", JPanel(BorderLayout()).apply {
                add(JScrollPane(stackTraceTable))
            })
        }

        val eventsTableModel = EventsTableModel()
        val eventsTable = JTable(eventsTableModel).apply {
            autoCreateRowSorter = true
            autoCreateColumnsFromModel = true

            selectionModel.addListSelectionListener {
                if (it.source === selectionModel) {
                    var selectedIndex: Int = selectedRow
                    if (selectedIndex >= 0) {
                        selectedIndex = convertRowIndexToModel(selectedIndex)
                        // new selection here means we need to update the properties table and possibly the stack trace table
                        eventsTableModel.getAtRow(selectedIndex).let { event ->
                            eventPropertiesTableModel.setRecordedEvent(event)
                            stackTraceTableModel.setRecordedStackTrace(event)
                        }
                    } else {
                        eventPropertiesTableModel.setRecordedEvent(null)
                        stackTraceTableModel.setRecordedStackTrace(null)
                    }
                }
            }
        }


        val treeModel = EventTypesByCategoryTreeModel()
        eventConsumer = Consumer {
            events = it
            treeModel.populateTree(it)
        }
        val tree = JTree(treeModel).apply {
            selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
            selectionModel.addTreeSelectionListener {
                val path: TreePath = it.path

                val treeCategorisation: List<String> = pathAsCategories(path)
                
                val filteredEvents = events.stream().filter { itemIterable ->
                    val eventType = itemIterable.type
                    val eventCategories = (model as EventTypesByCategoryTreeModel).typeToCategory[eventType]?.categories
                        ?: throw IllegalArgumentException("No category for event type $eventType")

                    if (eventCategories.size < treeCategorisation.size - 1) {
                        return@filter false
                    }

                    if (eventCategories.size == treeCategorisation.size - 1) {
                        if (eventType.name != treeCategorisation.last()) {
                            return@filter false
                        }
                        for (i in 0 until treeCategorisation.size - 1) {
                            if (eventCategories[i] != treeCategorisation[i]) {
                                return@filter false
                            }
                        }
                    }

                    if (eventCategories.size >= treeCategorisation.size) {
                        for (i in treeCategorisation.indices) {
                            if (eventCategories[i] != treeCategorisation[i]) {
                                return@filter false
                            }
                        }
                    }

                    true
                }

                (eventsTable.model as EventsTableModel).singleTypeEventCollection = ItemCollectionToolkit.build { filteredEvents }

            }
            // selectionPath = TreePath(model.firstLeaf.path)
            minimumSize = preferredSize
        }


        return JPanel(BorderLayout()).apply {
            add(
                JSplitPane(
                    JSplitPane.HORIZONTAL_SPLIT,
                    JScrollPane(tree).apply {

                    },
                    JSplitPane(
                        JSplitPane.VERTICAL_SPLIT,
                        JScrollPane(eventsTable),
                        eventDetailsTabs
                    ).apply {
                        autoSize(0.5)
                    }
                ).apply {
                    autoSize(0.2)
                }
            )
        }
    }


    private fun pathAsCategories(path: TreePath): List<String> {
        val result: MutableList<String> = ArrayList()
        val nodesInPath = path.path
        for (i in 1 until nodesInPath.size) {
            val node = nodesInPath[i] as DefaultMutableTreeNode
            val content: EventTypesByCategoryTreeModel.CategoryOrType =
                node.userObject as EventTypesByCategoryTreeModel.CategoryOrType
            result.add(content.category)
        }
        return result
    }
}