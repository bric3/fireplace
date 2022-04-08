package io.github.bric3.fireplace;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.util.SystemInfo;
import com.github.weisj.darklaf.platform.decorations.DecorationsColorProvider;
import com.github.weisj.darklaf.platform.decorations.ExternalLafDecorator;
import com.github.weisj.darklaf.platform.preferences.SystemPreferencesManager;
import io.github.bric3.fireplace.core.ui.Colors;
import io.github.bric3.fireplace.icons.darkMode_moon;
import io.github.bric3.fireplace.icons.darkMode_sun;

import javax.swing.*;
import java.awt.*;

class AppearanceControl {
    private static final AppearanceControl INSTANCE = new AppearanceControl();
    public static final String TO_APPEARANCE = "CURRENT_MODE";
    public static final String TO_LIGHT_LAF = "LIGHT";
    public static final String TO_DARK_LAF = "DARK";
    private static final SystemPreferencesManager manager = new SystemPreferencesManager();
    public static final Runnable SYNC_THEME_CHANGER = () -> {
        // System.out.println(">>>> theme preference changed = " + ThemePreferencesHandler.getSharedInstance().getPreferredThemeStyle());
        switch (manager.getPreferredThemeStyle().getColorToneRule()) {
            case DARK:
                Colors.setDarkMode(true);
                FlatDarculaLaf.setup();
                break;
            case LIGHT:
                Colors.setDarkMode(false);
                FlatIntelliJLaf.setup();
                break;
        }

        updateUI();
        FlatAnimatedLafChange.hideSnapshotWithAnimation();
    };

    static {
        manager.addListener(style -> SYNC_THEME_CHANGER.run());
    }


    static void install(JFrame frame) {
        setGlobalProperties(frame.getTitle());

        // configure frame for transparency
        var rootPane = frame.getRootPane();
        if (SystemInfo.isMacOS) {
            // allows to place swing components on the whole window
            rootPane.putClientProperty("apple.awt.fullWindowContent", true);
            // makes the title bar transparent
            rootPane.putClientProperty("apple.awt.transparentTitleBar", true);
        }

        ExternalLafDecorator.instance().install();
        ExternalLafDecorator.instance().setColorProvider(new DecorationsColorProvider() {
            @Override
            public Color backgroundColor() {
                return UIManager.getColor("TitlePane.background");
            }

            @Override
            public Color activeForegroundColor() {
                // ignored on macOs
                return UIManager.getColor("TitlePane.foreground");
            }

            @Override
            public Color inactiveForegroundColor() {
                return Color.ORANGE;
            }

            @Override
            public TitleColor windowTitleColor() {
                return FlatLaf.isLafDark() ? TitleColor.LIGHT : TitleColor.DARK;
            }
        });
        INSTANCE.start();
    }

    private static void setGlobalProperties(String title) {
        if (SystemInfo.isLinux) {
            // most linux distros have ugly font rendering, but these here can fix that:
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
            System.setProperty("sun.java2d.xrender", "true");
        }

        if (SystemInfo.isMacOS) {
            System.setProperty("apple.laf.useScreenMenuBar", "true"); // moves menu bar from JFrame window to top of screen
            System.setProperty("apple.awt.application.name", title); // application name used in screen menu bar
            // appearance of window title bars
            // possible values:
            //   - "system": use current macOS appearance (light or dark)
            //   - "NSAppearanceNameAqua": use light appearance
            //   - "NSAppearanceNameDarkAqua": use dark appearance
            //            System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua");
            System.setProperty("apple.awt.application.appearance", "system");
        }
    }

    static void installAppIcon(JFrame jFrame) {
        var resource = AppearanceControl.class.getClassLoader().getResource("fire.png");
        var image = Toolkit.getDefaultToolkit().getImage(resource);

        try {
            Taskbar.getTaskbar().setIconImage(image);
        } catch (UnsupportedOperationException | SecurityException ignored) {
        }
        jFrame.setIconImage(image);
    }

    private void start() {
        SYNC_THEME_CHANGER.run();
        toggleSyncWithOS(true);
    }

    private static void toggleSyncWithOS(boolean sync) {
        manager.enableReporting(sync);
    }

    static JComponent getComponent() {
        var appearanceModeButton = new JButton();
        {
            var iconSize = 10;
            var toLightMode = darkMode_sun.of(iconSize, iconSize);
            var toDarkMode = darkMode_moon.of(iconSize, iconSize);
            Runnable syncIconUpdater = () -> {
                switch (manager.getPreferredThemeStyle().getColorToneRule()) {
                    case DARK:
                        appearanceModeButton.putClientProperty(TO_APPEARANCE, TO_LIGHT_LAF);
                        appearanceModeButton.setIcon(toLightMode);
                        break;
                    case LIGHT:
                        appearanceModeButton.putClientProperty(TO_APPEARANCE, TO_DARK_LAF);
                        appearanceModeButton.setIcon(toDarkMode);
                        break;
                }
            };
            manager.addListener(e -> syncIconUpdater.run());
            appearanceModeButton.addPropertyChangeListener("enabled", e -> syncIconUpdater.run());
            syncIconUpdater.run();

            appearanceModeButton.addActionListener(e -> {
                toggleSyncWithOS(false);
                switch (appearanceModeButton.getClientProperty(TO_APPEARANCE).toString()) {
                    case TO_DARK_LAF:
                        FlatDarculaLaf.setup();
                        Colors.setDarkMode(true);
                        appearanceModeButton.putClientProperty(TO_APPEARANCE, TO_LIGHT_LAF);
                        appearanceModeButton.setIcon(toLightMode);
                        break;
                    case TO_LIGHT_LAF:
                        FlatIntelliJLaf.setup();
                        Colors.setDarkMode(false);
                        appearanceModeButton.putClientProperty(TO_APPEARANCE, TO_DARK_LAF);
                        appearanceModeButton.setIcon(toDarkMode);
                        break;
                }
                updateUI();
                FlatAnimatedLafChange.hideSnapshotWithAnimation();
            });
            appearanceModeButton.setEnabled(false);
            appearanceModeButton.setVisible(true);
        }

        var syncAppearanceButton = new JCheckBox("Sync appearance");
        {
            syncAppearanceButton.addActionListener(e -> {
                toggleSyncWithOS(syncAppearanceButton.isSelected());
                appearanceModeButton.setEnabled(!syncAppearanceButton.isSelected());
                SYNC_THEME_CHANGER.run();
            });
            syncAppearanceButton.setSelected(true);
        }

        var appearanceControlsPanel = new JPanel(new FlowLayout());
        appearanceControlsPanel.add(appearanceModeButton);
        appearanceControlsPanel.add(syncAppearanceButton);

        return appearanceControlsPanel;
    }

    public static void updateUI() {
        for (var window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
        }
    }
}
