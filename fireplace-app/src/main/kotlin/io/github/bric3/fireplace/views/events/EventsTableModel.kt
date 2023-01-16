package io.github.bric3.fireplace.views.events

import io.github.bric3.fireplace.Utils
import io.github.bric3.fireplace.formatValue
import io.github.bric3.fireplace.getMemberFromEvent
import org.openjdk.jmc.common.IDescribable
import org.openjdk.jmc.common.item.IAccessorKey
import org.openjdk.jmc.common.item.IItem
import org.openjdk.jmc.common.item.IItemCollection
import org.openjdk.jmc.common.item.IType
import org.openjdk.jmc.common.item.ItemCollectionToolkit
import org.openjdk.jmc.flightrecorder.JfrAttributes
import javax.swing.table.AbstractTableModel
import kotlin.streams.toList

internal class EventsTableModel : AbstractTableModel() {
    private lateinit var type: IType<IItem>
    private var singleTypeEvents: List<IItem> = listOf()
    private lateinit var commonFields: List<Pair<IAccessorKey<*>, IDescribable>>

    var singleTypeEventCollection: IItemCollection = ItemCollectionToolkit.EMPTY
        set(events) {
            field = events
            val eventPerTypeIterable = field.stream().toList()
            val eventTypes = eventPerTypeIterable.map { it.type }.distinct()
            assert(eventTypes.size == 1) { "Expected the events a single event type, but got " + eventPerTypeIterable.size + ": " + eventPerTypeIterable.joinToString { it.type.identifier } }
            this.type = eventTypes.single()
            this.singleTypeEvents = eventPerTypeIterable.flatten()
            
            extractEventFields()
            fireTableStructureChanged()
        }

    override fun getColumnName(column: Int): String? {
        if (singleTypeEvents.isEmpty()) {
            return null
        }
        if (column < 0) return null
        return commonFields[column].second.name
    }

    private fun extractEventFields() {
        commonFields = type.accessorKeys.filterKeys {
            println("attr ids: " + it.identifier)
            it != JfrAttributes.EVENT_STACKTRACE.key && it != JfrAttributes.EVENT_TYPE.key
        }.toList()
    }

    override fun getRowCount(): Int {
        return singleTypeEvents.size
    }

    override fun getColumnCount(): Int {
        if (singleTypeEvents.isEmpty()) {
            return 0
        }
        return commonFields.size
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        singleTypeEvents[rowIndex].let {
            val descriptor = commonFields[columnIndex]
            val value = type.getAccessor(descriptor.first).getMemberFromEvent(it)

            if (Utils.isDebugging()) {
                println("${descriptor.first.contentType} -> ${if (value == null) null else value::class.java}")
            }

            return descriptor.first.contentType.defaultFormatter.formatValue(value)
        }
    }

    fun getAtRow(selectedIndex: Int): IItem? {
        return singleTypeEvents.getOrNull(selectedIndex)
    }
}