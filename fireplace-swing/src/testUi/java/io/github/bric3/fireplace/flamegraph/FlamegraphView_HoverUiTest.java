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
import org.junit.jupiter.api.Timeout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@Tag("ui")
@Timeout(20)
class FlamegraphView_HoverUiTest {
    @Test
    void hover_callbacks_follow_frames_empty_space_and_exit_with_tooltip_configuration() {
        var model = FrameModelFixture.rootWithSiblings();
        var renderer = new RecordingFrameRenderer<String>();
        var left = model.frames.get(1);
        var leaf = model.frames.get(2);
        var right = model.frames.get(3);
        var hover = new HoverProbe();
        try (var graph = FlamegraphViewFixture.<String>builder().model(model).renderer(renderer)
                                             .mode(FlamegraphView.Mode.ICICLEGRAPH).build()) {
            var tooltip = graph.onEdt(() -> { return new JToolTip(); });
            var textChanges = new AtomicInteger();
            var creations = new AtomicInteger();
            var tooltipFrame = new AtomicReference<FrameBox<String>>();
            graph.onEdt(() -> {
                tooltip.setBackground(Color.YELLOW);
                graph.view().setHoverListener(hover);
                graph.view().setTooltipTextFunction((frames, frame) -> {
                    assertSame(model, frames);
                    tooltipFrame.set(frame);
                    return frame == leaf ? "leaf text" : "shared text";
                });
                graph.view().setTooltipComponentSupplier(() -> {
                    creations.incrementAndGet();
                    return tooltip;
                });
                graph.canvas().addPropertyChangeListener(JComponent.TOOL_TIP_TEXT_KEY,
                                                         event -> textChanges.incrementAndGet());
            });
            graph.await("initial sibling rendering", () -> renderer.mainPaint(left) != null
                                                          && renderer.mainPaint(right) != null
                                                          && renderer.mainPaint(leaf) != null);

            var leftPoint = graph.onEdt(() -> center(renderer.mainPaint(left).bounds));
            var leftEvent = move(graph, leftPoint);
            var leftHover = hover.await(graph, leftEvent);
            assertTrue(leftHover.entered);
            assertSame(left, leftHover.frame);
            assertTrue(leftHover.rectangle.contains(leftPoint));
            assertFrameRectangle(renderer, left, leftHover.rectangle);
            graph.onEdt(() -> {
                assertSame(graph.scrollPane(), leftHover.event.getSource());
                assertEquals(SwingUtilities.convertPoint(graph.canvas(), leftPoint, graph.scrollPane()),
                             leftHover.event.getPoint());
                assertEquals("shared text", graph.canvas().getToolTipText());
                assertSame(left, tooltipFrame.get());
                assertSame(tooltip, graph.canvas().createToolTip());
                assertSame(tooltip, graph.canvas().createToolTip());
                assertSame(graph.canvas(), tooltip.getComponent());
                assertEquals(Color.YELLOW, graph.canvas().createToolTip().getBackground());
            });
            assertEquals(1, creations.get());

            var rightPoint = graph.onEdt(() -> center(renderer.mainPaint(right).bounds));
            var rightHover = hover.await(graph, move(graph, rightPoint));
            assertTrue(rightHover.entered);
            assertSame(right, rightHover.frame);
            assertTrue(rightHover.rectangle.contains(rightPoint));
            assertFrameRectangle(renderer, right, rightHover.rectangle);
            assertSame(right, tooltipFrame.get());
            assertEquals(1, textChanges.get(), "equal tooltip text is not a hover completion signal");

            var leafPoint = graph.onEdt(() -> center(renderer.mainPaint(leaf).bounds));
            var leafHover = hover.await(graph, move(graph, leafPoint));
            assertSame(leaf, leafHover.frame);
            assertFrameRectangle(renderer, leaf, leafHover.rectangle);
            graph.onEdt(() -> {
                assertSame(leaf, tooltipFrame.get());
                assertEquals("leaf text", graph.canvas().getToolTipText());
            });
            rightHover = hover.await(graph, move(graph, rightPoint));

            // The right half has no frame at leaf depth.
            var empty = graph.onEdt(() -> new Point(graph.canvas().getWidth() * 3 / 4,
                                                    center(renderer.mainPaint(leaf).bounds).y));
            var stopped = hover.await(graph, move(graph, empty));
            assertFalse(stopped.entered);
            assertSame(right, stopped.frame);
            assertEquals(rightHover.rectangle, stopped.rectangle);

            hover.await(graph, move(graph, leftPoint));
            graph.pointerAt(new Point(-40, -40));
            var exit = graph.onEdt(() -> {
                var event = new MouseEvent(graph.scrollPane(), MouseEvent.MOUSE_EXITED,
                                           System.nanoTime(), 0, -40, -40, 0, false);
                graph.scrollPane().dispatchEvent(event);
                return event;
            });
            var outside = hover.await(graph, exit);
            assertFalse(outside.entered);
            assertNull(outside.frame, "the exit contract clears the previous frame");
            assertNull(outside.rectangle, "the exit contract clears the previous rectangle");
            assertSame(exit, outside.event);
        }
    }

    @Test
    void scrolling_recomputes_hover_under_a_stationary_screen_pointer() {
        var model = FrameModelFixture.deepChain(24);
        var renderer = new RecordingFrameRenderer<String>();
        var first = model.frames.get(1);
        var afterScroll = model.frames.get(3);
        var hover = new HoverProbe();
        try (var graph = FlamegraphViewFixture.<String>builder().model(model).renderer(renderer)
                                             .mode(FlamegraphView.Mode.ICICLEGRAPH).build()) {
            graph.onEdt(() -> graph.view().setHoverListener(hover));
            graph.await("first visible frame", () -> renderer.mainPaint(first) != null);
            var initialPoint = graph.onEdt(() -> center(renderer.mainPaint(first).bounds));
            assertSame(first, hover.await(graph, move(graph, initialPoint)).frame);
            var screenPointer = graph.onEdt(() -> MouseInfo.getPointerInfo().getLocation());
            int displacement = graph.onEdt(() -> renderer.mainPaint(first).bounds.height * 2);
            graph.onEdt(() -> graph.viewport().setViewPosition(new Point(0, displacement)));
            graph.await("scroll displacement", () -> graph.viewport().getViewPosition().y == displacement);
            var wheel = graph.onEdt(() -> {
                var local = new Point(screenPointer);
                SwingUtilities.convertPointFromScreen(local, graph.scrollPane());
                // Scroll has already occurred; this wheel notification recomputes hover without more movement.
                var event = new MouseWheelEvent(graph.scrollPane(), MouseEvent.MOUSE_WHEEL,
                                                System.nanoTime(), 0, local.x, local.y, 0, false,
                                                MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, 0);
                graph.scrollPane().dispatchEvent(event);
                return event;
            });
            var hovered = hover.await(graph, wheel);
            assertTrue(hovered.entered);
            assertSame(afterScroll, hovered.frame);
            assertSame(wheel, hovered.event);
            assertEquals(screenPointer, graph.onEdt(() -> MouseInfo.getPointerInfo().getLocation()));
            assertTrue(hovered.rectangle.contains(initialPoint.x, initialPoint.y + displacement));
            graph.await("scrolled frame paint", () -> renderer.mainPaint(afterScroll) != null);
            assertFrameRectangle(renderer, afterScroll, hovered.rectangle);
        }
    }

    private static void assertFrameRectangle(RecordingFrameRenderer<String> renderer, FrameBox<String> frame,
                                            Rectangle hoverRectangle) {
        var painted = renderer.mainPaint(frame).bounds;
        var padded = new Rectangle(painted);
        int gap = renderer.getFrameGapWidth();
        padded.grow(2 * gap, 2 * gap);
        assertTrue(hoverRectangle.contains(painted), "hover bounds must cover the painted frame");
        assertTrue(padded.contains(hoverRectangle), "hover bounds must stay within the frame's repaint padding");
    }

    private static MouseEvent move(FlamegraphViewFixture<String> graph, Point point) {
        graph.pointerAt(point);
        return graph.onEdt(() -> {
            var event = new MouseEvent(graph.canvas(), MouseEvent.MOUSE_MOVED,
                                       System.nanoTime(), 0, point.x, point.y, 0, false);
            graph.canvas().dispatchEvent(event);
            return event;
        });
    }

    private static Point center(Rectangle rectangle) {
        return new Point((int) rectangle.getCenterX(), (int) rectangle.getCenterY());
    }

    private static final class HoverProbe implements FlamegraphView.HoverListener<String> {
        private final List<HoverEvent> events = new ArrayList<>();

        @Override
        public void onFrameHover(FrameBox<String> frame, Rectangle rectangle, MouseEvent event) {
            events.add(new HoverEvent(true, frame, rectangle, event));
        }

        @Override
        public void onStopHover(FrameBox<String> frame, Rectangle rectangle, MouseEvent event) {
            events.add(new HoverEvent(false, frame, rectangle, event));
        }

        private HoverEvent await(FlamegraphViewFixture<String> graph, MouseEvent trigger) {
            graph.await("hover callback for event " + trigger.getWhen(),
                        () -> events.stream().anyMatch(event -> event.event.getWhen() == trigger.getWhen()));
            return graph.onEdt(() -> events.stream().filter(event -> event.event.getWhen() == trigger.getWhen())
                                          .findFirst().orElseThrow());
        }
    }

    private static final class HoverEvent {
        private final boolean entered;
        private final FrameBox<String> frame;
        private final Rectangle rectangle;
        private final MouseEvent event;

        private HoverEvent(boolean entered, FrameBox<String> frame, Rectangle rectangle, MouseEvent event) {
            this.entered = entered;
            this.frame = frame;
            this.rectangle = rectangle == null ? null : new Rectangle(rectangle);
            this.event = event;
        }
    }
}
