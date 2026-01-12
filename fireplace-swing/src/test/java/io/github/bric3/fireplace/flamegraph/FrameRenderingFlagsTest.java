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

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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

        @Test
        void flag_constants_are_unique() {
            var flags = new int[]{
                    MINIMAP_MODE,
                    HIGHLIGHTING,
                    HIGHLIGHTED_FRAME,
                    HOVERED,
                    HOVERED_SIBLING,
                    FOCUSING,
                    FOCUSED_FRAME,
                    PARTIAL_FRAME
            };

            // Each flag should be a power of 2 and unique
            for (int i = 0; i < flags.length; i++) {
                for (int j = i + 1; j < flags.length; j++) {
                    assertThat(flags[i] & flags[j])
                            .as("Flags %d and %d should not overlap", flags[i], flags[j])
                            .isZero();
                }
            }
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

            assertThat(result)
                    .startsWith("[")
                    .endsWith("]")
                    .contains("minimapMode")
                    .contains("hovered")
                    .contains("partial");
        }

        @Test
        void toString_all_flags_shows_all_flag_names() {
            int allFlags = MINIMAP_MODE | HIGHLIGHTING | HIGHLIGHTED_FRAME | HOVERED |
                           HOVERED_SIBLING | FOCUSING | FOCUSED_FRAME | PARTIAL_FRAME;

            String result = FrameRenderingFlags.toString(allFlags);

            assertThat(result)
                    .contains("minimapMode")
                    .contains("highlighting")
                    .contains("highlighted")
                    .contains("hovered")
                    .contains("hovered sibling")
                    .contains("focusing")
                    .contains("focused")
                    .contains("partial");
        }
    }

    @Nested
    @DisplayName("Round-trip Tests")
    @ExtendWith(SoftAssertionsExtension.class)
    class RoundTripTests {

        @Test
        void toFlags_and_checkers_round_trip(SoftAssertions softly) {
            // [minimapMode, highlighting, highlighted, hovered, hoveredSibling, focusing, focused, partial]
            var minimapOnly =        new boolean[]{true,  false, false, false, false, false, false, false};
            var highlightingOnly =   new boolean[]{false, true,  false, false, false, false, false, false};
            var highlightedOnly =    new boolean[]{false, false, true,  false, false, false, false, false};
            var hoveredOnly =        new boolean[]{false, false, false, true,  false, false, false, false};
            var hoveredSiblingOnly = new boolean[]{false, false, false, false, true,  false, false, false};
            var focusingOnly =       new boolean[]{false, false, false, false, false, true,  false, false};
            var focusedOnly =        new boolean[]{false, false, false, false, false, false, true,  false};
            var partialOnly =        new boolean[]{false, false, false, false, false, false, false, true};
            var allFlags =           new boolean[]{true,  true,  true,  true,  true,  true,  true,  true};
            var alternating =        new boolean[]{true,  false, true,  false, true,  false, true,  false};

            for (var input : new boolean[][]{
                    minimapOnly, highlightingOnly, highlightedOnly, hoveredOnly,
                    hoveredSiblingOnly, focusingOnly, focusedOnly, partialOnly,
                    allFlags, alternating
            }) {
                int flags = FrameRenderingFlags.toFlags(
                        input[0], input[1], input[2], input[3],
                        input[4], input[5], input[6], input[7]
                );

                softly.assertThat(isMinimapMode(flags)).as("minimapMode").isEqualTo(input[0]);
                softly.assertThat(isHighlighting(flags)).as("highlighting").isEqualTo(input[1]);
                softly.assertThat(isHighlightedFrame(flags)).as("highlighted").isEqualTo(input[2]);
                softly.assertThat(isHovered(flags)).as("hovered").isEqualTo(input[3]);
                softly.assertThat(isHoveredSibling(flags)).as("hoveredSibling").isEqualTo(input[4]);
                softly.assertThat(isFocusing(flags)).as("focusing").isEqualTo(input[5]);
                softly.assertThat(isInFocusedFlame(flags)).as("focused").isEqualTo(input[6]);
                softly.assertThat(isPartialFrame(flags)).as("partial").isEqualTo(input[7]);
            }
        }
    }
}