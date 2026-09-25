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

import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

class JTableHacker {
    static {
        if (!JTable.class.getModule().isOpen(JTable.class.getPackageName(), ClassLoader.getSystemClassLoader().getUnnamedModule())) {
            throw new IllegalStateException("Needs JVM option \"--add-opens java.desktop/javax.swing=ALL-UNNAMED\"");
        }
    }

    private static final Method getRowModelMethod;

    static {
        try {
            getRowModelMethod = JTable.class.getDeclaredMethod("getRowModel");
            getRowModelMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("JTable internal structure changed", e);
        }
    }

    private static final Field sortManagerField;

    static {
        try {
            sortManagerField = JTable.class.getDeclaredField("sortManager");
            sortManagerField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("JTable internal structure changed", e);
        }
    }

    private final JTable table;

    JTableHacker(JTable table) {
        this.table = table;
    }

    SizeSequence getRowModel() {
        try {
            return (SizeSequence) getRowModelMethod.invoke(table);
        } catch (Exception e) {
            throw new RuntimeException("JTable internal structure changed", e);
        }
    }

    void sortManager_setViewRowHeight(int row, int rowHeight) {
        try {
            var sortManager = sortManagerField.get(table);
            if (sortManager != null) {
                var setViewRowHeight = sortManager.getClass().getDeclaredMethod("setViewRowHeight", int.class, int.class);
                setViewRowHeight.setAccessible(true);
                setViewRowHeight.invoke(sortManager, row, rowHeight);
            }
        } catch (Exception e) {
            throw new RuntimeException("JTable internal structure changed", e);
        }
    }
}
