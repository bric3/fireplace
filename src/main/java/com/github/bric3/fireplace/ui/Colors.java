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

import java.awt.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

public class Colors {
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


        ;

        private final Color[] palette;

        Palette(String... hexacodes) {
            palette = Arrays.stream(hexacodes)
                            .flatMap(hex -> Pattern.compile("[-, ]").splitAsStream(hex))
                            .map(s -> s.charAt(0) == '#' ? s : "#" + s)
                            .map(Color::decode)
                            .toArray(Color[]::new);
            if (palette.length == 0) {
                throw new IllegalArgumentException("Invalid color palette: " + hexacodes);
            }

            var fgColorPalette = Arrays.stream(palette)
                                       .map(Colors::foregroundColor)
                                       .toArray(Color[]::new);
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

        return brightness < 128 ? Color.WHITE : Color.darkGray;
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
}
