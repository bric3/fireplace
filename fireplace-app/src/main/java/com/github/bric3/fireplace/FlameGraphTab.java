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

import com.github.bric3.fireplace.core.ui.Colors;
import com.github.bric3.fireplace.core.ui.Colors.Palette;
import com.github.bric3.fireplace.flamegraph.ColorMapper;
import com.github.bric3.fireplace.flamegraph.FlameGraph;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;

import static java.util.stream.Collectors.joining;

public class FlameGraphTab extends JPanel {
    private static final Palette defaultColorPalette = Palette.DATADOG;
    private static final JfrFrameColorMode defaultFrameColorMode = JfrFrameColorMode.BY_PACKAGE;
    private static final boolean defaultPaintFrameBorder = true;
    private FlameGraph<Node> jfrFlameGraph;

    public FlameGraphTab() {
        super(new BorderLayout());

        jfrFlameGraph = new FlameGraph<>();
        // jfrFlameGraph.setTooltipComponentSupplier(BalloonToolTip::new);
        jfrFlameGraph.setMinimapShadeColorSupplier(() -> Colors.isDarkMode() ? Colors.translucent_black_40 : Colors.translucent_white_80);
        var wrapper = new JPanel(new BorderLayout());
        wrapper.add(jfrFlameGraph.component);

        var timer = new Timer(2_000, e -> {
            jfrFlameGraph = new FlameGraph<>();
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
        colorPaletteJComboBox.setSelectedItem(defaultColorPalette);
        var colorModeJComboBox = new JComboBox<>(JfrFrameColorMode.values());
        colorModeJComboBox.setSelectedItem(defaultFrameColorMode);

        ActionListener actionListener = e -> {
            jfrFlameGraph.setColorFunction(
                    ((JfrFrameColorMode) colorModeJComboBox.getSelectedItem())
                            .colorMapperUsing(ColorMapper.ofObjectHashUsing(
                                    ((Palette) colorPaletteJComboBox.getSelectedItem()).colors())));
            jfrFlameGraph.requestRepaint();
        };
        colorPaletteJComboBox.addActionListener(actionListener);
        colorModeJComboBox.addActionListener(actionListener);

        var borderToggle = new JCheckBox("Border");
        borderToggle.addActionListener(e -> {
            jfrFlameGraph.setFrameGapEnabled(borderToggle.isSelected());
            jfrFlameGraph.requestRepaint();
        });
        borderToggle.setSelected(defaultPaintFrameBorder);

        var controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(colorPaletteJComboBox);
        controlPanel.add(colorModeJComboBox);
        controlPanel.add(borderToggle);
        controlPanel.add(refreshToggle);


        add(controlPanel, BorderLayout.NORTH);
        add(wrapper, BorderLayout.CENTER);
    }

    public FlameGraphTab(StacktraceTreeModel stacktraceTreeModel) {
        this();
        setStacktraceTreeModel(stacktraceTreeModel);
    }

    public void setStacktraceTreeModel(StacktraceTreeModel stackTraceTreeModel) {
        jfrFlameGraph.setData(
                JfrFrameNodeConverter.convert(stackTraceTreeModel),
                List.of(
                        node -> node.getFrame().getHumanReadableShortString(),
                        node -> node.getFrame().getMethod().getMethodName()
                ),
                node -> {
                    var events = stackTraceTreeModel.getItems()
                                                    .stream()
                                                    .map(iItems -> iItems.getType().getIdentifier())
                                                    .collect(joining(", "));
                    return "all (" + events + ")";
                },
                defaultFrameColorMode.colorMapperUsing(ColorMapper.ofObjectHashUsing(defaultColorPalette.colors())),
                frame -> {
                    if (frame.stackDepth == 0) {
                        return "";
                    }

                    var method = frame.actualNode.getFrame().getMethod();
                    var desc = FormatToolkit.getHumanReadable(method,
                                                              false,
                                                              false,
                                                              true,
                                                              true,
                                                              true,
                                                              false,
                                                              false);

                    return "<html>"
                           + "<b>" + frame.actualNode.getFrame().getHumanReadableShortString() + "</b><br>"
                           + desc + "<br><hr>"
                           + frame.actualNode.getCumulativeWeight() + " " + frame.actualNode.getWeight() + "<br>"
                           + "BCI: " + frame.actualNode.getFrame().getBCI() + " Line number: " + frame.actualNode.getFrame().getFrameLineNumber() + "<br>"
                           + "</html>";
                }
        );
    }
}
