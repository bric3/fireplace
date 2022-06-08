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

public interface FrameRender<T> {

    class DefaultFrameRender<T> implements FrameRender<T> {
        /**
         * The font used to display frame labels
         */
        private final Font frameLabelFont;

        /**
         * If a frame is clipped, we'll shift the label to make it visible but show it with
         * a modified (italicised by default) font to highlight that the frame is only partially
         * visible.
         */
        private final Font partialFrameLabelFont;

        /**
         * The font used to display frame labels
         */
        private final Font highlightedFrameLabelFont;

        /**
         * If a frame is clipped, we'll shift the label to make it visible but show it with
         * a modified (italicised by default) font to highlight that the frame is only partially
         * visible.
         */
        private final Font highlightedPartialFrameLabelFont;

        /**
         * The color used to draw frames that are highlighted.
         */
        private final Color highlightedColor;


        public DefaultFrameRender() {
            this.frameLabelFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
            this.partialFrameLabelFont = new Font(Font.SANS_SERIF, Font.ITALIC, 12);
            this.highlightedFrameLabelFont = new Font(Font.SANS_SERIF, Font.PLAIN | Font.BOLD, 12);
            this.highlightedPartialFrameLabelFont = new Font(Font.SANS_SERIF, Font.ITALIC | Font.BOLD, 12);
            this.highlightedColor = new Color(0xFFFFE771, true);
        }

        public Font getFrameLabelFont() {
            return this.frameLabelFont;
        }

        public Font getPartialFrameLabelFont() {
            return partialFrameLabelFont;
        }

        public Font getHighlightedFrameLabelFont() {
            return highlightedFrameLabelFont;
        }

        public Font getHighlightedPartialFrameLabelFont() {
            return highlightedPartialFrameLabelFont;
        }
    }

    Font getFrameLabelFont();
    Font getPartialFrameLabelFont();
    Font getHighlightedFrameLabelFont();
    Font getHighlightedPartialFrameLabelFont();
}
