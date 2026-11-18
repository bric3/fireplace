package io.github.bric3.fireplace.ui.table;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public class InteractiveTableLayoutDemo extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            var interactiveTableLayout = new InteractiveTableLayout();
            {
                interactiveTableLayout.addColumns("Item", "Component");
                // create data and put into table, first column is a string,
                // the second is derived from a JPanel.
                interactiveTableLayout.addRow(new JLabel("Chart 1"), new ChartPanel("block1", 20, 100, 20));
                interactiveTableLayout.addRow(new JLabel("Chart 2"), new ChartPanel("block2", 40, 150, 40));
                var jTextArea = new JTextArea(
                        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, " +
                        "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " +
                        "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris " +
                        "nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in " +
                        "reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla " +
                        "pariatur. Excepteur sint occaecat cupidatat non proident, sunt in " +
                        "culpa qui officia deserunt mollit anim id est laborum."
                );
                jTextArea.setLineWrap(true);
                jTextArea.setEditable(false);
                interactiveTableLayout.addRow(new JLabel("Text"), jTextArea);
                interactiveTableLayout.addRow(new JLabel("Expandable"), new ExpandablePanel());
            }

            var myTable = new InteractiveJTable(interactiveTableLayout);
            {
                myTable.setStriped(true);
                myTable.setShowHovered(true);
                myTable.setShowVerticalLines(false);

                // specify the custom renderer for the second (JPanel) column
                myTable.getColumnModel().getColumn(1).setCellRenderer(new CustomTableCellRenderer());
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

    public static class CustomTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            var c = (JComponent) value;
            c.setBorder(BorderFactory.createLineBorder(Color.magenta, 2));
            return c;
        }
    }

    static class ChartPanel extends JPanel {
        ChartPanel(String blockName, int start, int end, int preferredHeight) {
            // draw a single colored block in the panel
            setLayout(null);
            setPreferredSize(new Dimension(getPreferredSize().width, preferredHeight));
            Random rnd = new Random();

            var color = new AtomicReference<>(new Color(rnd.nextInt()));
            var color2 = new AtomicReference<>(new Color(rnd.nextInt()));
            final JLabel blockIcon = new JLabel(blockName) {
                @Override
                protected void paintComponent(Graphics g) {
                    g.setColor(color.get());
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.setColor(color2.get());
                    g.fillRoundRect(5, 5, getWidth() - 10, getHeight() - 10, 3, 3);
                    super.paintComponent(g);
                }
            };
            {
                blockIcon.setSize(end - start, 20);
                blockIcon.setLocation(start, 0);
                blockIcon.setToolTipText("tip1: " + blockName);
            }
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
    }

    static class ExpandablePanel extends JPanel {
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

            JPanel advanced = new JPanel();
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
                standard.add(new JLabel("Label 1"));
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
    }
}