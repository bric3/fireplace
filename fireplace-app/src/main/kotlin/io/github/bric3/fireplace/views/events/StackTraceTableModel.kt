/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.views.events

import io.github.bric3.fireplace.getMemberFromEvent
import org.openjdk.jmc.common.IMCStackTrace
import org.openjdk.jmc.common.item.IItem
import org.openjdk.jmc.flightrecorder.JfrAttributes
import javax.swing.table.AbstractTableModel

internal class StackTraceTableModel : AbstractTableModel() {
    private var stackTrace: IMCStackTrace? = null
    fun setRecordedStackTrace(event: IItem?) {
        stackTrace = if (event != null) {
            event.type
                .getAccessor(JfrAttributes.EVENT_STACKTRACE.key)
                ?.getMemberFromEvent(event) as? IMCStackTrace
        } else {
            null
        }
        fireTableDataChanged()
        fireTableStructureChanged()
    }

    override fun getRowCount(): Int {
        return stackTrace?.frames?.size ?: return 0
    }

    override fun getColumnCount(): Int {
        return 5
    }

    override fun getColumnName(column: Int): String {
        return when(column) {
            0 -> "Frame"
            1 -> "Line"
            2 -> "Byte Code Index"
            3 -> "Frame Type"
            4 -> "Hidden"
            else -> throw IllegalArgumentException("Unknown column $column")
        }
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        val imcStackTrace = stackTrace ?: return null
        val frame = imcStackTrace.frames[rowIndex]

        return when (columnIndex) {
            0 -> "${frame.method.type.typeName}.${frame.method.methodName}"
            1 -> frame.frameLineNumber
            2 -> frame.bci
            3 -> frame.type
            4 -> frame.method.isHidden
            else -> throw IllegalArgumentException("Unknown column $columnIndex")
        }
    }
}