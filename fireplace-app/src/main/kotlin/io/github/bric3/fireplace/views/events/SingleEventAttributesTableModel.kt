package io.github.bric3.fireplace.views.events

import io.github.bric3.fireplace.formatValue
import io.github.bric3.fireplace.getMemberFromEvent
import org.openjdk.jmc.common.IDescribable
import org.openjdk.jmc.common.item.IAccessorKey
import org.openjdk.jmc.common.item.IItem
import org.openjdk.jmc.flightrecorder.JfrAttributes
import javax.swing.table.AbstractTableModel

internal class SingleEventAttributesTableModel(private var event: IItem?) : AbstractTableModel() {
    private var fields: List<Pair<IAccessorKey<*>, IDescribable>> = listOf()

    init {
        refreshFields()
    }

    private fun refreshFields() {
        val event = event ?: return

        event.type.accessorKeys.entries.forEach {
            it.key.identifier
            val valueDescriptor = it.value.name
            println("${it.key.identifier} + $valueDescriptor")
        }

        fields = event.type.accessorKeys.filterKeys {
            println(it.identifier)
            it != JfrAttributes.EVENT_STACKTRACE.key && it != JfrAttributes.EVENT_TYPE.key
        }.toList()
    }

    fun setRecordedEvent(event: IItem?) {
        this.event = event
        refreshFields()
        fireTableDataChanged()
    }

    override fun getRowCount(): Int {
        return if (event == null) 0 else fields.size
    }

    override fun getColumnCount(): Int {
        return 2
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        val event = event ?: return ""
        val descriptor = fields[rowIndex]
        if (columnIndex == 0) {
            return descriptor.second.name
        } else if (columnIndex == 1) {
            val value = event.type.getAccessor(descriptor.first).getMemberFromEvent(event)
            return descriptor.first.contentType.defaultFormatter.formatValue(value)
        }
        return null
    }
}

