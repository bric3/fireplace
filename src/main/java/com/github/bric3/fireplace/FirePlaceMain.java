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

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.util.SystemInfo;
import com.github.bric3.fireplace.ui.Colors;
import com.github.bric3.fireplace.ui.JScrollPaneWithButton;
import com.github.bric3.fireplace.ui.debug.AssertiveRepaintManager;
import com.github.bric3.fireplace.ui.debug.CheckThreadViolationRepaintManager;
import com.github.bric3.fireplace.ui.debug.EventDispatchThreadHangMonitor;
import com.github.weisj.darklaf.platform.ThemePreferencesHandler;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toUnmodifiableList;

public class FirePlaceMain {

    public static final String SYSTEM_PROPERTIES = "System properties";
    public static final String NATIVE_LIBRARIES = "Native libraries";
    public static final String ALLOCATIONS = "Allocations";
    public static final String CPU = "CPU";

    public static void main(String[] args) {
        System.getProperties().forEach((k, v) -> System.out.println(k + " = " + v));

        if (args.length == 0) {
            System.err.println("Requires at least one JFR file:\n\nUsage: java -jar fireplace.jar <JFR file>");
            System.exit(1);
        }

        var paths = Arrays.stream(args).filter(arg -> !arg.matches("-NSRequiresAquaSystemAppearance|[Ff]alse|[Nn][Oo]|0")).map(Path::of).collect(toUnmodifiableList());
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
        var eventSupplier = Utils.memoize(() -> CompletableFuture.supplyAsync(() -> {
            IItemCollection events = null;
            try {
                events = JfrLoaderToolkit.loadEvents(jfrFiles);
            } catch (IOException e1) {
                throw new UncheckedIOException(e1);
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
        }).join());

//        otherEvents(events);


//        events.apply(ItemFilters.type(Set.of(
//                "jdk.CPULoad"
//        )));

        setupLaF();
        if (Boolean.getBoolean("fireplace.swing.debug")) {
            if (Objects.equals(System.getProperty("fireplace.swing.debug.thread.violation.checker"), "IJ")) {
                AssertiveRepaintManager.install();
            } else {
                CheckThreadViolationRepaintManager.install();
            }
            EventDispatchThreadHangMonitor.initMonitoring();
        }


        SwingUtilities.invokeLater(() -> {
            var openedFileLabel = new JTextField(jfrFiles.get(0).getAbsolutePath());
            openedFileLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            openedFileLabel.setEditable(false);

            var allocationFlameGraphPanel = new FlameGraphTab(() -> stackTraceAllocationFun(eventSupplier.get()));
            var cpuFlameGraphPanel = new FlameGraphTab(() -> stackTraceCPUFun(eventSupplier.get()));
            var nativeLibs = new JTextArea();
            nativeLibs.addPropertyChangeListener("text", evt -> CompletableFuture.runAsync(() -> updateContent(nativeLibs, t -> t.setText(nativeLibraries(eventSupplier.get())))));
            var sysProps = new JTextArea();
            sysProps.addPropertyChangeListener("text", evt -> CompletableFuture.runAsync(() -> updateContent(sysProps, t -> t.setText(jvmSystemProperties(eventSupplier.get())))));

            var jTabbedPane = new JTabbedPane();
            jTabbedPane.addTab(SYSTEM_PROPERTIES, JScrollPaneWithButton.create(() -> new JScrollPane(sysProps)));
            jTabbedPane.addTab(NATIVE_LIBRARIES, JScrollPaneWithButton.create(() -> new JScrollPane(nativeLibs)));
            jTabbedPane.addTab(ALLOCATIONS, allocationFlameGraphPanel);
            jTabbedPane.addTab(CPU, cpuFlameGraphPanel);
            jTabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
            //        jTabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

            jTabbedPane.addChangeListener(e -> updateTabContent(jTabbedPane, nativeLibs, sysProps, eventSupplier.get()));

            var mainPanel = new JPanel(new BorderLayout());
            mainPanel.add(openedFileLabel, BorderLayout.NORTH);
            mainPanel.add(jTabbedPane, BorderLayout.CENTER);


            var dimensionLabel = new JLabel("hello");
            dimensionLabel.setVerticalAlignment(JLabel.CENTER);
            dimensionLabel.setHorizontalAlignment(JLabel.CENTER);
            dimensionLabel.setOpaque(true);
            dimensionLabel.setBorder(BorderFactory.createLineBorder(Color.black));
            var dimensionOverlayPanel = new JPanel(new BorderLayout());
            dimensionOverlayPanel.add(dimensionLabel, BorderLayout.CENTER);
            dimensionOverlayPanel.setBackground(new Color(0, 0, 0, 0));
            dimensionOverlayPanel.setOpaque(false);
            dimensionOverlayPanel.setVisible(false);
            var textHeight = dimensionLabel.getFontMetrics(dimensionLabel.getFont()).getHeight();
            dimensionOverlayPanel.setMaximumSize(new Dimension(100, textHeight + 10));
            var panelHider = new Timer(2_000, e -> dimensionOverlayPanel.setVisible(false));
            panelHider.setCoalesce(true);

            var jLayeredPane = new JLayeredPane();
            jLayeredPane.setLayout(new OverlayLayout(jLayeredPane));
            jLayeredPane.setOpaque(false);
            jLayeredPane.setVisible(true);
            jLayeredPane.add(mainPanel, JLayeredPane.DEFAULT_LAYER);
            jLayeredPane.add(dimensionOverlayPanel, JLayeredPane.POPUP_LAYER);

            var frame = new JFrame("FirePlace");
            setIcon(frame);
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
                    dimensionOverlayPanel.setVisible(true);
                    panelHider.restart();
                }
            });
            frame.getGraphicsConfiguration(); // get active screen
            SwingUtilities.invokeLater(() -> updateTabContent(jTabbedPane, nativeLibs, sysProps, eventSupplier.get()));
        });
    }

    private static void setupLaF() {
        if (SystemInfo.isLinux) {
            // most linux distros have ugly font rendering, but these here can fix that:
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
            System.setProperty("sun.java2d.xrender", "true");
        }

        if (SystemInfo.isMacOS) {
            System.setProperty("apple.laf.useScreenMenuBar", "true"); // moves menu bar from JFrame window to top of screen
            System.setProperty("apple.awt.application.name", "FirePlace"); // application name used in screen menu bar
            // appearance of window title bars
            // possible values:
            //   - "system": use current macOS appearance (light or dark)
            //   - "NSAppearanceNameAqua": use light appearance
            //   - "NSAppearanceNameDarkAqua": use dark appearance
            //            System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua");
            System.setProperty("apple.awt.application.appearance", "system");
        }
        ThemePreferencesHandler.getSharedInstance().enablePreferenceChangeReporting(true);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> ThemePreferencesHandler.getSharedInstance().enablePreferenceChangeReporting(false)));

        Runnable themeChanger = () -> {
            System.out.println(">>>> theme preference changed = " + ThemePreferencesHandler.getSharedInstance().getPreferredThemeStyle());
            switch (ThemePreferencesHandler.getSharedInstance().getPreferredThemeStyle().getColorToneRule()) {
                case DARK:
                    FlatDarculaLaf.setup();
                    Colors.setDarkMode(true);
                    break;
                case LIGHT:
                    FlatIntelliJLaf.setup();
                    Colors.setDarkMode(false);
                    break;
            }
            FlatLaf.updateUI();
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
        };
        themeChanger.run();
        ThemePreferencesHandler.getSharedInstance().addThemePreferenceChangeListener(
                e -> themeChanger.run()
        );
    }

    private static void updateTabContent(JTabbedPane jTabbedPane, JTextArea nativeLibs, JTextArea sysProps, IItemCollection events) {
        switch (jTabbedPane.getTitleAt(jTabbedPane.getSelectedIndex())) {
            case SYSTEM_PROPERTIES:
                sysProps.firePropertyChange("text", -1, Clock.systemUTC().millis());
                break;

            case NATIVE_LIBRARIES:
                nativeLibs.firePropertyChange("text", -1, Clock.systemUTC().millis());
                break;

            case ALLOCATIONS:
            case CPU:
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

    private static StacktraceTreeModel stackTraceCPUFun(IItemCollection events) {
        var methodFrameSeparator = new FrameSeparator(FrameCategorization.METHOD, false);

        var allocCollection = events.apply(ItemFilters.type(Set.of(
                "jdk.ExecutionSample"
        )));

        var invertedStacks = false;
        return new StacktraceTreeModel(allocCollection,
                                       methodFrameSeparator,
                                       invertedStacks
//                                      , JdkAttributes.SAMPLE_WEIGHT
        );
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

    private static void setIcon(JFrame jFrame) {
        var resource = FirePlaceMain.class.getClassLoader().getResource("fire.png");
        var image = Toolkit.getDefaultToolkit().getImage(resource);

        try {
            Taskbar.getTaskbar().setIconImage(image);
        } catch (UnsupportedOperationException | SecurityException ignored) {
        }
        jFrame.setIconImage(image);
    }
}
