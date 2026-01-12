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

import io.github.bric3.fireplace.flamegraph.FlamegraphView.FlamegraphCanvas;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;

import static io.github.bric3.fireplace.flamegraph.SwingTestUtil.findScrollPane;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Test harness for {@link FlamegraphView} that provides fluent builder-style
 * setup of spies and mocks commonly needed in tests.
 *
 * <p>Example usage:
 * <pre>{@code
 * var fg = FlamegraphViewUnderTest.<String>builder()
 *     .withCanvasSpy()
 *     .withRenderEngineSpy()
 *     .build();
 *
 * fg.view().setModel(new FrameModel<>(List.of(frame)));
 * verify(fg.canvasSpy()).setModel(any());
 * }</pre>
 *
 * <p><b>Important:</b> When using spies with Mockito stubbing, always set up
 * stubs BEFORE calling any Swing event-triggering methods like {@code setSize()},
 * to avoid Mockito's "CannotStubVoidMethodWithReturnValue" error with Swing callbacks.
 *
 * @param <T> the type parameter for the flamegraph frames
 */
public final class FlamegraphViewUnderTest<T> {

    private final FlamegraphView<T> view;
    private final FlamegraphView.FlamegraphCanvas<T> canvasSpy;
    private final FlamegraphRenderEngine<T> renderEngineSpy;
    private final Graphics2D graphics2D;

    private FlamegraphViewUnderTest(
            FlamegraphView<T> view,
            FlamegraphView.FlamegraphCanvas<T> canvasSpy,
            FlamegraphRenderEngine<T> renderEngineSpy,
            Graphics2D graphics2D
    ) {
        this.view = view;
        this.canvasSpy = canvasSpy;
        this.renderEngineSpy = renderEngineSpy;
        this.graphics2D = graphics2D;
    }

    /**
     * Creates a new builder for FlamegraphViewUnderTest.
     *
     * @param <T> the type parameter for the flamegraph frames
     * @return a new builder instance
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Returns the underlying FlamegraphView.
     */
    public FlamegraphView<T> view() {
        return view;
    }

    /**
     * Returns the FlamegraphView's component for adding to containers.
     */
    public JComponent component() {
        return view.component;
    }

    /**
     * Returns the canvas spy, or throws if not configured.
     *
     * @throws IllegalStateException if canvas spy was not enabled in builder
     */
    @SuppressWarnings("ClassEscapesDefinedScope")
    public FlamegraphView.FlamegraphCanvas<T> canvasSpy() {
        if (canvasSpy == null) {
            throw new IllegalStateException("Canvas spy not configured. Use withCanvasSpy() in builder.");
        }
        return canvasSpy;
    }

    /**
     * Returns the render engine spy or mock, or throws if not configured.
     *
     * @throws IllegalStateException if render engine spy/mock was not enabled in builder
     */
    @SuppressWarnings("ClassEscapesDefinedScope")
    public FlamegraphRenderEngine<T> renderEngine() {
        if (renderEngineSpy == null) {
            throw new IllegalStateException("Render engine not configured. Use withRenderEngineSpy() or withRenderEngineMock() in builder.");
        }
        return renderEngineSpy;
    }

    /**
     * Returns the render engine spy, or throws if not configured.
     *
     * @throws IllegalStateException if render engine spy/mock was not enabled in builder
     * @deprecated Use {@link #renderEngine()} instead
     */
    @Deprecated
    @SuppressWarnings("ClassEscapesDefinedScope")
    public FlamegraphRenderEngine<T> renderEngineSpy() {
        return renderEngine();
    }

    /**
     * Returns the Graphics2D mock, or throws if not configured.
     *
     * @throws IllegalStateException if graphics was not enabled in builder
     */
    public Graphics2D graphics2D() {
        if (graphics2D == null) {
            throw new IllegalStateException("Graphics2D not configured. Use withGraphics2D() in builder.");
        }
        return graphics2D;
    }

    /**
     * Returns the JScrollPane containing the flamegraph canvas.
     *
     * @throws IllegalStateException if no scrollpane is found in the component hierarchy
     */
    public JScrollPane scrollPane() {
        var scrollPane = findScrollPane(view.component);
        if (scrollPane == null) {
            throw new IllegalStateException("ScrollPane not found in component hierarchy");
        }
        return scrollPane;
    }

    /**
     * Returns the viewport of the scrollpane.
     *
     * @throws IllegalStateException if no scrollpane is found in the component hierarchy
     */
    public JViewport viewport() {
        return scrollPane().getViewport();
    }

    /**
     * Returns the canvas component from the viewport.
     * This returns the spy if one was configured, otherwise the original canvas.
     *
     * @return the canvas component
     */
    @SuppressWarnings({"ClassEscapesDefinedScope", "unchecked"})
    public FlamegraphView.FlamegraphCanvas<T> canvas() {
        return (FlamegraphView.FlamegraphCanvas<T>) viewport().getView();
    }

    /**
     * Sets the flamegraph dimensions via reflection.
     * This is useful for tests that need specific flamegraph sizes.
     *
     * @param width  the desired width
     * @param height the desired height
     * @throws RuntimeException if reflection operations fail
     */
    public void setFlamegraphDimensions(int width, int height) {
        try {
            var canvas = canvas();
            var flamegraphDimensionField = canvas.getClass().getDeclaredField("flamegraphDimension");
            flamegraphDimensionField.setAccessible(true);
            var flamegraphDimension = (Dimension) flamegraphDimensionField.get(canvas);
            flamegraphDimension.width = width;
            flamegraphDimension.height = height;
        } catch (Exception e) {
            throw new RuntimeException("Failed to set flamegraph dimensions", e);
        }
    }

    /**
     * Returns the minimap bounds from the canvas via reflection.
     *
     * @return the minimap bounds rectangle
     * @throws RuntimeException if reflection operations fail
     */
    public Rectangle minimapBounds() {
        return canvas().getMinimapBounds();
    }

    /**
     * Builder for {@link FlamegraphViewUnderTest}.
     *
     * @param <T> the type parameter for the flamegraph frames
     */
    public static final class Builder<T> {
        private boolean withCanvasSpy;
        private boolean withRenderEngineSpy;
        private boolean withRenderEngineMock;
        private boolean withGraphics2D;
        private Dimension graphicsSize = new Dimension(800, 600);

        private Builder() {}

        /**
         * Configures the builder to create a spy for the canvas.
         * The spy will be injected into the FlamegraphView via reflection.
         *
         * @return this builder
         */
        public Builder<T> withCanvasSpy() {
            this.withCanvasSpy = true;
            return this;
        }

        /**
         * Configures the builder to create a spy for the render engine.
         * A spy wraps the real render engine, allowing you to stub specific methods
         * while letting unstubbed methods call the real implementation.
         *
         * <p>This is mutually exclusive with {@link #withRenderEngineMock()}.
         *
         * @return this builder
         * @throws IllegalStateException if {@link #withRenderEngineMock()} was already called
         */
        public Builder<T> withRenderEngineSpy() {
            if (this.withRenderEngineMock) {
                throw new IllegalStateException("Cannot use both withRenderEngineSpy() and withRenderEngineMock(). Choose one.");
            }
            this.withRenderEngineSpy = true;
            return this;
        }

        /**
         * Configures the builder to create a mock for the render engine.
         * A mock has no real behavior - all methods return default values (null, 0, false)
         * unless explicitly stubbed. This is useful for tests that verify methods are
         * never called or need complete control over return values.
         *
         * <p>This is mutually exclusive with {@link #withRenderEngineSpy()}.
         *
         * @return this builder
         * @throws IllegalStateException if {@link #withRenderEngineSpy()} was already called
         */
        public Builder<T> withRenderEngineMock() {
            if (this.withRenderEngineSpy) {
                throw new IllegalStateException("Cannot use both withRenderEngineSpy() and withRenderEngineMock(). Choose one.");
            }
            this.withRenderEngineMock = true;
            return this;
        }

        /**
         * Configures the builder to create a Graphics2D from a BufferedImage.
         * This is useful for headless testing where {@code getGraphics()} returns null.
         * The Graphics2D will be stubbed on the canvas spy via {@code doReturn(g2d).when(spy).getGraphics()}.
         *
         * <p>Note: This implicitly enables canvas spy if not already enabled.
         *
         * @return this builder
         */
        public Builder<T> withGraphics2D() {
            this.withGraphics2D = true;
            this.withCanvasSpy = true; // Graphics2D stubbing requires canvas spy
            return this;
        }

        /**
         * Configures the size of the BufferedImage used to create Graphics2D.
         * Default is 800x600.
         *
         * @param width the width of the image
         * @param height the height of the image
         * @return this builder
         */
        public Builder<T> withGraphicsSize(int width, int height) {
            this.graphicsSize = new Dimension(width, height);
            return this;
        }

        /**
         * Builds the FlamegraphViewUnderTest with all configured spies and mocks.
         *
         * @return a new FlamegraphViewUnderTest instance
         * @throws RuntimeException if reflection operations fail
         */
        @SuppressWarnings("unchecked")
        public FlamegraphViewUnderTest<T> build() {
            try {
                var view = new FlamegraphView<T>();
                FlamegraphView.FlamegraphCanvas<T> canvasSpy = null;
                FlamegraphRenderEngine<T> renderEngineSpy = null;
                Graphics2D graphics2D = null;

                // Access private canvas field using reflection
                var canvasField = FlamegraphView.class.getDeclaredField("canvas");
                canvasField.setAccessible(true);
                var canvas = (FlamegraphView.FlamegraphCanvas<T>) canvasField.get(view);

                if (withRenderEngineSpy) {
                    renderEngineSpy = spy(canvas.getFlamegraphRenderEngine());
                    canvas.setFlamegraphRenderEngine(renderEngineSpy);
                } else if (withRenderEngineMock) {
                    renderEngineSpy = mock(FlamegraphRenderEngine.class);
                    canvas.setFlamegraphRenderEngine(renderEngineSpy);
                }

                if (withCanvasSpy) {
                    canvasSpy = spy(canvas);

                    // Update canvas field and viewport
                    canvasField.set(view, canvasSpy);
                    var scrollPane = findScrollPane(view.component);
                    if (scrollPane != null) {
                        scrollPane.getViewport().setView(canvasSpy);
                    }

                    if (withGraphics2D) {
                        var image = new BufferedImage(
                                graphicsSize.width,
                                graphicsSize.height,
                                BufferedImage.TYPE_INT_ARGB
                        );
                        graphics2D = image.createGraphics();
                        doReturn(graphics2D).when(canvasSpy).getGraphics();
                    }

                    updateMouseAdaptersToSpy(canvas, canvasSpy);
                    updateMouseAdaptersToSpy(scrollPane, canvasSpy);
                }

                return new FlamegraphViewUnderTest<>(view, canvasSpy, renderEngineSpy, graphics2D);
            } catch (Exception e) {
                throw new RuntimeException("Failed to build FlamegraphViewUnderTest", e);
            }
        }

        /**
         * Updates the canvas reference in mouse adapters to point to the canvas spy.
         * This is necessary when stubbing methods like {@code isInsideMinimap} on the spy,
         * because the mouse adapters hold a reference to the canvas and call methods on it.
         *
         * <p>Call this method after setting up stubs on the canvas spy.
         *
         * @throws IllegalStateException if canvas spy was not enabled in builder
         * @throws RuntimeException if reflection operations fail
         */
        @SuppressWarnings("ClassEscapesDefinedScope")
        public void updateMouseAdaptersToSpy(JComponent component, FlamegraphCanvas<T> canvasSpy) {
            try {
                // The spy is set in the field, so we need to get listeners from the view's component hierarchy
                for (var listener : component.getMouseListeners()) {
                    if (listener.getClass().getSimpleName().contains("Mouse")) {
                        try {
                            var adapterCanvasField = listener.getClass().getDeclaredField("canvas");
                            adapterCanvasField.setAccessible(true);
                            adapterCanvasField.set(listener, canvasSpy);
                        } catch (NoSuchFieldException e) {
                            // Ignore if field doesn't exist
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to update mouse adapters", e);
            }
        }
    }
}