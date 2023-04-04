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

import io.github.bric3.fireplace.ui.SwingUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * A tailored {@link JTable} where cells are interactive.
 * <p>
 * In practice the table is triggering the edition of a cell when
 * the user interacts with it. It allows to either use
 * the table as layout.
 * The component height are resized according to the column width
 * and their preferred height.
 * <p>
 * When used as a layout, add the components directly to the <em>layout</em>
 * model {@link InteractiveTableLayout}.
 * <pre><code>
 * var interactiveTableLayout = new InteractiveTableLayout();
 * {
 *     interactiveTableLayout.addColumns("Chart Name", "Chart Panel");
 *     // create data and put into table, first column is a string,
 *     // the second is derived from a JPanel.
 *     interactiveTableLayout.addRow(new JLabel("Chart"), new ChartPanel("block4"));
 *     var jTextArea = new JTextArea(
 *             "Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
 *             "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " +
 *             "Ut enim ad minim veniam"
 *     );
 *     jTextArea.setLineWrap(true);
 *     jTextArea.setEditable(false);
 *     interactiveTableLayout.addRow(new JLabel("Text"), jTextArea);
 *     interactiveTableLayout.addRow(new JLabel("Exp"), new ExpandablePanel());
 * }
 * var myTable = new InteractiveJTable(interactiveTableLayout);
 * {
 *     myTable.setStriped(true);
 *     myTable.setShowHovered(true);
 *     myTable.setShowVerticalLines(false);
 * }
 * </code></pre>
 * <p>
 * Other use of the table allow to use it as a regular table,
 * where the component displaying the data can be interacted with..
 */
public class InteractiveJTable extends JTable {
    private final JTableHacker jTableHacker = new JTableHacker(this);

    private boolean striped = false;
    private boolean showHovered = false;
    private Color rowHoverBackground =
            Objects.requireNonNullElse(
                    UIManager.getColor("InteractiveJTable.rowHoverBackground"),
                    new Color(0xE5EBF3) // new Color(0x4E5357); // dark variant
            );
    private Color rowStripeBackground =
            Objects.requireNonNullElse(
                    UIManager.getColor("InteractiveJTable.rowStripeBackground"),
                    new Color(0xE6E9EE) // new Color(0x34383A); // dark variant
            );

    /**
     * Constructs an {@code InteractiveJTable} with {@code 0} rows and {@code 0} columns.
     * For adding data see the methods "addColumns" and "addRow".
     * Note that the method "addRow" automatically adds missing columns.
     */
    public InteractiveJTable() {
        this(new DefaultInteractiveTableModel(0, 0));
    }

    public InteractiveJTable(InteractiveTableModel model) {
        this(model, null);
    }

    public InteractiveJTable(InteractiveTableModel dm, TableColumnModel cm) {
        this(dm, cm, null);
    }

    public InteractiveJTable(InteractiveTableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super(dm, cm, sm);
        setDefaultRenderer(JComponent.class, new InteractiveTableCellRenderer<>());
        setDefaultEditor(JComponent.class, new InteractiveTableCellEditor<>());
        setSurrendersFocusOnKeystroke(true);

        ProperMouseEnterExitListener.install(this).register(
                new RowHoveringListener(this),
                new AutoEditCellListener(this)
        );

        adjustRowHeights();
    }

    @Override
    protected TableModel createDefaultDataModel() {
        return new DefaultInteractiveTableModel();
    }

    @Override
    public InteractiveTableModel getModel() {
        return (InteractiveTableModel) super.getModel();
    }

    @Override
    public void setModel(@NotNull TableModel dataModel) {
        if (!(dataModel instanceof InteractiveTableModel)) {
            throw new IllegalArgumentException("Model must be an instance of InteractiveTableModel");
        }
        setModel((InteractiveTableModel) dataModel);
    }

    public void setModel(@NotNull InteractiveTableModel dataModel) {
        super.setModel(dataModel);
    }

    @Override
    public void doLayout() {
        super.doLayout();
        adjustRowHeights();
    }

    public void adjustRowHeights() {
        for (int row = 0; row < getRowCount(); row++) {
            adjustRowHeight(row);
        }
        resizeAndRepaint();
    }

    private void adjustRowHeight(int row) {
        int rowHeight = getRowHeight();

        for (int column = 0; column < getColumnCount(); column++) {
            var editorComponent = getEditorComponent();
            if (getEditingRow() == row && getEditingColumn() == column && editorComponent != null) {
                editorComponent.setSize(getColumnModel().getColumn(column).getWidth(), 0);
                rowHeight = Math.max(rowHeight, editorComponent.getPreferredSize().height);
            } else {
                var comp = prepareRenderer(getCellRenderer(row, column), row, column);
                rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
            }
        }


        // same code as in JTable.setRowHeight(int, int), without the resizeAndRepaint() call
        if (rowHeight <= 0) {
            throw new IllegalArgumentException("New row height less than 1");
        }
        jTableHacker.getRowModel().setSize(row, rowHeight);
        jTableHacker.sortManager_setViewRowHeight(row, rowHeight);
    }

    public boolean isCellHoveringInteractive(int row, int column) {
        return getModel().isCellInteractive(
                convertRowIndexToModel(row),
                convertColumnIndexToModel(column)
        );
    }

    /**
     * Sets whether to show horizontal lines between cells, removing inter-cell spacing.
     *
     * @param show true if table view should draw horizontal lines
     */
    public void setShowHorizontalLines(boolean show) {
        var d = getIntercellSpacing();
        if (show) {
            super.setShowHorizontalLines(true);
            setIntercellSpacing(new Dimension(d.width, 1));
        } else {
            super.setShowHorizontalLines(false);
            setIntercellSpacing(new Dimension(d.width, 0));
        }
    }

    /**
     * Sets whether to show vertical lines between cells, removing inter-cell spacing.
     *
     * @param show true if table view should draw vertical lines
     */
    public void setShowVerticalLines(boolean show) {
        var d = getIntercellSpacing();
        if (show) {
            super.setShowVerticalLines(true);
            setIntercellSpacing(new Dimension(1, d.height));
        } else {
            super.setShowVerticalLines(false);
            setIntercellSpacing(new Dimension(0, d.height));
        }
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        var component = super.prepareRenderer(renderer, row, column);

        if (component instanceof JComponent && !isCellSelected(row, column)) {
            prepareRowBackground((JComponent) component, row, "renderer: ");
        }

        // Updating the size of the component here will force the table to be redrawn
        // see BasicTableUI.paintCell()
        // Yet it's necessary to update the preferred height of the component before
        // rendering as the content of the component might have changed, and
        // its bound outdated.
        return updatePreferredRowHeight(this, component, row, column);
    }

    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        var component = super.prepareEditor(editor, row, column);

        if (component instanceof JComponent && isCellHoveringInteractive(row, column)) {
            prepareRowBackground((JComponent) component, row, "editor  : ");
        }

        // Updating the size of the component here will force the table to be redrawn
        // see BasicTableUI.paintCell()
        // Yet it's necessary to update the preferred height of the component before
        // rendering as the content of the component might have changed, and
        // its bound outdated.
        return updatePreferredRowHeight(this, component, row, column);
    }

    private void prepareRowBackground(JComponent cellComponent, int row, String prepare) {
        if (isStriped()) {
            applyBackgroundToHierarchy(cellComponent, c -> c.setBackground(row % 2 == 1 ? getBackground() : getRowStripeBackground()));
        }
        if (isShowHovered()) {
            int hoveredRow = (int) Objects.requireNonNullElse(getClientProperty("hoveredRow"), -1);
            if (hoveredRow == row) {
                applyBackgroundToHierarchy(cellComponent, c -> {
                    c.setBackground(getRowHoverBackground(row));
                });
            } else {
                var background = isStriped() ?
                        row % 2 == 1 ? getBackground() : getRowStripeBackground() :
                        getBackground();
                applyBackgroundToHierarchy(cellComponent, c -> {
                    if (background != c.getBackground()) {
                        c.setBackground(background);
                    }
                });
            }
        }
    }

    @Override
    public void editingCanceled(ChangeEvent e) {
        super.editingCanceled(e);
    }

    private static void applyBackgroundToHierarchy(@NotNull JComponent container, Consumer<JComponent> backgroundApplier) {
        container.setOpaque(true);
        SwingUtils.descendantOrSelf(container)
                .filter(JComponent.class::isInstance)
                .map(JComponent.class::cast)
                .forEach(backgroundApplier);
    }

    /**
     * Returns the hover color for the given row.
     *
     * @param row The row index.
     * @return the background hover color.
     */
    protected Color getRowHoverBackground(int row) {
        return getRowHoverBackground();
    }


    public Color getRowHoverBackground() {
        return rowHoverBackground;
    }

    public void setRowHoverBackground(Color rowHoverBackground) {
        var oldHoverBackground = this.rowHoverBackground;
        this.rowHoverBackground = rowHoverBackground;
        firePropertyChange("rowHoverBackground", oldHoverBackground, rowHoverBackground);
        repaint();
    }

    public Color getRowStripeBackground() {
        return rowStripeBackground;
    }

    public void setRowStripeBackground(Color rowStripeBackground) {
        var oldStripeBackground = this.rowStripeBackground;
        this.rowStripeBackground = rowStripeBackground;
        firePropertyChange("rowStripeBackground", oldStripeBackground, rowStripeBackground);
        repaint();
    }

    public boolean isStriped() {
        return striped;
    }

    public void setStriped(boolean striped) {
        boolean oldStripped = this.striped;
        this.striped = striped;
        firePropertyChange("striped", oldStripped, striped);
        repaint();
    }

    public boolean isShowHovered() {
        return showHovered;
    }

    public void setShowHovered(boolean showHovered) {
        boolean oldHovered = this.showHovered;
        this.showHovered = showHovered;
        firePropertyChange("showHovered", oldHovered, showHovered);
        repaint();
    }

    /**
     * Workaround bug in JTable (negative y is mapped to row 0).
     * Still open 2023-01-24.
     *
     * @see <a href="https://bugs.openjdk.org/browse/JDK-6291631">JDK-6291631</a>
     */
    public int rowAtPoint(Point point) {
        if (point.y < 0) return -1;
        return super.rowAtPoint(point);
    }

    // Idea source: https://community.oracle.com/tech/developers/discussion/comment/5691659/#Comment_5691659
    @Override
    public TableCellRenderer getCellRenderer(final int row, final int column) {
        var tableColumn = getColumnModel().getColumn(column);
        var renderer = tableColumn.getCellRenderer();
        // get the default actual default renderer
        if (renderer == null) {
            var c = getColumnClass(column);
            if (c == Object.class) {
                var valueAt = getValueAt(row, column);
                if (valueAt != null) {
                    c = valueAt.getClass();
                }
            }
            renderer = getDefaultRenderer(c);
        }
        return renderer;
    }

    @Override
    public TableCellEditor getCellEditor(final int row, final int column) {
        var tableColumn = getColumnModel().getColumn(column);
        var editor = tableColumn.getCellEditor();
        // get the actual default editor
        if (editor == null) {
            var c = getColumnClass(column);
            if (c == Object.class) {
                var valueAt = getValueAt(row, column);
                if (valueAt != null) {
                    c = valueAt.getClass();
                }
            }
            editor = getDefaultEditor(c);
        }
        return editor;
    }

    /**
     * Returns the tooltip even for nested render components.
     *
     * @param event the {@code MouseEvent} that initiated the
     *              {@code ToolTip} display
     * @return the string containing the tooltip text to be displayed.
     */
    @Override
    public String getToolTipText(@NotNull MouseEvent event) {
        var point = event.getPoint();
        var row = rowAtPoint(point);
        var column = columnAtPoint(point);
        var value = getValueAt(row, column);

        if (value instanceof JComponent) {
            var cellRenderer = getCellRenderer(row, column);
            var component = prepareRenderer(cellRenderer, row, column);
            var cellRect = getCellRect(row, column, true);
            component.setBounds(cellRect);
            component.doLayout();
            point.translate(-cellRect.x, -cellRect.y);

            var deepestComponent = SwingUtilities.getDeepestComponentAt(component, point.x, point.y);
            if (deepestComponent instanceof JComponent) {
                // send a synthetic event to get the tooltip at the relative mouse location
                return ((JComponent) deepestComponent).getToolTipText(new MouseEvent(
                        deepestComponent,
                        event.getID(),
                        event.getWhen() + 100,
                        event.getModifiersEx(),
                        point.x, point.y,
                        event.getClickCount(),
                        event.isPopupTrigger(),
                        event.getButton()
                ));
            }
        }
        return super.getToolTipText(event);
    }

    public static <T extends Component> T updatePreferredRowHeight(JTable table, T cellComponent, int row, int column) {
        // Adjust cell height per component
        int originalPreferredHeight = cellComponent.getPreferredSize().height;
        cellComponent.setSize(
                table.getColumnModel().getColumn(column).getWidth(),
                originalPreferredHeight
        );
        int newPreferredHeight = cellComponent.getPreferredSize().height;
        if (table.getRowHeight(row) < newPreferredHeight) {
            table.setRowHeight(row, newPreferredHeight);
        }
        return cellComponent;
    }


    /**
     * Interactive cell editor, that assume the received value is a component.
     *
     * @param <C> The component type.
     */
    public static class InteractiveTableCellEditor<C extends JComponent> extends AbstractJComponentCellEditor<C> implements TableCellEditor {
        private final ValueEditorComponentConsumer<C> valueConsumer;

        public interface ValueEditorComponentConsumer<C> {
            void accept(
                    C component,
                    JTable table,
                    Object value,
                    boolean isSelected,
                    int row,
                    int column
            );
        }

        /**
         * Constructs an {@code InteractiveCellEditor}.
         * If the editor component is <code>null</code>, the editor component won't receive
         * {@link #updateUI} method, the editor can be set via
         * {@link #getTableCellEditorComponent(JTable, Object, boolean, int, int)}.
         * <p>
         * This implementation consider calls to {@link #getCellEditorValue()}
         * as a programming error.
         *
         * @see InteractiveTableCellEditor#InteractiveTableCellEditor(JComponent)
         */
        public InteractiveTableCellEditor() {
            this(null);
        }

        /**
         * Constructs an {@code InteractiveCellEditor} that uses a given editor component.
         *
         * @param editorComponent the component to be used as editor
         * @see InteractiveTableCellEditor#InteractiveTableCellEditor()
         */
        public InteractiveTableCellEditor(C editorComponent) {
            this(editorComponent, null);
        }

        public InteractiveTableCellEditor(C editorComponent, ValueEditorComponentConsumer<C> valueConsumer) {
            super(editorComponent);
            this.valueConsumer = valueConsumer;
            if (valueConsumer != null && editorComponent == null) {
                throw new IllegalArgumentException("The valueConsumer requires an editorComponent");
            }
        }

        @Override
        public Object getCellEditorValue() {
            throw new IllegalStateException("getCellEditorValue() should not be called");
        }

        @Override
        public boolean stopCellEditing() {
            cancelCellEditing();
            return true;
        }

        /**
         * Returns true as default implementation but activate as well the
         * component on the first click by emitting synthetic events.
         * <p>
         * One can extend this listener to provide hooks when the editor is
         * stopped/cancelled, typically in the constructor.
         * <pre><code>
         * addCellEditorListener(new CellEditorListener() {
         *     @Override
         *     public void editingStopped(ChangeEvent e) {
         *         ...
         *     }
         *     @Override
         *     public void editingCanceled(ChangeEvent e) {
         *         ...
         *     }
         * });
         * </code></pre>
         *
         * @param anEvent an event object
         * @return true
         */
        // Idea source: https://community.oracle.com/tech/developers/discussion/comment/5691659/#Comment_5691659
        @Override
        public boolean shouldSelectCell(final EventObject anEvent) {
            var locationWithin = new Point(3, 3); // arbitrary ?
            if (editorComponent != null
                    && anEvent instanceof MouseEvent
                    && ((MouseEvent) anEvent).getID() == MouseEvent.MOUSE_PRESSED
            ) {
                MouseEvent originalMouseEvent = (MouseEvent) anEvent;
                Component dispatchComponent =
                        SwingUtilities.getDeepestComponentAt(editorComponent, locationWithin.x, locationWithin.y);

                // dispatch a synthetic mouse release event
                dispatchComponent.dispatchEvent(new MouseEvent(
                        dispatchComponent,
                        MouseEvent.MOUSE_RELEASED,
                        originalMouseEvent.getWhen() + 100000,
                        originalMouseEvent.getModifiersEx(),
                        locationWithin.x, locationWithin.y,
                        originalMouseEvent.getClickCount(),
                        originalMouseEvent.isPopupTrigger()
                ));

                // dispatch a synthetic mouse click event
                dispatchComponent.dispatchEvent(new MouseEvent(
                        dispatchComponent,
                        MouseEvent.MOUSE_CLICKED,
                        originalMouseEvent.getWhen() + 100001,
                        originalMouseEvent.getModifiersEx(),
                        locationWithin.x, locationWithin.y,
                        1,
                        originalMouseEvent.isPopupTrigger()
                ));
            }
            return true;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Component getTableCellEditorComponent(
                final JTable table,
                final Object value,
                final boolean isSelected,
                final int row,
                final int column
        ) {
            if (value instanceof JComponent) {
                editorComponent = (C) value;
                return editorComponent;
            }
            if (valueConsumer != null) {
                valueConsumer.accept(editorComponent, table, value, isSelected, row, column);
            }
            return editorComponent;
        }
    }

    static class InteractiveTableCellRenderer<C extends JComponent> extends AbstractJComponentTableCellRenderer<C> implements TableCellRenderer {
        private final ValueRendererComponentConsumer<C> valueConsumer;

        public interface ValueRendererComponentConsumer<C> {
            void accept(
                    C component,
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            );
        }


        /**
         * Constructs an {@code InteractiveTableCellRenderer} that uses a given renderer component.
         * If the editor component is not set, the editor component won't receive
         * {@link #updateUI} method, the editor can be set via
         * {@link #getTableCellRendererComponent(JTable, Object, boolean, boolean, int, int)}.
         *
         * @see InteractiveTableCellRenderer#InteractiveTableCellRenderer(JComponent)
         */
        public InteractiveTableCellRenderer() {
            this(null);
        }

        /**
         * Constructs an {@code InteractiveTableCellRenderer} that uses a given renderer component.
         *
         * @param renderComponent the component to be used as renderer, <code>null</code> accepted.
         * @see InteractiveTableCellRenderer#InteractiveTableCellRenderer()
         */
        public InteractiveTableCellRenderer(C renderComponent) {
            this(renderComponent, null);
        }

        /**
         * Constructs an {@code InteractiveTableCellRenderer} that uses a given renderer component.
         *
         * @param renderComponent the component to be used as renderer, <code>null</code> accepted.
         * @see InteractiveTableCellRenderer#InteractiveTableCellRenderer()
         */
        public InteractiveTableCellRenderer(C renderComponent, ValueRendererComponentConsumer<C> valueConsumer) {
            super(renderComponent);
            this.valueConsumer = valueConsumer;
            if (valueConsumer != null && renderComponent == null) {
                throw new IllegalArgumentException("valueConsumer requires to have renderComponent");
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public Component getTableCellRendererComponent(
                final JTable table,
                final Object value,
                final boolean isSelected,
                final boolean hasFocus,
                final int row,
                final int column
        ) {
            if (value instanceof JComponent) {
                return (C) value;
            }
            if (valueConsumer != null) {
                valueConsumer.accept(renderComponent, table, value, isSelected, hasFocus, row, column);
            }
            return renderComponent;
        }
    }

    /**
     * Listen to mouse position to set the hovered row.
     *
     * @see InteractiveJTable#prepareEditor(TableCellEditor, int, int)
     * @see InteractiveJTable#prepareRenderer(TableCellRenderer, int, int)
     */
    private static class RowHoveringListener implements ProperMouseEnterExitListener.SimpleMouseListener {
        private final JTable table;

        public RowHoveringListener(InteractiveJTable interactiveTable) {
            this.table = interactiveTable;
        }

        @Override
        public void mouseMoveWithin(MouseEvent e) {
            performHovering(() -> table.rowAtPoint(e.getPoint()));
        }

        @Override
        public void mouseOut(MouseEvent e) {
            performHovering(() -> -1);
        }

        private void performHovering(IntSupplier rowSupplier) {
            int oldHoveredRow = (int) Objects.requireNonNullElse(table.getClientProperty("hoveredRow"), -1);
            int rowToHover = rowSupplier.getAsInt();
            table.putClientProperty("hoveredRow", rowToHover);
            if (oldHoveredRow != rowToHover) {
                // request repaint of the row
                requestRowRepaint(oldHoveredRow);
                requestRowRepaint(rowToHover);
            }
        }

        private void requestRowRepaint(int row) {
            if (row >= 0) {
                var cellRect = table.getCellRect(row, 0, false);
                int rowHeight = table.getRowHeight(row);
                table.repaint(0, cellRect.y, table.getWidth(), rowHeight);
            }
        }
    }

    /**
     * This allows to enter editing mode for this cell, thus making the cell interactive.
     */
    private static class AutoEditCellListener implements ProperMouseEnterExitListener.SimpleMouseListener {
        private final InteractiveJTable table;

        public AutoEditCellListener(InteractiveJTable interactiveTable) {
            this.table = interactiveTable;
        }

        @Override
        public void mouseOut(MouseEvent e) {
            if (table.isEditing()) {
                table.getCellEditor().cancelCellEditing();
            }
        }

        @Override
        public void mouseMoveWithin(MouseEvent e) {
            // Actually rowAtPoint or columnAtPoint will never return -1,
            // when handling mouse moved
            var point = e.getPoint();
            int r = table.rowAtPoint(point);
            int c = table.columnAtPoint(point);

            var currentCellEditor = table.getCellEditor();
            if (table.isCellEditable(r, c)
                    && (table.getEditingRow() != r || table.getEditingColumn() != c) // avoid flickering, when the mouse move over the same cell
            ) {
                // Cancel previous, otherwise editCellAt will invoke stopCellEditing which
                // actually get the current value from the editor and set it to the model (see editingStopped)
                if (table.isEditing() && r >= 0 && c >= 0) {
                    currentCellEditor.cancelCellEditing();
                }

                table.editCellAt(r, c);
                var newCellEditorComponent = table.getEditorComponent();
                var newCellEditor = table.getCellEditor();
                if (newCellEditor != null && newCellEditorComponent != null) {
                    newCellEditorComponent.addMouseListener(new MouseInputAdapter() {
                        @Override
                        public void mouseExited(MouseEvent e) {
                            newCellEditor.cancelCellEditing();
                            // Remove ourselves from the component, otherwise listeners will pile up
                            newCellEditorComponent.removeMouseListener(this);
                        }
                    });
                }
            } else {
                if (table.isEditing() && r < 0 && c < 0) {
                    currentCellEditor.cancelCellEditing();
                }
            }
        }
    }

    /**
     * Mouse exit are not properly tracked by the
     * regular mouse listeners, so we need to capture events higher
     * in the dispatcher via AWTEventListener.
     * In order to simplify integration, this utility also <em>merge</em>
     * mouse enter and mouse move events.
     *
     * @see SimpleMouseListener
     */
    private static class ProperMouseEnterExitListener {
        private final List<SimpleMouseListener> listeners = new ArrayList<>();

        public ProperMouseEnterExitListener(InteractiveJTable interactiveTable) {
            var dumbListenerForMouseEventSubscription = new MouseInputAdapter() {
            };
            interactiveTable.addMouseListener(dumbListenerForMouseEventSubscription);
            interactiveTable.addMouseMotionListener(dumbListenerForMouseEventSubscription);

            Toolkit.getDefaultToolkit().addAWTEventListener(awtEvent -> {
                var source = awtEvent.getSource();
                if (source instanceof JComponent) {
                    var comp = (JComponent) source;
                    // The actual received event may come from a different component than the table
                    var tableMouseEvent = SwingUtilities.convertMouseEvent(
                            comp,
                            (MouseEvent) awtEvent,
                            interactiveTable
                    );
                    if (interactiveTable.contains(tableMouseEvent.getPoint())) {
                        // The mouse is in the house...
                        listeners.forEach(l -> l.mouseMoveWithin(tableMouseEvent));
                    } else {
                        // Mouse is outside
                        listeners.forEach(l -> l.mouseOut(tableMouseEvent));
                    }
                }
            }, AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);
        }

        public static ProperMouseEnterExitListener install(InteractiveJTable interactiveTable) {
            return new ProperMouseEnterExitListener(interactiveTable);
        }

        public ProperMouseEnterExitListener register(SimpleMouseListener... listeners) {
            this.listeners.addAll(Arrays.asList(listeners));
            return this;
        }

        public interface SimpleMouseListener {
            void mouseMoveWithin(MouseEvent e);

            void mouseOut(MouseEvent e);
        }
    }
}
