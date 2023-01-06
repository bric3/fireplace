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

import io.github.bric3.fireplace.flamegraph.FrameBox;
import org.openjdk.jmc.common.IMCFrame.Type;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;

import java.awt.*;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * A JFR specific color mapping modes.
 */
public enum JfrFrameColorMode {
    BY_PACKAGE {
        final Pattern runtimePrefixes = Pattern.compile("(java\\.|javax\\.|sun\\.|com\\.sun\\.|com\\.oracle\\.|com\\.ibm\\.)");

        @Override
        public Color getJfrNodeColor(Function<Object, Color> colorMapper, Node frameNode) {
            if (frameNode.isRoot()) {
                return rootNodeColor;
            }
            var frame = frameNode.getFrame();
            if (frame.getType() == Type.UNKNOWN) {
                return undefinedColor;
            }

            Type type = frame.getType();
            if (type.equals(Type.NATIVE)
                || type.equals(Type.KERNEL)
                || type.equals(Type.CPP)) {
                return runtimeColor;
            }

            var name = frame.getMethod().getType().getPackage().getName();
            if (name != null && runtimePrefixes.matcher(name).lookingAt()) {
                return runtimeColor;
            }
            return colorMapper.apply(name);
        }
    },
    BY_MODULE {
        @Override
        public Color getJfrNodeColor(Function<Object, Color> colorMapper, Node frameNode) {
            if (frameNode.isRoot()) {
                return rootNodeColor;
            }
            return colorMapper.apply(frameNode.getFrame().getMethod().getType().getPackage().getModule());
        }
    },
    BY_FRAME_TYPE {
        @Override
        public Color getJfrNodeColor(Function<Object, Color> colorMapper, Node frameNode) {
            if (frameNode.isRoot()) {
                return rootNodeColor;
            }
            Type type = frameNode.getFrame().getType();
            if (type.equals(Type.INTERPRETED)) {
                return interpretedColor;
            } else if (type.equals(Type.INLINED)) {
                return inlinedColor;
            } else if (type.equals(Type.JIT_COMPILED)) {
                return jitCompiledColor;
            }
            return undefinedColor;
        }
    };

    public static Color rootNodeColor = new Color(198, 198, 198);
    public static Color runtimeColor = new Color(34, 107, 232);
    public static Color undefinedColor = new Color(108, 163, 189);
    public static Color jitCompiledColor = new Color(21, 110, 64);
    public static Color inlinedColor = Color.pink;
    public static Color interpretedColor = Color.orange;

    protected abstract Color getJfrNodeColor(Function<Object, Color> colorMapper, Node frameNode);

    public Function<FrameBox<Node>, Color> colorMapperUsing(Function<Object, Color> colorMapper) {
        return frameNode -> getJfrNodeColor(colorMapper, frameNode.actualNode);
    }
}
