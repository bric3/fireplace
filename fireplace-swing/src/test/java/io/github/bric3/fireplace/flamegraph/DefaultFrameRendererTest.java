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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import javax.swing.UIManager;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
        g2d = mock(Graphics2D.class, delegatesTo(image.createGraphics()));

        textProvider = FrameTextsProvider.of(frame -> frame.actualNode);
        colorProvider = FrameColorProvider.defaultColorProvider(frame -> Color.ORANGE);
        fontProvider = FrameFontProvider.defaultFontProvider();
    }

    @AfterEach
    void tearDown() {
        g2d.dispose();
    }

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        void with_valid_providers_creates_renderer() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );

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

        @ParameterizedTest
        @ValueSource(ints = {12, 24})
        void row_height_includes_font_ascent_and_padding(int fontSize) {
            var font = new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);
            var renderer = new DefaultFrameRenderer<>(textProvider, colorProvider, (frame, flags) -> font);

            // Two pixels of text padding and one pixel of gap on either side of the ascent.
            assertThat(renderer.getFrameBoxHeight(g2d)).isEqualTo(g2d.getFontMetrics(font).getAscent() + 6);
        }

        @Test
        void disabling_gap_preserves_row_height() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );

            int heightWithGap = renderer.getFrameBoxHeight(g2d);

            renderer.setDrawingFrameGap(false);
            int heightWithoutGap = renderer.getFrameBoxHeight(g2d);

            assertThat(heightWithoutGap).isEqualTo(heightWithGap);
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

            renderer.paintFrame(g2d, frame, model, frameRect, intersection, MINIMAP_MODE);

            assertThat(image.getRGB(10, 10)).isEqualTo(Color.ORANGE.getRGB());
            assertThat(image.getRGB(109, 29)).isEqualTo(Color.ORANGE.getRGB());
            assertThat(image.getRGB(110, 30)).isZero();
            verify(g2d, never()).drawString(anyString(), anyFloat(), anyFloat());
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

            renderer.paintFrame(g2d, frame, model, frameRect, intersection, 0);

            assertThat(image.getRGB(10, 10)).isEqualTo(Color.ORANGE.getRGB());
            assertThat(image.getRGB(208, 33)).isEqualTo(Color.ORANGE.getRGB());
            assertThat(image.getRGB(209, 10)).isZero();
            assertThat(image.getRGB(10, 34)).isZero();
            verify(g2d).drawString(eq("TestMethod"), anyFloat(), anyFloat());
        }

        @ParameterizedTest(name = "border enabled {0}, hovered {1}")
        @CsvSource({"false, false", "false, true", "true, false", "true, true"})
        void border_is_drawn_only_when_enabled_and_hovered(boolean borderEnabled, boolean hovered) {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );
            renderer.setPaintHoveredFrameBorder(borderEnabled);

            FrameBox<String> frame = new FrameBox<>("test", 0.0, 1.0, 1);
            FrameModel<String> model = new FrameModel<>(List.of(frame));
            Rectangle2D frameRect = new Rectangle2D.Double(10, 10, 100, 20);
            Rectangle2D intersection = new Rectangle2D.Double(10, 10, 100, 20);

            int flags = hovered ? HOVERED : 0;

            renderer.paintFrame(g2d, frame, model, frameRect, intersection, flags);

            if (borderEnabled && hovered) {
                var border = ArgumentCaptor.forClass(Shape.class);
                verify(g2d).draw(border.capture());
                assertThat(g2d.getColor()).isEqualTo(renderer.frameBorderColor.get());
                assertThat(border.getValue().getBounds2D()).isEqualTo(new Rectangle2D.Double(9.5, 9.5, 100, 20));
            } else {
                verify(g2d, never()).draw(any(Shape.class));
            }
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

            renderer.paintFrame(g2d, rootFrame, model, frameRect, intersection, 0);

            verify(g2d).drawString(eq("Root Title"), anyFloat(), anyFloat());
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

            renderer.paintFrame(g2d, frame, model, frameRect, intersection, 0);

            var label = ArgumentCaptor.forClass(String.class);
            verify(g2d).drawString(label.capture(), anyFloat(), anyFloat());
            String drawnText = label.getValue();
            assertThat(drawnText).endsWith("…").hasSizeGreaterThan(2);
            assertThat(frame.actualNode).startsWith(drawnText.substring(0, drawnText.length() - 1));
            assertThat(g2d.getFontMetrics().stringWidth(drawnText)).isLessThanOrEqualTo(44);
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

            renderer.paintFrame(g2d, frame, model, frameRect, intersection, 0);

            assertThat(image.getRGB(10, 10)).isEqualTo(Color.ORANGE.getRGB());
            verify(g2d, never()).drawString(anyString(), anyFloat(), anyFloat());
        }

        @Test
        void multiple_text_candidates_chooses_first_fit() {
            FrameTextsProvider<String> multiTextProvider = FrameTextsProvider.of(
                    frame -> "Very Long Name That Won't Fit",
                    frame -> "Medium Name",
                    frame -> frame.actualNode
            );
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    multiTextProvider, colorProvider, fontProvider
            );
            FrameBox<String> frame = new FrameBox<>("Short", 0.0, 1.0, 1);
            FrameModel<String> model = new FrameModel<>(List.of(frame));
            int width = g2d.getFontMetrics(fontProvider.getFont(frame, 0)).stringWidth("Medium Name") + 6;
            Rectangle2D frameRect = new Rectangle2D.Double(10, 10, width, 20);
            Rectangle2D intersection = new Rectangle2D.Double(10, 10, width, 20);

            renderer.paintFrame(g2d, frame, model, frameRect, intersection, 0);

            verify(g2d).drawString(eq("Medium Name"), anyFloat(), anyFloat());
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

            renderer.paintFrame(g2d, frame, model, frameRect, intersection, 0);

            assertThat(image.getRGB(10, 10)).isZero();
            assertThat(image.getRGB(15, 10)).isEqualTo(Color.ORANGE.getRGB());
            assertThat(image.getRGB(10, 15)).isEqualTo(Color.ORANGE.getRGB());
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

            renderer.paintFrame(g2d, frame, model, frameRect, intersection, 0);

            assertThat(image.getRGB(10, 10)).isEqualTo(Color.ORANGE.getRGB());
            assertThat(image.getRGB(109, 29)).isEqualTo(Color.ORANGE.getRGB());
            assertThat(image.getRGB(110, 29)).isZero();
            assertThat(image.getRGB(109, 30)).isZero();
        }
    }

    @Nested
    @DisplayName("frameBorderColor")
    class FrameBorderColor {

        @Test
        void follows_look_and_feel_focus_color_changes() {
            DefaultFrameRenderer<String> renderer = new DefaultFrameRenderer<>(
                    textProvider, colorProvider, fontProvider
            );

            Object previous = UIManager.put("Component.focusColor", Color.MAGENTA);
            try {
                assertThat(renderer.frameBorderColor.get()).isEqualTo(Color.MAGENTA);
                UIManager.put("Component.focusColor", Color.GREEN);
                assertThat(renderer.frameBorderColor.get()).isEqualTo(Color.GREEN);
            } finally {
                UIManager.put("Component.focusColor", previous);
            }
        }
    }
}
