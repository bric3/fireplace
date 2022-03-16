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

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.util.SystemInfo;
import io.github.bric3.fireplace.core.ui.Colors;
import io.github.bric3.fireplace.ui.FrameResizeLabel;
import io.github.bric3.fireplace.core.ui.JScrollPaneWithButton;
import io.github.bric3.fireplace.ui.HudPanel;
import io.github.bric3.fireplace.ui.debug.AssertiveRepaintManager;
import io.github.bric3.fireplace.ui.debug.CheckThreadViolationRepaintManager;
import io.github.bric3.fireplace.ui.debug.EventDispatchThreadHangMonitor;
import com.github.weisj.darklaf.platform.ThemePreferencesHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toUnmodifiableList;

public class FirePlaceMain {

    public static final String SYSTEM_PROPERTIES = "System properties";
    public static final String NATIVE_LIBRARIES = "Native libraries";
    public static final String ALLOCATIONS = "Allocations";
    public static final String CPU = "CPU";


    public static void main(String[] args) {
        System.getProperties().forEach((k, v) -> System.out.println(k + " = " + v));

        var paths = Arrays.stream(args)
                          .filter(arg -> !arg.matches("-NSRequiresAquaSystemAppearance|[Ff]alse|[Nn][Oo]|0"))
                          .map(Path::of)
                          .filter(path -> {
                              var exists = Files.exists(path);
                              if (!exists) {
                                  System.err.println("File '" + path + "' does not exist");
                              }
                              return exists;
                          })
                          .collect(toUnmodifiableList());

        var jfrBinder = new JFRBinder();

        initUI(jfrBinder, paths);
    }

    private static void initUI(JFRBinder jfrBinder, List<Path> cliPaths) {
        if (Boolean.getBoolean("fireplace.swing.debug") || Boolean.getBoolean("fireplace.debug")) {
            System.getProperties().forEach((k, v) -> System.out.println(k + " = " + v));
        }
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
            var openedFileLabel = new JTextField("");
            {
                openedFileLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                openedFileLabel.setEditable(false);
                openedFileLabel.setDropTarget(null);
                jfrBinder.bindPaths(p -> openedFileLabel.setText(p.get(0).toAbsolutePath().toString()));
            }

            var allocationFlameGraphPanel = new FlameGraphTab();
            {
                jfrBinder.bindEvents(JfrAnalyzer::stackTraceAllocationFun, allocationFlameGraphPanel::setStacktraceTreeModel);
            }
            var cpuFlameGraphPanel = new FlameGraphTab();
            {
                jfrBinder.bindEvents(JfrAnalyzer::stackTraceCPUFun, cpuFlameGraphPanel::setStacktraceTreeModel);
            }
            var nativeLibs = new JTextArea();
            {
                nativeLibs.setEditable(false);
                nativeLibs.setDropTarget(null);
                nativeLibs.getCaret().setVisible(true);
                nativeLibs.getCaret().setSelectionVisible(true);
                jfrBinder.bindEvents(JfrAnalyzer::nativeLibraries, nativeLibs::setText);
            }
            var sysProps = new JTextArea();
            {
                sysProps.setEditable(false);
                sysProps.setDropTarget(null);
                sysProps.getCaret().setVisible(true);
                sysProps.getCaret().setSelectionVisible(true);
                jfrBinder.bindEvents(JfrAnalyzer::jvmSystemProperties, sysProps::setText);
            }
            var jTabbedPane = new JTabbedPane();
            {
                jTabbedPane.addTab(SYSTEM_PROPERTIES, JScrollPaneWithButton.create(() -> new JScrollPane(sysProps)));
                jTabbedPane.addTab(NATIVE_LIBRARIES, JScrollPaneWithButton.create(() -> new JScrollPane(nativeLibs)));
                jTabbedPane.addTab(ALLOCATIONS, allocationFlameGraphPanel);
                jTabbedPane.addTab(CPU, cpuFlameGraphPanel);
                jTabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
            }

            var mainPanel = new JPanel(new BorderLayout());
            mainPanel.add(openedFileLabel, BorderLayout.NORTH);
            mainPanel.add(jTabbedPane, BorderLayout.CENTER);

            var frameResizeLabel = new FrameResizeLabel();
            var hudPanel = new HudPanel();
            jfrBinder.setOnLoadActions(() -> hudPanel.setProgressVisible(true), () -> hudPanel.setProgressVisible(false));

            var appLayers = new JLayeredPane();
            appLayers.setLayout(new OverlayLayout(appLayers));
            appLayers.setOpaque(false);
            appLayers.setVisible(true);
            appLayers.add(mainPanel, JLayeredPane.DEFAULT_LAYER);
            appLayers.add(hudPanel.getComponent(), JLayeredPane.MODAL_LAYER);
            appLayers.add(frameResizeLabel.getComponent(), JLayeredPane.POPUP_LAYER);

            JfrFilesDropHandler.install(jfrBinder::load, appLayers, hudPanel.getDnDTarget());

            var frame = new JFrame("FirePlace");
            setIcon(frame);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(new Dimension(1000, 600));
            frame.getContentPane().add(appLayers);
            frameResizeLabel.installListener(frame);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    if (cliPaths.isEmpty()) {
                        hudPanel.getDnDTarget().activate();
                    } else {
                        jfrBinder.load(cliPaths);
                    }
                }
            });

            frame.getGraphicsConfiguration(); // get active screen
            frame.setVisible(true);
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
            // System.out.println(">>>> theme preference changed = " + ThemePreferencesHandler.getSharedInstance().getPreferredThemeStyle());
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
