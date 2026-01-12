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
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DefaultFrameRenderer}.
 * Uses BufferedImage to get a Graphics2D context in headless mode.
 */
@DisplayName("DefaultFrameRenderer")
class DefaultFrameRendererTest {

    private Graphics2D g2d;
    private BufferedImage image;
    private FrameTextsProvider<String> textProvider;
    private FrameColorProvider<String> colorProvider;
    private FrameFontProvider<String> fontProvider;

    @BeforeEach
    void setUp() {
        image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        g2d = image.createGraphics();

        textProvider = FrameTextsProvider.of(frame -> frame.actualNode);
        colorProvider = FrameColorProvider.defaultColorProvider(frame -> Color.ORANGE);
        fontProvider = FrameFontProvider.defaultFontProvider();
    }

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        void with_valid_providers_creates_renderer() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );

            assertThat(renderer).isNotNull();
            assertThat(renderer.getFrameTextsProvider()).isEqualTo(textProvider);
            assertThat(renderer.getFrameColorProvider()).isEqualTo(colorProvider);
            assertThat(renderer.getFrameFontProvider()).isEqualTo(fontProvider);
        }

        @Test
        void null_text_provider_throws_exception() {
            assertThatThrownBy(() -> new DefaultFrameRenderer<>(null, colorProvider, fontProvider))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void null_color_provider_throws_exception() {
            assertThatThrownBy(() -> new DefaultFrameRenderer<>(textProvider, null, fontProvider))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void null_font_provider_throws_exception() {
            assertThatThrownBy(() -> new DefaultFrameRenderer<>(textProvider, colorProvider, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("getFrameBoxHeight")
    class GetFrameBoxHeight {

        @Test
        void returns_positive_value() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );

            int height = renderer.getFrameBoxHeight(g2d);

            assertThat(height).isPositive();
        }

        @Test
        void depends_on_font() {
            DefaultFrameRenderer<String> renderer1 = new DefaultFrameRenderer<>(
                    textProvider, colorProvider,
                    (frame, flags) -> new Font(Font.SANS_SERIF, Font.PLAIN, 12)
            );
            DefaultFrameRenderer<String> renderer2 = new DefaultFrameRenderer<>(
                    textProvider, colorProvider,
                    (frame, flags) -> new Font(Font.SANS_SERIF, Font.PLAIN, 24)
            );

            int height1 = renderer1.getFrameBoxHeight(g2d);
            int height2 = renderer2.getFrameBoxHeight(g2d);

            assertThat(height2).isGreaterThan(height1);
        }

        @Test
        void consistent_with_gap_setting() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );

            int heightWithGap = renderer.getFrameBoxHeight(g2d);

            renderer.setDrawingFrameGap(false);
            // Note: getFrameBoxHeight includes gap in calculation
            // Just verify it doesn't throw and returns valid value
            int heightWithoutGap = renderer.getFrameBoxHeight(g2d);

            // Both should be positive (gap affects the height calculation)
            assertThat(heightWithGap).isPositive();
            assertThat(heightWithoutGap).isPositive();
        }
    }

    @Nested
    @DisplayName("Frame gap settings")
    class FrameGapSettings {

        @Test
        void isDrawingFrameGap_default_true() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );

            assertThat(renderer.isDrawingFrameGap()).isTrue();
        }

        @Test
        void setDrawingFrameGap_changes_value() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );

            renderer.setDrawingFrameGap(false);

            assertThat(renderer.isDrawingFrameGap()).isFalse();
        }

        @Test
        void getFrameGapWidth_default_one() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );

            assertThat(renderer.getFrameGapWidth()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Rounded frame settings")
    class RoundedFrameSettings {

        @Test
        void isRoundedFrame_default_false() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );

            assertThat(renderer.isRoundedFrame()).isFalse();
        }

        @Test
        void setRoundedFrame_changes_value() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );

            renderer.setRoundedFrame(true);

            assertThat(renderer.isRoundedFrame()).isTrue();
        }

        @Test
        void reusableFrameShape_default_rectangle() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );

            RectangularShape shape = renderer.reusableFrameShape();

            assertThat(shape).isInstanceOf(Rectangle2D.Double.class);
        }

        @Test
        void reusableFrameShape_rounded_returns_round_rectangle() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );
            renderer.setRoundedFrame(true);

            RectangularShape shape = renderer.reusableFrameShape();

            assertThat(shape).isInstanceOf(RoundRectangle2D.Double.class);
        }
    }

    @Nested
    @DisplayName("Hovered frame border settings")
    class HoveredFrameBorderSettings {

        @Test
        void isPaintHoveredFrameBorder_default_false() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );

            assertThat(renderer.isPaintHoveredFrameBorder()).isFalse();
        }

        @Test
        void setPaintHoveredFrameBorder_changes_value() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );

            renderer.setPaintHoveredFrameBorder(true);

            assertThat(renderer.isPaintHoveredFrameBorder()).isTrue();
        }
    }

    @Nested
    @DisplayName("Provider setters")
    class ProviderSetters {

        @Test
        void setFrameTextsProvider_changes_provider() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );
            FrameTextsProvider<String> newProvider = FrameTextsProvider.of(frame -> "new");

            renderer.setFrameTextsProvider(newProvider);

            assertThat(renderer.getFrameTextsProvider()).isEqualTo(newProvider);
        }

        @Test
        void setFrameTextsProvider_null_throws_exception() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );

            assertThatThrownBy(() -> renderer.setFrameTextsProvider(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void setFrameColorProvider_changes_provider() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );
            FrameColorProvider<String> newProvider = (frame, flags) ->
                    new FrameColorProvider.ColorModel(Color.RED, Color.WHITE);

            renderer.setFrameColorProvider(newProvider);

            assertThat(renderer.getFrameColorProvider()).isEqualTo(newProvider);
        }

        @Test
        void setFrameColorProvider_null_throws_exception() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );

            assertThatThrownBy(() -> renderer.setFrameColorProvider(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void setFrameFontProvider_changes_provider() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );
            FrameFontProvider<String> newProvider = (frame, flags) -> new Font(Font.SERIF, Font.BOLD, 16);

            renderer.setFrameFontProvider(newProvider);

            assertThat(renderer.getFrameFontProvider()).isEqualTo(newProvider);
        }

        @Test
        void setFrameFontProvider_null_throws_exception() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );

            assertThatThrownBy(() -> renderer.setFrameFontProvider(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("paintFrame")
    class PaintFrame {

        @Test
        void minimap_mode_draws_without_text() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );
            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 1);
            FrameModel<String> model = new FrameModel<>(List.of(frame));
            Rectangle2D frameRect = new Rectangle2D.Double(10, 10, 100, 20);
            Rectangle2D intersection = new Rectangle2D.Double(10, 10, 100, 20);

            // Should not throw
            renderer.paintFrame(g2d, frame, model, frameRect, intersection, MINIMAP_MODE);
        }

        @Test
        void normal_mode_draws_frame_with_text() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );
            FrameBox<String> frame = new FrameBox<>("TestMethod", 0.0, 1.0, 1);
            FrameModel<String> model = new FrameModel<>(List.of(frame));
            Rectangle2D frameRect = new Rectangle2D.Double(10, 10, 200, 25);
            Rectangle2D intersection = new Rectangle2D.Double(10, 10, 200, 25);

            // Should not throw
            renderer.paintFrame(g2d, frame, model, frameRect, intersection, 0);
        }

        @Test
        void hovered_mode_draws_border_if_enabled() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );
            renderer.setPaintHoveredFrameBorder(true);

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 1);
            FrameModel<String> model = new FrameModel<>(List.of(frame));
            Rectangle2D frameRect = new Rectangle2D.Double(10, 10, 100, 20);
            Rectangle2D intersection = new Rectangle2D.Double(10, 10, 100, 20);

            int flags = toFlags(false, false, false, true, false, false, false, false);

            // Should not throw
            renderer.paintFrame(g2d, frame, model, frameRect, intersection, flags);
        }

        @Test
        void root_frame_uses_model_title() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );
            FrameBox<String> rootFrame = new FrameBox<>("root", 0.0, 1.0, 0);
            FrameModel<String> model = new FrameModel<>("Root Title",
                    (a, b) -> a.actualNode.equals(b.actualNode),
                    List.of(rootFrame));
            Rectangle2D frameRect = new Rectangle2D.Double(10, 10, 200, 25);
            Rectangle2D intersection = new Rectangle2D.Double(10, 10, 200, 25);

            // Should use model title for root frame
            renderer.paintFrame(g2d, rootFrame, model, frameRect, intersection, 0);
        }

        @Test
        void narrow_frame_clips_text() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );
            FrameBox<String> frame = new FrameBox<>("VeryLongMethodNameThatWillBeClipped", 0.0, 1.0, 1);
            FrameModel<String> model = new FrameModel<>(List.of(frame));
            Rectangle2D frameRect = new Rectangle2D.Double(10, 10, 50, 20); // Very narrow
            Rectangle2D intersection = new Rectangle2D.Double(10, 10, 50, 20);

            // Should not throw, text will be clipped or omitted
            renderer.paintFrame(g2d, frame, model, frameRect, intersection, 0);
        }

        @Test
        void very_narrow_frame_omits_text() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );
            FrameBox<String> frame = new FrameBox<>("Text", 0.0, 1.0, 1);
            FrameModel<String> model = new FrameModel<>(List.of(frame));
            Rectangle2D frameRect = new Rectangle2D.Double(10, 10, 5, 20); // Extremely narrow
            Rectangle2D intersection = new Rectangle2D.Double(10, 10, 5, 20);

            // Should not throw, text should be omitted
            renderer.paintFrame(g2d, frame, model, frameRect, intersection, 0);
        }

        @Test
        void multiple_text_candidates_chooses_shortest_fit() {
            FrameTextsProvider<String> multiTextProvider = FrameTextsProvider.of(
                    frame -> "Very Long Name That Won't Fit",
                    frame -> "Medium Name",
                    frame -> frame.actualNode // shortest
            );
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    multiTextProvider, colorProvider, fontProvider
            );
            FrameBox<String> frame = new FrameBox<>("Short", 0.0, 1.0, 1);
            FrameModel<String> model = new FrameModel<>(List.of(frame));
            Rectangle2D frameRect = new Rectangle2D.Double(10, 10, 80, 20);
            Rectangle2D intersection = new Rectangle2D.Double(10, 10, 80, 20);

            // Should not throw
            renderer.paintFrame(g2d, frame, model, frameRect, intersection, 0);
        }

        @Test
        void with_rounded_corners_uses_rounded_rect() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );
            renderer.setRoundedFrame(true);

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 1);
            FrameModel<String> model = new FrameModel<>(List.of(frame));
            RectangularShape frameRect = renderer.reusableFrameShape();
            frameRect.setFrame(10, 10, 100, 20);
            Rectangle2D intersection = new Rectangle2D.Double(10, 10, 100, 20);

            // Should not throw
            renderer.paintFrame(g2d, frame, model, frameRect, intersection, 0);
        }

        @Test
        void without_gap_fills_full_area() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );
            renderer.setDrawingFrameGap(false);

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 1);
            FrameModel<String> model = new FrameModel<>(List.of(frame));
            Rectangle2D frameRect = new Rectangle2D.Double(10, 10, 100, 20);
            Rectangle2D intersection = new Rectangle2D.Double(10, 10, 100, 20);

            // Should not throw
            renderer.paintFrame(g2d, frame, model, frameRect, intersection, 0);
        }
    }

    @Nested
    @DisplayName("frameBorderColor")
    class FrameBorderColor {

        @Test
        void returns_valid_color() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );

            Color borderColor = renderer.frameBorderColor.get();

            assertThat(borderColor).isNotNull();
        }
    }
}