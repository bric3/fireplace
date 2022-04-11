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

import java.awt.*;

public enum StringClipper {
    NONE {
        @Override
        public String clipString(Font font, FontMetrics metrics, double availTextWidth, String text, String clipString) {
            return text;
        }
    },
    RIGHT {
        public String clipString(Font font, FontMetrics metrics, double availTextWidth, String text, String clipString) {
            availTextWidth -= font.getStringBounds(clipString, metrics.getFontRenderContext()).getWidth();
            if (availTextWidth <= 0) {
                // can not fit any characters
                return clipString;
            }

            int stringLength = text.length();
            int width = 0;
            for (int nChars = 0; nChars < stringLength; nChars++) {
                width += metrics.charWidth(text.charAt(nChars));
                if (width > availTextWidth) {
                    text = text.substring(0, nChars);
                    break;
                }
            }

            return text + clipString;
        }
    };

    /**
     * A short string to display in place of labels that are too long to fit the
     * available space.
     */
    public static final String LONG_TEXT_PLACEHOLDER = "…";

    public abstract String clipString(Font font, FontMetrics metrics, double availTextWidth, String text, String clipString);

    public String clipString(Font font, FontMetrics metrics, double availTextWidth, String text) {
        return clipString(font, metrics, availTextWidth, text, LONG_TEXT_PLACEHOLDER);
    }
}
