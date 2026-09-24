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

import io.github.bric3.fireplace.flamegraph.FrameBox;
import io.github.bric3.fireplace.flamegraph.FrameModel;
import io.github.bric3.fireplace.flamegraph.FrameRenderer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

import static io.github.bric3.fireplace.flamegraph.FrameRenderingFlags.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecordingFrameRendererTest {
    private final FrameBox<String> root = new FrameBox<>("root", 0, 1, 0);
    private final FrameBox<String> child = new FrameBox<>("child", .25, .75, 1);
    private final FrameModel<String> model = new FrameModel<>(List.of(root, child));
    private final BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
    private final Graphics2D graphics = image.createGraphics();
    @SuppressWarnings("unchecked")
    private final FrameRenderer<String> delegate = mock(FrameRenderer.class);
    private final RecordingFrameRenderer<String> renderer = new RecordingFrameRenderer<>(delegate);

    @AfterEach
    void disposeGraphics() {
        graphics.dispose();
    }

    @Test
    void records_original_bounds_even_when_the_delegate_reuses_the_shape() {
        var bounds = new Rectangle(2, 3, 20, 10);
        var intersection = new Rectangle(bounds);
        doAnswer(invocation -> {
            assertNull(renderer.mainPaint(root), "painting has not completed inside the delegate");
            graphics.setColor(Color.MAGENTA);
            graphics.fill(bounds);
            bounds.setBounds(40, 40, 1, 1);
            return null;
        }).when(delegate).paintFrame(same(graphics), same(root), same(model), same(bounds), same(intersection), eq(HOVERED));

        renderer.paintFrame(graphics, root, model, bounds, intersection, HOVERED);

        var paint = renderer.mainPaint(root);
        assertNotNull(paint);
        assertEquals(new Rectangle(2, 3, 20, 10), paint.bounds);
        assertEquals(HOVERED, paint.flags);
        assertEquals(Color.MAGENTA.getRGB(), image.getRGB(10, 8), "the delegate still paints the supplied graphics");
        assertEquals(0, image.getRGB(0, 0));
    }

    @Test
    void minimap_painting_does_not_create_or_replace_main_canvas_observations() {
        var bounds = new Rectangle(2, 3, 20, 10);
        renderer.paintFrame(graphics, root, model, bounds, bounds, HOVERED);
        var mainPaint = renderer.mainPaint(root);
        var minimapBounds = new Rectangle(0, 0, 5, 1);

        renderer.paintFrame(graphics, root, model, minimapBounds, minimapBounds, MINIMAP_MODE | HOVERED);
        renderer.paintFrame(graphics, child, model, minimapBounds, minimapBounds, MINIMAP_MODE);

        assertNotNull(mainPaint);
        assertSame(mainPaint, renderer.mainPaint(root));
        assertNull(renderer.mainPaint(child));
        verify(delegate).paintFrame(graphics, root, model, minimapBounds, minimapBounds, MINIMAP_MODE | HOVERED);
        verify(delegate).paintFrame(graphics, child, model, minimapBounds, minimapBounds, MINIMAP_MODE);
    }

    @Test
    void later_paints_replace_only_their_frame_and_clear_removes_all_observations() {
        var initial = new Rectangle(2, 3, 20, 10);
        renderer.paintFrame(graphics, root, model, initial, initial, HOVERED);
        renderer.paintFrame(graphics, child, model, initial, initial, 0);
        var previousRoot = renderer.mainPaint(root);
        var previousChild = renderer.mainPaint(child);
        var changed = new Rectangle(4, 6, 40, 20);

        renderer.paintFrame(graphics, root, model, changed, changed, HIGHLIGHTED_FRAME);

        assertEquals(changed, renderer.mainPaint(root).bounds);
        assertEquals(HIGHLIGHTED_FRAME, renderer.mainPaint(root).flags);
        assertEquals(initial, previousRoot.bounds);
        assertEquals(HOVERED, previousRoot.flags);
        assertNotNull(previousChild);
        assertSame(previousChild, renderer.mainPaint(child));
        renderer.clear();
        assertNull(renderer.mainPaint(root));
        assertNull(renderer.mainPaint(child));
    }

    @Test
    void a_failed_delegate_paint_does_not_signal_completion() {
        var bounds = new Rectangle(2, 3, 20, 10);
        var failure = new AssertionError("delegate painting failed");
        doThrow(failure).when(delegate).paintFrame(graphics, root, model, bounds, bounds, HOVERED);

        assertSame(failure, assertThrows(AssertionError.class,
                () -> renderer.paintFrame(graphics, root, model, bounds, bounds, HOVERED)));
        assertNull(renderer.mainPaint(root));
    }
}
