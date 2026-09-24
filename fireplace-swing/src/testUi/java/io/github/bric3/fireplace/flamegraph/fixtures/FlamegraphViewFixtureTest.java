/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.fireplace.flamegraph.fixtures;

import io.github.bric3.fireplace.core.ui.fixtures.SwingWindowFixture;
import io.github.bric3.fireplace.flamegraph.FlamegraphView;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import static org.junit.jupiter.api.Assertions.*;

@Tag("ui")
@Timeout(20)
class FlamegraphViewFixtureTest {
    @Test
    void mounts_the_real_component_and_composes_window_lifecycle() {
        JFrame frame;
        var model = FrameModelFixture.rootWithSiblings();
        try (var fixture = FlamegraphViewFixture.<String>builder()
                                               .model(model)
                                               .mode(FlamegraphView.Mode.ICICLEGRAPH)
                                               .size(600, 400)
                                               .build()) {
            frame = fixture.onEdt(fixture::window);
            assertThrows(IllegalStateException.class, fixture::canvas);
            var synchronousFailure = new IllegalStateException("synchronous EDT failure");
            assertSame(synchronousFailure, assertThrows(IllegalStateException.class,
                    () -> fixture.onEdt(() -> { throw synchronousFailure; })));
            fixture.onEdt(() -> {
                assertTrue(fixture.canvas().isShowing());
                assertSame(fixture.canvas(), fixture.viewport().getView());
                assertSame(model, fixture.view().getFrameModel());
                assertEquals(FlamegraphView.Mode.ICICLEGRAPH, fixture.view().getMode());
                assertEquals(new Dimension(600, 400), fixture.window().getSize());
            });
        }
        assertFalse(SwingWindowFixture.runOnEdt(frame::isDisplayable));

        // A different fixture can compose exactly the same window/failure lifecycle.
        try (var window = SwingWindowFixture.open(300, 200)) {
            var panel = window.onEdt(() -> { return new JPanel(); });
            window.mount(panel);
            assertTrue(window.onEdt(panel::isShowing));
        }
    }

    @Test
    void pointer_scope_returns_fresh_points_and_drains_pending_hover_before_the_next_fixture() {
        JFrame previousWindow;
        try (var fixture = FlamegraphViewFixture.<String>builder()
                                               .model(FrameModelFixture.rootWithSiblings())
                                               .build()) {
            previousWindow = fixture.onEdt(fixture::window);
            var point = new Point(20, 30);
            fixture.pointerAt(point);
            fixture.onEdt(() -> {
                Point expected = new Point(point);
                SwingUtilities.convertPointToScreen(expected, fixture.canvas());
                var first = MouseInfo.getPointerInfo().getLocation();
                var second = MouseInfo.getPointerInfo().getLocation();
                assertEquals(expected, first);
                assertEquals(expected, second);
                assertNotSame(first, second);
                first.translate(200, 200);
                assertEquals(expected, MouseInfo.getPointerInfo().getLocation());

                // Leave the real 60 ms debounce pending. close must supersede and acknowledge it.
                fixture.scrollPane().dispatchEvent(new MouseWheelEvent(
                        fixture.scrollPane(), MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(),
                        0, 20, 30, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, 1
                ));
            });
        }
        assertFalse(SwingWindowFixture.runOnEdt(previousWindow::isDisplayable));
        try (var next = FlamegraphViewFixture.<String>builder()
                                            .model(FrameModelFixture.rootWithSiblings())
                                            .build()) {
            next.pointerAt(new Point(40, 50));
            next.onEdt(() -> {
                Point expected = new Point(40, 50);
                SwingUtilities.convertPointToScreen(expected, next.canvas());
                assertEquals(expected, MouseInfo.getPointerInfo().getLocation());
            });
        }
    }
}
