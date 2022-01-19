/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.github.bric3;

import java.awt.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

public enum ColorPalette {
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


    ;

    private final Color[] palette;

    ColorPalette(String... hexacodes) {
        palette = Arrays.stream(hexacodes)
                        .flatMap(hex -> Pattern.compile("[-, ]").splitAsStream(hex))
                        .map(s -> s.charAt(0) == '#' ? s : "#" + s)
                        .map(Color::decode)
                        .toArray(Color[]::new);
        if (palette.length == 0) {
            throw new IllegalArgumentException("Invalid color palette: " + hexacodes);
        }

        var fgColorPalette = Arrays.stream(palette)
                                   .map(ColorPalette::foregroundColor)
                                   .toArray(Color[]::new);
    }

    public Color mapToColor(Object value) {
        return value == null ?
               palette[0] :
               palette[Math.abs(Objects.hashCode(value)) % palette.length];
    }


    public static Color foregroundColor(Color backgroundColor) {
        // https://stackoverflow.com/questions/596216/formula-to-determine-perceived-brightness-of-rgb-color

        // sRGB luminance(Y) values
        var brightness = brightness(backgroundColor);

        return brightness < 128 ? Color.WHITE : Color.darkGray;


//        // From http://alienryderflex.com/hsp.html
//        // This coce computes the perceived brightness of a color
//        //
//        //     brightness  =  sqrt( .299 R2 + .587 G2 + .114 B2 )
//        //
//        // The three constants (.299, .587, and .114) represent the different degrees
//        // to which each of the primary (RGB) colors affects human perception of the overall
//        // brightness of a color.
//        // Notice that they sum to 1.
//
//        var hsp = Math.sqrt(0.299 * Math.pow(backgroundColor.getRed(), 2) +
//                            0.587 * Math.pow(backgroundColor.getGreen(), 2) +
//                            0.114 * Math.pow(backgroundColor.getBlue(), 2));
//
//        return hsp > 127.5 ? Color.WHITE : Color.BLACK;
    }


    // https://stackoverflow.com/questions/596216/formula-to-determine-perceived-brightness-of-rgb-color
    public static int brightness(Color backgroundColor) {
        double rY = 0.212655;
        double gY = 0.715158;
        double bY = 0.072187;
        return gammaFunction(
                rY * inverseOfGammaFunction(backgroundColor.getRed()) +
                gY * inverseOfGammaFunction(backgroundColor.getGreen()) +
                bY * inverseOfGammaFunction(backgroundColor.getBlue())
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

    public static void main(String[] args) {
        System.out.println(brightness(Color.BLACK));
        System.out.println(brightness(Color.WHITE));
        System.out.println(brightness(Color.RED));
        System.out.println(brightness(Color.YELLOW));
        System.out.println(brightness(Color.GREEN));
        System.out.println(brightness(Color.GRAY));
        System.out.println(brightness(Color.DARK_GRAY));
    }
}
