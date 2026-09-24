/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.core.ui.fixtures;

import io.github.bric3.fireplace.flamegraph.DefaultFrameRenderer;
import io.github.bric3.fireplace.flamegraph.FrameBox;
import io.github.bric3.fireplace.flamegraph.FrameColorProvider;
import io.github.bric3.fireplace.flamegraph.FrameFontProvider;
import io.github.bric3.fireplace.flamegraph.FrameModel;
import io.github.bric3.fireplace.flamegraph.FrameRenderingFlags;
import io.github.bric3.fireplace.flamegraph.FrameTextsProvider;
import io.github.bric3.fireplace.flamegraph.fixtures.FlamegraphViewFixture;
import io.github.bric3.fireplace.flamegraph.fixtures.FrameModelFixture;
import io.github.bric3.fireplace.flamegraph.fixtures.RecordingFrameRenderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockingDetails;

/** Runs in its own test-class JVM because this negative control deliberately poisons the UI scope. */
@Tag("ui")
@Timeout(20)
class SwingWindowFailureTest {
    @Test
    void drains_minimap_work_on_success_and_failure_then_refuses_a_poisoned_jvm() throws Exception {
        assertMinimapDrainedOnClose(false);
        assertMinimapDrainedOnClose(true);
    }

    private void assertMinimapDrainedOnClose(boolean failHover) throws Exception {
        var delayedFailure = new IllegalStateException("delayed callback negative control");
        var model = FrameModelFixture.rootWithSiblings();
        var child = model.frames.get(1);
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var completedPaints = new AtomicInteger();
        var renderer = new RecordingFrameRenderer<>(new DefaultFrameRenderer<String>(
                FrameTextsProvider.empty(), FrameColorProvider.defaultColorProvider(frame -> Color.ORANGE),
                FrameFontProvider.defaultFontProvider()) {
            @Override
            public void paintFrame(Graphics2D graphics, FrameBox<String> frame, FrameModel<String> frames,
                                   RectangularShape bounds, Rectangle2D intersection, int flags) {
                if (FrameRenderingFlags.isMinimapMode(flags)) {
                    started.countDown();
                    try {
                        assertTrue(release.await(5, TimeUnit.SECONDS), "test must release minimap painting");
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(interrupted);
                    }
                }
                super.paintFrame(graphics, frame, frames, bounds, intersection, flags);
                if (FrameRenderingFlags.isMinimapMode(flags)) {
                    completedPaints.incrementAndGet();
                }
            }
        });
        var graph = FlamegraphViewFixture.<String>builder().model(model).renderer(renderer).build();
        var window = graph.onEdt(graph::window);
        var closing = new FutureTask<Void>(() -> {
            graph.close();
            return null;
        });
        try {
            graph.onEdt(() -> graph.view().setShowMinimap(true));
            assertTrue(started.await(5, TimeUnit.SECONDS), "real minimap worker must start");
            if (failHover) {
                graph.await("child rendering", () -> renderer.mainPaint(child) != null);
                var point = graph.onEdt(() -> {
                    var bounds = renderer.mainPaint(child).bounds;
                    return new Point((int) bounds.getCenterX(), (int) bounds.getCenterY());
                });
                graph.pointerAt(point);
                graph.onEdt(() -> {
                    graph.view().setTooltipTextFunction((frames, frame) -> {
                        throw delayedFailure;
                    });
                    graph.canvas().dispatchEvent(new MouseEvent(graph.canvas(), MouseEvent.MOUSE_MOVED,
                            System.currentTimeMillis(), 0, point.x, point.y, 0, false));
                });
                var failure = assertThrows(AssertionError.class,
                        () -> graph.await("a callback which fails before delivering completion", () -> false));
                assertSame(delayedFailure, failure.getCause());
                assertEquals("Asynchronous EDT callback failed in this UI scope", failure.getMessage());
            }
            new Thread(closing, "close-ui-fixture").start();
            assertThrows(TimeoutException.class, () -> closing.get(300, TimeUnit.MILLISECONDS),
                         "close must wait for the blocked minimap worker");
        } finally {
            release.countDown();
            // Also cover setup/assertion failures before the close thread started.
            closing.run();
            try {
                closing.get(5, TimeUnit.SECONDS);
                assertFalse(failHover, "close must report the recorded hover failure");
            } catch (ExecutionException cleanup) {
                assertTrue(failHover);
                assertSame(delayedFailure, cleanup.getCause().getCause(), "cleanup must retain the EDT failure");
            }
        }
        assertTrue(completedPaints.get() > 0, "minimap painting completed before close returned");
        assertTrue(ForkJoinPool.commonPool().isQuiescent(), "no worker can enqueue another image installation");
        assertFalse(SwingWindowFixture.runOnEdt(window::isDisplayable));
        assertFalse(SwingWindowFixture.runOnEdt(() -> mockingDetails(MouseInfo.getPointerInfo()).isMock()),
                    "closed scopes must close their EDT pointer mock");
        if (failHover) {
            var reuseFailure = assertThrows(AssertionError.class, () -> SwingWindowFixture.open(300, 200));
            assertSame(delayedFailure, reuseFailure.getCause());
        }
    }
}
