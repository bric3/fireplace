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
import com.github.bric3.fireplace.flamegraph.FrameBox;
import com.github.bric3.fireplace.flamegraph.FrameColorMode;
import com.github.bric3.fireplace.ui.Colors.Palette;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class FlameGraphTab extends JPanel {
    private final Supplier<StacktraceTreeModel> stacktraceTreeModelSupplier;
    private FlameGraph flameGraph;
    private static final Function<FrameBox<Node>, String> extractToolTip = frame -> {
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
    };

    public FlameGraphTab(Supplier<StacktraceTreeModel> stacktraceTreeModelSupplier) {
        super(new BorderLayout());
        this.stacktraceTreeModelSupplier = Utils.memoize(stacktraceTreeModelSupplier);

        flameGraph = createFlameGraph();
        var wrapper = new JPanel(new BorderLayout());
        wrapper.add(flameGraph.component);

        var timer = new Timer(2_000, e -> {
            createFlameGraph();
            wrapper.removeAll();
            wrapper.add(flameGraph.component);
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
            flameGraph.flameGraphPainter.packageColorPalette = (Palette) colorPaletteJComboBox.getSelectedItem();
            wrapper.repaint();
        });
        colorPaletteJComboBox.setSelectedItem(flameGraph.flameGraphPainter.packageColorPalette);

        var colorModeJComboBox = new JComboBox<>(FrameColorMode.values());
        colorModeJComboBox.addActionListener(e -> {
            flameGraph.flameGraphPainter.frameColorMode = (FrameColorMode) colorModeJComboBox.getSelectedItem();
            wrapper.repaint();
        });
        colorModeJComboBox.setSelectedItem(flameGraph.flameGraphPainter.frameColorMode);

        var borderToggle = new JCheckBox("Border");
        borderToggle.addActionListener(e -> {
            flameGraph.flameGraphPainter.paintFrameBorder = borderToggle.isSelected();
            wrapper.repaint();
        });
        borderToggle.setSelected(flameGraph.flameGraphPainter.paintFrameBorder);

        var controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(colorPaletteJComboBox);
        controlPanel.add(colorModeJComboBox);
        controlPanel.add(borderToggle);
        controlPanel.add(refreshToggle);


        add(controlPanel, BorderLayout.NORTH);
        add(wrapper, BorderLayout.CENTER);
    }

    private FlameGraph createFlameGraph() {
        flameGraph = new FlameGraph(this.stacktraceTreeModelSupplier, extractToolTip);
        return flameGraph;
    }

}
