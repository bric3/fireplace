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

import java.awt.*;

import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isHighlightedFrame;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isPartialFrame;

/**
 * Strategy for choosing the font of a frame.
 *
 * @param <T> The type of the frame node (depends on the source of profiling data).
 */
@FunctionalInterface
public interface FrameFontProvider<T> {

    /**
     * Returns a font according to the frame and flags parameters.
     *
     * <p>
     * An implementation should return the base font if the <code>frame</code>
     * parameter is <code>null</code>. Possibly honoring the <code>flags</code>.
     * </p>
     *
     * @param frame The frame to get the font for, can be <code>null</code>.
     * @param flags The flags
     * @return The font to use for the frame and flags.
     */
    Font getFont(FrameBox<T> frame, int flags);

    static <T> FrameFontProvider<T> defaultFontProvider() {
        return new FrameFontProvider<T>() {
            /**
             * The font used to display frame labels
             */
            private final Font frameLabelFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

            /**
             * If a frame is clipped, we'll shift the label to make it visible but show it with
             * a modified (italicised by default) font to highlight that the frame is only partially
             * visible.
             */
            private final Font partialFrameLabelFont = new Font(Font.SANS_SERIF, Font.ITALIC, 12);

            /**
             * The font used to display frame labels
             */
            private final Font highlightedFrameLabelFont = new Font(Font.SANS_SERIF, Font.PLAIN | Font.BOLD, 12);

            /**
             * If a frame is clipped, we'll shift the label to make it visible but show it with
             * a modified (italicised by default) font to highlight that the frame is only partially
             * visible.
             */
            private final Font highlightedPartialFrameLabelFont = new Font(Font.SANS_SERIF, Font.ITALIC | Font.BOLD, 12);

            @Override
            public Font getFont(FrameBox<T> frame, int flags) {
                if (isHighlightedFrame(flags)) {
                    // when parent frame are larger than view port
                    return isPartialFrame(flags) ? highlightedPartialFrameLabelFont : highlightedFrameLabelFont;
                }
                // when parent frame are larger than view port
                return isPartialFrame(flags) ? partialFrameLabelFont : frameLabelFont;
            }
        };
    }
}
