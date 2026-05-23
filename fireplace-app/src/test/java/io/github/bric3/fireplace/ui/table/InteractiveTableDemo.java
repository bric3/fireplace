package io.github.bric3.fireplace.ui.table;

import com.formdev.flatlaf.FlatLightLaf;
import io.github.bric3.fireplace.core.ui.Colors;
import io.github.bric3.fireplace.ui.table.InteractiveJTable.InteractiveTableCellEditor;
import io.github.bric3.fireplace.ui.table.InteractiveJTable.InteractiveTableCellRenderer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class InteractiveTableDemo extends JFrame {
    public static void main(String[] args) {
        // Needs --add-opens java.desktop/javax.swing=ALL-UNNAMED
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            Colors.setDarkMode(false);
            //UIManager.put("InteractiveJTable.rowHoverBackground", new LightDarkColor(0xE5EBF3, 0x4E5357));
            //UIManager.put("InteractiveJTable.rowStripeBackground", new LightDarkColor(0xE6E9EE, 0x34383A));
            var interactiveTableLayout = new DefaultInteractiveTableModel() {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return column > 0;
                }
            };
            {
                interactiveTableLayout.addColumns("Chart Name", "Chart Panel");
                // create data and put into table, first column is a string,
                // the second is derived from a JPanel.

                interactiveTableLayout.addRow("Chart 1", new int[] { 20, 100});
                interactiveTableLayout.addRow("Chart 2", new int[] { 40, 150});
                // TODO fix JTextArea
                // interactiveTableLayout.addRow(
                //         "Text",
                //         "Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
                //         "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " +
                //         "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris " +
                //         "nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in " +
                //         "reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla " +
                //         "pariatur. Excepteur sint occaecat cupidatat non proident, sunt in " +
                //         "culpa qui officia deserunt mollit anim id est laborum."
                // );
                 interactiveTableLayout.addRow("Expandable 1", new boolean[] { true, false, true });
                 interactiveTableLayout.addRow("Expandable 2", new boolean[] { false, true, true });
            }

            var myTable = new InteractiveJTable(interactiveTableLayout);
            {
                myTable.setStriped(true);
                myTable.setShowHovered(true);
                myTable.setShowVerticalLines(true);

                // specify the custom renderer for the second (JPanel) column
                JTextArea jTextArea = new JTextArea();
                jTextArea.setLineWrap(true);
                jTextArea.setEditable(false);
                jTextArea.setWrapStyleWord(true);
                jTextArea.setForeground(Color.BLACK);
                myTable.setDefaultRenderer(
                        String.class,
                        new InteractiveTableCellRenderer<>(jTextArea, (component, table, v, isSelected, hasFocus, r, c) -> {
                            component.setText(v.toString());
                        })
                );
                myTable.setDefaultEditor(
                        String.class,
                        new InteractiveTableCellEditor<>(jTextArea, (component, table, v, isSelected, r, c) -> {
                            component.setText(v.toString());
                        })
                );

                myTable.setDefaultRenderer(
                        int[].class,
                        new InteractiveTableCellRenderer<>(new ChartPanel(), (component, table, v, isSelected, hasFocus, r, c) -> {
                            component.setValue((int[]) v);
                        })
                );
                myTable.setDefaultEditor(
                        int[].class,
                        new InteractiveTableCellEditor<>(new ChartPanel(), (component, table, v, isSelected, r, c) -> {
                            component.setValue((int[]) v);
                        })
                );

                myTable.setDefaultRenderer(
                        boolean[].class,
                        new InteractiveTableCellRenderer<>(new ExpandablePanel(), (component, table, v, isSelected, hasFocus, r, c) -> {
                            component.setValue((boolean[]) v);
                            // if component state is to be kept, the state has to be backed in the model
                            component.setExpanded(false);
                        })
                );
                myTable.setDefaultEditor(
                        boolean[].class,
                        new InteractiveTableCellEditor<>(new ExpandablePanel(), (component, table, v, isSelected, r, c) -> {
                            component.setValue((boolean[]) v);
                            // if component state is to be kept, the state has to be backed in the model
                            component.setExpanded(false);
                        })
                );
            }

            var contentPane = new JPanel(new BorderLayout());
            contentPane.add(new JScrollPane(myTable));

            var frame = new JFrame();
            {
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                frame.setTitle("Test of Interactive JPanel in a JTable");
                frame.setBackground(Color.white);
                frame.setContentPane(contentPane);
                frame.setSize(500, 500);
                frame.setVisible(true);
            }
        });
    }

    static class ChartPanel extends JPanel {
        private final JLabel blockIcon;

        ChartPanel() {
            // draw a single colored block in the panel
            setLayout(null);
            // setPreferredSize(new Dimension(getPreferredSize().width, preferredHeight));
            Random rnd = new Random();

            var color = new AtomicReference<>(new Color(rnd.nextInt()));
            var color2 = new AtomicReference<>(new Color(rnd.nextInt()));
            blockIcon = new JLabel(" ") {
                @Override
                protected void paintComponent(Graphics g) {
                    g.setColor(color.get());
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.setColor(color2.get());
                    g.fillRoundRect(5, 5, getWidth() - 10, getHeight() - 10, 3, 3);
                    super.paintComponent(g);
                }
            };
            blockIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    color.set(new Color(rnd.nextInt()));
                    blockIcon.repaint();
                }
            });
            blockIcon.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    color2.set(new Color(rnd.nextInt()));
                    blockIcon.repaint();
                }
            });
            add(blockIcon);
        }

        public void setValue(int[] v) {
            setPreferredSize(new Dimension(getPreferredSize().width, v[0]));
            int start = v[0];
            int end = v[1];
            blockIcon.setSize(end - start, 20);
            blockIcon.setLocation(start, 0);
            blockIcon.setToolTipText("tip1: " + start + " -> " + end);
        }
    }

    static class ExpandablePanel extends JPanel {

        private final JCheckBox live = new JCheckBox("Live");
        private final JCheckBox condition = new JCheckBox("Condition");
        private final JCheckBox disable = new JCheckBox("Disable");
        private final Consumer<Boolean> expandOrCollapse;

        ExpandablePanel() {
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

            JPanel advanced = new JPanel();
            {
                advanced.setLayout(new BoxLayout(advanced, BoxLayout.Y_AXIS));
                advanced.setBorder(new TitledBorder("Advanced Settings"));
                advanced.add(live);
                advanced.add(condition);
                advanced.add(disable);
            }
            advanced.setVisible(false);


            var standard = new JPanel();
            {
                standard.setLayout(new BoxLayout(standard, BoxLayout.X_AXIS));
                standard.add(new JLabel("Label 1"));
                standard.add(new JButton("Button 1"));
                var expandButton = new JButton("+");
                expandOrCollapse = toggle -> {
                    advanced.setVisible(toggle);
                    expandButton.setText(!toggle ? "+" : "-");
                };
                expandButton.addActionListener(e -> {
                    boolean visible = advanced.isVisible();
                    expandOrCollapse.accept(!advanced.isVisible());
                });
                standard.add(expandButton);
            }
            add(standard, gbc);

            gbc.gridy++;
            gbc.weighty = 0;
            add(advanced, gbc);
        }

        public void setValue(boolean[] v) {
            live.setSelected(v[0]);
            condition.setSelected(v[1]);
            disable.setSelected(v[2]);
        }

        public void setExpanded(boolean expanded) {
            expandOrCollapse.accept(expanded);
        }
    }
}