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

import io.github.bric3.fireplace.jfr.formatValue
import io.github.bric3.fireplace.jfr.getMemberFromEvent
import org.openjdk.jmc.common.IDescribable
import org.openjdk.jmc.common.item.*
import org.openjdk.jmc.flightrecorder.JfrAttributes
import javax.swing.table.AbstractTableModel

internal class EventsTableModel : AbstractTableModel() {
    private data class AttributeDescriptor(val accessorKey: IAccessorKey<*>, val describable: IDescribable) {
        override fun hashCode(): Int {
            return accessorKey.hashCode()
        }
        override fun equals(other: Any?): Boolean {
            return other is AttributeDescriptor && other.accessorKey == accessorKey
        }
    }

    private lateinit var commonFields: List<AttributeDescriptor>
    private var singleTypeEvents: List<IItem> = listOf()
    private var showEventTypesColumn: Boolean = false

    var singleTypeEventCollection: IItemCollection = ItemCollectionToolkit.EMPTY
        set(events) {
            field = events
            val eventPerTypeIterable = field.stream().toList()
            val eventTypes = eventPerTypeIterable.map { it.type }.distinct()
            
            this.singleTypeEvents = eventPerTypeIterable.flatten()

            extractEventFields(eventTypes)
            fireTableStructureChanged()
        }

    override fun getColumnName(column: Int): String? {
        if (singleTypeEvents.isEmpty()) {
            return null
        }
        if (column < 0) return null
        if (column == commonFields.size && showEventTypesColumn) {
            return "Event Type"
        }
        return commonFields[column].describable.name
    }

    private fun extractEventFields(types: List<IType<IItem>>) {
        if (types.isEmpty()) {
            return
        }

        commonFields = types.map { type ->
            type.accessorKeys.filterKeys {
//                if (Utils.isDebugging()) {
//                    println("attr ids: " + it.identifier)
//                }
                it != JfrAttributes.EVENT_STACKTRACE.key && it != JfrAttributes.EVENT_TYPE.key
            }.mapTo(LinkedHashSet()) { AttributeDescriptor(it.key, it.value) }
        }.reduce { acc, list ->
            acc.also {
                it.retainAll(list)
            }
        }.toList()

        showEventTypesColumn = types.size > 1
    }

    override fun getRowCount(): Int {
        return singleTypeEvents.size
    }

    override fun getColumnCount(): Int {
        if (singleTypeEvents.isEmpty()) {
            return 0
        }
        return commonFields.size + if (showEventTypesColumn) 1 else 0
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        singleTypeEvents[rowIndex].let {
            if (columnIndex == commonFields.size && showEventTypesColumn) {
                return it.type.name
            }

            val descriptor = commonFields[columnIndex]
            val value = it.type.getAccessor(descriptor.accessorKey).getMemberFromEvent(it)

//            if (Utils.isDebugging()) {
//                println("${descriptor.accessorKey.contentType} -> ${if (value == null) null else value::class.java}")
//            }

            return descriptor.accessorKey.contentType.defaultFormatter.formatValue(value)
        }
    }

    fun getAtRow(selectedIndex: Int): IItem? {
        return singleTypeEvents.getOrNull(selectedIndex)
    }
}