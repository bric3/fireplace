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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.bric3.fireplace.flamegraph.FlamegraphView.Mode.FLAMEGRAPH;
import static io.github.bric3.fireplace.flamegraph.FlamegraphView.Mode.ICICLEGRAPH;
import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.isMinimapMode;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("ui")
class FlamegraphView_MinimapUiTest {
    // Distinct thumbnail colors locate the painted image, without accessing private minimap geometry.
    private static final Color ROOT = Color.MAGENTA;
    private static final Color CHILD = Color.CYAN;
    private static final Color REPLACEMENT = Color.GREEN;

    @Test
    void minimap_press_and_drag_navigate_to_bounds_and_disabled_minimap_does_not() {
        var model = FrameModelFixture.deepChain(35);
        try (var graph = graph(model)) {
            graph.onEdt(() -> graph.view().zoomTo(model.frames.get(1)));
            graph.await("zoomed scrollable canvas", () -> graph.canvas().getWidth() > graph.viewport().getWidth()
                    && graph.canvas().getHeight() > graph.viewport().getHeight());
            enableMinimap(graph);
            graph.onEdt(() -> graph.viewport().setViewPosition(new Point()));

            dispatchInMinimap(graph, MouseEvent.MOUSE_PRESSED, .99, .99);
            graph.await("minimap press reaches lower right bounds", () -> atMaximum(graph));
            assertThat(graph.onEdt(() -> graph.viewport().getViewPosition().x)).isPositive();
            assertThat(graph.onEdt(() -> graph.viewport().getViewPosition().y)).isPositive();

            dispatchInMinimap(graph, MouseEvent.MOUSE_DRAGGED, .01, .01);
            graph.await("minimap drag reaches upper left bounds", () -> graph.viewport().getViewPosition().equals(new Point()));
            dispatchInMinimap(graph, MouseEvent.MOUSE_DRAGGED, .99, .99);
            graph.await("minimap drag reaches lower right bounds", () -> atMaximum(graph));
            dispatchInMinimap(graph, MouseEvent.MOUSE_RELEASED, .99, .99);

            var thumbnail = thumbnailBounds(paintViewport(graph));
            graph.onEdt(() -> {
                graph.view().setShowMinimap(false);
                graph.viewport().setViewPosition(new Point());
                graph.canvas().setToolTipText("graph tooltip");
            });
            assertThat(thumbnailBounds(paintViewport(graph)).isEmpty()).isTrue();
            graph.onEdt(() -> {
                var point = SwingUtilities.convertPoint(graph.viewport(),
                        thumbnail.x + thumbnail.width - 2, thumbnail.y + thumbnail.height - 2, graph.canvas());
                var press = event(graph, MouseEvent.MOUSE_PRESSED, point);
                assertThat(graph.canvas().getToolTipText(press)).isEqualTo("graph tooltip");
                graph.canvas().dispatchEvent(press);
                graph.canvas().dispatchEvent(event(graph, MouseEvent.MOUSE_RELEASED, point));
                assertThat(graph.viewport().getViewPosition()).isEqualTo(new Point());
            });
        }
    }

    @Test
    void model_replacement_refreshes_the_displayed_thumbnail() {
        try (var graph = graph(FrameModelFixture.rootWithSiblings())) {
            enableMinimap(graph);
            assertThat(colorBounds(paintViewport(graph), ROOT).isEmpty()).isFalse();
            var replacement = new FrameModel<>(List.of(
                    new FrameBox<>("replacement", 0, 1, 0),
                    new FrameBox<>("child", .25, .75, 1)));

            graph.onEdt(() -> graph.view().setModel(replacement));
            graph.await("replacement thumbnail installed", () -> !colorBounds(paintViewport(graph), REPLACEMENT).isEmpty());

            var image = paintViewport(graph);
            assertThat(colorBounds(image, ROOT).isEmpty()).isTrue();
            assertThat(colorBounds(image, CHILD).isEmpty()).isFalse();
        }
    }

    @Test
    void mode_and_window_layout_move_and_reorient_the_thumbnail() {
        try (var graph = graph(FrameModelFixture.rootWithSiblings())) {
            enableMinimap(graph);
            var initial = paintViewport(graph);
            var initialBounds = thumbnailBounds(initial);
            assertThat(colorBounds(initial, ROOT).getCenterY()).isLessThan(colorBounds(initial, CHILD).getCenterY());

            graph.onEdt(() -> graph.view().setMode(FLAMEGRAPH));
            graph.await("flamegraph thumbnail refreshed", () -> {
                var image = paintViewport(graph);
                return thumbnailBounds(image).y < initialBounds.y
                        && colorBounds(image, ROOT).getCenterY() > colorBounds(image, CHILD).getCenterY();
            });
            var flamegraphBounds = thumbnailBounds(paintViewport(graph));

            graph.onEdt(() -> graph.view().setMode(ICICLEGRAPH));
            graph.await("icicle thumbnail refreshed", () -> {
                var image = paintViewport(graph);
                return thumbnailBounds(image).y > flamegraphBounds.y
                        && colorBounds(image, ROOT).getCenterY() < colorBounds(image, CHILD).getCenterY();
            });
            var restored = paintViewport(graph);
            var restoredBounds = thumbnailBounds(restored);
            assertThat(restored.getHeight() - restoredBounds.y).isEqualTo(initial.getHeight() - initialBounds.y);
            int originalHeight = graph.onEdt(() -> graph.viewport().getHeight());
            graph.onEdt(() -> graph.window().setSize(graph.window().getWidth() + 80, graph.window().getHeight() + 90));
            graph.await("thumbnail follows resized viewport", () -> {
                int delta = graph.viewport().getHeight() - originalHeight;
                return delta > 0 && thumbnailBounds(paintViewport(graph)).y == restoredBounds.y + delta;
            });
        }
    }

    @Test
    void entering_minimap_stops_frame_hover_and_suppresses_its_tooltip() {
        var model = FrameModelFixture.rootWithSiblings();
        try (var graph = graph(model)) {
            enableMinimap(graph);
            var hovered = new AtomicReference<FrameBox<String>>();
            var stoppedBy = new AtomicReference<MouseEvent>();
            graph.onEdt(() -> {
                graph.view().setTooltipTextFunction((data, frame) -> frame == null ? "" : frame.actualNode);
                graph.view().setHoverListener(new FlamegraphView.HoverListener<>() {
                    @Override
                    public void onFrameHover(FrameBox<String> frame, Rectangle bounds, MouseEvent event) {
                        hovered.set(frame);
                    }

                    @Override
                    public void onStopHover(FrameBox<String> frame, Rectangle bounds, MouseEvent event) {
                        stoppedBy.set(event);
                    }
                });
            });
            var framePoint = new Point(20, 5);
            graph.pointerAt(framePoint);
            graph.onEdt(() -> graph.canvas().dispatchEvent(event(graph, MouseEvent.MOUSE_MOVED, framePoint)));
            graph.await("root hover", () -> hovered.get() == model.frames.get(0));

            var point = minimapPoint(graph, .5, .5);
            graph.pointerAt(point);
            var moved = graph.onEdt(() -> event(graph, MouseEvent.MOUSE_MOVED, point));
            graph.onEdt(() -> {
                hovered.set(null);
                graph.canvas().dispatchEvent(moved);
            });
            graph.await("minimap hover stop callback", () -> stoppedBy.get() != null
                    && stoppedBy.get().getWhen() == moved.getWhen());
            assertThat(hovered.get()).isNull();
            assertThat(graph.onEdt(() -> graph.canvas().getToolTipText(moved))).isEmpty();
        }
    }

    private static FlamegraphViewFixture<String> graph(FrameModel<String> model) {
        var renderer = new DefaultFrameRenderer<String>(FrameTextsProvider.of(frame -> ""), (frame, flags) -> {
            var color = !isMinimapMode(flags) ? Color.ORANGE
                    : frame.actualNode.equals("replacement") ? REPLACEMENT
                    : frame.isRoot() ? ROOT : CHILD;
            return new FrameColorProvider.ColorModel(color, Color.BLACK);
        }, FrameFontProvider.defaultFontProvider());
        return FlamegraphViewFixture.<String>builder().model(model).mode(ICICLEGRAPH)
                .renderer(renderer).size(720, 460).build();
    }

    private static void enableMinimap(FlamegraphViewFixture<String> graph) {
        graph.onEdt(() -> {
            graph.view().setMinimapShadeColorSupplier(() -> new Color(0, 0, 0, 0));
            graph.view().setShowMinimap(true);
        });
        graph.await("thumbnail rendered and installed", () -> !colorBounds(paintViewport(graph), CHILD).isEmpty());
    }

    private static boolean atMaximum(FlamegraphViewFixture<String> graph) {
        var viewport = graph.viewport();
        var position = viewport.getViewPosition();
        var size = viewport.getViewSize();
        var extent = viewport.getExtentSize();
        return position.x == size.width - extent.width && position.y == size.height - extent.height;
    }

    private static void dispatchInMinimap(FlamegraphViewFixture<String> graph, int id, double x, double y) {
        var point = minimapPoint(graph, x, y);
        graph.onEdt(() -> {
            var input = event(graph, id, point);
            assertThat(graph.canvas().getToolTipText(input)).as("point lies in the displayed minimap").isEmpty();
            graph.canvas().dispatchEvent(input);
        });
    }

    private static Point minimapPoint(FlamegraphViewFixture<String> graph, double x, double y) {
        var bounds = thumbnailBounds(paintViewport(graph));
        assertThat(bounds.isEmpty()).as("visible thumbnail pixels").isFalse();
        return graph.onEdt(() -> SwingUtilities.convertPoint(graph.viewport(),
                bounds.x + (int) (bounds.width * x), bounds.y + (int) (bounds.height * y), graph.canvas()));
    }

    private static MouseEvent event(FlamegraphViewFixture<String> graph, int id, Point point) {
        boolean button = id == MouseEvent.MOUSE_PRESSED || id == MouseEvent.MOUSE_RELEASED;
        return new MouseEvent(graph.canvas(), id, System.currentTimeMillis(),
                id == MouseEvent.MOUSE_DRAGGED ? MouseEvent.BUTTON1_DOWN_MASK : 0,
                point.x, point.y, button ? 1 : 0, false, button ? MouseEvent.BUTTON1 : MouseEvent.NOBUTTON);
    }

    private static BufferedImage paintViewport(FlamegraphViewFixture<?> graph) {
        return graph.onEdt(() -> {
            var viewport = graph.viewport();
            var image = new BufferedImage(viewport.getWidth(), viewport.getHeight(), BufferedImage.TYPE_INT_ARGB);
            var graphics = image.createGraphics();
            try {
                viewport.paint(graphics);
            } finally {
                graphics.dispose();
            }
            return image;
        });
    }

    private static Rectangle thumbnailBounds(BufferedImage image) {
        var bounds = colorBounds(image, ROOT);
        bounds.add(colorBounds(image, CHILD));
        bounds.add(colorBounds(image, REPLACEMENT));
        return bounds;
    }

    private static Rectangle colorBounds(BufferedImage image, Color color) {
        var bounds = new Rectangle(0, 0, -1, -1);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) == color.getRGB()) {
                    bounds.add(new Rectangle(x, y, 1, 1));
                }
            }
        }
        return bounds;
    }
}
