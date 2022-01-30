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

import com.github.bric3.fireplace.flamegraph.FrameColorMode;
import com.github.bric3.fireplace.ui.Colors.Palette;
import org.openjdk.jmc.common.IMCFrame.Type;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;

import java.awt.*;
import java.util.regex.Pattern;

public enum JfrFrameColorMode implements FrameColorMode<Node> {
    BY_PACKAGE {
        Pattern runtimePrefixes = Pattern.compile("(java\\.|javax\\.|sun\\.|com\\.sun\\.|com\\.oracle\\.|com\\.ibm\\.)");

        @Override
        public Color getColor(Palette colorPalette, Node frameNode) {
            if (frameNode.isRoot()) {
                return rootNodeColor;
            }
            var frame = frameNode.getFrame();
            if (frame.getType() == Type.UNKNOWN) {
                return undefinedColor;
            }

            var name = frame.getMethod().getType().getPackage().getName();
            if (runtimePrefixes.matcher(name).lookingAt()) {
                return runtimeColor;
            }
            return colorPalette.mapToColor(name);
        }
    },
    BY_MODULE {
        @Override
        public Color getColor(Palette colorPalette, Node frameNode) {
            if (frameNode.isRoot()) {
                return rootNodeColor;
            }
            return colorPalette.mapToColor(frameNode.getFrame().getMethod().getType().getPackage().getModule());
        }
    },
    BY_FRAME_TYPE {
        @Override
        public Color getColor(Palette colorPalette, Node frameNode) {
            if (frameNode.isRoot()) {
                return rootNodeColor;
            }
            switch (frameNode.getFrame().getType()) {
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
    public static Color runtimeColor = new Color(34, 107, 232);
    public static Color undefinedColor = new Color(108, 163, 189);
    public static Color jitCompiledColor = new Color(21, 110, 64);
    public static Color inlinedColor = Color.pink;
    public static Color interpretedColor = Color.orange;
}
