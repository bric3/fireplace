/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
@file:Suppress("unused")

package io.github.bric3.fireplace.ui.toolkit

import com.formdev.flatlaf.FlatDarculaLaf
import com.formdev.flatlaf.FlatIntelliJLaf
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.extras.FlatAnimatedLafChange
import com.github.weisj.darklaf.platform.DecorationsConstants
import com.github.weisj.darklaf.platform.SystemInfo
import com.github.weisj.darklaf.platform.decorations.DecorationsColorProvider
import com.github.weisj.darklaf.platform.decorations.DecorationsColorProvider.TitleColor
import com.github.weisj.darklaf.platform.decorations.ExternalLafDecorator
import com.github.weisj.darklaf.platform.preferences.SystemPreferencesManager
import com.github.weisj.darklaf.theme.spec.ColorToneRule
import io.github.bric3.fireplace.core.ui.Colors
import io.github.bric3.fireplace.icons.darkMode_moon
import io.github.bric3.fireplace.icons.darkMode_sun
import java.awt.Color
import java.awt.FlowLayout
import java.awt.Rectangle
import java.awt.Taskbar
import java.awt.Toolkit
import java.awt.Window
import javax.swing.*

@Suppress("unused")
internal object AppearanceControl {
    private const val TO_APPEARANCE = "CURRENT_MODE"
    private const val TO_LIGHT_LAF = "LIGHT"
    private const val TO_DARK_LAF = "DARK"
    private val manager = SystemPreferencesManager()

    @JvmStatic
    private val SYNC_THEME_CHANGER = Runnable {
        // System.out.println(">>>> theme preference changed = " + ThemePreferencesHandler.getSharedInstance().getPreferredThemeStyle());
        @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
        when (manager.preferredThemeStyle.colorToneRule) {
            ColorToneRule.DARK -> {
                Colors.setDarkMode(true)
                FlatDarculaLaf.setup()
            }

            ColorToneRule.LIGHT -> {
                Colors.setDarkMode(false)
                FlatIntelliJLaf.setup()
            }
        }

        updateUI()
        FlatAnimatedLafChange.hideSnapshotWithAnimation()
    }

    init {
        manager.addListener { SYNC_THEME_CHANGER.run() }
    }

    @JvmStatic
    private fun start() {
        SYNC_THEME_CHANGER.run()
        toggleSyncWithOS(true)
    }

    @JvmStatic
    fun install(frame: JFrame) {
        setGlobalProperties(frame.title)

        // configure frame for transparency
        if (SystemInfo.isMac) {
            frame.rootPane.apply {
                // Allow placing swing components on the whole window
                putClientProperty("apple.awt.fullWindowContent", true)
                // Makes the title bar transparent
                putClientProperty("apple.awt.transparentTitleBar", true)
                // Hide window title
                putClientProperty(DecorationsConstants.KEY_HIDE_TITLE, true)
                // Allow to enter full screen when pressing Alt
                putClientProperty("apple.awt.fullscreenable", true)
            }
        }
        ExternalLafDecorator.instance().install()
        ExternalLafDecorator.instance().setColorProvider(object : DecorationsColorProvider {
            override fun backgroundColor(): Color {
                return UIManager.getColor("TitlePane.background")
            }

            override fun activeForegroundColor(): Color {
                // ignored on macOS
                return UIManager.getColor("TitlePane.foreground")
            }

            override fun inactiveForegroundColor(): Color {
                return Color.ORANGE
            }

            override fun windowTitleColor(): TitleColor {
                return if (FlatLaf.isLafDark()) TitleColor.LIGHT else TitleColor.DARK
            }
        })

        run {
            // UIManager.put("TextComponent.arc", 5) // Text fields with rounded corners
            UIManager.put("Component.focusWidth", 1)
            UIManager.put("Component.innerFocusWidth", 1)
            UIManager.put("Tree.wideSelection", "false")
            UIManager.put("Tree.selectionArc", 5)
            UIManager.put("List.selectionArc", 5)
            UIManager.put("ComboBox.popupInsets", "1,0,1,0")
            UIManager.put("ComboBox.selectionArc", 5)
        }

        start()
    }

    fun getWindowButtonsRect(rootPaneContainer: RootPaneContainer): Rectangle {
        return getWindowButtonsRect(rootPaneContainer.rootPane)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun getWindowButtonsRect(rootPane: JRootPane): Rectangle {
        return ExternalLafDecorator.instance().decorationsManager().titlePaneLayoutInfo(rootPane).windowButtonRect()
    }

    @JvmStatic
    private fun setGlobalProperties(title: String) {
        if (SystemInfo.isLinux) {
            // most linux distros have ugly font rendering, but these here can fix that:
            System.setProperty("awt.useSystemAAFontSettings", "on")
            System.setProperty("swing.aatext", "true")
            System.setProperty("sun.java2d.xrender", "true")
        }
        if (SystemInfo.isMac) {
            System.setProperty(
                "apple.laf.useScreenMenuBar",
                "true"
            ) // moves menu bar from JFrame window to top of the screen
            System.setProperty("apple.awt.application.name", title) // application name used in screen menu bar
            // appearance of window title bars
            // possible values:
            //   - "system": use current macOS appearance (light or dark)
            //   - "NSAppearanceNameAqua": use light appearance
            //   - "NSAppearanceNameDarkAqua": use dark appearance
            //            System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua");
            System.setProperty("apple.awt.application.appearance", "system")
        }
    }

    fun installAppIcon(jFrame: JFrame) {
        val resource = AppearanceControl::class.java.classLoader.getResource("fire.png")
        Toolkit.getDefaultToolkit().getImage(resource).let {
            try {
                Taskbar.getTaskbar().iconImage = it
            } catch (_: UnsupportedOperationException) {
            } catch (_: SecurityException) {
            }
            jFrame.iconImage = it
        }
    }

    @JvmStatic
    private fun toggleSyncWithOS(sync: Boolean) {
        manager.enableReporting(sync)
    }

    val component: JComponent
        get() {
            val appearanceModeButton = JButton().apply {
                val iconSize = 10
                val toLightMode = darkMode_sun.of(iconSize, iconSize)
                val toDarkMode = darkMode_moon.of(iconSize, iconSize)
                val syncIconUpdater = Runnable {
                    @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
                    icon = when (manager.preferredThemeStyle.colorToneRule) {
                        ColorToneRule.DARK -> {
                            this.putClientProperty(TO_APPEARANCE, TO_LIGHT_LAF)
                            toLightMode
                        }

                        ColorToneRule.LIGHT -> {
                            this.putClientProperty(TO_APPEARANCE, TO_DARK_LAF)
                            toDarkMode
                        }
                    }
                }
                manager.addListener { syncIconUpdater.run() }
                addPropertyChangeListener("enabled") { syncIconUpdater.run() }
                syncIconUpdater.run()
                addActionListener {
                    toggleSyncWithOS(false)
                    icon = when (getClientProperty(TO_APPEARANCE).toString()) {
                        TO_DARK_LAF -> {
                            FlatDarculaLaf.setup()
                            Colors.setDarkMode(true)
                            putClientProperty(TO_APPEARANCE, TO_LIGHT_LAF)
                            toLightMode
                        }

                        TO_LIGHT_LAF -> {
                            FlatIntelliJLaf.setup()
                            Colors.setDarkMode(false)
                            putClientProperty(TO_APPEARANCE, TO_DARK_LAF)
                            toDarkMode
                        }

                        else -> throw IllegalStateException("Unknown appearance mode")
                    }
                    AppearanceControl.updateUI()
                    FlatAnimatedLafChange.hideSnapshotWithAnimation()
                }
                isEnabled = false
                isVisible = true
            }
            val syncAppearanceButton = JCheckBox("Sync appearance").apply {
                addActionListener {
                    toggleSyncWithOS(isSelected)
                    appearanceModeButton.isEnabled = !isSelected
                    SYNC_THEME_CHANGER.run()
                }
                isSelected = true
            }

            return JPanel(FlowLayout()).apply {
                add(appearanceModeButton)
                add(syncAppearanceButton)
            }
        }

    @JvmStatic
    @Suppress("MemberVisibilityCanBePrivate")
    fun updateUI() {
        for (window in Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window)
        }
    }
}