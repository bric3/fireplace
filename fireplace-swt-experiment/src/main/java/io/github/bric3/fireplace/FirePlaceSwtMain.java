package io.github.bric3.fireplace;

import io.github.bric3.fireplace.core.ui.Colors.Palette;
import io.github.bric3.fireplace.flamegraph.ColorMapper;
import io.github.bric3.fireplace.flamegraph.FlamegraphView;
import io.github.bric3.fireplace.flamegraph.FrameBox;
import io.github.bric3.fireplace.flamegraph.FrameColorProvider;
import io.github.bric3.fireplace.flamegraph.FrameFontProvider;
import io.github.bric3.fireplace.flamegraph.FrameTextsProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableList;

public class FirePlaceSwtMain {

    private Shell shell;
    private FlamegraphView<Node> flamegraph;

    public static void main(String[] args) {
        if (ProcessHandle.current().info().commandLine().stream().noneMatch(s -> s.contains("-XstartOnFirstThread")) &&
            System.getProperty("os.name").toLowerCase().contains("mac")) {
            System.err.println("java command requires -XstartOnFirstThread on macOs");
            System.exit(1);
        }

        Display.setAppName("FirePlace SWT Experiment");
        Display.setAppVersion("0.0.0");
        var display = new Display();
        // display.asyncExec(() -> {
        //     // scheduled work on the SWT event thread
        // });


        Shell shell = new FirePlaceSwtMain().launchApp(display, args);
        // shell.pack();
        shell.open();
        shell.forceActive();

        // Set up the SWT event loop
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                // If no more entries in the event queue
                display.sleep();
            }
        }
        display.dispose();
    }

    private Shell launchApp(Display display, String[] args) {
        shell = new Shell(display, SWT.BORDER | SWT.CLOSE | SWT.MIN | SWT.TITLE | SWT.RESIZE);
        shell.setText("FirePlace SWT Experiment");
        shell.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent event) {
                // do something
            }
        });
        shell.setLayout(new FillLayout(SWT.VERTICAL));
        shell.setSize(1000, 800);

        var parentComposite = new Composite(shell, SWT.CENTER);
        parentComposite.setLayout(new GridLayout(1, false));
        var label = new Label(parentComposite, SWT.BORDER);
        label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        label.setText(" ");
        label.pack();

        var swingComposite = new Composite(parentComposite, SWT.EMBEDDED | SWT.NO_BACKGROUND);
        swingComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        // swingComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        // swingComposite.setLayout(new GridLayout(1, false));

        var frame = SWT_AWT.new_Frame(swingComposite);
        // needed to properly terminate the app on close
        swingComposite.addDisposeListener(e -> {
            // SwingUtilities.invokeLater(() -> {
                try {
                    frame.removeNotify();
                } catch (Exception ignored) {}
            // });
        });
        SwingUtilities.invokeLater(() -> {
            JRootPane rootPane = new JRootPane();
            flamegraph = createFlameGraph();
            rootPane.getContentPane().add(flamegraph.component);

            Panel panel = new Panel();
            panel.setLayout(new BorderLayout());
            panel.add(rootPane);
            frame.add(panel);
            frame.pack();
            frame.setVisible(true);
        });

        loadJfr(args, label, swingComposite);

        return shell;
    }

    private FlamegraphView<Node> createFlameGraph() {
        var fg = new FlamegraphView<Node>();
        fg.putClientProperty(FlamegraphView.SHOW_STATS, true);


        return fg;
    }

    private void loadJfr(String[] args, Label text, Composite swingComposite) {
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

            SwingUtilities.invokeLater(() -> {
                flamegraph.setConfigurationAndData(
                        flatFrameList,
                        FrameTextsProvider.of(
                                frame -> {
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
                        FrameColorProvider.defaultColorProvider(
                                frame -> ColorMapper.ofObjectHashUsing(Palette.DATADOG.colors()).apply(frame.actualNode.getFrame().getMethod().getType().getPackage())
                        ),
                        FrameFontProvider.defaultFontProvider(),
                        frame -> ""
                );

                // don't fix anything...
                // the scroll bar don't show up
                Display.getDefault().asyncExec(() -> {
                    swingComposite.layout(true, true);
                    // var swingCompositeSize = swingComposite.getSize();
                    // flamegraph.component.setSize(swingCompositeSize.x, swingCompositeSize.y);
                });
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
