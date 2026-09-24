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
import io.github.bric3.fireplace.flamegraph.FrameBox;
import io.github.bric3.fireplace.flamegraph.FrameModel;
import io.github.bric3.fireplace.flamegraph.FrameRenderer;
import org.mockito.MockedStatic;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/** A real view composed with the shared window lifecycle; all Swing access belongs on the EDT. */
public final class FlamegraphViewFixture<T> implements AutoCloseable {
    private final SwingWindowFixture window;
    private final FlamegraphView<T> view;
    private final JComponent canvas;
    private final JScrollPane scrollPane;
    private MockedStatic<MouseInfo> pointer;
    private Point screenPoint;
    private boolean closed;

    private FlamegraphViewFixture(Builder<T> builder) {
        if (ForkJoinPool.getCommonPoolParallelism() < 2) {
            throw new IllegalStateException("UI tests require -Djava.util.concurrent.ForkJoinPool.common.parallelism=2"
                                            + " so minimap work can be drained before disposal");
        }
        window = SwingWindowFixture.open(builder.width, builder.height);
        try {
            view = window.onEdt(() -> {
                var graph = new FlamegraphView<T>();
                graph.setShowMinimap(false);
                if (builder.renderer != null) {
                    graph.setFrameRender(builder.renderer);
                }
                graph.setModel(builder.model);
                graph.setMode(builder.mode);
                return graph;
            });
            canvas = window.onEdt(() -> {
                var component = new JComponent[1];
                view.configureCanvas(c -> component[0] = c);
                return component[0];
            });
            scrollPane = window.onEdt(() -> (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, canvas));
            window.onEdt(() -> setScreenPointer(new Point(-10000, -10000)));
            window.mount(view.component);
            await("canvas layout", () -> canvas.isShowing() && canvas.getWidth() > 0 && canvas.getHeight() > 0);
        } catch (RuntimeException | Error failure) {
            try {
                disposeWindowAndPointer();
            } catch (RuntimeException | Error cleanup) {
                failure.addSuppressed(cleanup);
            }
            throw failure;
        }
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public FlamegraphView<T> view() {
        SwingWindowFixture.requireEdt();
        return view;
    }

    public JComponent canvas() {
        SwingWindowFixture.requireEdt();
        return canvas;
    }

    public JScrollPane scrollPane() {
        SwingWindowFixture.requireEdt();
        return scrollPane;
    }

    public JViewport viewport() {
        return scrollPane().getViewport();
    }

    public JFrame window() {
        return window.window();
    }

    public <V> V onEdt(Supplier<V> action) {
        return window.onEdt(action);
    }

    public void onEdt(Runnable action) {
        window.onEdt(action);
    }

    public void await(String description, BooleanSupplier condition) {
        window.await(description, condition);
    }

    /** The thread-local mock stays on the EDT through delayed callbacks and fixture cleanup. */
    public void pointerAt(Point canvasCoordinates) {
        onEdt(() -> {
            var point = new Point(canvasCoordinates);
            SwingUtilities.convertPointToScreen(point, canvas);
            setScreenPointer(point);
        });
    }

    private void setScreenPointer(Point point) {
        SwingWindowFixture.requireEdt();
        screenPoint = new Point(point);
        if (pointer == null) {
            PointerInfo info = mock(PointerInfo.class);
            when(info.getLocation()).thenAnswer(invocation -> new Point(screenPoint));
            pointer = mockStatic(MouseInfo.class);
            pointer.when(MouseInfo::getPointerInfo).thenReturn(info);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        Throwable failure = null;
        try {
            var completed = new AtomicBoolean();
            SwingWindowFixture.runOnEdt(() -> {
                window.retireMouseInput();
                view.setShowMinimap(false);
                setScreenPointer(new Point(-10000, -10000));
                var terminal = new MouseEvent(scrollPane, MouseEvent.MOUSE_MOVED,
                                              System.currentTimeMillis(), 0, -100, -100, 0, false);
                view.setHoverListener(new FlamegraphView.HoverListener<>() {
                    @Override
                    public void onStopHover(FrameBox<T> previous, Rectangle rectangle, MouseEvent event) {
                        if (event == terminal) {
                            completed.set(true);
                        }
                    }

                    @Override
                    public void onFrameHover(FrameBox<T> frame, Rectangle rectangle, MouseEvent event) {
                        if (event == terminal) {
                            completed.set(true);
                        }
                    }
                });
                // The scroll pane owns the debounce; canvas forwarding would copy this marker event.
                scrollPane.dispatchEvent(terminal);
                return null;
            });
            window.awaitCleanup("final hover callback before disposal", completed::get);
        } catch (RuntimeException | Error thrown) {
            failure = thrown;
            throw thrown;
        } finally {
            try {
                disposeWindowAndPointer();
            } catch (RuntimeException | Error cleanup) {
                if (failure == null) {
                    throw cleanup;
                }
                failure.addSuppressed(cleanup);
            }
        }
    }

    private void disposeWindowAndPointer() {
        try (var ownedWindow = window) {
            try {
                SwingWindowFixture.runOnEdt(() -> {
                    window.retireMouseInput();
                    if (view != null) {
                        view.setShowMinimap(false);
                    }
                    return null;
                });
                // Serial scenarios own this pool. Do not use awaitQuiescence: it can run a
                // blocked renderer on the calling thread and exceed its advertised timeout.
                window.awaitCleanup("pending minimap generation", ForkJoinPool.commonPool()::isQuiescent);
            } finally {
                // This EDT barrier follows the completed workers' queued image installations.
                // Keep pointer control through disposal, including synchronous exit callbacks.
                SwingWindowFixture.runOnEdt(() -> {
                    try (var ownedPointer = pointer) {
                        window.window().dispose();
                    }
                    return null;
                });
            }
        }
    }

    public static final class Builder<T> {
        private FrameModel<T> model = FrameModel.empty();
        private FlamegraphView.Mode mode = FlamegraphView.Mode.FLAMEGRAPH;
        private FrameRenderer<T> renderer;
        private int width = 640;
        private int height = 360;

        public Builder<T> model(FrameModel<T> model) {
            this.model = Objects.requireNonNull(model);
            return this;
        }

        public Builder<T> mode(FlamegraphView.Mode mode) {
            this.mode = Objects.requireNonNull(mode);
            return this;
        }

        public Builder<T> renderer(FrameRenderer<T> renderer) {
            this.renderer = Objects.requireNonNull(renderer);
            return this;
        }

        public Builder<T> size(int width, int height) {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Window dimensions must be positive");
            }
            this.width = width;
            this.height = height;
            return this;
        }

        public FlamegraphViewFixture<T> build() {
            return new FlamegraphViewFixture<>(this);
        }
    }
}
