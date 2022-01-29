/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.github.bric3.fireplace;

import com.github.bric3.fireplace.flamegraph.FlameGraph;
import com.github.bric3.fireplace.flamegraph.FrameNodeConverter;
import com.github.bric3.fireplace.ui.Colors.Palette;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Supplier;

import static java.util.stream.Collectors.joining;

public class FlameGraphTab extends JPanel {
    private final Supplier<StacktraceTreeModel> stacktraceTreeModelSupplier;
    private FlameGraph<Node> jfrFlameGraph;

    public FlameGraphTab(Supplier<StacktraceTreeModel> stacktraceTreeModelSupplier) {
        super(new BorderLayout());
        this.stacktraceTreeModelSupplier = Utils.memoize(stacktraceTreeModelSupplier);

        jfrFlameGraph = createFlameGraph();
        var wrapper = new JPanel(new BorderLayout());
        wrapper.add(jfrFlameGraph.component);

        var timer = new Timer(2_000, e -> {
            createFlameGraph();
            wrapper.removeAll();
            wrapper.add(jfrFlameGraph.component);
            wrapper.repaint(1_000);
            wrapper.revalidate();
        });
        timer.setInitialDelay(0);
        timer.setRepeats(true);

        var refreshToggle = new JToggleButton("Refresh");
        refreshToggle.addActionListener(e -> {
            if (timer.isRunning()) {
                timer.stop();
            } else {
                timer.start();
            }
        });

        var colorPaletteJComboBox = new JComboBox<>(Palette.values());
        colorPaletteJComboBox.addActionListener(e -> {
            jfrFlameGraph.flameGraphPainter.packageColorPalette = (Palette) colorPaletteJComboBox.getSelectedItem();
            wrapper.repaint();
        });
        colorPaletteJComboBox.setSelectedItem(jfrFlameGraph.flameGraphPainter.packageColorPalette);

        var colorModeJComboBox = new JComboBox<>(JfrFrameColorMode.values());
        colorModeJComboBox.addActionListener(e -> {
            jfrFlameGraph.flameGraphPainter.frameColorMode = (JfrFrameColorMode) colorModeJComboBox.getSelectedItem();
            wrapper.repaint();
        });
        colorModeJComboBox.setSelectedItem(jfrFlameGraph.flameGraphPainter.frameColorMode);

        var borderToggle = new JCheckBox("Border");
        borderToggle.addActionListener(e -> {
            jfrFlameGraph.flameGraphPainter.paintFrameBorder = borderToggle.isSelected();
            wrapper.repaint();
        });
        borderToggle.setSelected(jfrFlameGraph.flameGraphPainter.paintFrameBorder);

        var controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(colorPaletteJComboBox);
        controlPanel.add(colorModeJComboBox);
        controlPanel.add(borderToggle);
        controlPanel.add(refreshToggle);


        add(controlPanel, BorderLayout.NORTH);
        add(wrapper, BorderLayout.CENTER);
    }

    private FlameGraph<Node> createFlameGraph() {
        jfrFlameGraph = new FlameGraph<>(
                FrameNodeConverter.convert(this.stacktraceTreeModelSupplier.get()),
                List.of(
                        node -> node.getFrame().getHumanReadableShortString(),
                        node -> node.getFrame().getMethod().getMethodName()
                ),
                node -> {
                    var events = this.stacktraceTreeModelSupplier.get().getItems()
                                                                 .stream()
                                                                 .map(iItems -> iItems.getType().getIdentifier())
                                                                 .collect(joining(", "));
                    var str = "all (" + events + ")";
                    return str;
                },
                node -> jfrFlameGraph.flameGraphPainter.frameColorMode.getColor(
                        jfrFlameGraph.flameGraphPainter.packageColorPalette,
                        node
                ),
                frame -> {
                    if (frame.stackDepth == 0) {
                        return "";
                    }

                    var method = frame.jfrNode.getFrame().getMethod();
                    var desc = FormatToolkit.getHumanReadable(method, false, false, true, true, true, false, false);

                    return "<html>"
                           + "<b>" + frame.jfrNode.getFrame().getHumanReadableShortString() + "</b><br>"
                           + desc + "<br><hr>"
                           + frame.jfrNode.getCumulativeWeight() + " " + frame.jfrNode.getWeight() + "<br>"
                           + "BCI: " + frame.jfrNode.getFrame().getBCI() + " Line number: " + frame.jfrNode.getFrame().getFrameLineNumber() + "<br>"
                           + "</html>";
                },
                JfrFrameColorMode.BY_PACKAGE
        );
        return jfrFlameGraph;
    }

}
