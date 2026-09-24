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

import io.github.bric3.fireplace.flamegraph.FlamegraphView.FrameClickAction;
import io.github.bric3.fireplace.flamegraph.FlamegraphView.Mode;
import io.github.bric3.fireplace.flamegraph.fixtures.FlamegraphViewFixture;
import io.github.bric3.fireplace.flamegraph.fixtures.FrameModelFixture;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@Tag("ui")
@Timeout(value = 30, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SAME_THREAD)
class FlamegraphView_NavigationUiTest {
    @ParameterizedTest
    @EnumSource(Mode.class)
    void successiveSiblingZoomsFollowTheLatestFrame(Mode mode) {
        var model = FrameModelFixture.rootWithSiblings();
        try (var fixture = FlamegraphViewFixture.<String>builder().model(model).mode(mode).build()) {
            for (var target : List.of(model.frames.get(1), model.frames.get(3), model.frames.get(1))) {
                fixture.onEdt(() -> fixture.view().zoomTo(target));
                fixture.await("visible range follows " + target.actualNode, () ->
                        Math.abs(fixture.canvas().getWidth() * 0.5 - fixture.viewport().getExtentSize().width) <= 2
                        && Math.abs(fixture.viewport().getViewPosition().x
                                    - fixture.canvas().getWidth() * target.startX) <= 2);
                fixture.onEdt(() -> assertThat(
                        (double) fixture.viewport().getViewRect().getMaxX() / fixture.canvas().getWidth())
                        .isCloseTo(target.endX, within(0.005)));
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void zoomToKeepsTwoAncestorsVisibleAndResetRestoresTheRoot(Mode mode) {
        var model = FrameModelFixture.deepChain(40);
        var target = model.frames.get(4);
        try (var fixture = FlamegraphViewFixture.<String>builder()
                .model(model).mode(mode).renderer(twentyPixelRows()).build()) {
            fixture.onEdt(() -> fixture.view().zoomTo(target));
            fixture.await("the target fills the viewport", () ->
                    Math.abs(fixture.canvas().getWidth() * 0.5 - fixture.viewport().getExtentSize().width) <= 2);
            fixture.onEdt(() -> {
                assertThat(fixture.viewport().getViewPosition().x)
                        .isCloseTo((int) (fixture.canvas().getWidth() * target.startX), within(2));
                int topOfRange = mode == Mode.ICICLEGRAPH ? 40
                        : fixture.canvas().getHeight() - fixture.viewport().getExtentSize().height - 40;
                assertThat(fixture.viewport().getViewPosition().y).isCloseTo(topOfRange, within(1));
                assertThat(fixture.scrollPane().getHorizontalScrollBarPolicy()).isEqualTo(
                        mode == Mode.FLAMEGRAPH ? JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
                                                : JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                fixture.view().resetZoom();
            });
            fixture.await("reset restores the full horizontal range", () ->
                    fixture.viewport().getViewPosition().x == 0
                    && Math.abs(fixture.canvas().getWidth() - fixture.viewport().getExtentSize().width) <= 1);
            fixture.onEdt(() -> {
                assertThat(fixture.scrollPane().getHorizontalScrollBarPolicy()).isEqualTo(
                        mode == Mode.FLAMEGRAPH ? JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
                                                : JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                int rootPosition = mode == Mode.ICICLEGRAPH ? 0
                        : fixture.canvas().getHeight() - fixture.viewport().getExtentSize().height;
                assertThat(fixture.viewport().getViewPosition().y).isCloseTo(rootPosition, within(1));
            });
        }
    }

    @Test
    void draggingMovesTheViewportClampsAtTheOriginAndStopsOnRelease() {
        var model = FrameModelFixture.deepChain(40);
        try (var fixture = FlamegraphViewFixture.<String>builder().model(model).mode(Mode.ICICLEGRAPH).build()) {
            fixture.onEdt(() -> fixture.view().zoomTo(model.frames.get(1)));
            fixture.await("both directions can scroll", () ->
                    fixture.canvas().getWidth() > fixture.viewport().getWidth() + 200
                    && fixture.canvas().getHeight() > fixture.viewport().getHeight() + 120);
            fixture.onEdt(() -> {
                var scroll = fixture.scrollPane();
                var viewport = fixture.viewport();
                viewport.setViewPosition(new Point(200, 120));
                scroll.dispatchEvent(mouse(scroll, MouseEvent.MOUSE_PRESSED, 100, 100, MouseEvent.BUTTON1, 1));
                var drag = mouse(scroll, MouseEvent.MOUSE_DRAGGED, 150, 130, MouseEvent.NOBUTTON, 0);
                scroll.dispatchEvent(drag);
                assertThat(viewport.getViewPosition()).isEqualTo(new Point(150, 90));
                assertThat(drag.isConsumed()).isTrue();

                scroll.dispatchEvent(mouse(scroll, MouseEvent.MOUSE_RELEASED, 150, 130, MouseEvent.BUTTON1, 1));
                var afterRelease = viewport.getViewPosition();
                var releasedDrag = mouse(scroll, MouseEvent.MOUSE_DRAGGED, 180, 160, MouseEvent.NOBUTTON, 0);
                scroll.dispatchEvent(releasedDrag);
                assertThat(viewport.getViewPosition()).isEqualTo(afterRelease);
                assertThat(releasedDrag.isConsumed()).isFalse();

                viewport.setViewPosition(new Point(20, 20));
                scroll.dispatchEvent(mouse(scroll, MouseEvent.MOUSE_PRESSED, 100, 100, MouseEvent.BUTTON1, 1));
                scroll.dispatchEvent(mouse(scroll, MouseEvent.MOUSE_DRAGGED, 200, 200, MouseEvent.NOBUTTON, 0));
                assertThat(viewport.getViewPosition()).isEqualTo(new Point());
                scroll.dispatchEvent(mouse(scroll, MouseEvent.MOUSE_RELEASED, 200, 200, MouseEvent.BUTTON1, 1));
            });
        }
    }

    @ParameterizedTest
    @CsvSource({"ICICLEGRAPH, FOCUS_FRAME", "FLAMEGRAPH, FOCUS_FRAME",
                "ICICLEGRAPH, EXPAND_FRAME", "FLAMEGRAPH, EXPAND_FRAME"})
    void clickCountSelectsTheConfiguredZoomActionAndRepeatingItResets(Mode mode, FrameClickAction action) {
        try (var fixture = FlamegraphViewFixture.<String>builder()
                .model(FrameModelFixture.deepChain(40)).mode(mode).renderer(twentyPixelRows()).build()) {
            fixture.onEdt(() -> fixture.view().setFrameClickAction(action));
            // Depth four starts well inside the viewport, so focus and expand must differ vertically.
            var point = fixture.onEdt(() -> new Point(fixture.canvas().getWidth() / 2,
                    mode == Mode.ICICLEGRAPH ? 90 : fixture.canvas().getHeight() - 90));
            fixture.pointerAt(point);
            int initialWidth = fixture.onEdt(() -> fixture.canvas().getWidth());
            int initialY = fixture.onEdt(() -> fixture.viewport().getViewPosition().y);
            int zoomCount = action == FrameClickAction.EXPAND_FRAME ? 1 : 2;
            fixture.onEdt(() -> {
                var canvas = fixture.canvas();
                canvas.dispatchEvent(mouse(canvas, MouseEvent.MOUSE_CLICKED,
                                           point.x, point.y, MouseEvent.BUTTON1, 3 - zoomCount));
                assertThat(canvas.getWidth()).isEqualTo(initialWidth);
                assertThat(fixture.viewport().getViewPosition().y).isEqualTo(initialY);
                canvas.dispatchEvent(mouse(canvas, MouseEvent.MOUSE_CLICKED,
                                           point.x, point.y, MouseEvent.BUTTON1, zoomCount));
            });
            fixture.await("the configured click zooms into the branch", () ->
                    Math.abs(fixture.canvas().getWidth() / 2 - fixture.viewport().getExtentSize().width) <= 2);
            fixture.onEdt(() -> {
                assertThat(fixture.viewport().getViewPosition().x)
                        .isCloseTo(fixture.canvas().getWidth() / 4, within(2));
                int focusedY = mode == Mode.ICICLEGRAPH ? 80
                        : fixture.canvas().getHeight() - fixture.viewport().getExtentSize().height - 80;
                assertThat(fixture.viewport().getViewPosition().y)
                        .isCloseTo(action == FrameClickAction.FOCUS_FRAME ? focusedY : initialY, within(1));
            });
            var repeatedPoint = fixture.onEdt(() -> new Point(fixture.canvas().getWidth() / 2,
                    mode == Mode.ICICLEGRAPH ? 90 : fixture.canvas().getHeight() - 90));
            fixture.pointerAt(repeatedPoint);
            fixture.onEdt(() -> {
                var canvas = fixture.canvas();
                canvas.dispatchEvent(mouse(canvas, MouseEvent.MOUSE_CLICKED,
                                           repeatedPoint.x, repeatedPoint.y, MouseEvent.BUTTON1, zoomCount));
            });
            fixture.await("clicking the same frame again resets the horizontal range", () ->
                    fixture.viewport().getViewPosition().x == 0
                    && Math.abs(fixture.canvas().getWidth() - fixture.viewport().getExtentSize().width) <= 1);
        }
    }

    @ParameterizedTest
    @CsvSource({"true, false", "true, true", "false, false"})
    void zoomOverrideCanHandleDelegateOrFallBack(boolean handled, boolean delegate) {
        var model = FrameModelFixture.rootWithSiblings();
        var targetFrame = model.frames.get(1);
        var observed = new AtomicReference<ZoomTarget<?>>();
        try (var fixture = FlamegraphViewFixture.<String>builder().model(model).mode(Mode.ICICLEGRAPH).build()) {
            fixture.onEdt(() -> {
                var before = fixture.canvas().getBounds();
                fixture.view().overrideZoomAction(new FlamegraphView.ZoomAction() {
                    @Override
                    public <T> boolean zoom(FlamegraphView.ZoomableComponent<T> component, ZoomTarget<T> target) {
                        assertThat(component).isSameAs(fixture.canvas());
                        assertThat(target.targetFrame).isSameAs(targetFrame);
                        assertThat(observed.compareAndSet(null, target)).isTrue();
                        if (delegate) {
                            component.zoom(target);
                        }
                        return handled;
                    }
                });
                fixture.view().zoomTo(targetFrame);
                assertThat(observed.get()).isNotNull();
                assertThat(observed.get().getWidth()).isGreaterThan(before.width);
                assertThat(fixture.canvas().getBounds()).isEqualTo(
                        handled && !delegate ? before : observed.get().getTargetBounds());
            });
        }
    }

    private static DefaultFrameRenderer<String> twentyPixelRows() {
        return new DefaultFrameRenderer<String>(FrameTextsProvider.empty(),
                FrameColorProvider.defaultColorProvider(frame -> Color.BLUE),
                FrameFontProvider.defaultFontProvider()) {
            @Override
            public int getFrameBoxHeight(Graphics2D graphics) {
                return 20;
            }
        };
    }

    private static MouseEvent mouse(JComponent source, int id, int x, int y, int button, int clicks) {
        int modifiers = id == MouseEvent.MOUSE_PRESSED || id == MouseEvent.MOUSE_DRAGGED
                ? MouseEvent.BUTTON1_DOWN_MASK : 0;
        return new MouseEvent(source, id, System.currentTimeMillis(), modifiers,
                              x, y, clicks, false, button);
    }
}
