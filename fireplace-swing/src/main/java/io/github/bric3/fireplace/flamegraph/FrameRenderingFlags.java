/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.flamegraph;

import java.util.StringJoiner;

/**
 * Flags that can be used to alter some elements of the frame rendering.
 *
 * <p>
 *     In particular these flags will be passed to strategies like {@link FrameFontProvider}.
 *     <strong>Note these flags are currently incubating.</strong>
 * </p>
 *
 *
 * @see FrameFontProvider
 */
public abstract class FrameRenderingFlags {
    /**
     * Indicate the renderer is actually rendering a minimap.
     */
    public static final int MINIMAP_MODE = 1;

    /**
     * The renderer is currently highlighting some frames
     */
    public static final int HIGHLIGHTING = 1 << 1;

    /**
     * The renderer is currently rendering a highlighted frame.
     */
    public static final int HIGHLIGHTED_FRAME = 1 << 2;

    /**
     * The renderer is currently rendering a hovered frame
     */
    public static final int HOVERED = 1 << 3;

    /**
     * The renderer is currently rendering the sibling of a hovered frame
     */
    public static final int HOVERED_SIBLING = 1 << 4;

    /**
     * The renderer is currently focusing some frames (a "sub-flame")
     */
    public static final int FOCUSING = 1 << 5;

    /**
     * The renderer is currently rendering a focused frame.
     */
    public static final int FOCUSED_FRAME = 1 << 6;

    /**
     * The renderer is currently rendering a partial frame, e.g. it is larger
     * that the painting area.
     */
    public static final int PARTIAL_FRAME = 1 << 7;


    public static int toFlags(
            boolean minimapMode,
            boolean highlightingOn,
            boolean highlighted,
            boolean hovered,
            boolean hoveredSibling,
            boolean focusing,
            boolean focusedFrame,
            boolean partialFrame
    ) {
        return (minimapMode ? MINIMAP_MODE : 0)
               | (highlightingOn ? HIGHLIGHTING : 0)
               | (highlighted ? HIGHLIGHTED_FRAME : 0)
               | (hovered ? HOVERED : 0)
               | (hoveredSibling ? HOVERED_SIBLING : 0)
               | (focusing ? FOCUSING : 0)
               | (focusedFrame ? FOCUSED_FRAME : 0)
               | (partialFrame ? PARTIAL_FRAME : 0);
    }

    public static String toString(int flags) {
        var sb = new StringJoiner(", ", "[", "]");
        if ((flags & MINIMAP_MODE) != 0) sb.add("minimapMode");
        if ((flags & HIGHLIGHTING) != 0) sb.add("highlighting");
        if ((flags & HIGHLIGHTED_FRAME) != 0) sb.add("highlighted");
        if ((flags & HOVERED) != 0) sb.add("hovered");
        if ((flags & HOVERED_SIBLING) != 0) sb.add("hovered sibling");
        if ((flags & FOCUSING) != 0) sb.add("focusing");
        if ((flags & FOCUSED_FRAME) != 0) sb.add("focused");
        if ((flags & PARTIAL_FRAME) != 0) sb.add("partial");
        return sb.toString();
    }

    public static boolean isMinimapMode(int flags) {
        return (flags & MINIMAP_MODE) != 0;
    }

    public static boolean isHighlighting(int flags) {
        return (flags & HIGHLIGHTING) != 0;
    }

    public static boolean isHighlightedFrame(int flags) {
        return (flags & HIGHLIGHTED_FRAME) != 0;
    }

    public static boolean isHovered(int flags) {
        return (flags & HOVERED) != 0;
    }

    public static boolean isHoveredSibling(int flags) {
        return (flags & HOVERED_SIBLING) != 0;
    }

    public static boolean isFocusing(int flags) {
        return (flags & FOCUSING) != 0;
    }

    public static boolean isFocusedFrame(int flags) {
        return (flags & FOCUSED_FRAME) != 0;
    }

    public static boolean isPartialFrame(int flags) {
        return (flags & PARTIAL_FRAME) != 0;
    }


    private FrameRenderingFlags() {}
}
