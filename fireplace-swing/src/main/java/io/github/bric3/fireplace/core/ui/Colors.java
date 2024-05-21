/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.core.ui;

import io.github.bric3.fireplace.flamegraph.ColorMapper;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Various color related utilities.
 */
@SuppressWarnings("unused")
public class Colors {
    /**
     * Hue value position, in HSLA components.
     *
     * @see #hslaComponents(Color)
     */
    public static final int H = 0;
    /**
     * Saturation value position, in HSLA components.
     *
     * @see #hslaComponents(Color)
     */
    public static final int S = 1;
    /**
     * Luminance value position, in HSLA components.
     *
     * @see #hslaComponents(Color)
     */
    public static final int L = 2;

    /**
     * Alpha value position, in HSLA components.
     *
     * @see #hslaComponents(Color)
     */
    public static final int ALPHA = 3;
    private static volatile boolean darkMode = false;

    /**
     * Perceived brightness threshold between dark and light (between 0 and 255).
     *
     * <p>
     * Subtract 0.05 from 0.5, because WCAG (Web Content Accessibility Guidelines)
     * contrast ratio is defined as (L1 + 0.05) / (L2 + 0.05), where L1 and L2
     * are brightness scores. 0.05 stands for minimum brightness of the screen
     * which is never truly black due to highlighting.
     * See <a href="https://www.w3.org/TR/WCAG21/#dfn-relative-luminance">WCAG 2.1</a>
     * </p>
     */
    public static final int DARK_PERCEIVED_BRIGHTNESS_THRESHOLD = gammaFunction(0.45);

    public static final Color blue = new LightDarkColor(Color.decode("#2B4EFF"), Color.decode("#39ACE7"));

    /**
     * Color BLACK with alpha 0xD0.
     */
    public static Color translucent_black_D0 = new Color(0xD0000000, true);

    /**
     * Color BLACK with alpha 0xB0.
     */
    public static final Color translucent_black_B0 = new Color(0xB0000000, true);

    /**
     * Color BLACK with alpha 0x80.
     */
    public static final Color translucent_black_80 = new Color(0x80000000, true);

    /**
     * Color BLACK with alpha 0x60.
     */
    public static Color translucent_black_60 = new Color(0x60000000, true);

    /**
     * Color BLACK with alpha 0x40.
     */
    public static final Color translucent_black_40 = new Color(0x40000000, true);

    /**
     * Color BLACK with alpha 0x20.
     */
    public static Color translucent_black_20 = new Color(0x20000000, true);

    /**
     * Color BLACK with alpha 0x10.
     */
    public static final Color translucent_black_10 = new Color(0x10000000, true);

    /**
     * Color WHITE with alpha 0xD0.
     */
    public static final Color translucent_white_D0 = new Color(0xD0FFFFFF, true);

    /**
     * Color WHITE with alpha 0xB0.
     */
    public static final Color translucent_white_B0 = new Color(0xB0FFFFFF, true);

    /**
     * Color WHITE with alpha 0x80.
     */
    public static final Color translucent_white_80 = new Color(0x80FFFFFF, true);

    /**
     * Color WHITE with alpha 0x60.
     */
    public static Color translucent_white_60 = new Color(0x60FFFFFF, true);

    /**
     * Color WHITE with alpha 0x40.
     */
    public static Color translucent_white_40 = new Color(0x40FFFFFF, true);

    /**
     * Color WHITE with alpha 0x20.
     */
    public static Color translucent_white_20 = new Color(0x20FFFFFF, true);

    /**
     * Color WHITE with alpha 0x10.
     */
    public static Color translucent_white_10 = new Color(0x10FFFFFF, true);

    /**
     * Panel background color, from the current LaF (updated on dark mode change).
     */
    public static Color panelBackground = UIManager.getColor("Panel.background");
    /**
     * Panel foreground color, from the current LaF (updated on dark mode change).
     */
    public static Color panelForeground = UIManager.getColor("Panel.foreground");

    /**
     * Refresh the colors, usually done when the LaF changes.
     */
    public static void refreshColors() {
        panelBackground = UIManager.getColor("Panel.background");
        panelForeground = UIManager.getColor("Panel.foreground");
    }

    /**
     * Returns {@code true} if dark mode is active.
     *
     * @return Whether the dark mode is currently active
     */
    public static boolean isDarkMode() {
        return darkMode;
    }

    /**
     * Sets the flag that controls whether dark mode is active.
     *
     * @param darkMode The dark mode toggle
     */
    public static void setDarkMode(boolean darkMode) {
        Colors.darkMode = darkMode;
        refreshColors();
    }

    private Colors() {
        // no need to instantiate this class
    }

    /**
     * Preset color palettes.
     * This class implements {@link ColorMapper} and use the hashcode of the object
     * to assign a color.
     *
     * @see ColorMapper
     */
    public enum Palette {
        // use https://color.hailpixel.com/
        // https://www.toptal.com/designers/colourcode/

        /**
         * Light theme, black to yellow color palette.
         */
        LIGHT_BLACK_TO_YELLOW("#03071e", "#370617", "#6a040f", "#9d0208", "#d00000", "#dc2f02", "#e85d04", "#f48c06", "#faa307", "#ffba08"),

        /**
         * Light theme, red to blue color palette.
         */
        LIGHT_RED_TO_BLUE("#f94144", "#f3722c", "#f8961e", "#f9c74f", "#90be6d", "#43aa8b", "#577590"),

        /**
         * Light theme, violet to orange color palette.
         */
        LIGHT_VIOLET_TO_ORANGE("#54478c", "#2c699a", "#048ba8", "#0db39e", "#16db93", "#83e377", "#b9e769", "#efea5a", "#f1c453", "#f29e4c"),

        /**
         * Light theme, black to beige, to red color palette.
         */
        LIGHT_BLACK_TO_BEIGE_TO_RED("#001219", "#005f73", "#0a9396", "#94d2bd", "#e9d8a6", "#ee9b00", "#ca6702", "#bb3e03", "#ae2012", "#9b2226"),

        /**
         * Dark theme, black to slate color palette.
         */
        DARK_BLACK_TO_SLATE("#000000", "#241327", "#301934", "#3e1f3d", "#4c2445", "#5a2a4d", "#682f55", "#46324c", "#353347", "#243442"),

        /**
         * Dark theme, green to violet color palette.
         */
        DARK_GREENY_TO_VIOLET("#006466", "#065a60", "#0b525b", "#144552", "#1b3a4b", "#212f45", "#272640", "#312244", "#3e1f47", "#4d194d"),

        /**
         * Dark light color palette.
         */
        DARK_LIGHT("#E91E63", "#C2185B", "#9C27B0", "#5727B0", "#272AB0", "#2768B0", "#57ACDC", "#57DCBE", "#60C689"),

        /**
         * Dark custom color palette.
         */
        DARK_CUSTOM("#54B03B", "#2D8684", "#C2AB47", "#66CCB9", "#A1DD98", "#9D5B34", "#A190DA", "#623BB0", "#CF776E", "#DDD598", "#F2E30D"),

        /**
         * Dark rainbow color palette.
         */
        DARK_BLUE_GREEN_ORANGE_RED(
                /*blue*/ "#178FFF", "#45A5FF", "#73BCFF", "#A2D2FF",
                /*green*/ "#15CB49", "#44D566D", "#73E092", "#73E092",
                /*orange*/ "#FF9E2E", "#FFB157", "#FFC581", "#FFD9AB",
                /*red*/ "#F74241", "#F96767", "#FA8D8D", "#FCB3B3",
                /*yellow*/ "#FFE8B3", "#FFF2D9", "#FFF5E3", "#FFF8F6",
                /*violet*/ "#8C2DFF", "#B14BFF", "#D069FF", "#E891FF"
        ),

        /**
         * Light rainbow color palette.
         */
        LIGHT_BLUE_GREEN_ORANGE_RED(
                /*blue*/ "#003565", "#004F99", "#1272CB", "#0084FF",
                /*green*/ "#014A15", "#00701F", "#009529", "#00BA34",
                /*orange*/ "#643600", "#955000", "#C76B00", "#F98600",
                /*red*/ "#5D1113", "#8C1A19", "#BA2323", "#E92C2B",
                /*yellow*/ "#FFD966", "#FFE8A0", "#FFF0C6", "#FFF4E0",
                /*violet*/ "#5D00A8", "#9E00C9", "#D300E9", "#FF00FF"
        ),

        /**
         * Datadog color palette.
         */
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

        /**
         * Pyroscope color palette.
         */
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
            checkThat(palette.length == 0, "Invalid color palette: " + Arrays.toString(hexacodes));
        }

        Palette(Color... palette) {
            this.palette = palette;
        }

        /**
         * Returns the colors for the palette.
         *
         * @return The colors.
         */
        public Color[] colors() {
            // Maybe make a defensive copy of the array?
            return palette;
        }
    }

    /**
     * Pick a foreground color based on the perceived luminance of the
     * background color and on the dark mode.
     *
     * <p>
     * Assumes an sRGB color space.
     * </p>
     *
     * @param backgroundColor The background color.
     * @return The foreground color.
     */
    public static Color foregroundColor(Color backgroundColor) {
        // sRGB luminance(Y) values
        boolean isBright = isBright(backgroundColor);
        return isBright ?
               (darkMode ? Colors.panelBackground : Colors.panelForeground) :
               Color.white;
    }

    /**
     * Indicate whether a color is bright based on the perceived luminance of the color.
     *
     * <p>
     * Assumes an sRGB color space.
     * </p>
     *
     * @param color The color
     * @return true if bright, otherwise false
     */
    public static boolean isBright(Color color) {
        var brightness = brightness(color);
        //noinspection UnnecessaryLocalVariable
        var isBright = brightness >= DARK_PERCEIVED_BRIGHTNESS_THRESHOLD;
        return isBright;
    }

    /**
     * Produce a new color with the given alpha value.
     * 
     * @param color The color
     * @param alpha The alpha value [0; 1]
     * @return The new color
     */
    public static Color withAlpha(Color color, float alpha) {
        return withAlpha(color, (int) (alpha * 255));
    }

    /**
     * Produce a new color with the given alpha value.
     *
     * @param color The color
     * @param alpha The alpha value [0; 255]
     * @return The new color
     */
    public static Color withAlpha(Color color, int alpha) {
        Objects.checkIndex(alpha, 256); // "Alpha value must be between 0 and 255"
        return new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                alpha
        );
    }

    /**
     * Computes the perceived brightness of a color
     * <p>
     * See <a href="https://stackoverflow.com/questions/596216/formula-to-determine-perceived-brightness-of-rgb-color">Perceived brightness on StackOverflow</a>
     * </p>
     *
     * <p>
     * Assuming the given color is in the sRGB color space, it translates colors
     * into linear-lighting RGB color space, undoing the gamma encoding of sRGB space.
     * Then it multiplies each channel with a coefficient to take into account human perception of brightness.
     * Also see <a href="https://www.w3.org/TR/WCAG21/#dfn-relative-luminance">W3C doc</a>
     * </p>
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
        if (c <= 0.03928) {
            return c / 12.92;
        } else {
            return Math.pow((c + 0.055) / 1.055, 2.4);
        }
    }

    /**
     * Mix two colors.
     *
     * @param c0 Color A
     * @param c1 Color B
     * @return The blended color
     */
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

    /**
     * Dim the given color and returns a {@link #darkMode} aware color.
     *
     * @param color The color to dim
     * @return The dimmed color ({@link #darkMode} aware)
     * @see LightDarkColor
     */
    public static Color dim(Color color) {
        var hslaLight = hslaComponents(color);
        var hslaDark = Arrays.copyOf(hslaLight, hslaLight.length);

        {
            // dark mode
            // if color is grayish, keep the saturation, otherwise set it to 0.2
            hslaDark[S] = hslaDark[S] < 0.1f ? hslaDark[S] : 0.2f;
            hslaDark[L] = 0.2f;
        }
        {
            // light mode
            // if color is grayish, keep the saturation, otherwise set it to 0.4
            hslaLight[S] = hslaLight[S] < 0.2 ? hslaLight[S] : 0.4f;
            hslaLight[L] = 0.93f;
        }

        return new LightDarkColor(
                hsla(hslaLight[H], hslaLight[S], hslaLight[L], hslaLight[ALPHA]),
                hsla(hslaDark[H], hslaDark[S], hslaDark[L], hslaDark[ALPHA])
        );
    }

    /**
     * Half-Dim the given color and returns a {@link #darkMode} aware color.
     *
     * @param color The color to half-dim
     * @return The half-dimmed color ({@link #darkMode} aware)
     */
    public static Color halfDim(Color color) {
        var hslaLight = hslaComponents(color);
        var hslaDark = Arrays.copyOf(hslaLight, hslaLight.length);

        {
            // dark mode
            // if color is grayish, keep the saturation, otherwise set it to 0.2
            hslaDark[S] = hslaDark[S] < 0.1f ? hslaDark[S] : 0.2f;
            hslaDark[L] = 0.48f;
        }
        {
            // light mode
            // if color is grayish, keep the saturation, otherwise set it to 0.4
            hslaLight[S] = hslaLight[S] < 0.2 ? hslaLight[S] : 0.4f;
            hslaLight[L] = 0.68f;
        }

        return new LightDarkColor(
                hsla(hslaLight[H], hslaLight[S], hslaLight[L], hslaLight[ALPHA]),
                hsla(hslaDark[H], hslaDark[S], hslaDark[L], hslaDark[ALPHA])
        );
    }

    private static final float DARKER_FACTOR = 0.7f;
    private static final float BRIGHTER_FACTOR = 1 / DARKER_FACTOR;

    /**
     * Brighten the color by lowering the luminance.
     *
     * <p>
     * This differs from {@link Color#brighter()} as it computes
     * the value in the HSL space, also this method takes a {@code factor},
     * which is similar to applying {@link Color#brighter()} multiple times,
     * but without creating new objects.
     * </p>
     *
     * @param color The color to brighten.
     * @param k     The factor, min value 0.
     * @return The brightened color.
     * @see #darker(Color, float)
     */
    public static Color brighter(Color color, float k) {
        return brighter(color, k, 1f);
    }

    /**
     * Brighten the color by lowering the luminance.
     *
     * <p>
     * This differs from {@link Color#brighter()} as it computes
     * the value in the HSL space, also this method takes a {@code factor},
     * which is similar to applying {@link Color#brighter()} multiple times,
     * but without creating new objects.
     * </p>
     *
     * @param color        The color to brighten.
     * @param k            The factor, min value 0.
     * @param maxLuminance The higher bound for the luminance [0; 1].
     * @return The brightened color.
     * @see #darker(Color, float, float)
     */
    public static Color brighter(Color color, float k, float maxLuminance) {
        checkThat(k < 0, "k must be positive");
        checkThat(maxLuminance < 0 || maxLuminance > 1, "maxLuminance [0; 1], actual: " + maxLuminance);
        double factor = k == 0f ? BRIGHTER_FACTOR : Math.pow(BRIGHTER_FACTOR, k);
        var hsla = hslaComponents(color);
        return hsla(
                hsla[H],
                hsla[S],
                Math.min((float) (hsla[L] * factor), maxLuminance),
                hsla[ALPHA]
        );
    }

    /**
     * Darken the color by lowering the luminance.
     *
     * <p>
     * This differs from {@link Color#darker()} as it computes
     * the value in the HSL space, also this method takes a {@code factor},
     * which is similar to applying {@link Color#darker()}multiple times,
     * but without creating new objects.
     * </p>
     *
     * @param color The color to darken.
     * @param k     The factor, min value 0.
     * @return The darkened color.
     * @see #brighter(Color, float)
     */
    public static Color darker(Color color, float k) {
        return darker(color, k, 0f);
    }

    /**
     * Darken a color by lowering the luminance.
     *
     * <p>
     * This differs from {@link Color#darker()} as it computes
     * the value in the HSL space, also this method takes a {@code factor},
     * which is similar to applying {@link Color#darker()}multiple times,
     * but without creating new objects.
     * </p>
     *
     * @param color        The color to darken.
     * @param k            The factor, min value 0.
     * @param minLuminance The lower bound for the luminance [0; 1].
     * @return The darkened color.
     * @see #brighter(Color, float, float)
     */
    public static Color darker(Color color, float k, float minLuminance) {
        checkThat(k < 0, "k must be positive");
        checkThat(minLuminance < 0 || minLuminance > 1, "maxLuminance [0; 1], actual: " + minLuminance);
        double factor = k == 0f ? DARKER_FACTOR : Math.pow(DARKER_FACTOR, k);
        var hsla = hslaComponents(color);
        return hsla(
                hsla[H],
                hsla[S],
                Math.max((float) (hsla[L] * factor), minLuminance),
                hsla[ALPHA]
        );
    }

    /**
     * Convert an RGB Color to its corresponding HSL components and alpha channel.
     * <p>
     * From <a href="https://github.com/d3/d3-color/blob/958249d3a17aaff499d2a9fc9a0f7b8b8e8a47c8/src/color.js">d3-colors</a>.
     *
     * @param color The color to convert
     * @return an array containing the 3 HSL values, and the alpha channel.
     * @see #H
     * @see #S
     * @see #L
     * @see #ALPHA
     */
    public static float[] hslaComponents(Color color) {
        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float alpha = color.getAlpha() / 255.0f;

        float min = Math.min(r, Math.min(g, b));
        float max = Math.max(r, Math.max(g, b));
        float h = 0f;
        float s = max - min;
        float l = (max + min) / 2;
        if (s >= 0) {
            if (r == max) {
                h = (g - b) / s + ((g < b) ? 6 : 0);
            } else if (g == max) {
                h = (b - r) / s + 2;
            } else {
                h = (r - g) / s + 4;
            }
            s /= l < 0.5 ? max + min : 2 - max - min;
            h *= 60;
        } else {
            s = ((l > 0) && (l < 1)) ? 0 : h;
        }

        return new float[]{h, s, l, alpha};
    }

    /**
     * Convert HSL values (with alpha channel) to an RGB Color.
     * <p>
     * From <a href="https://github.com/d3/d3-color/blob/958249d3a17aaff499d2a9fc9a0f7b8b8e8a47c8/src/color.js">d3-colors</a>.
     *
     * @param h     Hue is specified as degrees in the range [0; 360].
     * @param s     Saturation is specified as a percentage in the range [0; 1].
     * @param l     Luminance is specified as a percentage in the range [0; 1].
     * @param alpha the alpha value between [0; 1]
     * @return the RGB Color object
     */
    public static Color hsla(float h, float s, float l, float alpha) {
        h = h % 360 + ((h < 0) ? 360 : 0);
        s = Float.isNaN(h) || Float.isNaN(s) ? 0 : s;

        checkThat(s < 0.0f || s > 1.0f, "Saturation [0; 1], actual: " + s);
        checkThat(l < 0.0f || l > 1.0f, "Luminance [0; 1], actual: " + l);
        checkThat(alpha < 0.0f || alpha > 1.0f, "Alpha [0; 1], actual: " + alpha);

        float m2 = l + (l < 0.5 ? l : 1 - l) * s;
        float m1 = 2 * l - m2;

        return new Color(
                ((int) (alpha * 255)) << 24
                | hueToRgb(h >= 240 ? h - 240 : h + 120, m1, m2) << 16
                | hueToRgb(h, m1, m2) << 8
                | hueToRgb(h < 120 ? h + 240 : h - 120, m1, m2)

        );
    }

    /* From FvD 13.37, CSS Color Module Level 3 */
    private static int hueToRgb(float h, float m1, float m2) {
        return (int) (((h < 60) ? (m1 + (m2 - m1) * h / 60)
                                : ((h < 180) ? m2
                                             : ((h < 240) ? (m1 + (m2 - m1) * (240 - h) / 60)
                                                          : m1
                                   )
                       )
                      ) * 255);
    }

    public static Color rgba(int r, int g, int b, float a) {
        return new Color(r, g, b, (int) (a * 255));
    }

    public static String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static void checkThat(boolean invalid, String message) {
        if (invalid) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Utility method to print out the colors for the look and feel defaults.
     */
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
