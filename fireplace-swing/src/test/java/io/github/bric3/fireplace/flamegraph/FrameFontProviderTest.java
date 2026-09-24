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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.awt.Font;
import java.util.stream.Stream;

import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.*;
import static org.assertj.core.api.Assertions.assertThat;

class FrameFontProviderTest {
    @ParameterizedTest(name = "{0}, flags {1}: style {2}")
    @MethodSource("fontCases")
    void default_font_respects_root_highlight_focus_and_partial_state(FrameBox<String> frame, int flags, int style) {
        FrameFontProvider<String> provider = FrameFontProvider.defaultFontProvider();

        assertThat(provider.getFont(frame, flags)).isEqualTo(new Font(Font.SANS_SERIF, style, 12));
    }

    private static Stream<Arguments> fontCases() {
        var root = new FrameBox<>("root", 0, 1, 0);
        var child = new FrameBox<>("child", 0, 1, 1);
        return Stream.of(
                Arguments.of(null, 0, Font.PLAIN),
                Arguments.of(null, PARTIAL_FRAME, Font.ITALIC),
                Arguments.of(root, 0, Font.BOLD),
                Arguments.of(root, PARTIAL_FRAME, Font.BOLD),
                Arguments.of(child, 0, Font.PLAIN),
                Arguments.of(child, PARTIAL_FRAME, Font.ITALIC),
                Arguments.of(child, HIGHLIGHTED_FRAME, Font.BOLD),
                Arguments.of(child, HIGHLIGHTED_FRAME | PARTIAL_FRAME, Font.BOLD | Font.ITALIC),
                Arguments.of(child, HIGHLIGHTED_FRAME | FOCUSING, Font.PLAIN),
                Arguments.of(child, HIGHLIGHTED_FRAME | FOCUSING | PARTIAL_FRAME, Font.ITALIC),
                Arguments.of(child, HIGHLIGHTED_FRAME | FOCUSING | FOCUSED_FRAME, Font.BOLD),
                Arguments.of(child, HIGHLIGHTED_FRAME | FOCUSING | FOCUSED_FRAME | PARTIAL_FRAME, Font.BOLD | Font.ITALIC)
        );
    }
}
