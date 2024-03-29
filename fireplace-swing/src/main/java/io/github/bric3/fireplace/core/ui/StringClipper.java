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

/**
 * A utility class to clip strings to fit a given width.
 * <p>
 * The clipping strategy is defined by the enum value.
 * The clipping strategy can be changed by implementing the {@link #clipString(Font, FontMetrics, double, String, String)} method.
 *
 * @see #NONE
 * @see #RIGHT
 */
@FunctionalInterface
public interface StringClipper {
    /**
     * No clipping is performed.
     */
    StringClipper NONE = (font, metrics, availTextWidth, text, clipString) -> text;

    /**
     * Clips the string from the left.
     */
    StringClipper RIGHT = (font, metrics, availTextWidth, text, clipString) -> {
        availTextWidth -= font.getStringBounds(clipString, metrics.getFontRenderContext()).getWidth();
        if (availTextWidth <= 0) {
            // cannot fit any characters
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
    };

    /**
     * A short string to display in place of labels that are too long to fit the
     * available space.
     */
    String LONG_TEXT_PLACEHOLDER = "…";

    /**
     * Clip the string to fit the available width according to the font metrics.
     *
     * @param font           the font used to render the text
     *                       (used to calculate the width of the text to clip)
     * @param metrics        the font metrics (usually obtained from {@link  Graphics#getFontMetrics(Font)}
     * @param availTextWidth the available width for the text
     * @param text           the text to clip
     * @param clipString     the string to indicating where the clip happened, according to the clipping strategy
     * @return the clipped string
     * @see #clipString(Font, FontMetrics, double, String)
     */
    String clipString(Font font, FontMetrics metrics, double availTextWidth, String text, String clipString);

    /**
     * Clip the string to fit the available width according to the font metrics.
     * <p>
     * This method uses {@link #LONG_TEXT_PLACEHOLDER} as the clip string.
     *
     * @param font           the font used to render the text
     *                       (used to calculate the width of the text to clip)
     * @param metrics        the font metrics (usually obtained from {@link  Graphics#getFontMetrics(Font)}
     * @param availTextWidth the available width for the text
     * @param text           the text to clip
     * @return the clipped string
     * @see #clipString(Font, FontMetrics, double, String, String)
     */
    default String clipString(Font font, FontMetrics metrics, double availTextWidth, String text) {
        return clipString(font, metrics, availTextWidth, text, LONG_TEXT_PLACEHOLDER);
    }
}
