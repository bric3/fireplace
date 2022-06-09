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

import io.github.bric3.fireplace.core.ui.Colors;
import io.github.bric3.fireplace.core.ui.StringClipper;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.function.Function;

class FrameRender<T> {
    public static int MINIMAP_MODE = 1;
    public static int HIGHLIGHTING = 1 << 1;
    public static int HIGHLIGHTED_FRAME = 1 << 2;
    public static int HOVERED = 1 << 3;
    public static int FOCUSING = 1 << 4;
    public static int FOCUSED_FRAME = 1 << 5;

    public static int toFlags(boolean minimapMode, boolean highlightingOn, boolean highlighted, boolean hovered, boolean focusing, boolean focusedFrame) {
        return (minimapMode ? MINIMAP_MODE : 0)
               | (highlightingOn ? HIGHLIGHTING : 0)
               | (highlighted ? HIGHLIGHTED_FRAME : 0)
               | (hovered ? HOVERED : 0)
               | (focusing ? FOCUSING : 0)
               | (focusedFrame ? FOCUSED_FRAME : 0);
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

    
    private final NodeDisplayStringProvider<T> nodeToTextProvider;
    private Function<FrameBox<T>, Color> frameColorFunction;


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
     * The space in pixels between the frame label text and the frame's border.
     */
    private final int frameTextPadding = 2;

    /**
     * A flag that controls whether a gap is shown at the right and bottom of each frame.
     */
    private boolean drawingFrameGap = true;

    /**
     * The size of the gap at the <strong>right</strong> and <strong>bottom</strong> of each frame.
     */
    private final int frameGapWidth = 1;


    /**
     * The color used to draw frames that are highlighted.
     */
    private final Color highlightedColor;


    /**
     * @param nodeToTextProvider functions that create a label for a node
     * @param frameColorFunction a function that maps frames to color.
     */
    public FrameRender(
            NodeDisplayStringProvider<T> nodeToTextProvider,
            Function<FrameBox<T>, Color> frameColorFunction
    ) {
        this.nodeToTextProvider = nodeToTextProvider;
        this.frameColorFunction = frameColorFunction;

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

    public int getFrameTextPadding() {
        return frameTextPadding;
    }

    public int getFrameGapWidth() {
        return frameGapWidth;
    }

    public void setDrawingFrameGap(boolean drawingFrameGap) {
        this.drawingFrameGap = drawingFrameGap;
    }

    public boolean isDrawingFrameGap() {
        return drawingFrameGap;
    }

    /**
     * The width of the border drawn around the hovered frame.
     */
    public int frameBorderWidth = 1;

    /**
     * The stroke used to draw a border around the hovered frame.
     */
    public Stroke frameBorderStroke = new BasicStroke(frameBorderWidth);

    public int getFrameBoxHeight(Graphics2D g2) {
        return g2.getFontMetrics(frameLabelFont).getAscent() + (frameTextPadding * 2) + frameGapWidth * 2;
    }

    public float getFrameBoxTextOffset(Graphics2D g2) {
        return getFrameBoxHeight(g2) - (g2.getFontMetrics(frameLabelFont).getDescent() / 2f) - frameTextPadding - frameGapWidth;
    }


    /**
     * Paints the frame.
     *
     * @param g2                    the graphics target.
     * @param frameRect             the frame region (may fall outside visible area).
     * @param frame                 the frame to paint
     * @param paintableIntersection the intersection between the frame rectangle and the visible region
     *                              (used to position the text label).
     * @param flags                 The rendering flags (minimap, selection, hovered, highlight).
     */
    public void paintFrame(
            Graphics2D g2,
            Rectangle2D frameRect,
            FrameBox<T> frame,
            Rectangle2D paintableIntersection,
            int flags
    ) {
        boolean minimapMode = isMinimapMode(flags);
        var bgColor = tweakBgColor(frameColorFunction.apply(frame), flags);

        paintFrameRectangle(g2, frameRect, bgColor, minimapMode);
        if (minimapMode) {
            return;
        }

        var labelFont = tweakLabelFont(frameRect, paintableIntersection, flags);

        var text = calculateFrameText(
                g2,
                labelFont,
                paintableIntersection.getWidth() - frameTextPadding * 2 - frameGapWidth * 2,
                frame
        );

        if (text == null) {
            return;
        }

        g2.setFont(labelFont);
        g2.setColor(Colors.foregroundColor(bgColor));
        g2.drawString(
                text,
                (float) (paintableIntersection.getX() + frameTextPadding + frameBorderWidth),
                (float) (frameRect.getY() + getFrameBoxTextOffset(g2))
        );
    }

    // TODO extract as strategy
    private Font tweakLabelFont(
            Rectangle2D frameRect,
            Rectangle2D intersection,
            int flags
    ) {
        if (isHighlightedFrame(flags)) {
            return frameRect.getX() == intersection.getX() ?
                   getHighlightedFrameLabelFont() :
                   // when parent frame are larger than view port
                   getHighlightedPartialFrameLabelFont();
        }
        return frameRect.getX() == intersection.getX() ?
               getFrameLabelFont() :
               // when parent frame are larger than view port
               getPartialFrameLabelFont();
    }

    // TODO extract as strategy
    private Color tweakBgColor(
            Color baseColor,
            int flags
    ) {
        Color color = baseColor;
        if (isFocusing(flags) && !isFocusedFrame(flags)) {
            color = Colors.blend(baseColor, Colors.translucent_black_80);
        }
        if (isHighlighting(flags)) {
            color = Colors.isDarkMode() ?
                    Colors.blend(color, Colors.translucent_black_B0) :
                    Colors.blend(color, Color.WHITE);
            if (isHighlightedFrame(flags)) {
                color = baseColor;
            }
        }
        if (isHovered(flags)) {
            color = Colors.blend(color, Colors.translucent_black_40);
        }
        return color;
    }


    private void paintFrameRectangle(
            Graphics2D g2,
            Rectangle2D frameRect,
            Color bgColor,
            boolean minimapMode
    ) {
        var gapThickness = minimapMode ?
                           0 :
                           drawingFrameGap ? frameGapWidth : 0;

        var x = frameRect.getX();
        var y = frameRect.getY();
        var w = frameRect.getWidth() - gapThickness;
        var h = frameRect.getHeight() - gapThickness;
        frameRect.setRect(x, y, w, h);

        g2.setColor(bgColor);
        g2.fill(frameRect);
    }

    // layout text
    private String calculateFrameText(
            Graphics2D g2,
            Font font,
            double targetWidth,
            FrameBox<T> frame
    ) {
        var metrics = g2.getFontMetrics(font);

        // don't use stream to avoid allocations during painting
        var textCandidate = "";
        for (Function<FrameBox<T>, String> nodeToTextCandidate : nodeToTextProvider.frameToTextCandidates()) {
            textCandidate = nodeToTextCandidate.apply(frame);
            var textBounds = metrics.getStringBounds(textCandidate, g2);
            if (textBounds.getWidth() <= targetWidth) {
                return textCandidate;
            }
        }
        // only try clip the last candidate
        textCandidate = nodeToTextProvider.clipStrategy().clipString(
                font,
                metrics,
                targetWidth,
                textCandidate,
                StringClipper.LONG_TEXT_PLACEHOLDER
        );
        var textBounds = metrics.getStringBounds(textCandidate, g2);
        if (textBounds.getWidth() > targetWidth || textCandidate.length() <= StringClipper.LONG_TEXT_PLACEHOLDER.length() + 1) {
            // don't draw text, if too long or too short (like "r…")
            return null;
        }
        return textCandidate;

    }



    public void setFrameColorFunction(Function<FrameBox<T>, Color> frameColorFunction) {
        this.frameColorFunction = frameColorFunction;
    }
}