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

import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link FrameRenderingFlags}.
 * These tests run in headless mode without requiring a display.
 */
@DisplayName("FrameRenderingFlags")
class FrameRenderingFlagsTest {

    @Nested
    @DisplayName("Flag Constants")
    class FlagConstantsTests {

        @Test
        void flag_constants_are_powers_of_two() {
            assertThat(MINIMAP_MODE).isEqualTo(1);
            assertThat(HIGHLIGHTING).isEqualTo(2);
            assertThat(HIGHLIGHTED_FRAME).isEqualTo(4);
            assertThat(HOVERED).isEqualTo(8);
            assertThat(HOVERED_SIBLING).isEqualTo(16);
            assertThat(FOCUSING).isEqualTo(32);
            assertThat(FOCUSED_FRAME).isEqualTo(64);
            assertThat(PARTIAL_FRAME).isEqualTo(128);
        }

    }

    @Nested
    @DisplayName("toFlags()")
    class ToFlagsTests {

        @Test
        void toFlags_all_false_returns_zero() {
            int flags = FrameRenderingFlags.toFlags(
                    false, false, false, false, false, false, false, false
            );

            assertThat(flags).isZero();
        }

        @Test
        void toFlags_all_true_returns_all_flags() {
            int flags = FrameRenderingFlags.toFlags(
                    true, true, true, true, true, true, true, true
            );

            int expected = MINIMAP_MODE | HIGHLIGHTING | HIGHLIGHTED_FRAME | HOVERED |
                           HOVERED_SIBLING | FOCUSING | FOCUSED_FRAME | PARTIAL_FRAME;

            assertThat(flags).isEqualTo(expected);
        }

        @Test
        void toFlags_single_flag_minimap_mode() {
            int flags = FrameRenderingFlags.toFlags(
                    true, false, false, false, false, false, false, false
            );

            assertThat(flags).isEqualTo(MINIMAP_MODE);
        }

        @Test
        void toFlags_single_flag_highlighting() {
            int flags = FrameRenderingFlags.toFlags(
                    false, true, false, false, false, false, false, false
            );

            assertThat(flags).isEqualTo(HIGHLIGHTING);
        }

        @Test
        void toFlags_single_flag_highlighted_frame() {
            int flags = FrameRenderingFlags.toFlags(
                    false, false, true, false, false, false, false, false
            );

            assertThat(flags).isEqualTo(HIGHLIGHTED_FRAME);
        }

        @Test
        void toFlags_single_flag_hovered() {
            int flags = FrameRenderingFlags.toFlags(
                    false, false, false, true, false, false, false, false
            );

            assertThat(flags).isEqualTo(HOVERED);
        }

        @Test
        void toFlags_single_flag_hovered_sibling() {
            int flags = FrameRenderingFlags.toFlags(
                    false, false, false, false, true, false, false, false
            );

            assertThat(flags).isEqualTo(HOVERED_SIBLING);
        }

        @Test
        void toFlags_single_flag_focusing() {
            int flags = FrameRenderingFlags.toFlags(
                    false, false, false, false, false, true, false, false
            );

            assertThat(flags).isEqualTo(FOCUSING);
        }

        @Test
        void toFlags_single_flag_focused_frame() {
            int flags = FrameRenderingFlags.toFlags(
                    false, false, false, false, false, false, true, false
            );

            assertThat(flags).isEqualTo(FOCUSED_FRAME);
        }

        @Test
        void toFlags_single_flag_partial_frame() {
            int flags = FrameRenderingFlags.toFlags(
                    false, false, false, false, false, false, false, true
            );

            assertThat(flags).isEqualTo(PARTIAL_FRAME);
        }

        @Test
        void toFlags_combined_flags() {
            int flags = FrameRenderingFlags.toFlags(
                    true, true, false, true, false, false, false, false
            );

            assertThat(flags).isEqualTo(MINIMAP_MODE | HIGHLIGHTING | HOVERED);
        }
    }

    @Nested
    @DisplayName("Individual Flag Checkers")
    class IndividualFlagCheckerTests {

        @Nested
        @DisplayName("isMinimapMode()")
        class IsMinimapModeTests {

            @Test
            void isMinimapMode_flag_set_returns_true() {
                assertThat(isMinimapMode(MINIMAP_MODE)).isTrue();
                assertThat(isMinimapMode(MINIMAP_MODE | HOVERED)).isTrue();
            }

            @Test
            void isMinimapMode_flag_not_set_returns_false() {
                assertThat(isMinimapMode(0)).isFalse();
                assertThat(isMinimapMode(HOVERED)).isFalse();
            }
        }

        @Nested
        @DisplayName("isHighlighting()")
        class IsHighlightingTests {

            @Test
            void isHighlighting_flag_set_returns_true() {
                assertThat(isHighlighting(HIGHLIGHTING)).isTrue();
                assertThat(isHighlighting(HIGHLIGHTING | MINIMAP_MODE)).isTrue();
            }

            @Test
            void isHighlighting_flag_not_set_returns_false() {
                assertThat(isHighlighting(0)).isFalse();
                assertThat(isHighlighting(MINIMAP_MODE)).isFalse();
            }
        }

        @Nested
        @DisplayName("isHighlightedFrame()")
        class IsHighlightedFrameTests {

            @Test
            void isHighlightedFrame_flag_set_returns_true() {
                assertThat(isHighlightedFrame(HIGHLIGHTED_FRAME)).isTrue();
                assertThat(isHighlightedFrame(HIGHLIGHTED_FRAME | HOVERED)).isTrue();
            }

            @Test
            void isHighlightedFrame_flag_not_set_returns_false() {
                assertThat(isHighlightedFrame(0)).isFalse();
                assertThat(isHighlightedFrame(HIGHLIGHTING)).isFalse();
            }
        }

        @Nested
        @DisplayName("isHovered()")
        class IsHoveredTests {

            @Test
            void isHovered_flag_set_returns_true() {
                assertThat(isHovered(HOVERED)).isTrue();
                assertThat(isHovered(HOVERED | FOCUSING)).isTrue();
            }

            @Test
            void isHovered_flag_not_set_returns_false() {
                assertThat(isHovered(0)).isFalse();
                assertThat(isHovered(HOVERED_SIBLING)).isFalse();
            }
        }

        @Nested
        @DisplayName("isHoveredSibling()")
        class IsHoveredSiblingTests {

            @Test
            void isHoveredSibling_flag_set_returns_true() {
                assertThat(isHoveredSibling(HOVERED_SIBLING)).isTrue();
                assertThat(isHoveredSibling(HOVERED_SIBLING | MINIMAP_MODE)).isTrue();
            }

            @Test
            void isHoveredSibling_flag_not_set_returns_false() {
                assertThat(isHoveredSibling(0)).isFalse();
                assertThat(isHoveredSibling(HOVERED)).isFalse();
            }
        }

        @Nested
        @DisplayName("isFocusing()")
        class IsFocusingTests {

            @Test
            void isFocusing_flag_set_returns_true() {
                assertThat(isFocusing(FOCUSING)).isTrue();
                assertThat(isFocusing(FOCUSING | HIGHLIGHTED_FRAME)).isTrue();
            }

            @Test
            void isFocusing_flag_not_set_returns_false() {
                assertThat(isFocusing(0)).isFalse();
                assertThat(isFocusing(FOCUSED_FRAME)).isFalse();
            }
        }

        @Nested
        @DisplayName("isInFocusedFlame()")
        class IsInFocusedFlameTests {

            @Test
            void isInFocusedFlame_flag_set_returns_true() {
                assertThat(isInFocusedFlame(FOCUSED_FRAME)).isTrue();
                assertThat(isInFocusedFlame(FOCUSED_FRAME | FOCUSING)).isTrue();
            }

            @Test
            void isInFocusedFlame_flag_not_set_returns_false() {
                assertThat(isInFocusedFlame(0)).isFalse();
                assertThat(isInFocusedFlame(FOCUSING)).isFalse();
            }
        }

        @Nested
        @DisplayName("isPartialFrame()")
        class IsPartialFrameTests {

            @Test
            void isPartialFrame_flag_set_returns_true() {
                assertThat(isPartialFrame(PARTIAL_FRAME)).isTrue();
                assertThat(isPartialFrame(PARTIAL_FRAME | HOVERED)).isTrue();
            }

            @Test
            void isPartialFrame_flag_not_set_returns_false() {
                assertThat(isPartialFrame(0)).isFalse();
                assertThat(isPartialFrame(MINIMAP_MODE)).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringTests {

        @Test
        void toString_no_flags_returns_empty_brackets() {
            assertThat(FrameRenderingFlags.toString(0)).isEqualTo("[]");
        }

        @Test
        void toString_single_flag_shows_flag_name() {
            assertThat(FrameRenderingFlags.toString(MINIMAP_MODE)).isEqualTo("[minimapMode]");
            assertThat(FrameRenderingFlags.toString(HIGHLIGHTING)).isEqualTo("[highlighting]");
            assertThat(FrameRenderingFlags.toString(HIGHLIGHTED_FRAME)).isEqualTo("[highlighted]");
            assertThat(FrameRenderingFlags.toString(HOVERED)).isEqualTo("[hovered]");
            assertThat(FrameRenderingFlags.toString(HOVERED_SIBLING)).isEqualTo("[hovered sibling]");
            assertThat(FrameRenderingFlags.toString(FOCUSING)).isEqualTo("[focusing]");
            assertThat(FrameRenderingFlags.toString(FOCUSED_FRAME)).isEqualTo("[focused]");
            assertThat(FrameRenderingFlags.toString(PARTIAL_FRAME)).isEqualTo("[partial]");
        }

        @Test
        void toString_multiple_flags_shows_all_flag_names() {
            int flags = MINIMAP_MODE | HOVERED | PARTIAL_FRAME;
            String result = FrameRenderingFlags.toString(flags);

            assertThat(result).isEqualTo("[minimapMode, hovered, partial]");
        }

        @Test
        void toString_all_flags_shows_all_flag_names() {
            int allFlags = MINIMAP_MODE | HIGHLIGHTING | HIGHLIGHTED_FRAME | HOVERED |
                           HOVERED_SIBLING | FOCUSING | FOCUSED_FRAME | PARTIAL_FRAME;

            String result = FrameRenderingFlags.toString(allFlags);

            assertThat(result).isEqualTo("[minimapMode, highlighting, highlighted, hovered, hovered sibling, focusing, focused, partial]");
        }
    }

}