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
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("ui")
@Timeout(20)
class FlamegraphView_SelectionUiTest {
    @ParameterizedTest
    @CsvSource({"1, 2", "3, 1"})
    void only_a_single_left_click_invokes_the_selection_consumer(int button, int clickCount) {
        var model = FrameModelFixture.rootWithSiblings();
        var target = model.frames.get(1);
        var renderer = new RecordingFrameRenderer<String>();
        var selections = new ArrayList<Callback>();
        try (var graph = FlamegraphViewFixture.<String>builder().model(model).renderer(renderer)
                                             .mode(FlamegraphView.Mode.ICICLEGRAPH).build()) {
            graph.onEdt(() -> graph.view().setSelectedFrameConsumer(
                    (frame, event) -> selections.add(new Callback(frame, event))));
            graph.await("target rendering", () -> renderer.mainPaint(target) != null);
            var point = graph.onEdt(() -> {
                var bounds = renderer.mainPaint(target).bounds;
                return new Point((int) bounds.getCenterX(), (int) bounds.getCenterY());
            });
            graph.pointerAt(point);
            graph.onEdt(() -> {
                graph.canvas().dispatchEvent(event(graph, MouseEvent.MOUSE_CLICKED, point, MouseEvent.BUTTON1, false));
                assertEquals(1, selections.size());
                assertSame(target, selections.get(0).frame);

                var ignored = new MouseEvent(graph.canvas(), MouseEvent.MOUSE_CLICKED,
                                             System.currentTimeMillis(), 0, point.x, point.y,
                                             clickCount, false, button);
                graph.canvas().dispatchEvent(ignored);
                assertEquals(1, selections.size(), "double and non-left clicks must not select a frame");
            });
        }
    }

    @ParameterizedTest
    @EnumSource(FlamegraphView.Mode.class)
    void selection_and_both_popup_triggers_hit_real_frames_before_and_after_navigation(FlamegraphView.Mode mode) {
        var frames = new ArrayList<>(FrameModelFixture.deepChain(24).frames);
        // Below level two, only the left half of the branch remains: empty space stays visible after zoom.
        for (int level = 3; level < frames.size(); level++) {
            frames.set(level, new FrameBox<>(frames.get(level).actualNode, .25, .5, level));
        }
        var model = new FrameModel<>(frames);
        var target = model.frames.get(3);
        var renderer = new RecordingFrameRenderer<String>();
        var selections = new ArrayList<Callback>();
        var popups = new ArrayList<Callback>();
        try (var graph = FlamegraphViewFixture.<String>builder().model(model).mode(mode)
                                             .renderer(renderer).size(640, 300).build()) {
            graph.onEdt(() -> {
                graph.view().setSelectedFrameConsumer((frame, event) -> selections.add(new Callback(frame, event)));
                graph.view().setPopupConsumer((frame, event) -> popups.add(new Callback(frame, event)));
            });
            graph.await("initial target rendering", () -> renderer.mainPaint(target) != null);
            assertCallbacks(graph, renderer, target, selections, popups);

            int originalWidth = graph.onEdt(() -> graph.canvas().getWidth());
            graph.onEdt(() -> {
                renderer.clear();
                graph.view().zoomTo(model.frames.get(1));
            });
            graph.await("zoomed target rendering", () -> graph.canvas().getWidth() > originalWidth
                                                          && renderer.mainPaint(target) != null);
            int rowHeight = graph.onEdt(() -> renderer.mainPaint(target).bounds.height);
            var beforeScroll = graph.onEdt(() -> graph.viewport().getViewPosition());
            var scrolled = new Point(beforeScroll.x,
                                     beforeScroll.y + (mode == FlamegraphView.Mode.ICICLEGRAPH ? 2 : -2) * rowHeight);
            graph.onEdt(() -> {
                renderer.clear();
                graph.viewport().setViewPosition(scrolled);
                graph.canvas().repaint();
            });
            graph.await("scrolled target rendering", () -> graph.viewport().getViewPosition().equals(scrolled)
                                                            && renderer.mainPaint(target) != null);
            assertNotEquals(beforeScroll, scrolled);
            assertCallbacks(graph, renderer, target, selections, popups);
        }
    }

    private static void assertCallbacks(FlamegraphViewFixture<String> graph, RecordingFrameRenderer<String> renderer,
                                        FrameBox<String> target, List<Callback> selections, List<Callback> popups) {
        Point point = graph.onEdt(() -> {
            var visible = renderer.mainPaint(target).bounds.intersection(graph.canvas().getVisibleRect());
            assertTrue(visible.width > 0 && visible.height > 0, "target must be visibly clickable");
            return new Point((int) visible.getCenterX(), (int) visible.getCenterY());
        });
        graph.pointerAt(point);
        graph.onEdt(() -> {
            selections.clear();
            var click = event(graph, MouseEvent.MOUSE_CLICKED, point, MouseEvent.BUTTON1, false);
            graph.canvas().dispatchEvent(click);
            assertEquals(1, selections.size());
            assertSame(target, selections.get(0).frame);
            assertSame(click, selections.get(0).event);

            for (int trigger : new int[]{MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED}) {
                popups.clear();
                var popup = event(graph, trigger, point, MouseEvent.BUTTON3, true);
                graph.canvas().dispatchEvent(popup);
                assertEquals(1, popups.size(), "one callback for popup trigger " + trigger);
                assertSame(target, popups.get(0).frame);
                assertSame(popup, popups.get(0).event);

                graph.canvas().dispatchEvent(event(graph, trigger, point, MouseEvent.BUTTON3, false));
                assertEquals(1, popups.size(), "a right click without the popup flag is not a trigger");
            }
        });

        var empty = graph.onEdt(() -> {
            var visible = graph.canvas().getVisibleRect();
            return new Point(visible.x + visible.width - 2, point.y);
        });
        graph.pointerAt(empty);
        graph.onEdt(() -> {
            selections.clear();
            popups.clear();
            graph.canvas().dispatchEvent(event(graph, MouseEvent.MOUSE_CLICKED, empty, MouseEvent.BUTTON1, false));
            graph.canvas().dispatchEvent(event(graph, MouseEvent.MOUSE_PRESSED, empty, MouseEvent.BUTTON3, true));
            graph.canvas().dispatchEvent(event(graph, MouseEvent.MOUSE_RELEASED, empty, MouseEvent.BUTTON3, true));
            assertTrue(selections.isEmpty(), "empty space has no selection callback");
            assertTrue(popups.isEmpty(), "empty space has no popup callback");
        });
    }

    private static MouseEvent event(FlamegraphViewFixture<String> graph, int id, Point point, int button, boolean popup) {
        return new MouseEvent(graph.canvas(), id, System.currentTimeMillis(), 0,
                              point.x, point.y, 1, popup, button);
    }

    private static final class Callback {
        private final FrameBox<String> frame;
        private final MouseEvent event;

        private Callback(FrameBox<String> frame, MouseEvent event) {
            this.frame = frame;
            this.event = event;
        }
    }
}
