package io.github.bric3.fireplace.ui.table;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class DynamicCellRow {
    public static void main(String[] args) {

        var contentPane = new JPanel(new BorderLayout());
        contentPane.add(new JScrollPane(makeTable()));

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("DynamicCellRow");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(contentPane);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    @NotNull
    private static JTable makeTable() {
        var jTable = new JTable(
                new Object[][]{
                        {"a", "charly"},
                        {"b", "tango"}
                },
                new Object[]{"id", "control"}
        ) {
            @Override
            public void doLayout() {
                super.doLayout();
                adjustRowHeights();
            }

            private void adjustRowHeights() {
                for (int row = 0; row < getRowCount(); row++) {
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

                    setRowHeight(row, rowHeight);
                }
            }
        };
        {
            var richColumn = jTable.getColumnModel().getColumn(1);
            richColumn.setCellRenderer(new ExpandablePanelCellRenderer());
            richColumn.setCellEditor(new DynamicExpndablePanelCellEditor());
        }
        return jTable;
    }


    static class ExpandablePanel extends JPanel {

        private final JLabel comp;
        private final JPanel advanced;

        ExpandablePanel() {
            setBorder(BorderFactory.createLineBorder(Color.black));
            setLayout(new GridBagLayout());

            GridBagConstraints gbc = new GridBagConstraints();
            // horizontal left-to-right layout
            gbc.gridx = 0;
            gbc.gridy = 0;

            // resizing behavior
            gbc.weightx = 1;
            gbc.weighty = 1;

            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.fill = GridBagConstraints.BOTH;

            advanced = new JPanel();
            {
                advanced.setLayout(new BoxLayout(advanced, BoxLayout.Y_AXIS));
                advanced.setBorder(new TitledBorder("Advance Settings"));
                advanced.add(new JCheckBox("Live"));
                advanced.add(new JCheckBox("Condition"));
                advanced.add(new JCheckBox("Disable"));
            }
            advanced.setVisible(false);

            var standard = new JPanel();
            {
                standard.setLayout(new BoxLayout(standard, BoxLayout.X_AXIS));
                comp = new JLabel("Label 1");
                standard.add(comp);
                standard.add(new JButton("Button 1"));
                var expandButton = new JButton("+");
                expandButton.addActionListener(e -> {
                    if (advanced.isVisible()) {
                        advanced.setVisible(false);
                        expandButton.setText("+");
                    } else {
                        advanced.setVisible(true);
                        expandButton.setText("-");
                    }
                });
                standard.add(expandButton);
            }
            add(standard, gbc);


            gbc.gridy++;
            gbc.weighty = 0;
            add(advanced, gbc);
        }

        public void setValue(Object value) {
            comp.setText(value.toString());
        }

        public void setAdvancedVisibility(boolean visible) {
            advanced.setVisible(visible);
        }
    }

    private static class DynamicExpndablePanelCellEditor extends AbstractCellEditor implements TableCellEditor {
        Object value;
        @Override
        public Object getCellEditorValue() {
            return value; // not changing
        }

        private final ExpandablePanel expandablePanel = new ExpandablePanel();
        {
            addCellEditorListener(new CellEditorListener() {
                @Override
                public void editingStopped(ChangeEvent e) {
                    expandablePanel.setAdvancedVisibility(false);
                }

                @Override
                public void editingCanceled(ChangeEvent e) {
                    expandablePanel.setAdvancedVisibility(false);
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.value = value;
            expandablePanel.setValue(value);

            return expandablePanel;
        }
    }

    private static class ExpandablePanelCellRenderer implements TableCellRenderer {
        private final ExpandablePanel expandablePanelRenderComponent = new ExpandablePanel();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            expandablePanelRenderComponent.setValue(value);
            return expandablePanelRenderComponent;
        }
    }
}