/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.ui.table;

import javax.swing.table.DefaultTableModel;

public class DefaultInteractiveTableModel extends DefaultTableModel implements InteractiveTableModel {
    public DefaultInteractiveTableModel() {
        this(0, 0);
    }

    public DefaultInteractiveTableModel(int rows, int columns) {
        super(rows, columns);
    }

    public DefaultInteractiveTableModel(Object[][] data, Object[] columnNames) {
        super(data, columnNames);
    }

    @Override
    public boolean isCellInteractive(int row, int column) {
        return true;
    }

    /**
     * @param columnNames one or more names to be added as columns
     */
    public void addColumns(String... columnNames) {
        for (String name : columnNames) {
            addColumn(name);
        }
    }

    /**
     * Adds row elements.
     *
     * @param components one or more components to be added as a row
     */
    public void addRow(Object... components) {
        while (getColumnCount() < components.length) {
            addColumn("");
        }
        super.addRow(components);
    }
}
