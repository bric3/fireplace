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

import io.github.bric3.fireplace.core.ui.fixtures.SwingEdtExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link FlamegraphView} frame rendering configuration.
 */
@DisplayName("FlamegraphView - Rendering")
@org.junit.jupiter.api.extension.ExtendWith(SwingEdtExtension.class)
class FlamegraphView_RenderingTest {

    private FlamegraphView<String> fg;

    @BeforeEach
    void setUp() {
        fg = new FlamegraphView<>();
        var customRenderer = mock(FrameRenderer.class);
        when(customRenderer.isDrawingFrameGap()).thenReturn(true);
        when(customRenderer.getFrameBoxHeight(any())).thenReturn(20);
        fg.setFrameRender(customRenderer);
    }

    @SuppressWarnings("removal")
    @Nested
    @DisplayName("Deprecated Methods Exception Branches")
    class DeprecatedMethodsExceptionBranchesTests {

        @Test
        void getFrameColorProvider_with_custom_renderer_throws_exception() {
            assertThatThrownBy(() -> fg.getFrameColorProvider())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DefaultFrameRenderer");
        }

        @Test
        void setFrameColorProvider_with_custom_renderer_throws_exception() {
            var colorProvider = FrameColorProvider.<String>defaultColorProvider(f -> Color.RED);
            assertThatThrownBy(() -> fg.setFrameColorProvider(colorProvider))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DefaultFrameRenderer");
        }

        @Test
        void getFrameFontProvider_with_custom_renderer_throws_exception() {
            assertThatThrownBy(() -> fg.getFrameFontProvider())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DefaultFrameRenderer");
        }

        @Test
        void setFrameFontProvider_with_custom_renderer_throws_exception() {
            var fontProvider = FrameFontProvider.<String>defaultFontProvider();
            assertThatThrownBy(() -> fg.setFrameFontProvider(fontProvider))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DefaultFrameRenderer");
        }

        @Test
        void getFrameTextsProvider_with_custom_renderer_throws_exception() {
            assertThatThrownBy(() -> fg.getFrameTextsProvider())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DefaultFrameRenderer");
        }

        @Test
        void setFrameTextsProvider_with_custom_renderer_throws_exception() {
            var textsProvider = FrameTextsProvider.<String>of(f -> "test");
            assertThatThrownBy(() -> fg.setFrameTextsProvider(textsProvider))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DefaultFrameRenderer");
        }

        @Test
        void setFrameGapEnabled_with_custom_renderer_throws_exception() {
            assertThatThrownBy(() -> fg.setFrameGapEnabled(false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DefaultFrameRenderer");
        }
    }
}
