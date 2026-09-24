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

import io.github.bric3.fireplace.flamegraph.DefaultFrameRenderer;
import io.github.bric3.fireplace.flamegraph.FrameBox;
import io.github.bric3.fireplace.flamegraph.FrameColorProvider;
import io.github.bric3.fireplace.flamegraph.FrameFontProvider;
import io.github.bric3.fireplace.flamegraph.FrameModel;
import io.github.bric3.fireplace.flamegraph.FrameRenderer;
import io.github.bric3.fireplace.flamegraph.FrameRenderingFlags;
import io.github.bric3.fireplace.flamegraph.FrameTextsProvider;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Observes the public renderer boundary while retaining real painting and layout. */
public final class RecordingFrameRenderer<T> implements FrameRenderer<T> {
    private final FrameRenderer<T> delegate;
    private final Map<FrameBox<T>, Paint> mainPaints = new ConcurrentHashMap<>();

    public RecordingFrameRenderer() {
        this(new DefaultFrameRenderer<>(FrameTextsProvider.of(frame -> ""),
                FrameColorProvider.defaultColorProvider(frame -> Color.ORANGE),
                FrameFontProvider.defaultFontProvider()));
    }

    public RecordingFrameRenderer(FrameRenderer<T> delegate) {
        this.delegate = delegate;
    }

    public Paint mainPaint(FrameBox<T> frame) {
        return mainPaints.get(frame);
    }

    public void clear() {
        mainPaints.clear();
    }

    @Override
    public void paintFrame(Graphics2D graphics, FrameBox<T> frame, FrameModel<T> model,
                           RectangularShape bounds, Rectangle2D intersection, int flags) {
        // The engine and default renderer both reuse and mutate their shape objects.
        var paint = FrameRenderingFlags.isMinimapMode(flags) ? null : new Paint(bounds.getBounds(), flags);
        delegate.paintFrame(graphics, frame, model, bounds, intersection, flags);
        if (paint != null) {
            mainPaints.put(frame, paint);
        }
    }

    @Override
    public int getFrameBoxHeight(Graphics2D graphics) {
        return delegate.getFrameBoxHeight(graphics);
    }

    @Override
    public int getFrameGapWidth() {
        return delegate.getFrameGapWidth();
    }

    @Override
    public boolean isDrawingFrameGap() {
        return delegate.isDrawingFrameGap();
    }

    @Override
    public RectangularShape reusableFrameShape() {
        return delegate.reusableFrameShape();
    }

    public static final class Paint {
        public final Rectangle bounds;
        public final int flags;

        private Paint(Rectangle bounds, int flags) {
            this.bounds = bounds;
            this.flags = flags;
        }
    }
}
