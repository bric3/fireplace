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

import io.github.bric3.fireplace.core.ui.JScrollPaneWithBackButton;
import io.github.bric3.fireplace.core.ui.MouseInputListenerWorkaroundForToolTipEnabledComponent;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.lang.Boolean.TRUE;

/**
 * Swing component that allows to display a flame graph.
 * <p>
 * In general the Flamegraph's raw data is an actual tree. However walking
 * this tree require substantial effort to process during painting.
 * For this reason the actual tree must be pre-processed as a list of
 * {@link FrameBox}.
 * </p>
 * <p>
 * It can be used as follows:
 * <pre><code>
 * var flamegraphView = new FlamegraphView&lt;MyNode&gt;();
 * flamegraphView.setShowMinimap(false);
 * flamegraphView.setRenderConfiguration(
 *     frameTextProvider,           // string representation candidates
 *     frameColorProvider,          // color the frame
 *     frameFontProvider,           // returns a given font for a frame
 * );
 * flamegraphView.setTooltipTextFunction(
 *     frameToToolTipTextFunction   // text tooltip function
 * );
 *
 * panel.add(flamegraphView.component);
 *
 * // then later
 * flamegraphView.setModel(
 *      new FrameModel&lt;&gt;(
 *          "title",                // title of the flamegraph, used in root node
 *          frameEqualityFunction,  // equality function for frames, used for sibling detection
 *          listOfFrameBox()        // list of frames (FrameBox&lt;MyNode&gt;)
 *      )
 * )
 * </code></pre>
 *
 * <p>
 * The created and <em>final</em> {@code component} is a composite that is based
 * on a {@link JScrollPane}.
 * </p>
 *
 * @param <T> The type of the node data.
 * @see FlamegraphImage
 * @see FrameModel
 * @see FrameBox
 * @see HoverListener
 * @see FrameColorProvider
 * @see FrameTextsProvider
 * @see FrameFontProvider
 * @see FlamegraphRenderEngine
 * @see FrameRenderer
 */
public class FlamegraphView<T> {
    /**
     * Internal key to get the Flamegraph from the component.
     */
    private static final String OWNER_KEY = "flamegraphOwner";
    /**
     * The key for a client property that controls the display of rendering statistics.
     */
    public static final String SHOW_STATS = "flamegraph.show_stats";

    @NotNull
    private final FlamegraphCanvas<T> canvas;

    /**
     * The final composite component that can display a flame graph.
     */
    @NotNull
    public final JComponent component;

    /**
     * Mouse input listener used to move the canvas over the JScrollPane
     * as well as trigger other behavior on the canvas.
     */
    @NotNull
    private final FlamegraphView.FlamegraphHoveringScrollPaneMouseListener<T> scrollPaneListener;

    /**
     * The precomputed list of frames.
     */
    @NotNull
    private FrameModel<T> framesModel = FrameModel.empty();

    /**
     * Display mode for this stack-frame tree.
     */
    public enum Mode {
        FLAMEGRAPH, ICICLEGRAPH
    }

    /**
     * Represents a custom action when zooming.
     */
    public interface ZoomAction {
        /**
         * Called when the zoom action is triggered.
         *
         * <p>
         * Typical implementation will use the passed {@code zoomTarget}, and
         * invoke {@link ZoomableComponent#zoom(ZoomTarget)}. These implementation
         * could for example compute intermediate zoom target in order to produce
         * an animation.
         * </p>
         *
         * @param zoomableComponent The canvas to zoom on.
         * @param zoomTarget        the zoom target.
         * @return Whether zooming has been performed.
         */
        <T> boolean zoom(
                @NotNull ZoomableComponent<T> zoomableComponent,
                @NotNull ZoomTarget<T> zoomTarget
        );
    }

    /**
     * Represents a zoomable JComponent.
     */
    public interface ZoomableComponent<T> {
        /**
         * Actually perform the zooming operation on the component.
         *
         * <p>
         * This likely involves revalidation and repainting of the component.
         * </p>
         *
         * @param zoomTarget The zoom target.
         */
        void zoom(@NotNull ZoomTarget<T> zoomTarget);

        /**
         * @return the width of the component.
         */
        int getWidth();

        /**
         * @return the height of the component
         */
        int getHeight();

        /**
         * @return the location of the component in the parent container.
         */
        @NotNull
        Point getLocation();
    }

    /**
     * Listener for hovered frames.
     *
     * @param <T> The type of the node data.
     */
    public interface HoverListener<T> {
        /**
         * @param previousHoveredFrame      The previous frame that was hovered, or {@code null} if the mouse is exiting the component.
         * @param prevHoveredFrameRectangle The rectangle of the previous hovered frame, or {@code null} if the mouse is exiting the component.
         * @param e                         The mouse event
         */
        default void onStopHover(
                @Nullable FrameBox<@NotNull T> previousHoveredFrame,
                @Nullable Rectangle prevHoveredFrameRectangle,
                @NotNull MouseEvent e
        ) {}

        /**
         * @param frame                 The frame that is hovered.
         * @param hoveredFrameRectangle The rectangle of the hovered frame.
         * @param e                     The mouse event
         */
        void onFrameHover(
                @NotNull FrameBox<@NotNull T> frame,
                @NotNull Rectangle hoveredFrameRectangle,
                @NotNull MouseEvent e
        );

        /**
         * Utility method to get a point from a mouse event with the vertical coordinate set to
         * the given frame rectangle.
         *
         * @param frameRect  The frame rectangle.
         * @param mouseEvent The mouse event, event is expected to come from
         *                   {@link HoverListener} methods.
         * @return The point
         */
        @NotNull
        static Point getPointLeveledToFrameDepth(@NotNull MouseEvent mouseEvent, @NotNull Rectangle frameRect) {
            var scrollPane = (JScrollPane) mouseEvent.getComponent();
            var canvas = scrollPane.getViewport().getView();

            var ownerFg = FlamegraphView.from(scrollPane)
                                        .orElseThrow(() -> new IllegalStateException("Cannot find FlamegraphView owner"));

            // SwingUtilities.convertRectangle()
            var pointOnCanvas = SwingUtilities.convertPoint(scrollPane, mouseEvent.getPoint(), canvas);
            pointOnCanvas.y = frameRect.y;
            return SwingUtilities.convertPoint(canvas, pointOnCanvas, ownerFg.component);
        }
    }

    /**
     * Return the {@code FlamegraphView} that created the passed component.
     * <p>
     * If this wasn't returned by a {@code FlamegraphView} then return empty.
     *
     * @param component the <code>JComponent</code>
     * @param <T>       The type of the node data.
     * @return The {@code FlamegraphView} instance that crated this JComponent or empty.
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<FlamegraphView<@NotNull T>> from(@NotNull JComponent component) {
        return Optional.ofNullable((FlamegraphView<T>) component.getClientProperty(OWNER_KEY));
    }

    /**
     * Creates an empty flame graph.
     * To use in Swing just access the {@link #component} field.
     */
    public FlamegraphView() {
        canvas = new FlamegraphCanvas<>(this);
        // default configuration
        setRenderConfiguration(
                FrameTextsProvider.of(frameBox -> frameBox.actualNode.toString()),
                FrameColorProvider.defaultColorProvider(f -> UIManager.getColor("Button.background")),
                FrameFontProvider.defaultFontProvider()
        );
        canvas.putClientProperty(OWNER_KEY, this);
        scrollPaneListener = new FlamegraphHoveringScrollPaneMouseListener<>(canvas);
        var scrollPane = createScrollPane();
        scrollPane.putClientProperty(OWNER_KEY, this);
        var layeredScrollPane = JScrollPaneWithBackButton.create(
                () -> {
                    // Code to tweak the actions
                    // https://stackoverflow.com/a/71009104/48136
                    // see javax.swing.plaf.basic.BasicScrollPaneUI.Actions
                    //                    var actionMap = scrollPane.getActionMap();
                    //                    var inputMap = scrollPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
                    // var inputMap = scrollPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

                    scrollPane.getVerticalScrollBar().setUnitIncrement(16);
                    scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
                    scrollPaneListener.install(scrollPane);
                    new MouseInputListenerWorkaroundForToolTipEnabledComponent(scrollPane).install(canvas);
                    canvas.setupListeners(scrollPane);


                    return scrollPane;
                }
        );
        canvas.addPropertyChangeListener(FlamegraphCanvas.GRAPH_MODE, evt -> {
            var mode = (Mode) evt.getNewValue();
            layeredScrollPane.firePropertyChange(
                    JScrollPaneWithBackButton.BACK_TO_DIRECTION,
                    -1,
                    mode == Mode.ICICLEGRAPH ? SwingConstants.NORTH : SwingConstants.SOUTH
            );
        });

        component = wrap(layeredScrollPane, bg -> {
            scrollPane.setBorder(null);
            scrollPane.setBackground(bg);
            scrollPane.getVerticalScrollBar().setBackground(bg);
            scrollPane.getHorizontalScrollBar().setBackground(bg);
            canvas.setBackground(bg);
        });
    }

    @NotNull
    private JScrollPane createScrollPane() {
        var jScrollPane = new JScrollPane(canvas);
        var viewport = new JViewport() {
            @Override
            protected LayoutManager createLayoutManager() {
                // Custom layout manager to handle the viewport resizing
                // Since the canvas size expands or shrink proportionally to the viewport,
                // a special handling is necessary when the VP is resized.
                return new ViewportLayout() {
                    private final Dimension oldViewPortSize = new Dimension(); // reusable
                    private final Dimension flamegraphSize = new Dimension(); // reusable
                    private final Point flamegraphLocation = new Point(); // reusable

                    @Override
                    public void layoutContainer(Container parent) {
                        // Custom layout code to handle container shrinking.
                        // The default view port layout asks the preferred size
                        // of the view, but that cannot work since the canvas won't update
                        // its width, as it receives its size from the layout container.
                        //
                        // However, the default algorithm only updates the size
                        // after it has received the preferred size, or if the
                        // viewport got bigger.
                        //
                        // This code makes the necessary query to the canvas to
                        // ask if it needs a new size given the viewport width change,
                        // to keep the same zoom factor.
                        //
                        // The view location is also updated.

                        var vp = (JViewport) parent;
                        var canvas = (FlamegraphCanvas<?>) vp.getView();
                        int oldVpWidth = oldViewPortSize.width;
                        var vpSize = vp.getSize(oldViewPortSize);

                        // Never show the horizontal scrollbar when the scale factor is 1.0
                        // Only change it when necessary
                        int horizontalScrollBarPolicy = jScrollPane.getHorizontalScrollBarPolicy();
                        double lastScaleFactor = canvas.zoomModel.getLastScaleFactor();
                        int newPolicy = lastScaleFactor == 1.0 ?
                                     ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER :
                                     ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
                        if (horizontalScrollBarPolicy != newPolicy) {
                            jScrollPane.setHorizontalScrollBarPolicy(newPolicy);
                        }

                        // view port has been resized
                        if (vpSize.width != oldVpWidth) {
                            // scale the fg size with the new viewport width
                            canvas.updateFlamegraphDimension(
                                    flamegraphSize,
                                    (int) (((double) vpSize.width) / lastScaleFactor)
                            );
                            vp.setViewSize(flamegraphSize);

                            // if view position X > 0
                            //   the fg is zoomed
                            //   => get the latest position ratio resulting from user interaction
                            //   => apply ratio to the current fg width
                            int oldFlamegraphX = Math.abs(flamegraphLocation.x);
                            if (oldFlamegraphX > 0) {
                                double positionRatio = canvas.zoomModel.getLastUserInteractionStartX();

                                flamegraphLocation.x = Math.abs((int) (positionRatio * flamegraphSize.width));
                                flamegraphLocation.y = Math.abs(flamegraphLocation.y);

                                vp.setViewPosition(flamegraphLocation);
                            }
                        } else {
                            super.layoutContainer(parent);
                            // capture the sizes
                            vp.getSize(oldViewPortSize);
                            canvas.getSize(flamegraphSize);
                            canvas.getLocation(flamegraphLocation);
                        }
                    }
                };
            }
        };
        jScrollPane.setViewport(viewport);
        jScrollPane.setViewportView(canvas);
        return jScrollPane;
    }

    /**
     * Murky workaround to propagate the background color to the canvas
     * since JLayer is final.
     */
    private @NotNull JPanel wrap(@NotNull JComponent owner, @NotNull Consumer<@NotNull Color> configureBorderAndBackground) {
        var wrapper = new JPanel(new BorderLayout()) {
            @Override
            public void updateUI() {
                super.updateUI();
                configureBorderAndBackground.accept(getBackground());
            }

            @Override
            public void setBackground(Color bg) {
                super.setBackground(bg);
                configureBorderAndBackground.accept(bg);
            }
        };
        wrapper.setBorder(null);
        wrapper.add(owner);
        wrapper.putClientProperty(OWNER_KEY, this);
        return wrapper;
    }

    /**
     * Experimental configuration hook for the underlying canvas.
     *
     * @param canvasConfigurer The configurer for the canvas.
     */
    public void configureCanvas(@NotNull Consumer<@NotNull JComponent> canvasConfigurer) {
        Objects.requireNonNull(canvasConfigurer).accept(canvas);
    }

    /**
     * Replaces the frame colors provider.
     *
     * @param frameColorProvider A provider that takes a frame and returns its colors.
     * @see FrameColorProvider
     */
    public void setFrameColorProvider(@NotNull FrameColorProvider<@NotNull T> frameColorProvider) {
        this.canvas.getFlamegraphRenderEngine()
                   .getFrameRenderer()
                   .setFrameColorProvider(frameColorProvider);
    }

    /**
     * Returns the frame colors provider.
     *
     * @return The frame colors provider, may return <code>null</code> if renderer not configured.
     */
    @NotNull
    public FrameColorProvider<@NotNull T> getFrameColorProvider() {
        return canvas.getFlamegraphRenderEngine()
                     .getFrameRenderer()
                     .getFrameColorProvider();
    }

    /**
     * Replaces the frame font provider.
     *
     * @param frameFontProvider A provider that takes a frame and returns its font.
     * @see FrameFontProvider
     */
    public void setFrameFontProvider(@NotNull FrameFontProvider<@NotNull T> frameFontProvider) {
        this.canvas.getFlamegraphRenderEngine()
                   .getFrameRenderer()
                   .setFrameFontProvider(frameFontProvider);
    }

    /**
     * Returns the frane font provider.
     *
     * @return The frame font provider, may return <code>null</code> if renderer not configured.
     */
    @NotNull
    public FrameFontProvider<@NotNull T> getFrameFontProvider() {
        return canvas.getFlamegraphRenderEngine()
                     .getFrameRenderer()
                     .getFrameFontProvider();
    }

    /**
     * Replaces the frame to text candidates provider.
     *
     * @param frameTextsProvider A provider that takes a frame and returns its colors.
     * @see FrameTextsProvider
     */
    public void setFrameTextsProvider(@NotNull FrameTextsProvider<@NotNull T> frameTextsProvider) {
        this.canvas.getFlamegraphRenderEngine()
                   .getFrameRenderer()
                   .setFrameTextsProvider(frameTextsProvider);
    }

    /**
     * Returns the frame texts candidate provider.
     *
     * @return The frame texts candidate provider, may return <code>null</code> if renderer not configured.
     */
    @NotNull
    public FrameTextsProvider<@NotNull T> getFrameTextsProvider() {
        return canvas.getFlamegraphRenderEngine()
                     .getFrameRenderer()
                     .getFrameTextsProvider();
    }

    /**
     * Toggle the display of a gap between frames.
     *
     * @param frameGapEnabled {@code true} to show a gap between frames, {@code false} otherwise.
     */
    public void setFrameGapEnabled(boolean frameGapEnabled) {
        canvas.getFlamegraphRenderEngine()
              .getFrameRenderer()
              .setDrawingFrameGap(frameGapEnabled);
    }

    /**
     * Whether gap between frames is displayed.
     *
     * @return {@code true} if gap between frames is shown, {@code false} otherwise.
     */
    public boolean isFrameGapEnabled() {
        return canvas.getFlamegraphRenderEngine()
                     .getFrameRenderer()
                     .isDrawingFrameGap();
    }

    /**
     * Replaces the default color shade for the minimap.
     * Alpha colors are supported.
     *
     * @param minimapShadeColorSupplier Color supplier.
     */
    public void setMinimapShadeColorSupplier(@NotNull Supplier<@NotNull Color> minimapShadeColorSupplier) {
        canvas.setMinimapShadeColorSupplier(Objects.requireNonNull(minimapShadeColorSupplier));
    }

    /**
     * Sets a flag that controls whether the minimap is visible.
     *
     * @param showMinimap {@code true} to show the minimap, {@code false} otherwise.
     */
    public void setShowMinimap(boolean showMinimap) {
        canvas.showMinimap(showMinimap);
    }

    /**
     * Sets a flag that controls whether the minimap is visible.
     *
     * @return {@code true} if the minimap shown, {@code false} otherwise.
     */
    public boolean isShowMinimap() {
        return canvas.isShowMinimap();
    }

    /**
     * Sets a flag that controls whether the siblings of the hovered frame are highlighted.
     *
     * @param showHoveredSiblings {@code true} to show the siblings of the hovered frame, {@code false} otherwise.
     */
    public void setShowHoveredSiblings(boolean showHoveredSiblings) {
        canvas.getFlamegraphRenderEngine().setShowHoveredSiblings(showHoveredSiblings);
    }

    /**
     * Whether the siblings of the hovered frame are highlighted.
     *
     * @return {@code true} if the siblings of the hovered frame are highlighted, {@code false} otherwise.
     */
    public boolean isShowHoveredSiblings() {
        return canvas.getFlamegraphRenderEngine()
                     .isShowHoveredSiblings();
    }

    /**
     * Sets the display mode, either {@link FlamegraphView.Mode#FLAMEGRAPH} or {@link Mode#ICICLEGRAPH}.
     *
     * @param mode The display mode.
     */
    public void setMode(@NotNull FlamegraphView.Mode mode) {
        canvas.setMode(mode);
    }

    /**
     * Returns the current display mode.
     *
     * @return the current display mode.
     * @throws IllegalStateException if invoking this before renderer is configured.
     */
    @NotNull
    public FlamegraphView.Mode getMode() {
        return canvas.getMode();
    }

    /**
     * Replaces the default tooltip component.
     *
     * @param tooltipComponentSupplier The tooltip component supplier.
     */
    public void setTooltipComponentSupplier(@NotNull Supplier<@NotNull JToolTip> tooltipComponentSupplier) {
        canvas.setTooltipComponentSupplier(Objects.requireNonNull(tooltipComponentSupplier));
    }

    /**
     * Sets a callback that provides a reference to a frame when the user performs a
     * "popup" action on the frame graph (typically a right-click with the mouse).
     *
     * @param consumer the consumer ({@code null} permitted).
     */
    public void setPopupConsumer(@NotNull BiConsumer<@NotNull FrameBox<@NotNull T>, @NotNull MouseEvent> consumer) {
        canvas.setPopupConsumer(Objects.requireNonNull(consumer));
    }

    /**
     * Sets a callback that provides a reference to a frame when the user performs a
     * "popup" action on the frame graph (typically a right-click with the mouse).
     *
     * @param consumer the consumer ({@code null} permitted).
     */
    public void setSelectedFrameConsumer(@NotNull BiConsumer<@NotNull FrameBox<@NotNull T>, @NotNull MouseEvent> consumer) {
        canvas.setSelectedFrameConsumer(Objects.requireNonNull(consumer));
    }

    /**
     * Sets a listener that will be called when the mouse hovers a frame, or when it stops hovering.
     *
     * @param hoverListener the listener ({@code null} permitted).
     */
    public void setHoverListener(@NotNull HoverListener<@NotNull T> hoverListener) {
        scrollPaneListener.setHoverListener(hoverListener);
    }

    /**
     * Sets the {@link FrameModel}.
     *
     * <p>
     * It takes a {@link FrameModel} object that wraps the actual data.
     * </p>
     *
     * @param frameModel The {@code FrameBox} list to display.
     */
    public void setModel(@NotNull FrameModel<@NotNull T> frameModel) {
        framesModel = Objects.requireNonNull(frameModel);
        canvas.getFlamegraphRenderEngine().init(frameModel);

        // force invalidation of the canvas so that the scroll-pane will fetch the new preferredSize
        // otherwise old cached preferredSize will be used.
        canvas.invalidate();
        component.revalidate();
        component.repaint();
    }

    /**
     * Configures a new rendering engine of {@link FlamegraphView}.
     *
     * <p>
     * When this method is invoked after a model has been set this request
     * a new repaint.
     * </p>
     *
     * <p>
     * In particular, this function defines the behavior to access the typed data:
     * <ul>
     *     <li>Possible string candidates to display in frames, those are
     *     selected based on the available space</li>
     *     <li>The root node text to display, if something specific is relevant,
     *     like the type of events, their number, etc.</li>
     *     <li>The frame background and foreground colors.</li>
     * </ul>
     *
     * @param frameTextsProvider The function to display label in frames.
     * @param frameColorProvider The frame to background color function.
     * @param frameFontProvider  The frame font provider.
     */
    public void setRenderConfiguration(
            @NotNull FrameTextsProvider<@NotNull T> frameTextsProvider,
            @NotNull FrameColorProvider<@NotNull T> frameColorProvider,
            @NotNull FrameFontProvider<@NotNull T> frameFontProvider
    ) {
        var flamegraphRenderEngine = new FlamegraphRenderEngine<>(
                new FrameRenderer<>(
                        frameTextsProvider,
                        frameColorProvider,
                        frameFontProvider
                )
        ).init(framesModel);

        canvas.setFlamegraphRenderEngine(flamegraphRenderEngine);

        canvas.revalidate();
        canvas.repaint();
    }

    /**
     * Configures the tooltip text of {@link FlamegraphView}.
     *
     * <p>
     * This is only useful when actually using swing tool tips.
     * </p>
     *
     * @param tooltipTextFunction The frame tooltip text function.
     */
    public void setTooltipTextFunction(
            @NotNull BiFunction<@NotNull FrameModel<@NotNull T>, FrameBox<@NotNull T>, String> tooltipTextFunction
    ) {
        canvas.setToolTipTextFunction(Objects.requireNonNull(tooltipTextFunction));
    }

    /**
     * Clear all data.
     */
    public void clear() {
        framesModel = FrameModel.empty();

        canvas.getFlamegraphRenderEngine().reset();
        canvas.revalidate();
        canvas.repaint();
    }

    @NotNull
    public FrameModel<@NotNull T> getFrameModel() {
        return framesModel;
    }

    @NotNull
    public List<@NotNull FrameBox<@NotNull T>> getFrames() {
        return framesModel.frames;
    }

    /**
     * Adds an arbitrary key/value "client property".
     *
     * @param key   the key, use {@code null} to remove.
     * @param value the value.
     * @see JComponent#putClientProperty(Object, Object)
     */
    public <V> void putClientProperty(@NotNull String key, V value) {
        // value can be null, it means removing the key (see putClientProperty)
        canvas.putClientProperty(Objects.requireNonNull(key), value);
    }

    /**
     * Returns the value of the property with the specified key.
     *
     * @param key the key.
     * @return the value
     * @see JComponent#getClientProperty(Object)
     */
    @SuppressWarnings("unchecked")
    public <V> V getClientProperty(@NotNull String key) {
        return (V) canvas.getClientProperty(Objects.requireNonNull(key));
    }

    /**
     * Triggers a repaint of the component.
     */
    public void requestRepaint() {
        canvas.revalidate();
        canvas.repaint();
        canvas.triggerMinimapGeneration();
    }

    public void overrideZoomAction(@NotNull FlamegraphView.ZoomAction zoomActionOverride) {
        Objects.requireNonNull(zoomActionOverride);
        this.canvas.zoomActionOverride = zoomActionOverride;
    }

    /**
     * Reset the zoom to 1:1.
     */
    public void resetZoom() {
        zoom(canvas, canvas.getResetZoomTarget());
    }

    /**
     * Programmatic zoom to the specified frame.
     *
     * @param frame The frame to zoom to.
     */
    public void zoomTo(@NotNull FrameBox<@NotNull T> frame) {
        zoom(canvas, canvas.getFrameZoomTarget(frame));
    }

    /**
     * Higher level zoom op that operates on the view and model.
     * <p>
     * This method will call under the hood {@link FlamegraphCanvas#zoom(ZoomTarget)}.
     * Possibly a zoom override ({@link #overrideZoomAction(FlamegraphView.ZoomAction)} will be set up and may be call instead,
     * but under the hood this will call {@link FlamegraphCanvas#zoom(ZoomTarget)} via it's
     * {@link FlamegraphView.ZoomableComponent#zoom(ZoomTarget)}.
     *
     * @param canvas     the canvas.
     * @param zoomTarget the zoom target.
     * @param <T>        the type of the node data.
     */
    private static <T> void zoom(@NotNull FlamegraphCanvas<@NotNull T> canvas, @Nullable ZoomTarget<@NotNull T> zoomTarget) {
        if (zoomTarget == null) {
            // NOOP
            return;
        }

        // adjust zoom target location for horizontal scrollbar height if canvas bigger than viewRect
        if (canvas.getMode() == Mode.FLAMEGRAPH) {
            var visibleRect = canvas.getVisibleRect();
            var viewPort = (JViewport) SwingUtilities.getUnwrappedParent(canvas);
            var scrollPane = (JScrollPane) viewPort.getParent();

            var hsb = scrollPane.getHorizontalScrollBar();
            if (!hsb.isVisible() && visibleRect.getWidth() < zoomTarget.getWidth()) {
                var modifiedRect = zoomTarget.getTargetBounds();
                modifiedRect.y -= hsb.getPreferredSize().height;

                zoomTarget = new ZoomTarget<>(modifiedRect, zoomTarget.targetFrame);
            }
        }

        // Set the zoom model to the Zoom Target
        canvas.zoomModel.recordLastPositionFromZoomTarget(canvas, zoomTarget);

        if (canvas.zoomActionOverride == null || !canvas.zoomActionOverride.zoom(canvas, zoomTarget)) {
            canvas.zoom(zoomTarget);
        }
    }

    /**
     * Sets frames that need to be highlighted.
     * <p>
     * The passed collection must be a subset of the frames that were used
     * in {@link #setModel(FrameModel)}, this triggers a repaint event.
     * </p>
     *
     * <p>
     * To remove highlighting pass an empty collection.
     * </p>
     *
     * @param framesToHighlight Subset of frames to highlight.
     * @param searched          Text searched for.
     */
    public void highlightFrames(@NotNull Set<@NotNull FrameBox<@NotNull T>> framesToHighlight, @NotNull String searched) {
        Objects.requireNonNull(framesToHighlight);
        Objects.requireNonNull(searched);
        canvas.getFlamegraphRenderEngine()
              .setHighlightFrames(framesToHighlight, searched);
        canvas.repaint();
    }

    static class FlamegraphCanvas<T> extends JPanel implements ZoomableComponent<T> {
        public static final String GRAPH_MODE = "mode";
        @Nullable
        private Image minimap;
        @Nullable
        private JToolTip toolTip;
        private FlamegraphRenderEngine<@NotNull T> flamegraphRenderEngine;
        @Nullable
        private BiFunction<@NotNull FrameModel<@NotNull T>, @NotNull FrameBox<@NotNull T>, @NotNull String> tooltipToTextFunction;
        @NotNull
        private Dimension flamegraphDimension = new Dimension();

        /**
         * Bounds used to compute painting and interactions with the minimap.
         * Note about {@code y} coordinate: it represents the vertical axes from the bottom of the canvas.
         */
        private final Rectangle minimapBounds = new Rectangle(50, 50, 200, 100);
        private final int minimapInset = 10;
        @Nullable
        private Supplier<@NotNull Color> minimapShadeColorSupplier = null;
        private boolean showMinimap = true;
        @Nullable
        private Supplier<@NotNull JToolTip> tooltipComponentSupplier;
        @Nullable
        private ZoomAction zoomActionOverride;
        @Nullable
        private BiConsumer<@NotNull FrameBox<@NotNull T>, @NotNull MouseEvent> popupConsumer;
        @Nullable
        private BiConsumer<@NotNull FrameBox<@NotNull T>, @NotNull MouseEvent> selectedFrameConsumer;
        @NotNull
        private final FlamegraphView<@NotNull T> flamegraphView;
        private final ZoomModel<T> zoomModel = new ZoomModel<>();

        private long lastDrawTime;

        public FlamegraphCanvas(@NotNull FlamegraphView<@NotNull T> flamegraphView) {
            this.flamegraphView = flamegraphView;
        }

        /**
         * Override this method to listen to LaF changes.
         */
        @SuppressWarnings("EmptyMethod")
        @Override
        public void updateUI() {
            super.updateUI();
        }

        @Override
        public void addNotify() {
            super.addNotify();
            var fgCanvas = this;

            // Adjusts the width of the canvas to the width of the view-rect when
            // the scrollbar is made visible, this prevents the horizontal scrollbar
            // from appearing on first display, see #96.
            // Since a scrollbar is made visible once, this listener is called only once,
            // which is the intended behavior (otherwise it affects zooming).
            var parent = SwingUtilities.getUnwrappedParent(fgCanvas);
            if (parent instanceof JViewport) {
                var viewport = (JViewport) parent;
                var scrollPane = (JScrollPane) viewport.getParent();
                var vsb = scrollPane.getVerticalScrollBar();
                vsb.addComponentListener(new ComponentAdapter() {
                    private int frameModelHashCode = 0;

                    @Override
                    public void componentShown(ComponentEvent e) {
                        // On the first display, the flamegraph has the same width as the enclosing container,
                        // but if flamegraph is zoomed-in the canvas width will be different.
                        // * So don't run this listener to prevent the canvas from being wrongly resized
                        //   if the model didn't change.
                        // * The guard uses the hash code of the model because the model can be changed,
                        //   and running this listener is necessary to prevent the horizontal scrollbar as well.
                        if (fgCanvas.flamegraphRenderEngine != null
                            && !fgCanvas.flamegraphRenderEngine.getFrameModel().frames.isEmpty()) {
                            int newHashCode = fgCanvas.flamegraphRenderEngine.getFrameModel().hashCode();
                            if (newHashCode == frameModelHashCode) {
                                return;
                            }
                            frameModelHashCode = newHashCode;
                        }

                        SwingUtilities.invokeLater(() -> {
                            var canvasWidth = fgCanvas.getWidth();
                            if (canvasWidth == 0) {
                                return;
                            }

                            // Adjust the width of the canvas to the width of the viewport rect
                            // to prevent the horizontal scrollbar from appearing on the first display.
                            fgCanvas.setSize(viewport.getViewRect().width, getHeight());
                        });
                    }
                });

                installMinimapTriggers(fgCanvas, vsb);
            }
        }

        private void installMinimapTriggers(FlamegraphCanvas<T> fgCanvas, JScrollBar vsb) {
            fgCanvas.addPropertyChangeListener(GRAPH_MODE, evt -> SwingUtilities.invokeLater(() -> {
                var value = vsb.getValue();
                var bounds = fgCanvas.getBounds();
                var visibleRect = fgCanvas.getVisibleRect();

                // This computes the new view location based on the current view location
                switch ((Mode) evt.getNewValue()) {
                    case ICICLEGRAPH:
                        vsb.setValue(
                                value == vsb.getMaximum() ?
                                vsb.getMinimum() :
                                bounds.height - Math.abs(bounds.y) - visibleRect.height
                        );
                        break;
                    case FLAMEGRAPH:
                        vsb.setValue(
                                value == vsb.getMinimum() ?
                                vsb.getMaximum() :
                                bounds.height - visibleRect.height - value
                        );
                        break;
                }
                fgCanvas.triggerMinimapGeneration();
            }));

            fgCanvas.addPropertyChangeListener("preferredSize", evt -> {
                SwingUtilities.invokeLater(() -> {
                    // trigger minimap generation, when the flamegraph is zoomed, more
                    // frames become visible, and this may make the visible depth higher,
                    // this allows updating the minimap when more details are available.
                    if (isVisible() && showMinimap) {
                        fgCanvas.triggerMinimapGeneration();
                    }
                });
            });
        }

        @Override
        public Dimension getPreferredSize() {
            var oldFlamegraphDimension = this.flamegraphDimension;
            var preferredSize = new Dimension(10, 10);
            var flamegraphWidth = getWidth();
            // This method can be called before a Graphics2D is available, or before it has an initial size.
            if (flamegraphRenderEngine == null || flamegraphWidth == 0 || getGraphics() == null) {
                // super.setPreferredSize(preferredSize);
                this.flamegraphDimension = preferredSize;
                firePropertyChange("preferredSize", oldFlamegraphDimension, preferredSize);
                return preferredSize;
            }

            var flamegraphHeight = flamegraphRenderEngine.computeVisibleFlamegraphHeight(
                    (Graphics2D) getGraphics(),
                    flamegraphWidth,
                    true
            );
            preferredSize.width = Math.max(preferredSize.width, flamegraphWidth);
            preferredSize.height = Math.max(preferredSize.height, flamegraphHeight);

            if (!flamegraphDimension.equals(preferredSize)) {
                this.flamegraphDimension = preferredSize;
                firePropertyChange("preferredSize", oldFlamegraphDimension, preferredSize);
            }
            return preferredSize;
        }

        @SuppressWarnings("UnusedReturnValue")
        @NotNull
        protected Dimension updateFlamegraphDimension(@NotNull Dimension dimension, int flamegraphWidth) {
            var flamegraphHeight = flamegraphRenderEngine.computeVisibleFlamegraphHeight(
                    (Graphics2D) getGraphics(),
                    flamegraphWidth,
                    true
            );

            dimension.width = flamegraphWidth;
            dimension.height = flamegraphHeight;
            return dimension;
        }

        @Override
        protected void paintComponent(@NotNull Graphics g) {
            long start = System.currentTimeMillis();

            super.paintComponent(g);
            var g2 = (Graphics2D) g.create();
            var visibleRect = getVisibleRect();
            if (flamegraphRenderEngine == null) {
                String message = "No data to display";
                var font = g2.getFont();
                // calculate center position
                var bounds = g2.getFontMetrics(font).getStringBounds(message, g2);
                int xx = visibleRect.x + (int) ((visibleRect.width - bounds.getWidth()) / 2.0);
                int yy = visibleRect.y + (int) ((visibleRect.height + bounds.getHeight()) / 2.0);
                g2.drawString(message, xx, yy);
                g2.dispose();
                return;
            }

            flamegraphRenderEngine.paint(g2, getBounds(), visibleRect);
            paintMinimap(g2, visibleRect);

            lastDrawTime = System.currentTimeMillis() - start;
            paintDetails(g2);
            g2.dispose();
        }

        private void paintDetails(@NotNull Graphics2D g2) {
            if (getClientProperty(SHOW_STATS) == TRUE) {
                var viewRect = getVisibleRect();
                var bounds = getBounds();
                var zoomFactor = bounds.getWidth() / viewRect.getWidth();
                var stats =
                        "Canvas (" + bounds.getWidth() + ", " + bounds.getHeight() + ") " +
                        "Zoom Factor " + zoomFactor + " " +
                        "Coordinate (" + viewRect.getX() + ", " + viewRect.getY() + ") " +
                        "View (" + viewRect.getWidth() + ", " + viewRect.getHeight() + "), " +
                        "Visible " + flamegraphRenderEngine.getVisibleDepth() + " " +
                        "Draw time: " + lastDrawTime + " ms";
                var frameTextPadding = 3;

                var w = viewRect.getWidth();
                var h = 16;
                var x = viewRect.getX();
                var y = getMode() == Mode.ICICLEGRAPH ? viewRect.getY() + viewRect.getHeight() - h : viewRect.getY();


                g2.setColor(new Color(0xa4404040, true));
                g2.fillRect((int) x, (int) y, (int) w, h);
                g2.setColor(Color.YELLOW);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.drawString(
                        stats,
                        (int) (x + frameTextPadding),
                        (int) (y + h - frameTextPadding)
                );
            }
        }

        private void paintMinimap(@NotNull Graphics g, @NotNull Rectangle visibleRect) {
            if (showMinimap && minimap != null) {
                var g2 = (Graphics2D) g.create(
                        visibleRect.x + minimapBounds.x,
                        visibleRect.y + visibleRect.height - minimapBounds.height - minimapBounds.y,
                        minimapBounds.width + minimapInset * 2,
                        minimapBounds.height + minimapInset * 2
                );

                g2.setColor(getBackground());
                int minimapRadius = 10;
                g2.fillRoundRect(
                        1,
                        1,
                        minimapBounds.width + 2 * minimapInset - 1,
                        minimapBounds.height + 2 * minimapInset - 1,
                        minimapRadius,
                        minimapRadius
                );
                g2.drawImage(minimap, minimapInset, minimapInset, null);

                // the image is already rendered, so the hints are only for the shapes below
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setColor(getForeground());
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(
                        1,
                        1,
                        minimapBounds.width + 2 * minimapInset - 2,
                        minimapBounds.height + 2 * minimapInset - 2,
                        minimapRadius,
                        minimapRadius
                );

                {
                    // Zoom zone
                    double zoomZoneScaleX = ((double) minimapBounds.width) / ((double) flamegraphDimension.width);
                    double zoomZoneScaleY = ((double) minimapBounds.height) / ((double) flamegraphDimension.height);

                    int x = (int) (visibleRect.x * zoomZoneScaleX);
                    int y = (int) (visibleRect.y * zoomZoneScaleY);
                    int w = Math.min((int) (visibleRect.width * zoomZoneScaleX), minimapBounds.width);
                    int h = Math.min((int) (visibleRect.height * zoomZoneScaleY), minimapBounds.height);

                    var zoomZone = new Area(new Rectangle(minimapInset, minimapInset, minimapBounds.width, minimapBounds.height));
                    zoomZone.subtract(new Area(new Rectangle(x + minimapInset, y + minimapInset, w, h)));


                    var color = minimapShadeColorSupplier == null ?
                                new Color(getBackground().getRGB() & 0x90_FFFFFF, true) :
                                minimapShadeColorSupplier.get();
                    g2.setColor(color);
                    g2.fill(zoomZone);

                    g2.setColor(getForeground());
                    g2.setStroke(new BasicStroke(1));
                    g2.drawRect(x + minimapInset, y + minimapInset, w, h);
                }
                g2.dispose();
            }
        }

        @Override
        @Nullable
        public String getToolTipText(@NotNull MouseEvent e) {
            if (isInsideMinimap(e.getPoint())) {
                return "";
            }

            return super.getToolTipText(e);
        }

        public boolean isInsideMinimap(@NotNull Point point) {
            if (!showMinimap) {
                return false;
            }
            var visibleRect = getVisibleRect();
            var rectangle = new Rectangle(
                    visibleRect.x + minimapBounds.y,
                    visibleRect.y + visibleRect.height - minimapBounds.height - minimapBounds.y,
                    minimapBounds.width + 2 * minimapInset,
                    minimapBounds.height + 2 * minimapInset
            );

            return rectangle.contains(point);
        }

        public void setToolTipText(FrameBox<T> frame) {
            if (tooltipToTextFunction == null) {
                return;
            }
            setToolTipText(tooltipToTextFunction.apply(flamegraphView.framesModel, frame));
        }

        @Override
        @NotNull
        public JToolTip createToolTip() {
            if (tooltipComponentSupplier == null) {
                return super.createToolTip();
            }
            if (toolTip == null) {
                toolTip = tooltipComponentSupplier.get();
                toolTip.setComponent(this);
            }

            return toolTip;
        }


        private void triggerMinimapGeneration() {
            if (!showMinimap || flamegraphRenderEngine == null) {
                repaintMinimapArea();
                return;
            }

            CompletableFuture.runAsync(() -> {
                var height = flamegraphRenderEngine.computeVisibleFlamegraphMinimapHeight(minimapBounds.width);
                // Don't generate minimap if there's no data, e.g. 1
                // moreover there a random problematic interaction with the layout
                // if the minimap is generated too early.
                if (height <= 1) {
                    return;
                }

                var e = GraphicsEnvironment.getLocalGraphicsEnvironment();
                var c = e.getDefaultScreenDevice().getDefaultConfiguration();
                var minimapImage = c.createCompatibleImage(minimapBounds.width, height, Transparency.TRANSLUCENT);
                var minimapGraphics = minimapImage.createGraphics();
                minimapGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                minimapGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                minimapGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                var bounds = new Rectangle(minimapBounds.width, height);
                flamegraphRenderEngine.paintMinimap(minimapGraphics, bounds);
                minimapGraphics.dispose();

                SwingUtilities.invokeLater(() -> this.setMinimapImage(minimapImage));
            }).handle((__, t) -> {
                if (t != null) {
                    System.getLogger(FlamegraphCanvas.class.getName())
                          .log(Level.ERROR, "Error generating minimap, no thumbnail", t);
                }
                return null;
            });
        }

        private void setMinimapImage(@NotNull BufferedImage minimapImage) {
            this.minimap = minimapImage.getScaledInstance(minimapBounds.width, minimapBounds.height, Image.SCALE_SMOOTH);
            repaintMinimapArea();
        }

        private void repaintMinimapArea() {
            var visibleRect = getVisibleRect();
            repaint(visibleRect.x + minimapBounds.x,
                    visibleRect.y + visibleRect.height - minimapBounds.height - minimapBounds.y,
                    minimapBounds.width + minimapInset * 2,
                    minimapBounds.height + minimapInset * 2);
        }

        public void setupListeners(@NotNull JScrollPane scrollPane) {
            var mouseAdapter = new MouseInputAdapter() {
                private Point pressedPoint;

                @Override
                public void mouseClicked(@NotNull MouseEvent e) {
                    if (e.getClickCount() != 1 || e.getButton() != MouseEvent.BUTTON1) {
                        return;
                    }
                    if (selectedFrameConsumer == null) {
                        return;
                    }
                    FlamegraphCanvas<T> canvas = FlamegraphCanvas.this;
                    flamegraphRenderEngine.getFrameAt((Graphics2D) canvas.getGraphics(), canvas.getBounds(), e.getPoint())
                                          .ifPresent(frame -> selectedFrameConsumer.accept(frame, e));
                }

                @Override
                public void mousePressed(@NotNull MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        if (isInsideMinimap(e.getPoint())) {
                            processMinimapMouseEvent(e);
                            pressedPoint = e.getPoint();
                        } else {
                            // don't trigger minimap behavior if the pressed point is outside the minimap
                            pressedPoint = null;
                        }
                        return;
                    }
                    handlePopup(e);
                }

                @Override
                public void mouseReleased(@NotNull MouseEvent e) {
                    handlePopup(e);
                }

                private void handlePopup(@NotNull MouseEvent e) {
                    if (!e.isPopupTrigger()) {
                        return;
                    }
                    if (popupConsumer == null) {
                        return;
                    }
                    var canvas = FlamegraphCanvas.this;
                    flamegraphRenderEngine.getFrameAt((Graphics2D) canvas.getGraphics(), canvas.getBounds(), e.getPoint())
                                          .ifPresent(frame -> popupConsumer.accept(frame, e));
                }

                @Override
                public void mouseDragged(@NotNull MouseEvent e) {
                    if (isInsideMinimap(e.getPoint()) && pressedPoint != null) {
                        processMinimapMouseEvent(e);
                    }
                }

                private void processMinimapMouseEvent(@NotNull MouseEvent e) {
                    var pt = e.getPoint();
                    if (!(e.getComponent() instanceof FlamegraphView.FlamegraphCanvas)) {
                        return;
                    }

                    var visibleRect = ((FlamegraphCanvas<?>) e.getComponent()).getVisibleRect();

                    double zoomZoneScaleX = (double) minimapBounds.width / flamegraphDimension.width;
                    double zoomZoneScaleY = (double) minimapBounds.height / flamegraphDimension.height;

                    var h = (pt.x - (visibleRect.x + minimapBounds.x)) / zoomZoneScaleX;
                    var horizontalBarModel = scrollPane.getHorizontalScrollBar().getModel();
                    horizontalBarModel.setValue((int) h - horizontalBarModel.getExtent());


                    var v = (pt.y - (visibleRect.y + visibleRect.height - minimapBounds.height - minimapBounds.y)) / zoomZoneScaleY;
                    var verticalBarModel = scrollPane.getVerticalScrollBar().getModel();
                    verticalBarModel.setValue((int) v - verticalBarModel.getExtent());
                }

                @Override
                public void mouseMoved(@NotNull MouseEvent e) {
                    setCursor(isInsideMinimap(e.getPoint()) ?
                              Cursor.getPredefinedCursor(System.getProperty("os.name").startsWith("Mac") ? Cursor.HAND_CURSOR : Cursor.MOVE_CURSOR) :
                              Cursor.getDefaultCursor());
                }
            };
            this.addMouseListener(mouseAdapter);
            this.addMouseMotionListener(mouseAdapter);

            new UserPositionRecorderMouseAdapter(this).install(scrollPane);
        }

        void setFlamegraphRenderEngine(@NotNull FlamegraphRenderEngine<@NotNull T> flamegraphRenderEngine) {
            this.flamegraphRenderEngine = flamegraphRenderEngine;
        }

        @NotNull
        FlamegraphRenderEngine<@NotNull T> getFlamegraphRenderEngine() {
            return flamegraphRenderEngine;
        }

        public void setToolTipTextFunction(@NotNull BiFunction<@NotNull FrameModel<@NotNull T>, FrameBox<@NotNull T>, @NotNull String> tooltipTextFunction) {
            this.tooltipToTextFunction = tooltipTextFunction;
        }

        public void setTooltipComponentSupplier(@NotNull Supplier<@NotNull JToolTip> tooltipComponentSupplier) {
            this.tooltipComponentSupplier = tooltipComponentSupplier;
        }

        public void setMinimapShadeColorSupplier(@NotNull Supplier<@NotNull Color> minimapShadeColorSupplier) {
            this.minimapShadeColorSupplier = minimapShadeColorSupplier;
        }

        public void showMinimap(boolean showMinimap) {
            if (this.showMinimap == showMinimap) {
                return;
            }
            this.showMinimap = showMinimap;
            firePropertyChange("minimap", !showMinimap, showMinimap);
            triggerMinimapGeneration();
        }

        public boolean isShowMinimap() {
            return showMinimap;
        }

        public void setMode(@NotNull Mode mode) {
            var oldMode = getMode();
            if (oldMode == mode) {
                return;
            }

            getFlamegraphRenderEngine().setIcicle(Mode.ICICLEGRAPH == mode);
            firePropertyChange(GRAPH_MODE, oldMode, mode);
        }

        @NotNull
        public FlamegraphView.Mode getMode() {
            return getFlamegraphRenderEngine().isIcicle() ? Mode.ICICLEGRAPH : Mode.FLAMEGRAPH;
        }

        public void setPopupConsumer(
                @NotNull BiConsumer<@NotNull FrameBox<@NotNull T>, @NotNull MouseEvent> consumer
        ) {
            this.popupConsumer = consumer;
        }

        public void setSelectedFrameConsumer(
                @NotNull BiConsumer<@NotNull FrameBox<@NotNull T>, @NotNull MouseEvent> consumer
        ) {
            this.selectedFrameConsumer = consumer;
        }

        @Nullable
        public ZoomTarget<@NotNull T> getResetZoomTarget() {
            var graphics = (Graphics2D) getGraphics();
            if (graphics == null) {
                return null;
            }

            var visibleRect = getVisibleRect();
            var bounds = getBounds();

            var newHeight = flamegraphRenderEngine.computeVisibleFlamegraphHeight(
                    graphics,
                    visibleRect.width
            );

            return new ZoomTarget<>(
                    0,
                    getMode() == Mode.FLAMEGRAPH ? -(bounds.height - visibleRect.height) : 0,
                    visibleRect.width,
                    newHeight,
                    null
            );
        }

        @Nullable
        public ZoomTarget<@NotNull T> getFrameZoomTarget(@NotNull FrameBox<T> frame) {
            var graphics = (Graphics2D) getGraphics();
            if (graphics == null) {
                return null;
            }

            return flamegraphRenderEngine.calculateZoomTargetFrame(
                    graphics,
                    getBounds(),
                    getVisibleRect(),
                    frame,
                    2,
                    0
            );
        }

        /**
         * Internal zoom operation only on the canvas.
         * Changing this will trigger a layout of the wrapping JViewPort/JScrollPane
         *
         * @param zoomTarget The zoom target.
         */
        @Override
        public void zoom(@NotNull ZoomTarget<@NotNull T> zoomTarget) {
            // Changing the size triggers a revalidation, which triggers a layout
            // Not calling setBounds from the Timeline may provoke EDT violations
            // however calling invokeLater makes the animation out of order, and not smooth.
            setBounds(zoomTarget.getTargetBounds());
        }
    }

    /**
     * The internal mouse listener that is attached to the scrollPane.
     * <p>
     * This listener will be responsible to trigger some behaviors on the canvas itself :
     * * Dragging the canvas
     * * Double-clicking on the canvas, if it's a frame
     * * Hovering frames
     * </p>
     *
     * @param <T>
     * @see FlamegraphView.HoverListener
     */
    private static class FlamegraphHoveringScrollPaneMouseListener<T> implements MouseInputListener, FocusListener {
        private Point pressedPoint;
        private final FlamegraphCanvas<T> canvas;
        private Rectangle hoveredFrameRectangle;
        private HoverListener<T> hoverListener;
        private FrameBox<T> hoveredFrame;
        private final Rectangle tmpBounds = new Rectangle(); // reusable

        public FlamegraphHoveringScrollPaneMouseListener(@NotNull FlamegraphCanvas<@NotNull T> canvas) {
            this.canvas = canvas;
        }

        @Override
        public void mouseDragged(@NotNull MouseEvent e) {
            if ((e.getSource() instanceof JScrollPane) && pressedPoint != null) {
                var scrollPane = (JScrollPane) e.getComponent();
                var viewPort = scrollPane.getViewport();
                if (viewPort == null) {
                    return;
                }

                var dx = e.getX() - pressedPoint.x;
                var dy = e.getY() - pressedPoint.y;
                var viewPortViewPosition = viewPort.getViewPosition();
                viewPort.setViewPosition(new Point(Math.max(0, viewPortViewPosition.x - dx),
                                                   Math.max(0, viewPortViewPosition.y - dy)));
                pressedPoint = e.getPoint();
                e.consume();
            }
        }

        @Override
        public void mousePressed(@NotNull MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                // don't drag canvas if the mouse was interacting within minimap
                if (canvas.isInsideMinimap(SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), canvas))) {
                    pressedPoint = null;
                    return;
                }
                pressedPoint = e.getPoint();
            }
        }

        @Override
        public void mouseReleased(@NotNull MouseEvent e) {
            pressedPoint = null;
        }

        @Override
        public void mouseClicked(@NotNull MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e) && e.getSource() instanceof JScrollPane) {
                return;
            }
            var scrollPane = (JScrollPane) e.getComponent();
            var viewPort = scrollPane.getViewport();

            // this seems to enable key navigation
            scrollPane.requestFocus();

            var latestMouseLocation = MouseInfo.getPointerInfo().getLocation();
            SwingUtilities.convertPointFromScreen(latestMouseLocation, canvas);

            if (canvas.isInsideMinimap(latestMouseLocation)) {
                // bail out
                return;
            }

            if (e.getClickCount() == 2) {
                // find zoom target then do an animated transition
                canvas.getFlamegraphRenderEngine().calculateZoomTargetForFrameAt(
                        (Graphics2D) canvas.getGraphics(),
                        canvas.getBounds(tmpBounds),
                        canvas.getVisibleRect(),
                        latestMouseLocation
                ).ifPresent(zoomTarget -> zoom(canvas, zoomTarget));
                return;
            }

            canvas.getFlamegraphRenderEngine()
                  .toggleSelectedFrameAt(
                          (Graphics2D) viewPort.getView().getGraphics(),
                          canvas.getBounds(tmpBounds),
                          latestMouseLocation,
                          (frame, r) -> canvas.repaint()
                  );
        }


        @Override
        public void mouseEntered(@NotNull MouseEvent e) {
        }

        @Override
        public void mouseExited(@NotNull MouseEvent e) {
            if ((e.getSource() instanceof JScrollPane)) {
                var source = (JScrollPane) e.getSource();
                hoveredFrameRectangle = null;
                hoveredFrame = null;
                canvas.getFlamegraphRenderEngine()
                      .stopHover(
                              (Graphics2D) canvas.getGraphics(),
                              canvas.getBounds(tmpBounds),
                              canvas::repaint
                      );
                canvas.repaint();

                // mouse exit is triggered when the pointer leaves the scroll pane and enters the canvas
                // this part is only interested to pass event when the pointer leaves the scroll pane area
                var latestMouseLocation = MouseInfo.getPointerInfo().getLocation();
                SwingUtilities.convertPointFromScreen(latestMouseLocation, source);
                if (hoverListener != null && !source.getBounds(tmpBounds).contains(latestMouseLocation)) {
                    hoverListener.onStopHover(hoveredFrame, null, e);
                }
            }
        }

        @Override
        public void mouseMoved(@NotNull MouseEvent e) {
            var latestMouseLocation = MouseInfo.getPointerInfo().getLocation();
            SwingUtilities.convertPointFromScreen(latestMouseLocation, canvas);

            if (canvas.isInsideMinimap(latestMouseLocation)) {
                if (hoverListener != null) {
                    hoverListener.onStopHover(hoveredFrame, hoveredFrameRectangle, e);
                }
                // bail out
                return;
            }

            // handle frame hovering
            // =====================
            // still hovering the same frame, avoid unnecessary work
            // and reuse what we got before
            if (hoveredFrameRectangle != null && hoveredFrameRectangle.contains(latestMouseLocation)) {
                if (hoverListener != null) {
                    hoverListener.onFrameHover(hoveredFrame, hoveredFrameRectangle, e);
                }
                return;
            }
            // Find hovered frame, repaint previous hovered frame and current hovered frame
            // configure tooltip, invoke hover listener
            var fgp = canvas.getFlamegraphRenderEngine();
            var canvasGraphics = (Graphics2D) canvas.getGraphics();
            var canvasBounds = canvas.getBounds(tmpBounds);
            fgp.getFrameAt(
                       canvasGraphics,
                       canvasBounds,
                       latestMouseLocation
               )
               .ifPresentOrElse(
                       frame -> {
                           fgp.hoverFrame(
                                   frame,
                                   canvasGraphics,
                                   canvasBounds,
                                   canvas::repaint
                           );
                           canvas.setToolTipText(frame);
                           hoveredFrameRectangle = fgp.getFrameRectangle(
                                   canvasGraphics,
                                   canvasBounds,
                                   frame
                           );
                           hoveredFrame = frame;
                           if (hoverListener != null) {
                               hoverListener.onFrameHover(frame, hoveredFrameRectangle, e);
                           }
                       },
                       () -> {
                           fgp.stopHover(
                                   canvasGraphics,
                                   canvasBounds,
                                   canvas::repaint
                           );
                           var prevHoveredFrameRectangle = hoveredFrameRectangle;
                           var prevHoveredFrame = hoveredFrame;
                           hoveredFrameRectangle = null;
                           hoveredFrame = null;
                           if (hoverListener != null) {
                               hoverListener.onStopHover(prevHoveredFrame, prevHoveredFrameRectangle, e);
                           }
                       }
               );
        }

        public void setHoverListener(HoverListener<T> hoveringListener) {
            this.hoverListener = hoveringListener;
        }

        @Override
        public void focusGained(@NotNull FocusEvent e) {
            // no op
        }

        @Override
        public void focusLost(@NotNull FocusEvent e) {
            // idea is to stop hover when focus is lost
            // if (hoverListener != null) {
            //     System.out.println("stop hover because focus lost");
            //     hoverListener.onStopHover(hoveredFrame, hoveredFrameRectangle,e);
            // }
        }

        public void install(@NotNull JScrollPane sp) {
            sp.addMouseListener(this);
            sp.addMouseMotionListener(this);
            sp.addFocusListener(this);
        }
    }

    private static class UserPositionRecorderMouseAdapter extends MouseAdapter {
        private final FlamegraphCanvas<?> canvas;

        public <T> UserPositionRecorderMouseAdapter(FlamegraphCanvas<T> canvas) {
            this.canvas = canvas;
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            canvas.zoomModel.recordLastPositionFromUserInteraction(canvas);
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            canvas.zoomModel.recordLastPositionFromUserInteraction(canvas);
        }

        public void install(JScrollPane scrollPane) {
            scrollPane.addMouseListener(this); // regular mouse
            scrollPane.addMouseWheelListener(this); // mousewheel and trackpad
        }
    }


    /**
     * The zoom model is responsible to keep track
     * of the current zoom target and the last user interaction.
     *
     * @param <T>
     */
    @Experimental
    private static class ZoomModel<T> {
        /**
         * The current zoom target, it allows to keep track of the current zoom target.
         * So when the view is resized, the zoom ratio can be recomputed from this target.
         */
        @Nullable
        private ZoomTarget<T> currentZoomTarget = null;

        /**
         * Internal field to track the last user interaction
         * of the user that leads to a modification of the position ratio on the interval [0.0, 1.0].
         * This is also referred to as <em>startX</em>.
         * Modified either the last zoom, the last mouse drag, or the last scroll (trackpad included).
         */
        private double lastUserInteractionStartX = 0.0;
        /**
         * Internal field to track the last user interaction
         * of the user that leads to a modification of the end position on the interval [0.0, 1.0].
         * This is also referred to as <em>endX</em>.
         * Modified either the last zoom, the last mouse drag, or the last scroll (trackpad included).
         */
        private double lastUserInteractionEndX = 0.0;
        /**
         * The scale factor resulting from the last user interaction.
         */
        private double lastScaleFactor = 1.0;

        private final Rectangle canvasVisibleRect = new Rectangle(); // reused

        public void recordLastPositionFromZoomTarget(
                JPanel canvas,
                @Nullable ZoomTarget<T> currentZoomTarget
        ) {
            this.currentZoomTarget = currentZoomTarget;

            if (currentZoomTarget == null || currentZoomTarget.targetFrame == null) {
                lastUserInteractionStartX = 0.0;
                lastUserInteractionEndX = 1.0;
                lastScaleFactor = 1.0;
            } else {
                lastUserInteractionStartX = currentZoomTarget.targetFrame.startX;
                lastUserInteractionEndX = currentZoomTarget.targetFrame.endX;

                canvas.computeVisibleRect(canvasVisibleRect);
                this.lastScaleFactor = FlamegraphRenderEngine.getScaleFactor(
                        canvasVisibleRect.width,
                        currentZoomTarget.getWidth(),
                        1.0
                );
            }

        }

        public void recordLastPositionFromUserInteraction(JPanel canvas) {
            int width = canvas.getWidth();
            canvas.computeVisibleRect(canvasVisibleRect);
            this.lastUserInteractionStartX = (double) canvasVisibleRect.x / (double) width;
            this.lastUserInteractionEndX = ((double) canvasVisibleRect.x + canvasVisibleRect.width) / (double) width;
            this.lastScaleFactor = FlamegraphRenderEngine.getScaleFactor(
                    canvasVisibleRect.width,
                    width,
                    1.0
            );
        }

        public @Nullable ZoomTarget<T> getCurrentZoomTarget() {
            return currentZoomTarget;
        }

        private double getLastUserInteractionStartX() {
            return lastUserInteractionStartX;
        }

        private double getLastUserInteractionEndX() {
            return lastUserInteractionEndX;
        }

        private double getLastUserInteractionWidthX() {
            return lastUserInteractionEndX - lastUserInteractionStartX;
        }

        public double getLastScaleFactor() {
            return lastScaleFactor;
        }
    }
}
