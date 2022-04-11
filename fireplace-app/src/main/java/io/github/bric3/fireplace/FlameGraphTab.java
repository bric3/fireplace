/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace;

import io.github.bric3.fireplace.core.ui.Colors;
import io.github.bric3.fireplace.core.ui.Colors.Palette;
import io.github.bric3.fireplace.flamegraph.ColorMapper;
import io.github.bric3.fireplace.flamegraph.FlameGraph;
import io.github.bric3.fireplace.flamegraph.NodeDisplayStringProvider;
import io.github.bric3.fireplace.flamegraph.ZoomAnimation;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.joining;

public class FlameGraphTab extends JPanel {
    private static final Palette defaultColorPalette = Palette.DATADOG;
    private static final JfrFrameColorMode defaultFrameColorMode = JfrFrameColorMode.BY_PACKAGE;
    private static final boolean defaultPaintFrameBorder = true;
    private FlameGraph<Node> jfrFlameGraph;
    private Consumer<FlameGraph<Node>> dataApplier;

    public FlameGraphTab() {
        super(new BorderLayout());

        jfrFlameGraph = new FlameGraph<>();
        jfrFlameGraph.configureCanvas(ToolTipManager.sharedInstance()::registerComponent);
        jfrFlameGraph.putClientProperty(FlameGraph.SHOW_STATS, true);
        // jfrFlameGraph.setTooltipComponentSupplier(BalloonToolTip::new);
        jfrFlameGraph.setMinimapShadeColorSupplier(() -> Colors.isDarkMode() ? Colors.translucent_black_40 : Colors.translucent_white_80);
        var zoomAnimation = new ZoomAnimation();
        zoomAnimation.install(jfrFlameGraph);

        var colorPaletteJComboBox = new JComboBox<>(Palette.values());
        colorPaletteJComboBox.setSelectedItem(defaultColorPalette);
        var colorModeJComboBox = new JComboBox<>(JfrFrameColorMode.values());
        colorModeJComboBox.setSelectedItem(defaultFrameColorMode);

        ActionListener updateColorSettingsListener = e -> {
            jfrFlameGraph.setColorFunction(
                    ((JfrFrameColorMode) colorModeJComboBox.getSelectedItem())
                            .colorMapperUsing(ColorMapper.ofObjectHashUsing(
                                    ((Palette) colorPaletteJComboBox.getSelectedItem()).colors())));
            jfrFlameGraph.requestRepaint();
        };
        colorPaletteJComboBox.addActionListener(updateColorSettingsListener);
        colorModeJComboBox.addActionListener(updateColorSettingsListener);

        var borderToggle = new JCheckBox("Border");
        borderToggle.addActionListener(e -> {
            jfrFlameGraph.setFrameGapEnabled(borderToggle.isSelected());
            jfrFlameGraph.requestRepaint();
        });
        borderToggle.setSelected(defaultPaintFrameBorder);

        var animateToggle = new JCheckBox("Animate");
        animateToggle.addActionListener(e -> {
            zoomAnimation.setAnimateZoomTransitions(animateToggle.isSelected());
        });
        animateToggle.setSelected(true);


        var wrapper = new JPanel(new BorderLayout());
        {
            var component = jfrFlameGraph.component;
            component.setBorder(null);
            wrapper.add(component);
        }

        var timer = new Timer(2_000, e -> {
            jfrFlameGraph = new FlameGraph<>();
            jfrFlameGraph.configureCanvas(ToolTipManager.sharedInstance()::registerComponent);
            jfrFlameGraph.putClientProperty(FlameGraph.SHOW_STATS, true);
            jfrFlameGraph.setMinimapShadeColorSupplier(() -> Colors.isDarkMode() ? Colors.translucent_black_40 : Colors.translucent_white_80);
            zoomAnimation.install(jfrFlameGraph);
            if (dataApplier != null) {
                dataApplier.accept(jfrFlameGraph);
            }
            updateColorSettingsListener.actionPerformed(null);

            wrapper.removeAll();
            {
                var cmp = jfrFlameGraph.component;
                cmp.setBorder(null);
                wrapper.add(cmp);
            }
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

        var resetZoom = new JButton("1:1");
        resetZoom.addActionListener(e -> {
            jfrFlameGraph.resetZoom();
        });

        var searchField = new JTextField("");
        searchField.addActionListener(e -> {
            var searched = searchField.getText();
            if (searched.isEmpty()) {
                jfrFlameGraph.highlightFrames(emptySet(), searched);
                return;
            }
            CompletableFuture.runAsync(() -> {
                try {
                    var matches = jfrFlameGraph.getFrames()
                                               .stream()
                                               .filter(frame -> {
                                                   var method = frame.actualNode.getFrame().getMethod();
                                                   return method.getMethodName().contains(searched)
                                                          || method.getType().getTypeName().contains(searched)
                                                          || (method.getType().getPackage().getName() != null && method.getType().getPackage().getName().contains(searched))
                                                          || (method.getType().getPackage().getModule() != null && method.getType().getPackage().getModule().getName().contains(searched))
                                                          || method.getFormalDescriptor().replace('/', '.').contains(searched)
                                                           ;
                                               })
                                               .collect(Collectors.toCollection(() -> Collections.newSetFromMap(new IdentityHashMap<>())));
                    jfrFlameGraph.highlightFrames(matches, searched);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        });


        var controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));
        controlPanel.add(colorPaletteJComboBox);
        controlPanel.add(colorModeJComboBox);
        controlPanel.add(borderToggle);
        controlPanel.add(animateToggle);
        controlPanel.add(refreshToggle);
        controlPanel.add(resetZoom);
        controlPanel.add(searchField);


        add(controlPanel, BorderLayout.NORTH);
        add(wrapper, BorderLayout.CENTER);
    }

    public FlameGraphTab(StacktraceTreeModel stacktraceTreeModel) {
        this();
        setStacktraceTreeModel(stacktraceTreeModel);
    }

    public void setStacktraceTreeModel(StacktraceTreeModel stacktraceTreeModel) {
        dataApplier = dataApplier(stacktraceTreeModel);
        dataApplier.accept(jfrFlameGraph);
    }

    private Consumer<FlameGraph<Node>> dataApplier(StacktraceTreeModel stacktraceTreeModel) {
        var flatFrameList = JfrFrameNodeConverter.convert(stacktraceTreeModel);
        return (flameGraph) -> flameGraph.setData(
                flatFrameList,
                NodeDisplayStringProvider.of(
                        (frame) -> {
                            if (frame.isRoot()) {
                                var events = stacktraceTreeModel.getItems()
                                                                .stream()
                                                                .map(iItems -> iItems.getType().getIdentifier())
                                                                .collect(joining(", "));
                                return "all (" + events + ")";
                            } else {
                                return frame.actualNode.getFrame().getHumanReadableShortString();
                            }
                        },
                        frame -> frame.isRoot() ? "" : FormatToolkit.getHumanReadable(frame.actualNode.getFrame().getMethod(), false, false, false, false, true, false),
                        frame -> frame.isRoot() ? "" : frame.actualNode.getFrame().getMethod().getMethodName()
                ),
                defaultFrameColorMode.colorMapperUsing(ColorMapper.ofObjectHashUsing(defaultColorPalette.colors())),
                frame -> {
                    if (frame.isRoot()) {
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
