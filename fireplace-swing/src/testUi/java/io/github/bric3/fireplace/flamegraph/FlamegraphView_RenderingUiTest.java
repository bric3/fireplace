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

import io.github.bric3.fireplace.flamegraph.fixtures.FlamegraphViewFixture;
import io.github.bric3.fireplace.flamegraph.fixtures.FrameModelFixture;
import io.github.bric3.fireplace.flamegraph.fixtures.RecordingFrameRenderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.*;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("ui")
class FlamegraphView_RenderingUiTest {
    @Test
    void highlight_replace_and_clear_update_flags_and_painted_colors() {
        var model = FrameModelFixture.rootWithSiblings();
        var left = model.frames.get(1);
        var right = model.frames.get(3);
        var renderer = new RecordingFrameRenderer<String>();
        try (var graph = FlamegraphViewFixture.<String>builder().model(model).renderer(renderer).build()) {
            graph.await("initial frame paints", () -> renderer.mainPaint(left) != null && renderer.mainPaint(right) != null);
            var baseline = paint(graph);
            int leftColor = centerColor(baseline, renderer.mainPaint(left).bounds);
            int rightColor = centerColor(baseline, renderer.mainPaint(right).bounds);

            graph.onEdt(() -> {
                renderer.clear();
                graph.view().highlightFrames(Set.of(left), "left");
            });
            awaitHighlights(graph, renderer, left, right);
            assertThat(renderer.mainPaint(left).flags & (HIGHLIGHTING | HIGHLIGHTED_FRAME))
                    .isEqualTo(HIGHLIGHTING | HIGHLIGHTED_FRAME);
            assertThat(renderer.mainPaint(right).flags & (HIGHLIGHTING | HIGHLIGHTED_FRAME))
                    .isEqualTo(HIGHLIGHTING);
            var highlighted = paint(graph);
            assertThat(centerColor(highlighted, renderer.mainPaint(left).bounds)).isEqualTo(leftColor);
            assertThat(centerColor(highlighted, renderer.mainPaint(right).bounds)).isNotEqualTo(rightColor);

            graph.onEdt(() -> {
                renderer.clear();
                graph.view().highlightFrames(Set.of(right), "right");
            });
            awaitHighlights(graph, renderer, left, right);
            assertThat(renderer.mainPaint(left).flags & (HIGHLIGHTING | HIGHLIGHTED_FRAME)).isEqualTo(HIGHLIGHTING);
            assertThat(renderer.mainPaint(right).flags & (HIGHLIGHTING | HIGHLIGHTED_FRAME))
                    .isEqualTo(HIGHLIGHTING | HIGHLIGHTED_FRAME);

            graph.onEdt(() -> {
                renderer.clear();
                graph.view().highlightFrames(Set.of(), "");
            });
            awaitHighlights(graph, renderer, left, right);
            assertThat(renderer.mainPaint(left).flags & (HIGHLIGHTING | HIGHLIGHTED_FRAME)).isZero();
            assertThat(renderer.mainPaint(right).flags & (HIGHLIGHTING | HIGHLIGHTED_FRAME)).isZero();
            var cleared = paint(graph);
            assertThat(centerColor(cleared, renderer.mainPaint(left).bounds)).isEqualTo(leftColor);
            assertThat(centerColor(cleared, renderer.mainPaint(right).bounds)).isEqualTo(rightColor);
        }
    }

    @Test
    void replacing_renderer_repaints_existing_model_with_new_configuration() {
        var model = FrameModelFixture.rootWithSiblings();
        var frame = model.frames.get(1);
        var initial = new RecordingFrameRenderer<String>();
        try (var graph = FlamegraphViewFixture.<String>builder().model(model).renderer(initial).build()) {
            graph.await("initial paint", () -> initial.mainPaint(frame) != null);
            assertThat(centerColor(paint(graph), initial.mainPaint(frame).bounds)).isEqualTo(Color.ORANGE.getRGB());

            var replacement = new RecordingFrameRenderer<>(new DefaultFrameRenderer<String>(
                    FrameTextsProvider.of(node -> ""),
                    (node, flags) -> new FrameColorProvider.ColorModel(Color.BLUE, Color.WHITE),
                    FrameFontProvider.defaultFontProvider()));
            graph.onEdt(() -> graph.view().setFrameRender(replacement));
            graph.await("replacement renderer repaint", () -> replacement.mainPaint(frame) != null);

            assertThat(centerColor(paint(graph), replacement.mainPaint(frame).bounds)).isEqualTo(Color.BLUE.getRGB());
            assertThat(replacement.mainPaint(frame).bounds.width).isPositive();
        }
    }

    @Test
    void request_repaint_applies_changes_to_the_existing_renderer() {
        var model = FrameModelFixture.rootWithSiblings();
        var frame = model.frames.get(1);
        var configuredRenderer = new DefaultFrameRenderer<String>(
                FrameTextsProvider.of(node -> ""),
                (node, flags) -> new FrameColorProvider.ColorModel(Color.ORANGE, Color.BLACK),
                FrameFontProvider.defaultFontProvider());
        var renderer = new RecordingFrameRenderer<>(configuredRenderer);
        try (var graph = FlamegraphViewFixture.<String>builder().model(model).renderer(renderer).build()) {
            graph.await("initial paint", () -> renderer.mainPaint(frame) != null);
            assertThat(centerColor(paint(graph), renderer.mainPaint(frame).bounds)).isEqualTo(Color.ORANGE.getRGB());

            graph.onEdt(() -> {
                renderer.clear();
                configuredRenderer.setFrameColorProvider(
                        (node, flags) -> new FrameColorProvider.ColorModel(Color.BLUE, Color.WHITE));
                graph.view().requestRepaint();
            });
            graph.await("requested repaint", () -> renderer.mainPaint(frame) != null);
            assertThat(centerColor(paint(graph), renderer.mainPaint(frame).bounds)).isEqualTo(Color.BLUE.getRGB());
        }
    }

    @Test
    void sibling_setting_controls_equal_nodes_on_subsequent_hover_entries() {
        var left = new FrameBox<>("same call", 0, .5, 1);
        var right = new FrameBox<>("same call", .5, 1, 1);
        var root = new FrameBox<>("root", 0, 1, 0);
        var model = new FrameModel<>(List.of(root, left, right));
        var renderer = new RecordingFrameRenderer<String>();
        try (var graph = FlamegraphViewFixture.<String>builder().model(model).renderer(renderer).build()) {
            graph.await("initial frame paints", () -> renderer.mainPaint(root) != null && renderer.mainPaint(left) != null);
            for (boolean enabled : new boolean[]{true, false, true}) {
                graph.onEdt(() -> graph.view().setShowHoveredSiblings(enabled));
                hover(graph, renderer, root);
                hover(graph, renderer, left);
                paint(graph);

                assertThat(isHovered(renderer.mainPaint(left).flags)).isTrue();
                assertThat(isHovered(renderer.mainPaint(right).flags)).isFalse();
                assertThat(isHoveredSibling(renderer.mainPaint(right).flags)).isEqualTo(enabled);
                assertThat(isHoveredSibling(renderer.mainPaint(root).flags)).isFalse();
            }
        }
    }

    private static void awaitHighlights(FlamegraphViewFixture<String> graph,
                                        RecordingFrameRenderer<String> renderer,
                                        FrameBox<String> left, FrameBox<String> right) {
        graph.await("highlight repaint", () -> renderer.mainPaint(left) != null && renderer.mainPaint(right) != null);
    }

    private static void hover(FlamegraphViewFixture<String> graph, RecordingFrameRenderer<String> renderer,
                              FrameBox<String> frame) {
        var bounds = renderer.mainPaint(frame).bounds;
        var point = new Point((int) bounds.getCenterX(), (int) bounds.getCenterY());
        var delivered = new AtomicBoolean();
        graph.pointerAt(point);
        graph.onEdt(() -> {
            var event = new MouseEvent(graph.canvas(), MouseEvent.MOUSE_MOVED,
                    System.currentTimeMillis(), 0, point.x, point.y, 0, false);
            graph.view().setHoverListener(new FlamegraphView.HoverListener<>() {
                @Override
                public void onFrameHover(FrameBox<String> hovered, Rectangle rectangle, MouseEvent input) {
                    if (hovered == frame && input.getWhen() == event.getWhen()) {
                        delivered.set(true);
                    }
                }
            });
            graph.canvas().dispatchEvent(event);
        });
        graph.await("hover callback for " + frame.actualNode, delivered::get);
    }

    private static BufferedImage paint(FlamegraphViewFixture<?> graph) {
        return graph.onEdt(() -> {
            var canvas = graph.canvas();
            var image = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
            var graphics = image.createGraphics();
            try {
                canvas.paint(graphics);
            } finally {
                graphics.dispose();
            }
            return image;
        });
    }

    private static int centerColor(BufferedImage image, Rectangle bounds) {
        return image.getRGB((int) bounds.getCenterX(), (int) bounds.getCenterY());
    }
}
