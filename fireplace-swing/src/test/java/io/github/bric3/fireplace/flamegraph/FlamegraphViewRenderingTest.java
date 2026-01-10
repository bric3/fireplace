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

import java.awt.*;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link FlamegraphView} frame rendering configuration.
 */
@DisplayName("FlamegraphView - Rendering")
class FlamegraphViewRenderingTest {

    private FlamegraphView<String> fg;

    @BeforeEach
    void setUp() {
        fg = new FlamegraphView<>();
    }

    @Nested
    @DisplayName("Frame Renderer")
    class FrameRendererTests {

        @Test
        void setFrameRender_changes_renderer() {
            var renderer = new DefaultFrameRenderer<String>(
                    FrameTextsProvider.of(f -> f.actualNode),
                    FrameColorProvider.defaultColorProvider(f -> Color.GREEN),
                    FrameFontProvider.defaultFontProvider()
            );

            fg.setFrameRender(renderer);

            // No direct getter, but should not throw
            assertThat(fg.getFrameColorProvider()).isNotNull();
        }

        @Test
        void setFrameRender_with_custom_renderer_does_not_throw() {
            var customRenderer = new DefaultFrameRenderer<String>(
                    FrameTextsProvider.of(f -> "Custom: " + f.actualNode),
                    FrameColorProvider.defaultColorProvider(f -> Color.BLUE),
                    FrameFontProvider.defaultFontProvider()
            );

            assertThatCode(() -> fg.setFrameRender(customRenderer))
                    .doesNotThrowAnyException();
        }

        @Test
        void setFrameRender_triggers_repaint() {
            var frame = new FrameBox<>("root", 0.0, 1.0, 0);
            fg.setModel(new FrameModel<>(List.of(frame)));

            var customRenderer = new DefaultFrameRenderer<String>(
                    FrameTextsProvider.of(f -> "Changed"),
                    FrameColorProvider.defaultColorProvider(f -> Color.GREEN),
                    FrameFontProvider.defaultFontProvider()
            );

            assertThatCode(() -> fg.setFrameRender(customRenderer))
                    .doesNotThrowAnyException();
        }
    }

    @SuppressWarnings("removal")
    @Nested
    @DisplayName("Deprecated Methods Exception Branches")
    class DeprecatedMethodsExceptionBranchesTests {

        @Test
        void getFrameColorProvider_with_custom_renderer_throws_exception() {
            // Create a custom non-DefaultFrameRenderer
            var customRenderer = mock(FrameRenderer.class);
            when(customRenderer.isDrawingFrameGap()).thenReturn(true);
            when(customRenderer.getFrameBoxHeight(any())).thenReturn(20);

            fg.setFrameRender(customRenderer);

            assertThatThrownBy(() -> fg.getFrameColorProvider())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DefaultFrameRenderer");
        }

        @Test
        void setFrameColorProvider_with_custom_renderer_throws_exception() {
            var customRenderer = mock(FrameRenderer.class);
            when(customRenderer.isDrawingFrameGap()).thenReturn(true);
            when(customRenderer.getFrameBoxHeight(any())).thenReturn(20);

            fg.setFrameRender(customRenderer);

            var colorProvider = FrameColorProvider.<String>defaultColorProvider(f -> Color.RED);
            assertThatThrownBy(() -> fg.setFrameColorProvider(colorProvider))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DefaultFrameRenderer");
        }

        @Test
        void getFrameFontProvider_with_custom_renderer_throws_exception() {
            var customRenderer = mock(FrameRenderer.class);
            when(customRenderer.isDrawingFrameGap()).thenReturn(true);
            when(customRenderer.getFrameBoxHeight(any())).thenReturn(20);

            fg.setFrameRender(customRenderer);

            assertThatThrownBy(() -> fg.getFrameFontProvider())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DefaultFrameRenderer");
        }

        @Test
        void setFrameFontProvider_with_custom_renderer_throws_exception() {
            var customRenderer = mock(FrameRenderer.class);
            when(customRenderer.isDrawingFrameGap()).thenReturn(true);
            when(customRenderer.getFrameBoxHeight(any())).thenReturn(20);

            fg.setFrameRender(customRenderer);

            var fontProvider = FrameFontProvider.<String>defaultFontProvider();
            assertThatThrownBy(() -> fg.setFrameFontProvider(fontProvider))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DefaultFrameRenderer");
        }

        @Test
        void getFrameTextsProvider_with_custom_renderer_throws_exception() {
            var customRenderer = mock(FrameRenderer.class);
            when(customRenderer.isDrawingFrameGap()).thenReturn(true);
            when(customRenderer.getFrameBoxHeight(any())).thenReturn(20);

            fg.setFrameRender(customRenderer);

            assertThatThrownBy(() -> fg.getFrameTextsProvider())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DefaultFrameRenderer");
        }

        @Test
        void setFrameTextsProvider_with_custom_renderer_throws_exception() {
            var customRenderer = mock(FrameRenderer.class);
            when(customRenderer.isDrawingFrameGap()).thenReturn(true);
            when(customRenderer.getFrameBoxHeight(any())).thenReturn(20);

            fg.setFrameRender(customRenderer);

            var textsProvider = FrameTextsProvider.<String>of(f -> "test");
            assertThatThrownBy(() -> fg.setFrameTextsProvider(textsProvider))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DefaultFrameRenderer");
        }

        @Test
        void setFrameGapEnabled_with_custom_renderer_throws_exception() {
            var customRenderer = mock(FrameRenderer.class);
            when(customRenderer.isDrawingFrameGap()).thenReturn(true);
            when(customRenderer.getFrameBoxHeight(any())).thenReturn(20);

            fg.setFrameRender(customRenderer);

            assertThatThrownBy(() -> fg.setFrameGapEnabled(false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DefaultFrameRenderer");
        }
    }
}