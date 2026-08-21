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

import javax.swing.table.TableModel;

interface InteractiveTableModel extends TableModel {
    /**
     * Whether this cell can be interacted with.
     * Regular tables will only render the cell
     * using the cell renderer.
     * Returning <code>true</code> will make {@link InteractiveJTable}
     * trigger the editor mode when the mose is over that cell.
     *
     * @param row The row index
     * @param column The column index
     * @return Wheter that cell should be interactive,
     * if true this will edit the cell with the given component.
     */
    boolean isCellInteractive(int row, int column);
}
