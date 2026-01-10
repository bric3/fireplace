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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link FrameFontProvider}.
 * These tests run in headless mode without requiring a display.
 */
@DisplayName("FrameFontProvider")
class FrameFontProviderTest {

    @Nested
    @DisplayName("defaultFontProvider")
    class DefaultFontProviderTests {

        @Nested
        @DisplayName("regular frames")
        class RegularFrames {

            @Test
            void regularFrame_returnsPlainFont() {
                FrameFontProvider<String> provider = FrameFontProvider.defaultFontProvider();
                FrameBox<String> frame = new FrameBox<>("method", 0.0, 1.0, 1);

                Font font = provider.getFont(frame, 0);

                assertThat(font.getStyle()).isEqualTo(Font.PLAIN);
                assertThat(font.isItalic()).isFalse();
                assertThat(font.isBold()).isFalse();
            }

            @Test
            void nullFrame_returnsBaseFont() {
                FrameFontProvider<String> provider = FrameFontProvider.defaultFontProvider();

                Font font = provider.getFont(null, 0);

                assertThat(font).isNotNull();
                assertThat(font.getStyle()).isEqualTo(Font.PLAIN);
            }

            @Test
            void nullFrameWithPartialFlag_returnsItalic() {
                FrameFontProvider<String> provider = FrameFontProvider.defaultFontProvider();

                int flags = toFlags(false, false, false, false, false, false, false, true);
                Font font = provider.getFont(null, flags);

                assertThat(font.isItalic()).isTrue();
            }
        }

        @Nested
        @DisplayName("root frames")
        class RootFrames {

            @Test
            void rootFrame_returnsBoldFont() {
                FrameFontProvider<String> provider = FrameFontProvider.defaultFontProvider();
                FrameBox<String> rootFrame = new FrameBox<>("root", 0.0, 1.0, 0);

                Font font = provider.getFont(rootFrame, 0);

                assertThat(font.isBold()).isTrue();
                assertThat(font.isItalic()).isFalse();
            }

            @Test
            void rootFrameWithPartial_returnsBold() {
                // Root frame should always be bold, even if partial
                FrameFontProvider<String> provider = FrameFontProvider.defaultFontProvider();
                FrameBox<String> rootFrame = new FrameBox<>("root", 0.0, 1.0, 0);

                int partialFlags = toFlags(false, false, false, false, false, false, false, true);
                Font font = provider.getFont(rootFrame, partialFlags);

                // Root is handled first, so it returns bold (not italic-bold)
                assertThat(font.isBold()).isTrue();
            }
        }

        @Nested
        @DisplayName("partial frames")
        class PartialFrames {

            @Test
            void partialFrame_returnsItalicFont() {
                FrameFontProvider<String> provider = FrameFontProvider.defaultFontProvider();
                FrameBox<String> frame = new FrameBox<>("method", 0.0, 1.0, 1);

                int flags = toFlags(false, false, false, false, false, false, false, true);
                Font font = provider.getFont(frame, flags);

                assertThat(font.isItalic()).isTrue();
                assertThat(font.isBold()).isFalse();
            }
        }

        @Nested
        @DisplayName("highlighted frames")
        class HighlightedFrames {

            @Test
            void highlightedFrame_returnsBoldFont() {
                FrameFontProvider<String> provider = FrameFontProvider.defaultFontProvider();
                FrameBox<String> frame = new FrameBox<>("method", 0.0, 1.0, 1);

                // HIGHLIGHTED_FRAME flag set (not focusing)
                int flags = toFlags(false, false, true, false, false, false, false, false);
                Font font = provider.getFont(frame, flags);

                assertThat(font.isBold()).isTrue();
            }

            @Test
            void highlightedAndPartial_returnsItalicBoldFont() {
                FrameFontProvider<String> provider = FrameFontProvider.defaultFontProvider();
                FrameBox<String> frame = new FrameBox<>("method", 0.0, 1.0, 1);

                // HIGHLIGHTED_FRAME and PARTIAL_FRAME flags set
                int flags = toFlags(false, false, true, false, false, false, false, true);
                Font font = provider.getFont(frame, flags);

                assertThat(font.isBold()).isTrue();
                assertThat(font.isItalic()).isTrue();
            }
        }

        @Nested
        @DisplayName("focused frames")
        class FocusedFrames {

            @Test
            void focusingAndHighlightedAndInFocused_returnsBold() {
                FrameFontProvider<String> provider = FrameFontProvider.defaultFontProvider();
                FrameBox<String> frame = new FrameBox<>("method", 0.0, 1.0, 1);

                // FOCUSING, HIGHLIGHTED_FRAME, and FOCUSED_FRAME all set
                int flags = toFlags(false, false, true, false, false, true, true, false);
                Font font = provider.getFont(frame, flags);

                assertThat(font.isBold()).isTrue();
            }

            @Test
            void focusingAndHighlightedButNotInFocused_returnsPlain() {
                FrameFontProvider<String> provider = FrameFontProvider.defaultFontProvider();
                FrameBox<String> frame = new FrameBox<>("method", 0.0, 1.0, 1);

                // FOCUSING and HIGHLIGHTED_FRAME set, but NOT FOCUSED_FRAME
                int flags = toFlags(false, false, true, false, false, true, false, false);
                Font font = provider.getFont(frame, flags);

                // Should be plain because frame is highlighted but not in focused flame
                assertThat(font.isBold()).isFalse();
                assertThat(font.isItalic()).isFalse();
            }
        }

        @Nested
        @DisplayName("font properties")
        class FontProperties {

            @Test
            void fontIsSansSerif() {
                FrameFontProvider<String> provider = FrameFontProvider.defaultFontProvider();
                FrameBox<String> frame = new FrameBox<>("method", 0.0, 1.0, 1);

                Font font = provider.getFont(frame, 0);

                assertThat(font.getFamily()).isEqualTo(Font.SANS_SERIF);
            }

            @Test
            void fontSize12() {
                FrameFontProvider<String> provider = FrameFontProvider.defaultFontProvider();
                FrameBox<String> frame = new FrameBox<>("method", 0.0, 1.0, 1);

                Font font = provider.getFont(frame, 0);

                assertThat(font.getSize()).isEqualTo(12);
            }

            @Test
            void allFontsHaveSameSize() {
                FrameFontProvider<String> provider = FrameFontProvider.defaultFontProvider();
                FrameBox<String> frame = new FrameBox<>("method", 0.0, 1.0, 1);
                FrameBox<String> root = new FrameBox<>("root", 0.0, 1.0, 0);

                Font regular = provider.getFont(frame, 0);
                Font bold = provider.getFont(root, 0);
                Font italic = provider.getFont(frame, toFlags(false, false, false, false, false, false, false, true));
                Font italicBold = provider.getFont(frame, toFlags(false, false, true, false, false, false, false, true));

                assertThat(regular.getSize()).isEqualTo(12);
                assertThat(bold.getSize()).isEqualTo(12);
                assertThat(italic.getSize()).isEqualTo(12);
                assertThat(italicBold.getSize()).isEqualTo(12);
            }
        }
    }

    @Nested
    @DisplayName("functional interface implementation")
    class FunctionalInterfaceTests {

        @Test
        void canBeImplementedAsLambda() {
            Font customFont = new Font(Font.MONOSPACED, Font.BOLD, 14);
            FrameFontProvider<String> provider = (frame, flags) -> customFont;

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 0);
            Font font = provider.getFont(frame, 0);

            assertThat(font).isEqualTo(customFont);
        }

        @Test
        void canAccessFrameData() {
            FrameFontProvider<String> provider = (frame, flags) -> {
                if (frame != null && frame.actualNode.startsWith("important")) {
                    return new Font(Font.SANS_SERIF, Font.BOLD, 14);
                }
                return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
            };

            FrameBox<String> importantFrame = new FrameBox<>("important.method", 0.0, 1.0, 0);
            FrameBox<String> regularFrame = new FrameBox<>("regular.method", 0.0, 1.0, 0);

            assertThat(provider.getFont(importantFrame, 0).isBold()).isTrue();
            assertThat(provider.getFont(importantFrame, 0).getSize()).isEqualTo(14);
            assertThat(provider.getFont(regularFrame, 0).isBold()).isFalse();
            assertThat(provider.getFont(regularFrame, 0).getSize()).isEqualTo(12);
        }

        @Test
        void canAccessFlags() {
            FrameFontProvider<String> provider = (frame, flags) -> {
                if (isHovered(flags)) {
                    return new Font(Font.SERIF, Font.BOLD, 14);
                }
                return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
            };

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 0);
            int hoveredFlags = toFlags(false, false, false, true, false, false, false, false);

            assertThat(provider.getFont(frame, 0).getFamily()).isEqualTo(Font.SANS_SERIF);
            assertThat(provider.getFont(frame, hoveredFlags).getFamily()).isEqualTo(Font.SERIF);
        }
    }
}
