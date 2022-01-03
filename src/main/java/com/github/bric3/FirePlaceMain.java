package com.github.bric3;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.util.SystemInfo;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.ItemToolkit;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toUnmodifiableList;

public class FirePlaceMain {

    public static void main(String[] args) throws CouldNotLoadRecordingException, IOException {

        var paths = Arrays.stream(args).map(Path::of).collect(toUnmodifiableList());

        if (!paths.stream().allMatch(path -> {
            var exists = Files.exists(path);
            if (!exists) {
                System.err.println("File '" + path + "' does not exist");
            }
            return exists;
        })) {
            System.exit(1);
        }

        var jfrFiles = paths.stream()
                            .peek(path -> System.out.println("Loading " + path))
                            .map(Path::toFile)
                            .collect(toUnmodifiableList());
        var events = JfrLoaderToolkit.loadEvents(jfrFiles);
//        events.stream().flatMap(IItemIterable::stream).map(IItem::getType).map(IType::getIdentifier).distinct().forEach(System.out::println);


        otherEvents(events);


        //        events.apply(ItemFilters.type(Set.of(
//                "jdk.ExecutionSample",
//                "jdk.CPULoad"
//        )));


        if (SystemInfo.isMacOS) {
            System.setProperty("apple.laf.useScreenMenuBar", "true"); // moves menu bar from JFrame window to top of screen
            System.setProperty("apple.awt.application.name", "FirePlace"); // application name used in screen menu bar
            // appearance of window title bars
            // possible values:
            //   - "system": use current macOS appearance (light or dark)
            //   - "NSAppearanceNameAqua": use light appearance
            //   - "NSAppearanceNameDarkAqua": use dark appearance
            System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua");
        }
        FlatDarculaLaf.setup();


        var flameGraphPanel = new FlameGraphPanel(() -> stackTraceAllocationFun(events));
        var nativeLibs = new JTextArea();
        nativeLibs.addPropertyChangeListener("text", evt -> SwingUtilities.invokeLater(() -> updateContent(nativeLibs, t -> t.setText(nativeLibraries(events)))));
        var sysProps = new JTextArea();
        sysProps.addPropertyChangeListener("text", evt -> SwingUtilities.invokeLater(() -> updateContent(sysProps, t -> t.setText(jvmSystemProperties(events)))));

        var jTabbedPane = new JTabbedPane();
        jTabbedPane.addTab("System properties", new JScrollPane(sysProps));
        jTabbedPane.addTab("Native libraries", new JScrollPane(nativeLibs));
        jTabbedPane.addTab("FlameGraph", new JScrollPane(flameGraphPanel));
        jTabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
//        jTabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        updateTabContent(jTabbedPane, nativeLibs, sysProps, events);
        jTabbedPane.addChangeListener(e -> updateTabContent(jTabbedPane, nativeLibs, sysProps, events));


        var dimensionLabel = new JLabel("hello");
        dimensionLabel.setVerticalAlignment(JLabel.CENTER);
        dimensionLabel.setHorizontalAlignment(JLabel.CENTER);
        dimensionLabel.setOpaque(true);
        dimensionLabel.setBorder(BorderFactory.createLineBorder(Color.black));
        var jPanel = new JPanel(new BorderLayout());
        jPanel.add(dimensionLabel, BorderLayout.CENTER);
        jPanel.setBackground(new Color(0, 0, 0, 0));
        jPanel.setOpaque(false);
        jPanel.setVisible(false);
        var textHeight = dimensionLabel.getFontMetrics(dimensionLabel.getFont()).getHeight();
        jPanel.setMaximumSize(new Dimension(100, textHeight + 10));
        var panelHider = new Timer(2_000, e -> jPanel.setVisible(false));
        panelHider.setCoalesce(true);

        var jLayeredPane = new JLayeredPane();
        jLayeredPane.setLayout(new OverlayLayout(jLayeredPane));
        jLayeredPane.setOpaque(false);
        jLayeredPane.setVisible(true);
        jLayeredPane.add(jTabbedPane, JLayeredPane.DEFAULT_LAYER);
        jLayeredPane.add(jPanel, JLayeredPane.POPUP_LAYER);

        SwingUtilities.invokeLater(() -> {
            var frame = new JFrame("FirePlace");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(new Dimension(1000, 600));
            frame.getContentPane().add(jLayeredPane);
            frame.setVisible(true);
            frame.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    int height = frame.getHeight();
                    int width = frame.getWidth();
                    dimensionLabel.setText(height + " x " + width);
                    jPanel.setVisible(true);
                    panelHider.restart();
                }
            });
        });
    }

    private static void updateTabContent(JTabbedPane jTabbedPane, JTextArea nativeLibs, JTextArea sysProps, IItemCollection events) {
        switch (jTabbedPane.getTitleAt(jTabbedPane.getSelectedIndex())) {
            case "System properties":
                sysProps.firePropertyChange("text", -1, Clock.systemUTC().millis());
                break;

            case "Native libraries":
                nativeLibs.firePropertyChange("text", -1, Clock.systemUTC().millis());
                break;

            case "FlameGraph":
            default:
        }
    }

    private static <T extends JComponent> void updateContent(T sysProps, Consumer<T> updater) {
        if (sysProps.getClientProperty("UP_TO_DATE") == null) {
            updater.accept(sysProps);
            sysProps.putClientProperty("UP_TO_DATE", true);
        }
    }

    private static StacktraceTreeModel stackTraceAllocationFun(IItemCollection events) {
        var methodFrameSeparator = new FrameSeparator(FrameCategorization.METHOD, false);

        var allocCollection = events.apply(ItemFilters.type(Set.of(
                "jdk.ObjectAllocationInNewTLAB",
                "jdk.ObjectAllocationOutsideTLAB"
        )));


        return new StacktraceTreeModel(allocCollection,
                                       methodFrameSeparator,
                                       false
//                                       , JdkAttributes.ALLOCATION_SIZE
        );


//        allocCollection.forEach(eventsCollection -> {
//            var stackAccessor = eventsCollection.getType().getAccessor(JfrAttributes.EVENT_STACKTRACE.getKey());
//            eventsCollection.stream().limit(10).forEach(item -> {
//                var stack = stackAccessor.getMember(item);
//
//                if (stack == null || stack.getFrames() == null) {
//                    return;
//                }
//
//
//            });
//        });
    }

    private static void otherEvents(IItemCollection events) {
        events.apply(ItemFilters.type(Set.of(
                "jdk.CPUInformation",
                "jdk.OSInformation",
                "jdk.ActiveRecording",
//                "jdk.ActiveSetting", // async profiler settings ?
                "jdk.JVMInformation"
        ))).forEach(eventsCollection -> {
            eventsCollection.stream().limit(10).forEach(event -> {
                System.out.println("\n" + event.getType().getIdentifier());
                IType<IItem> itemType = ItemToolkit.getItemType(event);
                itemType.getAccessorKeys().keySet().forEach((accessorKey) -> {
                    System.out.println(accessorKey.getIdentifier() + "=" + itemType.getAccessor(accessorKey).getMember(event));
                });
            });
        });
    }

    private static String jvmSystemProperties(IItemCollection events) {
        var stringBuilder = new StringBuilder();

        events.apply(ItemFilters.type(Set.of(
                "jdk.InitialSystemProperty"
        ))).forEach(eventsCollection -> {
            var keyAccessor = eventsCollection.getType().getAccessor(JdkAttributes.ENVIRONMENT_KEY.getKey());
            var valueAccessor = eventsCollection.getType().getAccessor(JdkAttributes.ENVIRONMENT_VALUE.getKey());
            eventsCollection.stream().forEach(event -> stringBuilder.append(keyAccessor.getMember(event)).append(" = ").append(valueAccessor.getMember(event)).append("\n"));
        });

        return stringBuilder.toString();
    }

    private static String nativeLibraries(IItemCollection events) {
        var stringBuilder = new StringBuilder();

        events.apply(ItemFilters.type(Set.of(
                "jdk.NativeLibrary"
        ))).forEach(eventsCollection -> {
            var nativeLibNameAccessor = eventsCollection.getType().getAccessor(JdkAttributes.NATIVE_LIBRARY_NAME.getKey());
            eventsCollection.stream().forEach(event -> stringBuilder.append(nativeLibNameAccessor.getMember(event)).append("\n"));
        });
        return stringBuilder.toString();
    }
}
