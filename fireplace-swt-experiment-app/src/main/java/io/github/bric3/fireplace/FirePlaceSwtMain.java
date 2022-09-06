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
import io.github.bric3.fireplace.flamegraph.*;
import io.github.bric3.fireplace.swt_awt.EmbeddingComposite;
import io.github.bric3.fireplace.swt_awt.SWTKeyLogger;
import io.github.bric3.fireplace.swt_awt.SWT_AWTBridge;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.openjdk.jmc.common.item.*;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import javax.swing.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableList;

public class FirePlaceSwtMain {

    private FlamegraphView<Node> flamegraph;

    public static void main(String[] args) throws InterruptedException, InvocationTargetException {
        if (ProcessHandle.current().info().commandLine().stream().noneMatch(s -> s.contains("-XstartOnFirstThread")) &&
            System.getProperty("os.name").toLowerCase().contains("mac")) {
            System.err.println("java command requires -XstartOnFirstThread on macOs");
            System.exit(1);
        }

        Display.setAppName("FirePlace SWT Experiment");
        Display.setAppVersion("0.0.0");
        var display = new Display();

        try {
            Shell shell = new FirePlaceSwtMain().launchApp(display, args);
            try {
                shell.open();

                // Set up the SWT event loop
                while (!shell.isDisposed()) {
                    if (!display.readAndDispatch()) {
                        // If no more entries in the event queue
                        display.sleep();
                    }
                }
            } finally {
                if (!shell.isDisposed()) {
                    shell.dispose();
                }
            }
        } finally {
            display.dispose();
        }
    }

    private Shell launchApp(Display display, String[] args) throws InterruptedException, InvocationTargetException {
        var shell = new Shell(display, SWT.BORDER | SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.RESIZE);
        var shellBgAwtColor = SWT_AWTBridge.toAWTColor(shell.getBackground());
        Colors.setDarkMode(!Colors.isBright(shellBgAwtColor));

        shell.setText("FirePlace SWT Experiment");
        shell.addDisposeListener(event -> System.out.println("shell disposed"));
        shell.addShellListener(new ShellAdapter() {
            @Override
            public void shellClosed(ShellEvent e) {
                System.out.println("shell closed");
            }
        });
        var listener = new SWTKeyLogger();
        shell.addListener(SWT.KeyDown, listener);
        shell.addListener(SWT.KeyUp, listener);

        shell.setLayout(new FillLayout(SWT.VERTICAL));
        shell.setSize(1000, 800);
        
        var parentComposite = new Composite(shell, SWT.CENTER);
        parentComposite.setLayout(new GridLayout(1, false));
        var label = new Label(parentComposite, SWT.BORDER);
        label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        label.setText(" ");
        label.pack();


        var embeddingComposite = new EmbeddingComposite(parentComposite, SWT.NONE);

        var tooltip = new StyledToolTip(embeddingComposite, org.eclipse.jface.window.ToolTip.NO_RECREATE, true);
        {
            tooltip.setPopupDelay(500);
            tooltip.setShift(new org.eclipse.swt.graphics.Point(10, 5));

            if (Colors.isDarkMode()) {
                tooltip.setForegroundColor(new Color(223, 225, 229));
                tooltip.setBackgroundColor(new Color(43, 45, 48));
            }

            embeddingComposite.addListener(
                    SWT.MouseExit,
                    event -> shell.getDisplay().timerExec(300, tooltip::hide)
            );
        }

        embeddingComposite.init(() -> {
            flamegraph = createFlameGraph(embeddingComposite, tooltip);
            new ZoomAnimation().install(flamegraph);
            flamegraph.component.setBackground(shellBgAwtColor);
            return flamegraph.component;
        });

        loadJfr(args, label);

        return shell;
    }

    private FlamegraphView<Node> createFlameGraph(Composite owner, DefaultToolTip tooltip) {
        var fg = new FlamegraphView<Node>();
        fg.putClientProperty(FlamegraphView.SHOW_STATS, true);
        fg.setRenderConfiguration(
                FrameTextsProvider.of(
                        frame -> {
                            if (frame.isRoot()) {
                                return "root";
                            } else {
                                return frame.actualNode.getFrame().getHumanReadableShortString();
                            }
                        },
                        frame -> frame.isRoot() ? "" : FormatToolkit.getHumanReadable(
                                frame.actualNode.getFrame().getMethod(),
                                false,
                                false,
                                false,
                                false,
                                true,
                                false
                        ),
                        frame -> frame.isRoot() ? "" : frame.actualNode.getFrame().getMethod().getMethodName()
                ),
                new DimmingFrameColorProvider<>(
                        frame -> ColorMapper.ofObjectHashUsing(Palette.DATADOG.colors())
                                            .apply(frame.actualNode.getFrame()
                                                                   .getMethod()
                                                                   .getType()
                                                                   .getPackage())
                ),
                FrameFontProvider.defaultFontProvider()
        );

        // no tooltips as this is handled by SWT / JFACE code below
        fg.setHoverListener((frameBox, frameRect, mouseEvent) -> {
            var toolTipTarget = FlamegraphView.HoverListener.getPointLeveledToFrameDepth(mouseEvent, frameRect);
            toolTipTarget.y += frameRect.height;

            if (frameBox.isRoot()) {
                return;
            }

            var method = frameBox.actualNode.getFrame().getMethod();

            StringBuilder sb = new StringBuilder();
            sb.append("<form><p>")
              .append("<b>")
              .append(frameBox.actualNode.getFrame().getHumanReadableShortString())
              .append("</b><br/>");

            var packageName = method.getType().getPackage();
            if (packageName != null) {
                sb.append(packageName).append("<br/>");
            }
            sb.append("<hr/>Weight: ")
              .append(frameBox.actualNode.getCumulativeWeight())
              .append("<br/>")
              .append("Type: ")
              .append(frameBox.actualNode.getFrame().getType())
              .append("<br/>");

            Integer bci = frameBox.actualNode.getFrame().getBCI();
            if (bci != null) {
                sb.append("BCI: ")
                  .append(bci)
                  .append("<br/>");
            }
            Integer frameLineNumber = frameBox.actualNode.getFrame().getFrameLineNumber();
            if (frameLineNumber != null) {
                sb.append("Line number: ")
                  .append(frameLineNumber)
                  .append("<br/>");
            }
            sb.append("</p></form>");
            var text = sb.toString();

            try {
                // Not sure how to prevent that, but the call to asyncExec may deadlock
                owner.getDisplay().asyncExec(() -> {
                    var control = Display.getDefault().getCursorControl();

                    if (Objects.equals(owner, control)) {
                        tooltip.setText(text);

                        tooltip.hide();
                        tooltip.show(new org.eclipse.swt.graphics.Point(toolTipTarget.x, toolTipTarget.y));
                    }
                });
            } catch (SWTException e) {
                System.out.println("If this happen, the display has been disposed");
            }
        });

        return fg;
    }

    private void loadJfr(String[] args, Label text) {
        var jfrFiles = Arrays.stream(args)
                             .map(Path::of)
                             .filter(path -> {
                                 var exists = Files.exists(path);
                                 if (!exists) {
                                     System.err.println("File '" + path + "' does not exist");
                                 }
                                 return exists;
                             })
                             .peek(path -> System.out.println("Loading " + path))
                             .map(Path::toFile)
                             .collect(toUnmodifiableList());

        if (jfrFiles.isEmpty()) {
            System.err.println("No JFR files specified");
            return;
        }

        text.setText(jfrFiles.get(0).toString());

        var eventSupplier = CompletableFuture.supplyAsync(() -> {
            IItemCollection events = null;
            try {
                events = JfrLoaderToolkit.loadEvents(jfrFiles);
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            } catch (CouldNotLoadRecordingException e1) {
                throw new RuntimeException(e1);
            }
            events.stream()
                  .flatMap(IItemIterable::stream)
                  .map(IItem::getType)
                  .map(IType::getIdentifier)
                  .distinct()
                  .forEach(System.out::println);

            return events;
        });

        CompletableFuture.runAsync(() -> {
            var itemCollection = eventSupplier.join();
            var stacktraceTreeModel = stackTraceCPUFun(itemCollection);
            var flatFrameList = convert(stacktraceTreeModel);
            var title = stacktraceTreeModel.getItems()
                                           .stream()
                                           .map(iItems -> iItems.getType().getIdentifier())
                                           .collect(joining(", ", "all (", ")"));

            SwingUtilities.invokeLater(() -> {
                flamegraph.setModel(new FrameModel<>(
                        title,
                        (a, b) -> Objects.equals(a.actualNode.getFrame(), b.actualNode.getFrame()),
                        flatFrameList
                ));
            });
        });
    }

    static StacktraceTreeModel stackTraceCPUFun(IItemCollection events) {
        var methodFrameSeparator = new FrameSeparator(FrameCategorization.METHOD, false);

        var allocCollection = events.apply(ItemFilters.type(Set.of(
                "jdk.ExecutionSample"
        )));

        var invertedStacks = false;
        return new StacktraceTreeModel(allocCollection,
                methodFrameSeparator,
                invertedStacks
        );
    }


    public static List<FrameBox<Node>> convert(StacktraceTreeModel model) {
        var nodes = new ArrayList<FrameBox<Node>>();

        FrameBox.flattenAndCalculateCoordinate(
                nodes,
                model.getRoot(),
                Node::getChildren,
                Node::getCumulativeWeight,
                n -> n.getChildren().stream().mapToDouble(Node::getCumulativeWeight).sum(),
                0.0d,
                1.0d,
                0
        );

        assert nodes.get(0).actualNode.isRoot() : "First node should be the root node";
        return nodes;
    }
}
