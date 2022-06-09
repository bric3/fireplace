/*
 * Copyright 2021 Datadog, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.bric3.fireplace.flamegraph;

public abstract class FrameRenderingFlags {
    public static int MINIMAP_MODE = 1;
    public static int HIGHLIGHTING = 1 << 1;
    public static int HIGHLIGHTED_FRAME = 1 << 2;
    public static int HOVERED = 1 << 3;
    public static int FOCUSING = 1 << 4;
    public static int FOCUSED_FRAME = 1 << 5;
    public static int PARTIAL_FRAME = 1 << 6;


    public static int toFlags(boolean minimapMode, boolean highlightingOn, boolean highlighted, boolean hovered, boolean focusing, boolean focusedFrame, boolean partialFrame) {
        return (minimapMode ? MINIMAP_MODE : 0)
               | (highlightingOn ? HIGHLIGHTING : 0)
               | (highlighted ? HIGHLIGHTED_FRAME : 0)
               | (hovered ? HOVERED : 0)
               | (focusing ? FOCUSING : 0)
               | (focusedFrame ? FOCUSED_FRAME : 0)
               | (partialFrame ? PARTIAL_FRAME : 0);
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
