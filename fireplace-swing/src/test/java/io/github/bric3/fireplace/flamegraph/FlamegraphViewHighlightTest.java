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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link FlamegraphView} frame highlighting functionality.
 */
@DisplayName("FlamegraphView - Highlight")
class FlamegraphViewHighlightTest {

    private FlamegraphView<String> fg;

    @BeforeEach
    void setUp() {
        fg = new FlamegraphView<>();
    }

    @Nested
    @DisplayName("Highlight Frames")
    class HighlightFramesTests {

        @Test
        void highlightFrames_with_valid_set_does_not_throw() {
            var frame = new FrameBox<>("root", 0.0, 1.0, 0);
            fg.setModel(new FrameModel<>(List.of(frame)));

            assertThatCode(() -> fg.highlightFrames(Set.of(frame), "root"))
                    .doesNotThrowAnyException();
        }

        @Test
        void highlightFrames_empty_set_clears_highlights() {
            fg.setModel(new FrameModel<>(List.of(new FrameBox<>("root", 0.0, 1.0, 0))));

            assertThatCode(() -> fg.highlightFrames(Set.of(), ""))
                    .doesNotThrowAnyException();
        }

        @Test
        void highlightFrames_null_set_throws_exception() {
            assertThatThrownBy(() -> fg.highlightFrames(null, "test"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void highlightFrames_null_searched_throws_exception() {
            assertThatThrownBy(() -> fg.highlightFrames(Set.of(), null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void highlightFrames_with_frames_from_model() {
            var root = new FrameBox<>("root", 0.0, 1.0, 0);
            var child1 = new FrameBox<>("child1", 0.0, 0.5, 1);
            var child2 = new FrameBox<>("child2", 0.5, 1.0, 1);
            fg.setModel(new FrameModel<>(List.of(root, child1, child2)));

            assertThatCode(() -> fg.highlightFrames(Set.of(child1, child2), "child"))
                    .doesNotThrowAnyException();
        }

        @Test
        void highlightFrames_clear_then_set() {
            var frame = new FrameBox<>("root", 0.0, 1.0, 0);
            fg.setModel(new FrameModel<>(List.of(frame)));

            // Clear highlights
            fg.highlightFrames(Set.of(), "");

            // Set highlights
            fg.highlightFrames(Set.of(frame), "root");

            // Clear again
            assertThatCode(() -> fg.highlightFrames(Set.of(), ""))
                    .doesNotThrowAnyException();
        }
    }
}