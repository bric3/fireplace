/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace

import io.github.bric3.fireplace.core.ui.Colors
import java.awt.Color
import java.awt.Component
import javax.swing.JComponent
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.RowSorter
import javax.swing.SortOrder
import javax.swing.SwingUtilities
import javax.swing.border.Border
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.border.MatteBorder
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableRowSorter


fun simpleReadOnlyTable(
    data: Array<Array<out Any?>>,
    cols: Array<String>
): JScrollPane {
    val model = ReadOnlyUpdatableTableModel(data, cols)

    return ReadOnlyTable(model).apply {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        columnSelectionAllowed = false
        rowSelectionAllowed = true
        setShowGrid(false)
        gridColor = Colors.panelBackground
        rowSorter = model.getRowSorter().apply {
            sortKeys = listOf(RowSorter.SortKey(0, SortOrder.ASCENDING))
            sort()
        }
        dropTarget = null
    }.let {
        JScrollPane(it)
    }
}

fun JScrollPane.unwrappedTable(): ReadOnlyTable = (SwingUtilities.getUnwrappedView(viewport) as ReadOnlyTable)

class ReadOnlyUpdatableTableModel(
    data: Array<Array<out Any?>>,
    private val columnNames: Array<String>
) : DefaultTableModel(data, columnNames) {
    override fun isCellEditable(row: Int, column: Int): Boolean = false
    fun getRowSorter(): TableRowSorter<out DefaultTableModel> =
        TableRowSorter<DefaultTableModel>(this).apply {
            sortsOnUpdates = true
        }

    // https://typealias.com/guides/star-projections-and-how-they-work/#how-differs-from-any
    // on why we need to use Array<out ...>
    fun setData(data: Array<out Array<out Any?>>) {
        this.setDataVector(data, columnNames)
    }
}

class ReadOnlyTable(model: DefaultTableModel) : JTable(model) {
    private val outside: Border = MatteBorder(1, 0, 1, 0, Color.RED)
    private val inside: Border = EmptyBorder(0, 1, 0, 1)
    private val highlight: Border = CompoundBorder(outside, inside)

    override fun prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): Component {
        val jComponent = super.prepareRenderer(renderer, row, column) as JComponent
        //  Striped row color
        if (!isRowSelected(row)) {
            jComponent.background = if (row % 2 == 0) background else Colors.translucent_black_10
        }

        // Add a border to the selected row
        if (isRowSelected(row)) {
            jComponent.border = highlight
        }
        return jComponent
    }

    override fun getModel(): ReadOnlyUpdatableTableModel {
        return super.getModel() as ReadOnlyUpdatableTableModel
    }
}