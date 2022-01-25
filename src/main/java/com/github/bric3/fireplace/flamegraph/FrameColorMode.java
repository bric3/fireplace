/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.github.bric3.fireplace.flamegraph;

import com.github.bric3.fireplace.ui.Colors.Palette;
import org.openjdk.jmc.common.IMCFrame.Type;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.AggregatableFrame;

import java.awt.*;

public enum FrameColorMode {
    BY_PACKAGE {
        @Override
        public Color getColor(Palette colorPalette, AggregatableFrame frame) {
            if (frame.getType() == Type.UNKNOWN) {
                return undefinedColor;
            }

            return colorPalette.mapToColor(frame.getMethod().getType().getPackage().getName());
        }
    },
    BY_MODULE {
        @Override
        public Color getColor(Palette colorPalette, AggregatableFrame frame) {
            return colorPalette.mapToColor(frame.getMethod().getType().getPackage().getModule());
        }
    },
    BY_FRAME_TYPE {
        @Override
        public Color getColor(Palette colorPalette, AggregatableFrame frame) {
            switch (frame.getType()) {
                case INTERPRETED:
                    return interpretedColor;
                case INLINED:
                    return inlinedColor;
                case JIT_COMPILED:
                    return jitCompiledColor;
                case UNKNOWN:
                default:
                    return undefinedColor;
            }
        }
    };

    public static Color rootNodeColor = new Color(198, 198, 198);
    public static Color undefinedColor = new Color(108, 163, 189);
    public static Color jitCompiledColor = new Color(21, 110, 64);
    public static Color inlinedColor = Color.pink;
    public static Color interpretedColor = Color.orange;

    abstract public Color getColor(Palette colorPalette, AggregatableFrame frame);
}
