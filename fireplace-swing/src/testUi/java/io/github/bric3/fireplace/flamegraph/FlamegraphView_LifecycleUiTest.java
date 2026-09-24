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

import io.github.bric3.fireplace.flamegraph.FlamegraphView.Mode;
import io.github.bric3.fireplace.flamegraph.fixtures.FlamegraphViewFixture;
import io.github.bric3.fireplace.flamegraph.fixtures.FrameModelFixture;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@Tag("ui")
@Timeout(value = 30, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SAME_THREAD)
class FlamegraphView_LifecycleUiTest {
    @ParameterizedTest
    @EnumSource(Mode.class)
    void firstDisplayFitsTheWidthAndUsesTheExpectedScrollbars(Mode mode) {
        try (var fixture = FlamegraphViewFixture.<String>builder()
                .model(FrameModelFixture.deepChain(40)).mode(mode).build()) {
            fixture.await("first display fits the available width", () ->
                    fixture.canvas().getWidth() == fixture.viewport().getExtentSize().width
                    && fixture.scrollPane().getVerticalScrollBarPolicy() == JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            fixture.onEdt(() -> {
                assertThat(fixture.canvas().getHeight()).isGreaterThan(fixture.viewport().getExtentSize().height);
                assertThat(fixture.scrollPane().getHorizontalScrollBarPolicy()).isEqualTo(
                        mode == Mode.FLAMEGRAPH ? JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
                                                : JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                assertThat(fixture.viewport().getViewPosition().x).isZero();
            });
        }
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void resizingAfterZoomAndPanPreservesTheVisibleHorizontalRange(Mode mode) {
        var model = FrameModelFixture.deepChain(40);
        try (var fixture = FlamegraphViewFixture.<String>builder().model(model).mode(mode).build()) {
            fixture.onEdt(() -> fixture.view().zoomTo(model.frames.get(10)));
            fixture.await("the frame is zoomed", () ->
                    Math.abs(fixture.canvas().getWidth() * 0.5 - fixture.viewport().getExtentSize().width) <= 2);
            var before = fixture.onEdt(() -> {
                var viewport = fixture.viewport();
                var position = viewport.getViewPosition();
                position.x += 40;
                viewport.setViewPosition(position);
                var scroll = fixture.scrollPane();
                // The regular release route records the latest user position for subsequent layouts.
                scroll.dispatchEvent(new MouseEvent(scroll, MouseEvent.MOUSE_RELEASED,
                        System.currentTimeMillis(), 0, 100, 100, 1, false, MouseEvent.BUTTON1));
                var range = new double[]{
                        (double) viewport.getViewPosition().x / fixture.canvas().getWidth(),
                        (double) viewport.getExtentSize().width / fixture.canvas().getWidth(),
                        viewport.getExtentSize().width
                };
                fixture.window().setSize(820, 450);
                fixture.window().validate();
                return range;
            });
            fixture.await("resized viewport preserves zoom", () ->
                    fixture.viewport().getExtentSize().width > before[2]
                    && Math.abs((double) fixture.viewport().getExtentSize().width / fixture.canvas().getWidth()
                                - before[1]) < 0.01);
            fixture.onEdt(() -> {
                assertThat((double) fixture.viewport().getViewPosition().x / fixture.canvas().getWidth())
                        .isCloseTo(before[0], within(0.01));
                assertThat(fixture.canvas().getVisibleRect().getMaxY()).isLessThanOrEqualTo(fixture.canvas().getHeight());
                assertThat(fixture.viewport().getViewPosition().y).isGreaterThanOrEqualTo(0);
            });
        }
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void replacingAndClearingAModelLeavesAUsableViewport(Mode mode) {
        var original = FrameModelFixture.deepChain(40);
        var replacement = FrameModelFixture.rootWithSiblings();
        try (var fixture = FlamegraphViewFixture.<String>builder().model(original).mode(mode).build()) {
            fixture.onEdt(() -> fixture.view().zoomTo(original.frames.get(20)));
            fixture.await("original model is zoomed", () ->
                    fixture.canvas().getWidth() > fixture.viewport().getExtentSize().width);
            fixture.onEdt(() -> {
                fixture.view().setModel(replacement);
                fixture.view().resetZoom();
            });
            fixture.await("the replacement fits without vertical scrolling", () ->
                    fixture.viewport().getViewPosition().equals(new Point())
                    && fixture.canvas().getWidth() == fixture.viewport().getExtentSize().width
                    && fixture.scrollPane().getVerticalScrollBarPolicy() == JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            fixture.onEdt(() -> {
                assertThat(fixture.view().getFrameModel()).isSameAs(replacement);
                assertThat(fixture.canvas().getHeight()).isEqualTo(fixture.viewport().getExtentSize().height);
                fixture.view().clear();
            });
            fixture.await("clearing removes the old content and scroll offset", () ->
                    fixture.view().getFrames().isEmpty()
                    && fixture.viewport().getViewPosition().equals(new Point())
                    && fixture.scrollPane().getVerticalScrollBarPolicy() == JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            fixture.onEdt(() -> {
                assertThat(fixture.canvas().isShowing()).isTrue();
                assertThat(fixture.canvas().getWidth()).isEqualTo(fixture.viewport().getExtentSize().width);
                fixture.view().setModel(replacement);
            });
            fixture.onEdt(() -> assertThat(fixture.view().getFrames()).containsExactlyElementsOf(replacement.frames));
        }
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void backControlReturnsToTheRootAndSettles(Mode mode) {
        try (var fixture = FlamegraphViewFixture.<String>builder()
                .model(FrameModelFixture.deepChain(40)).mode(mode).build()) {
            fixture.await("the graph can scroll vertically", () ->
                    fixture.canvas().getHeight() > fixture.viewport().getExtentSize().height + 100);
            var point = fixture.onEdt(() -> {
                var viewport = fixture.viewport();
                int maximum = fixture.canvas().getHeight() - viewport.getExtentSize().height;
                viewport.setViewPosition(new Point(0, maximum / 2));
                assertThat(viewport.getViewPosition().y).isBetween(1, maximum - 1);
                // The painted back control occupies the lower-right 30px square, inset by 15px.
                var center = new Point(viewport.getX() + viewport.getWidth() - 30,
                                       viewport.getY() + viewport.getHeight() - 30);
                return SwingUtilities.convertPoint(fixture.scrollPane(), center, fixture.canvas());
            });
            fixture.pointerAt(point);
            fixture.onEdt(() -> {
                var scroll = fixture.scrollPane();
                var layer = (JLayer<?>) SwingUtilities.getAncestorOfClass(JLayer.class, scroll);
                layer.paintImmediately(0, 0, layer.getWidth(), layer.getHeight());
                var center = SwingUtilities.convertPoint(fixture.canvas(), point, scroll);
                scroll.dispatchEvent(new MouseEvent(scroll, MouseEvent.MOUSE_CLICKED,
                        System.currentTimeMillis(), 0, center.x, center.y, 1, false, MouseEvent.BUTTON1));
            });
            fixture.await("back control reaches the root edge", () -> atRoot(fixture, mode));

            var settled = new AtomicBoolean();
            var observation = fixture.onEdt(() -> {
                // Observe beyond the animation's next 20ms stop tick, while EDT failures remain owned.
                var timer = new Timer(60, event -> {
                    assertThat(atRoot(fixture, mode)).isTrue();
                    settled.set(true);
                });
                timer.setRepeats(false);
                timer.start();
                return timer;
            });
            try {
                fixture.await("back control remains settled", settled::get);
            } finally {
                fixture.onEdt(observation::stop);
            }
        }
    }

    private static boolean atRoot(FlamegraphViewFixture<?> fixture, Mode mode) {
        int expected = mode == Mode.ICICLEGRAPH ? 0
                : fixture.canvas().getHeight() - fixture.viewport().getExtentSize().height;
        return fixture.viewport().getViewPosition().y == expected;
    }
}
