/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.github.bric3.fireplace.ui;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

public class Colors {
    public static volatile boolean darkMode = false;

    /**
     * Perceived brightness threshold between dark and light.
     * <p>
     * Between 0 and 255
     */
    public static final int DARK_PERCEIVED_BRIGHTNESS_THRESHOLD = 128;
    public static Color translucent_black_D0 = new Color(0xD0000000, true);
    public static Color translucent_black_B0 = new Color(0xB0000000, true);
    public static Color translucent_black_80 = new Color(0x80000000, true);
    public static Color translucent_black_60 = new Color(0x60000000, true);
    public static Color translucent_black_40 = new Color(0x40000000, true);
    public static Color translucent_black_20 = new Color(0x20000000, true);
    public static Color translucent_white_D0 = new Color(0xD0FFFFFF, true);
    public static Color translucent_white_B0 = new Color(0xB0FFFFFF, true);
    public static Color translucent_white_80 = new Color(0x80FFFFFF, true);
    public static Color translucent_white_60 = new Color(0x60FFFFFF, true);
    public static Color translucent_white_40 = new Color(0x40FFFFFF, true);
    public static Color translucent_white_20 = new Color(0x20FFFFFF, true);


    public static Color panelBackGround = UIManager.getColor("Panel.background");
    public static Color panelForeGround = UIManager.getColor("Panel.foreground");

    /**
     * Refresh the colors when the LaF changes
     */
    public static void refreshColors() {
        panelBackGround = UIManager.getColor("Panel.background");
        panelForeGround = UIManager.getColor("Panel.foreground");
    }

    public static void setDarkMode(boolean darkMode) {
        Colors.darkMode = darkMode;
        refreshColors();
    }

    public enum Palette {
        // use https://color.hailpixel.com/
        // https://www.toptal.com/designers/colourcode/

        LIGHT_BLACK_TO_YELLOW("#03071e", "#370617", "#6a040f", "#9d0208", "#d00000", "#dc2f02", "#e85d04", "#f48c06", "#faa307", "#ffba08"),
        LIGHT_RED_TO_BLUE("#f94144", "#f3722c", "#f8961e", "#f9c74f", "#90be6d", "#43aa8b", "#577590"),
        LIGHT_VIOLET_TO_ORANGE("#54478c", "#2c699a", "#048ba8", "#0db39e", "#16db93", "#83e377", "#b9e769", "#efea5a", "#f1c453", "#f29e4c"),
        LIGHT_BLACK_TO_BEIGE_TO_RED("#001219", "#005f73", "#0a9396", "#94d2bd", "#e9d8a6", "#ee9b00", "#ca6702", "#bb3e03", "#ae2012", "#9b2226"),
        DARK_BLACK_TO_SLATE("#000000", "#241327", "#301934", "#3e1f3d", "#4c2445", "#5a2a4d", "#682f55", "#46324c", "#353347", "#243442"),
        DARK_GREENY_TO_VIOLET("#006466", "#065a60", "#0b525b", "#144552", "#1b3a4b", "#212f45", "#272640", "#312244", "#3e1f47", "#4d194d"),
        DARK_LIGHT("#E91E63", "#C2185B", "#9C27B0", "#5727B0", "#272AB0", "#2768B0", "#57ACDC", "#57DCBE", "#60C689"),
        DARK_CUSTOM("#54B03B,2D8684,C2AB47,66CCB9,A1DD98,9D5B34,A190DA,623BB0,CF776E,DDD598,F2E30D"),

        DARK_BLUE_GREEN_ORANGE_RED(
                /*blue*/ "#178FFF", "#45A5FF", "#73BCFF", "#A2D2FF",
                /*green*/ "#15CB49", "#44D566D", "#73E092", "#73E092",
                /*orange*/ "#FF9E2E", "#FFB157", "#FFC581", "#FFD9AB",
                /*red*/ "#F74241", "#F96767", "#FA8D8D", "#FCB3B3",
                /*yellow*/ "#FFE8B3", "#FFF2D9", "#FFF5E3", "#FFF8F6",
                /*violet*/ "#8C2DFF", "#B14BFF", "#D069FF", "#E891FF"
        ),
        LIGHT_BLUE_GREEN_ORANGE_RED(
                /*blue*/ "#003565", "#004F99", "#1272CB", "#0084FF",
                /*green*/ "#014A15", "#00701F", "#009529", "#00BA34",
                /*orange*/ "#643600", "#955000", "#C76B00", "#F98600",
                /*red*/ "#5D1113", "#8C1A19", "#BA2323", "#E92C2B",
                /*yellow*/ "#FFD966", "#FFE8A0", "#FFF0C6", "#FFF4E0",
                /*violet*/ "#5D00A8", "#9E00C9", "#D300E9", "#FF00FF"
        ),

        DATADOG(
                new Color(0x3399CC),
                new Color(0x927FB9),
                new Color(0xFFCC00),
                new Color(0x57B79A),
                new Color(0xBE53BB),
                new Color(0xDD8451),
                new Color(0x3969B3),
                new Color(0xBED017),
                new Color(0x8934A4),
                new Color(0x3BCBCB),
                new Color(0x6E69CC),
                new Color(0x50931F),
                new Color(0xC86B74),
                new Color(0xFCAF2B),
                new Color(0x2EB0DE),
                new Color(0xC68CCD),
                new Color(0x457557),
                new Color(0xCC3C71),
                new Color(0x985083),
                new Color(0xA7B342)
        ),

        PYROSCOPE(
                new Color(0xDF8B53),
                new Color(0xE0AD6C),
                new Color(0x68B7CF),
                new Color(0x59C0A3),
                new Color(0x6897CA),
                new Color(0x8982C9),
                new Color(0xEBA8E6),
                new Color(0xFFE175),

                new Color(0xb7dbab),
                new Color(0xf4d598),
                new Color(0x70dbed),
                new Color(0xf9ba8f),
                new Color(0xf29191),
                new Color(0x82b5d8),
                new Color(0xe5a8e2),
                new Color(0xaea2e0),
                new Color(0x9ac48a),
                new Color(0xf2c96d),
                new Color(0x65c5db),
                new Color(0xf9934e),
                new Color(0xea6460),
                new Color(0x5195ce),
                new Color(0xd683ce),
                new Color(0x806eb7)
        );

        private final Color[] palette;

        Palette(String... hexacodes) {
            palette = Arrays.stream(hexacodes)
                            .flatMap(hex -> Pattern.compile("[-, ]").splitAsStream(hex))
                            .map(s -> s.charAt(0) == '#' ? s : "#" + s)
                            .map(Color::decode)
                            .toArray(Color[]::new);
            if (palette.length == 0) {
                throw new IllegalArgumentException("Invalid color palette: " + Arrays.toString(hexacodes));
            }
        }

        Palette(Color... palette) {
            this.palette = palette;
        }

        public Color mapToColor(Object value) {
            return value == null ?
                   palette[0] :
                   palette[Math.abs(Objects.hashCode(value)) % palette.length];
        }
    }

    public static Color foregroundColor(Color backgroundColor) {
        // sRGB luminance(Y) values
        var brightness = brightness(backgroundColor);

        return brightness < DARK_PERCEIVED_BRIGHTNESS_THRESHOLD ?
               Color.white :
               darkMode ? Colors.panelBackGround : Colors.panelForeGround;
    }

    /**
     * Perceived brightness of a color
     * <p>
     * See <a href="https://stackoverflow.com/questions/596216/formula-to-determine-perceived-brightness-of-rgb-color">Perceived brightness on StackOverflow</a>
     *
     * @param color The color
     * @return The perceived brightness between 0 and 255
     */
    public static int brightness(Color color) {
        double rY = 0.212655;
        double gY = 0.715158;
        double bY = 0.072187;
        return gammaFunction(
                rY * inverseOfGammaFunction(color.getRed()) +
                gY * inverseOfGammaFunction(color.getGreen()) +
                bY * inverseOfGammaFunction(color.getBlue())
        );
    }

    private static int gammaFunction(double v) {
        if (v <= 0.0031308) {
            v *= 12.92;
        } else {
            v = 1.055 * Math.pow(v, 1 / 2.4) - 0.055;
        }
        return (int) Math.round(v * 255);
    }

    private static double inverseOfGammaFunction(int ic) {
        double c = ic / 255.0;
        if (c <= 0.04045) {
            return c / 12.92;
        } else {
            return Math.pow((c + 0.055) / 1.055, 2.4);
        }
    }

    public static Color blend(Color c0, Color c1) {
        double totalAlpha = c0.getAlpha() + c1.getAlpha();
        double weight0 = c0.getAlpha() / totalAlpha;
        double weight1 = c1.getAlpha() / totalAlpha;

        double r = weight0 * c0.getRed() + weight1 * c1.getRed();
        double g = weight0 * c0.getGreen() + weight1 * c1.getGreen();
        double b = weight0 * c0.getBlue() + weight1 * c1.getBlue();
        double a = Math.max(c0.getAlpha(), c1.getAlpha());

        return new Color((int) r, (int) g, (int) b, (int) a);
    }

    public static void printLafColorProperties() {
        UIManager.getLookAndFeelDefaults()
                 .entrySet()
                 .stream()
                 .filter(e -> e.getValue() instanceof Color)
                 .map(e -> e.getKey() + ": " + String.format("#%06X", (0xFFFFFF & ((Color) (e.getValue())).getRGB())))
                 .sorted()
                 .forEach(System.out::println);
    }

    /*
        Button.background: #EEEEEE
        Button.darkShadow: #000000
        Button.disabledText: #808080
        Button.foreground: #000000
        Button.highlight: #FFFFFF
        Button.light: #0749D9
        Button.select: #FF6666
        Button.shadow: #000000
        CheckBox.background: #EEEEEE
        CheckBox.disabledText: #808080
        CheckBox.foreground: #000000
        CheckBox.select: #FF6666
        CheckBoxMenuItem.acceleratorForeground: #000000
        CheckBoxMenuItem.acceleratorSelectionForeground: #000000
        CheckBoxMenuItem.background: #FFFFFF
        CheckBoxMenuItem.disabledBackground: #FFFFFF
        CheckBoxMenuItem.disabledForeground: #808080
        CheckBoxMenuItem.foreground: #000000
        CheckBoxMenuItem.selectionBackground: #074CF1
        CheckBoxMenuItem.selectionForeground: #FFFFFF
        ColorChooser.background: #EEEEEE
        ColorChooser.foreground: #000000
        ColorChooser.swatchesDefaultRecentColor: #FFFFFF
        ComboBox.background: #EEEEEE
        ComboBox.buttonBackground: #FFFFFF
        ComboBox.buttonDarkShadow: #000000
        ComboBox.buttonHighlight: #FFFFFF
        ComboBox.buttonShadow: #000000
        ComboBox.disabledBackground: #FFFFFF
        ComboBox.disabledForeground: #808080
        ComboBox.foreground: #000000
        ComboBox.selectionBackground: #074CF1
        ComboBox.selectionForeground: #FFFFFF
        Desktop.background: #4169AA
        DesktopIcon.borderColor: #000000
        DesktopIcon.borderRimColor: #C0C0C0
        DesktopIcon.labelBackground: #000000
        EditorPane.background: #FFFFFF
        EditorPane.caretForeground: #000000
        EditorPane.foreground: #000000
        EditorPane.inactiveBackground: #FFFFFF
        EditorPane.inactiveForeground: #808080
        EditorPane.selectionBackground: #A5CDFF
        EditorPane.selectionForeground: #000000
        Focus.color: #074CF1
        FormattedTextField.background: #FFFFFF
        FormattedTextField.caretForeground: #000000
        FormattedTextField.foreground: #000000
        FormattedTextField.inactiveBackground: #FFFFFF
        FormattedTextField.inactiveForeground: #808080
        FormattedTextField.selectionBackground: #A5CDFF
        FormattedTextField.selectionForeground: #000000
        InternalFrame.activeTitleBackground: #EEEEEE
        InternalFrame.activeTitleForeground: #000000
        InternalFrame.background: #EEEEEE
        InternalFrame.borderColor: #EEEEEE
        InternalFrame.borderDarkShadow: #00FF00
        InternalFrame.borderHighlight: #0000FF
        InternalFrame.borderLight: #FFFF00
        InternalFrame.borderShadow: #FF0000
        InternalFrame.inactiveTitleBackground: #EEEEEE
        InternalFrame.inactiveTitleForeground: #808080
        InternalFrame.optionDialogBackground: #EEEEEE
        InternalFrame.paletteBackground: #EEEEEE
        Label.background: #EEEEEE
        Label.disabledForeground: #808080
        Label.disabledShadow: #404040
        Label.foreground: #000000
        List.background: #FFFFFF
        List.dropLineColor: #000000
        List.foreground: #000000
        List.selectionBackground: #0749D9
        List.selectionForeground: #FFFFFF
        List.selectionInactiveBackground: #D4D4D4
        List.selectionInactiveForeground: #000000
        Menu.acceleratorForeground: #000000
        Menu.acceleratorSelectionForeground: #000000
        Menu.background: #FFFFFF
        Menu.disabledBackground: #FFFFFF
        Menu.disabledForeground: #808080
        Menu.foreground: #000000
        Menu.selectionBackground: #074CF1
        Menu.selectionForeground: #FFFFFF
        MenuBar.background: #FFFFFF
        MenuBar.disabledBackground: #FFFFFF
        MenuBar.disabledForeground: #808080
        MenuBar.foreground: #000000
        MenuBar.highlight: #FFFFFF
        MenuBar.selectionBackground: #074CF1
        MenuBar.selectionForeground: #FFFFFF
        MenuBar.shadow: #000000
        MenuItem.acceleratorForeground: #000000
        MenuItem.acceleratorSelectionForeground: #000000
        MenuItem.background: #FFFFFF
        MenuItem.disabledBackground: #FFFFFF
        MenuItem.disabledForeground: #808080
        MenuItem.foreground: #000000
        MenuItem.selectionBackground: #074CF1
        MenuItem.selectionForeground: #FFFFFF
        OptionPane.background: #EEEEEE
        OptionPane.foreground: #000000
        OptionPane.messageForeground: #000000
        Panel.background: #EEEEEE
        Panel.foreground: #000000
        PasswordField.background: #FFFFFF
        PasswordField.capsLockIconColor: #000000
        PasswordField.caretForeground: #000000
        PasswordField.foreground: #000000
        PasswordField.inactiveBackground: #FFFFFF
        PasswordField.inactiveForeground: #808080
        PasswordField.selectionBackground: #A5CDFF
        PasswordField.selectionForeground: #000000
        PopupMenu.background: #FFFFFF
        PopupMenu.foreground: #000000
        PopupMenu.selectionBackground: #074CF1
        PopupMenu.selectionForeground: #FFFFFF
        PopupMenu.translucentBackground: #FFFFFF
        ProgressBar.background: #EEEEEE
        ProgressBar.foreground: #000000
        ProgressBar.selectionBackground: #FFFFFF
        ProgressBar.selectionForeground: #000000
        RadioButton.background: #EEEEEE
        RadioButton.darkShadow: #000000
        RadioButton.disabledText: #808080
        RadioButton.foreground: #000000
        RadioButton.highlight: #FFFFFF
        RadioButton.light: #0749D9
        RadioButton.select: #FF6666
        RadioButton.shadow: #000000
        RadioButtonMenuItem.acceleratorForeground: #000000
        RadioButtonMenuItem.acceleratorSelectionForeground: #000000
        RadioButtonMenuItem.background: #FFFFFF
        RadioButtonMenuItem.disabledBackground: #FFFFFF
        RadioButtonMenuItem.disabledForeground: #808080
        RadioButtonMenuItem.foreground: #000000
        RadioButtonMenuItem.selectionBackground: #074CF1
        RadioButtonMenuItem.selectionForeground: #FFFFFF
        ScrollBar.background: #FFFFFF
        ScrollBar.foreground: #000000
        ScrollBar.thumb: #FFFFFF
        ScrollBar.thumbDarkShadow: #000000
        ScrollBar.thumbHighlight: #FFFFFF
        ScrollBar.thumbShadow: #000000
        ScrollBar.track: #9A9A9A
        ScrollBar.trackHighlight: #000000
        ScrollPane.background: #FFFFFF
        ScrollPane.foreground: #000000
        Separator.foreground: #D4D4D4
        Separator.highlight: #FFFFFF
        Separator.shadow: #000000
        Slider.background: #EEEEEE
        Slider.focus: #000000
        Slider.foreground: #000000
        Slider.highlight: #FFFFFF
        Slider.shadow: #000000
        Slider.tickColor: #808080
        Spinner.background: #EEEEEE
        Spinner.foreground: #000000
        SplitPane.background: #EEEEEE
        SplitPane.darkShadow: #000000
        SplitPane.highlight: #FFFFFF
        SplitPane.shadow: #000000
        SplitPaneDivider.draggingColor: #404040
        TabbedPane.background: #EEEEEE
        TabbedPane.darkShadow: #000000
        TabbedPane.focus: #000000
        TabbedPane.foreground: #000000
        TabbedPane.highlight: #FFFFFF
        TabbedPane.light: #0749D9
        TabbedPane.nonSelectedTabTitleNormalColor: #000000
        TabbedPane.selectedTabTitleDisabledColor: #FFFFFF
        TabbedPane.selectedTabTitleNormalColor: #FFFFFF
        TabbedPane.selectedTabTitlePressedColor: #F0F0F0
        TabbedPane.selectedTabTitleShadowDisabledColor: #000000
        TabbedPane.selectedTabTitleShadowNormalColor: #000000
        TabbedPane.shadow: #000000
        Table.background: #FFFFFF
        Table.dropLineColor: #000000
        Table.dropLineShortColor: #000000
        Table.focusCellBackground: #000000
        Table.focusCellForeground: #A5CDFF
        Table.foreground: #000000
        Table.gridColor: #FFFFFF
        Table.selectionBackground: #0749D9
        Table.selectionForeground: #FFFFFF
        Table.selectionInactiveBackground: #D4D4D4
        Table.selectionInactiveForeground: #000000
        Table.sortIconColor: #000000
        TableHeader.background: #FFFFFF
        TableHeader.focusCellBackground: #FFFFFF
        TableHeader.foreground: #000000
        TextArea.background: #FFFFFF
        TextArea.caretForeground: #000000
        TextArea.foreground: #000000
        TextArea.inactiveBackground: #FFFFFF
        TextArea.inactiveForeground: #808080
        TextArea.selectionBackground: #A5CDFF
        TextArea.selectionForeground: #000000
        TextComponent.selectionBackgroundInactive: #D4D4D4
        TextField.background: #FFFFFF
        TextField.caretForeground: #000000
        TextField.darkShadow: #000000
        TextField.foreground: #000000
        TextField.highlight: #FFFFFF
        TextField.inactiveBackground: #FFFFFF
        TextField.inactiveForeground: #808080
        TextField.light: #0749D9
        TextField.selectionBackground: #A5CDFF
        TextField.selectionForeground: #000000
        TextField.shadow: #000000
        TextPane.background: #FFFFFF
        TextPane.caretForeground: #000000
        TextPane.foreground: #000000
        TextPane.inactiveBackground: #FFFFFF
        TextPane.inactiveForeground: #808080
        TextPane.selectionBackground: #A5CDFF
        TextPane.selectionForeground: #000000
        TitledBorder.titleColor: #000000
        ToggleButton.background: #EEEEEE
        ToggleButton.darkShadow: #000000
        ToggleButton.disabledText: #808080
        ToggleButton.foreground: #000000
        ToggleButton.highlight: #FFFFFF
        ToggleButton.light: #0749D9
        ToggleButton.shadow: #000000
        ToolBar.background: #EEEEEE
        ToolBar.borderHandleColor: #8C8C8C
        ToolBar.darkShadow: #000000
        ToolBar.dockingBackground: #EEEEEE
        ToolBar.dockingForeground: #0749D9
        ToolBar.floatingBackground: #EEEEEE
        ToolBar.floatingForeground: #404040
        ToolBar.foreground: #808080
        ToolBar.highlight: #FFFFFF
        ToolBar.light: #0749D9
        ToolBar.shadow: #000000
        ToolTip.background: #FFFFCC
        ToolTip.foreground: #000000
        Tree.background: #FFFFFF
        Tree.dropLineColor: #000000
        Tree.foreground: #000000
        Tree.hash: #FFFFFF
        Tree.line: #FFFFFF
        Tree.selectionBackground: #0749D9
        Tree.selectionBorderColor: #0749D9
        Tree.selectionForeground: #FFFFFF
        Tree.selectionInactiveBackground: #D4D4D4
        Tree.selectionInactiveForeground: #000000
        Tree.textBackground: #FFFFFF
        Tree.textForeground: #000000
        Viewport.background: #FFFFFF
        Viewport.foreground: #000000
        activeCaption: #FFFFFF
        activeCaptionBorder: #FFFFFF
        activeCaptionText: #000000
        control: #EEEEEE
        controlDkShadow: #000000
        controlHighlight: #0749D9
        controlLtHighlight: #FFFFFF
        controlShadow: #000000
        controlText: #000000
        desktop: #22FF06
        inactiveCaption: #6C6C6C
        inactiveCaptionBorder: #6C6C6C
        inactiveCaptionText: #6C6C6C
        info: #FFFFFF
        infoText: #000000
        menu: #FFFFFF
        menuText: #000000
        scrollbar: #9A9A9A
        text: #FFFFFF
        textHighlight: #A5CDFF
        textHighlightText: #000000
        textInactiveText: #000000
        textText: #000000
        window: #EEEEEE
        windowBorder: #9A9A9A
        windowText: #000000
     */
}
