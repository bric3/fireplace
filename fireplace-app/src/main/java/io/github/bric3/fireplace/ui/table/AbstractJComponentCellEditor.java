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
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;
import java.util.EventObject;

/**
 * Cell editor that can propagate <code>updateUI</code> for any custom cell editor component.
 * <p>
 * In order to use as a cell editor extends this class and implements the required interface:
 * {@link javax.swing.table.TableCellEditor}, {@link javax.swing.tree.TreeCellEditor}.
 * <p>
 * Host of copy/pasted code from {@link javax.swing.AbstractCellEditor}, but extends a {@link JComponent} instead.
 * <p>
 * This class exists because it allows propagating `updateUI` to a custom editor component.
 * From {@code JTable.updateUI} -> {@code javax.swing.SwingUtilities.updateRendererOrEditorUI} we can
 * see that {@link javax.swing.DefaultCellEditor} is supported however, {@link javax.swing.DefaultCellEditor}
 * doesn't support custom editor component (see its constructor), also it comes with many more tools not needed
 * for this specific purpose.
 *
 * <p>
 * An {@link javax.swing.AbstractCellEditor} would be appropriate, but it is not recognized by the specific
 * updateUI mechanism of a {@link javax.swing.JTable}.
 *
 * <p>
 * In order to accommodate this, this class extends {@link JComponent} so that it can receive
 * {@code updateUI}, and copies {@link javax.swing.AbstractCellEditor} methods. In this case {@code JComponent}
 * only acts as a contract (like an interface).
 *
 * @see javax.swing.JTable#updateUI
 * @see javax.swing.SwingUtilities#updateRendererOrEditorUI
 * @see BasicJComponentCellRenderer
 */
@SuppressWarnings("JavadocReference") // needed because some methods are not public
public abstract class AbstractJComponentCellEditor<C extends JComponent> extends JComponent implements CellEditor {
    /**
     * The editor that should get the updateUI propagation. Nullable.
     */
    protected C editorComponent;

    /**
     * Constructs an {@code AbstractJComponentCellEditor} that uses a given editor component.
     * @param editorComponent the component to be used as editor, <code>null</code> accepted.
     */
    public AbstractJComponentCellEditor(C editorComponent) {
        this.editorComponent = editorComponent;
    }

    /**
     * Update the editor component UI.
     * In order to propagate the LaF changes or scale changes
     * a cell editor or renderer needs to be a JComponent
     * @see javax.swing.JTable#updateUI
     * @see javax.swing.SwingUtilities#updateRendererOrEditorUI
     */
    @Override
    public void updateUI() {
        if (editorComponent != null) {
            SwingUtilities.updateComponentTreeUI(editorComponent);
        }
    }

    //// Class field method order not respected to mark the copy/paste of AbstractCellEditor methods.
    //// The rest of this class is copy/paste of AbstractCellEditor methods, **untouched**

    /**
     * The list of listeners.
     */
    protected EventListenerList listenerList = new EventListenerList();

    /**
     * The change event.
     */
    protected transient ChangeEvent changeEvent = null;

    /**
     * Returns true.
     * @param e  an event object
     * @return true
     */
    public boolean isCellEditable(EventObject e) {
        return true;
    }

    /**
     * Returns true.
     * @param anEvent  an event object
     * @return true
     */
    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }

    /**
     * Calls <code>fireEditingStopped</code> and returns true.
     * @return true
     */
    public boolean stopCellEditing() {
        fireEditingStopped();
        return true;
    }

    /**
     * Calls <code>fireEditingCanceled</code>.
     */
    public void  cancelCellEditing() {
        fireEditingCanceled();
    }

    /**
     * Adds a <code>CellEditorListener</code> to the listener list.
     * @param l  the new listener to be added
     */
    public void addCellEditorListener(CellEditorListener l) {
        listenerList.add(CellEditorListener.class, l);
    }

    /**
     * Removes a <code>CellEditorListener</code> from the listener list.
     * @param l  the listener to be removed
     */
    public void removeCellEditorListener(CellEditorListener l) {
        listenerList.remove(CellEditorListener.class, l);
    }

    /**
     * Returns an array of all the <code>CellEditorListener</code>s added
     * to this AbstractCellEditor with addCellEditorListener().
     *
     * @return all of the <code>CellEditorListener</code>s added or an empty
     *         array if no listeners have been added
     * @since 1.4
     */
    public CellEditorListener[] getCellEditorListeners() {
        return listenerList.getListeners(CellEditorListener.class);
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is created lazily.
     *
     * @see EventListenerList
     */
    protected void fireEditingStopped() {
        var listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == CellEditorListener.class) {
                // Lazily create the event:
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((CellEditorListener) listeners[i + 1]).editingStopped(changeEvent);
            }
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is created lazily.
     *
     * @see EventListenerList
     */
    protected void fireEditingCanceled() {
        // Guaranteed to return a non-null array
        var listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==CellEditorListener.class) {
                // Lazily create the event:
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((CellEditorListener)listeners[i+1]).editingCanceled(changeEvent);
            }
        }
    }
}
