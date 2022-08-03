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

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.Boolean.TRUE;

/**
 * Class that allows to display a flame graph.
 * <p>
 * In general the Flamegraph's raw data is an actual tree. However walking
 * this tree require substantial effort to process during painting.
 * For this reason the actual tree must be pre-processed as a list of
 * {@link FrameBox}.
 * </p>
 * <p>
 * It can be used is as follows:
 * <pre><code>
 * FlamegraphView&lt;MyNode&gt; flamegraphView = new FlamegraphView&lt;&gt;();
 * flamegraphView.setShowMinimap(false);
 * flamegraphView.setRenderConfiguration(
 *     frameTextProvider,                              // string representation candidates
 *     frameColorProvider,                             // color the frame
 *     frameFontProvider,                              // returns a given font for a frame
 * );
 * flamegraphView.setTooltipTextFunction(
 *     frameToToolTipTextFunction                      // text tooltip function
 * );
 *
 * panel.add(flamegraphView.component);
 *
 * // then later
 * flamegraphView.setModel(
 *      new FrameModel(
 *          "title",                                   // title of the flamegraph, used in root node
 *          frameEqualityFunction,                     // equality function for frames, used for sibling detection
 *          (FrameBox&lt;MyNode&gt;) listOfFrameBox()  // list of frames
 *      )
 * )
 * </code></pre>
 * <p>
 * The created and <em>final</em> {@code component} is a composite that is based
 * on a {@link JScrollPane}.
 * </p>
 *
 * @param <T> The type of the node data.
 * @see FlamegraphRenderEngine
 */
public class FlamegraphView<T> {
    /**
     * Internal key to get the Flamegraph from the component.
     */
    private static final String OWNER_KEY = "flamegraphOwner";
    /**
     * The key for a client property that controls the display of rendering statistics.
     */
    public static String SHOW_STATS = "flamegraph.show_stats";

    private final FlamegraphCanvas<T> canvas;

    /**
     * The final composite component that can display a flame graph.
     */
    public final JComponent component;

    /**
     * Mouse input listener used to move the canvas over the JScrollPane
     * as well as trigger other behavior on the canvas.
     */
    private final FlamegraphScrollPaneMouseInputListener<T> scrollPaneListener;

    /**
     * The precomputed list of frames.
     */
    private FrameModel<T> framesModel = FrameModel.empty();

    public RenderedImage saveGraphToImage(int width, Mode mode) {
        return canvas.getFlamegraphRenderEngine()
                     .map(fre -> {
                         var height = fre.computeVisibleFlamegraphHeight(
                                 new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics(),
                                 width,
                                 width
                         );
                         BufferedImage imageCanvas = new BufferedImage(
                                 width,
                                 height,
                                 BufferedImage.TYPE_INT_RGB
                         );
                         Graphics2D g2 = imageCanvas.createGraphics();

                         
                         var size = new Rectangle(0, 0, imageCanvas.getWidth(), imageCanvas.getHeight());

                         fre.paintToImage(g2, size, mode == Mode.ICICLEGRAPH);

                         g2.dispose();

                         return imageCanvas;
                     })
                     .orElse(null);
    }

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
        boolean zoom(ZoomableComponent zoomableComponent, ZoomTarget zoomTarget);
    }

    /**
     * Represents a zoomable JComponent.
     */
    public interface ZoomableComponent {
        /**
         * Actually perform the zooming operation on the component.
         *
         * <p>
         * This likely involves revalidation and repainting of the component.
         * </p>
         *
         * @param zoomTarget The zoom target.
         */
        void zoom(ZoomTarget zoomTarget);

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
        Point getLocation();
    }

    /**
     * Represents a custom actions when zooming
     *
     * @param <T> The type of the node data.
     */
    public interface HoverListener<T> {
        default void onStopHover(FrameBox<T> previousHoveredFrame, Rectangle prevHoveredFrameRectangle, MouseEvent e) {}

        void onFrameHover(FrameBox<T> frame, Rectangle hoveredFrameRectangle, MouseEvent e);

        /**
         * Utility method to get a point from a mouse event with the vertical coordinate set to
         * the given frame rectangle.
         *
         * @param frameRect  The frame rectangle.
         * @param mouseEvent The mouse event, event is expected to come from
         *                   {@link HoverListener} methods.
         * @return The point
         */
        static Point getPointLeveledToFrameDepth(MouseEvent mouseEvent, Rectangle frameRect) {
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
     * Represents a custom actions when zooming
     *
     * @param <T> The type of the node data.
     */
    @Deprecated(forRemoval = true)
    public interface HoveringListener<T> {
        default void onStopHover(MouseEvent e) {}

        void onFrameHover(FrameBox<T> frame, Rectangle hoveredFrameRectangle, MouseEvent e);
    }

    /**
     * Return the {@code Flamegraph} that created the passed component.
     * <p>
     * If this wasn't returned by a {@code Flamegraph} then return empty.
     *
     * @param component the JComponent
     * @param <T>       The type of the node data.
     * @return The {@code Flamegraph} instance that crated this JComponent or empty.
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<FlamegraphView<T>> from(JComponent component) {
        return Optional.ofNullable((FlamegraphView<T>) component.getClientProperty(OWNER_KEY));
    }

    /**
     * Creates an empty flame graph.
     * In order to use in Swing just access the {@link #component} field.
     */
    public FlamegraphView() {
        canvas = new FlamegraphCanvas<>(this);
        canvas.putClientProperty(OWNER_KEY, this);
        scrollPaneListener = new FlamegraphScrollPaneMouseInputListener<>(canvas);
        var scrollPane = new JScrollPane(canvas);
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
                    canvas.linkListenerTo(scrollPane);

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

    /**
     * Murky workaround to propagate the background color to the canvas
     * since JLayer is final.
     */
    private JPanel wrap(JComponent owner, final Consumer<Color> configureBorderAndBackground) {
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
     * Experimental configuration hook for the underlying hook.
     */
    public void configureCanvas(Consumer<JComponent> canvasConfigurer) {
        Objects.requireNonNull(canvasConfigurer).accept(canvas);
    }

    /**
     * Replaces the frame to color function.
     *
     * @param frameColorFunction A function that takes a frame and returns a color.
     */
    @Deprecated(forRemoval = true)
    public void setColorFunction(Function<FrameBox<T>, Color> frameColorFunction) {
        setFrameColorProvider(FrameColorProvider.defaultColorProvider(frameColorFunction));
    }

    /**
     * Replaces the frame colors provider.
     *
     * @param frameColorProvider A provider that takes a frame and returns its colors.
     * @see FrameColorProvider
     */
    public void setFrameColorProvider(FrameColorProvider<T> frameColorProvider) {
        this.canvas.getFlamegraphRenderEngine()
                   .ifPresent(fgp -> fgp.getFrameRenderer()
                                        .setFrameColorProvider(frameColorProvider));
    }

    /**
     * Replaces the frame font provider.
     *
     * @param frameFontProvider A provider that takes a frame and returns its font.
     * @see FrameFontProvider
     */
    public void setFrameFontProvider(FrameFontProvider<T> frameFontProvider) {
        this.canvas.getFlamegraphRenderEngine()
                   .ifPresent(fgp -> fgp.getFrameRenderer()
                                        .setFrameFontProvider(frameFontProvider));
    }

    /**
     * Replaces the frame to texts candidate provider.
     *
     * @param frameTextsProvider A provider that takes a frame and returns its colors.
     * @see FrameTextsProvider
     */
    public void setFrameTextsProvider(FrameTextsProvider<T> frameTextsProvider) {
        this.canvas.getFlamegraphRenderEngine()
                   .ifPresent(fgp -> fgp.getFrameRenderer()
                                        .setFrameTextsProvider(frameTextsProvider));
    }

    /**
     * Toggle the display of a gap between frames.
     *
     * @param frameGapEnabled {@code true} to show a gap between frames, {@code false} otherwise.
     */
    public void setFrameGapEnabled(boolean frameGapEnabled) {
        canvas.getFlamegraphRenderEngine()
              .ifPresent(fgp -> fgp.getFrameRenderer().setDrawingFrameGap(frameGapEnabled));
    }

    /**
     * Replaces the default color shade for the minimap.
     * Alpha color are supported.
     *
     * @param minimapShadeColorSupplier Color supplier.
     */
    public void setMinimapShadeColorSupplier(Supplier<Color> minimapShadeColorSupplier) {
        canvas.setMinimapShadeColorSupplier(Objects.requireNonNull(minimapShadeColorSupplier));
    }

    /**
     * Sets a flag that controls whether the minimap is visible.
     *
     * @param showMinimap {@code true} to show the minimap, {@code false} otherwise.
     */
    @Deprecated(forRemoval = true)
    public void showMinimap(boolean showMinimap) {
        setShowMinimap(showMinimap);
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
        canvas.getFlamegraphRenderEngine()
              .ifPresent(fre -> fre.setShowHoveredSiblings(showHoveredSiblings));
    }

    /**
     * Whether the siblings of the hovered frame are highlighted.
     *
     * @return {@code true} if the siblings of the hovered frame are highlighted, {@code false} otherwise.
     */
    public boolean isShowHoveredSiblings() {
        return canvas.getFlamegraphRenderEngine().map(FlamegraphRenderEngine::isShowHoveredSiblings).orElse(false);
    }

    /**
     * Sets the display mode, either {@link Mode#FLAMEGRAPH} or {@link Mode#ICICLEGRAPH}.
     *
     * @param mode The display mode.
     */
    public void setMode(FlamegraphView.Mode mode) {
        canvas.setMode(mode);
    }

    /**
     * Returns the current display mode.
     *
     * @return the current display mode.
     */
    public FlamegraphView.Mode getMode() {
        return canvas.getMode();
    }

    /**
     * Replaces the default tooltip component.
     *
     * @param tooltipComponentSupplier The tooltip component supplier.
     */
    public void setTooltipComponentSupplier(Supplier<JToolTip> tooltipComponentSupplier) {
        canvas.setTooltipComponentSupplier(Objects.requireNonNull(tooltipComponentSupplier));
    }

    /**
     * Sets a callback that provides a reference to a frame when the user performs a
     * "popup" action on the frame graph (typically a right-click with the mouse).
     *
     * @param consumer the consumer ({@code null} permitted).
     */
    public void setPopupConsumer(BiConsumer<FrameBox<T>, MouseEvent> consumer) {
        canvas.setPopupConsumer(Objects.requireNonNull(consumer));
    }

    /**
     * Sets a callback that provides a reference to a frame when the user performs a
     * "popup" action on the frame graph (typically a right-click with the mouse).
     *
     * @param consumer the consumer ({@code null} permitted).
     */
    public void setSelectedFrameConsumer(BiConsumer<FrameBox<T>, MouseEvent> consumer) {
        canvas.setSelectedFrameConsumer(Objects.requireNonNull(consumer));
    }

    /**
     * Sets a listener that will be called when the mouse hovers a frame, or when it stops hovering.
     *
     * @param hoverListener the listener ({@code null} permitted).
     */
    @Deprecated(forRemoval = true)
    public void setHoveringListener(HoveringListener<T> hoverListener) {
        setHoverListener(new HoverListener<>() {
            @Override
            public void onStopHover(FrameBox<T> prevHoveredFrame, Rectangle prevHoveredFrameRectangle, MouseEvent e) {
                hoverListener.onStopHover(e);
            }

            @Override
            public void onFrameHover(FrameBox<T> frame, Rectangle hoveredFrameRectangle, MouseEvent e) {
                hoverListener.onFrameHover(frame, hoveredFrameRectangle, e);
            }
        });
    }

    /**
     * Sets a listener that will be called when the mouse hovers a frame, or when it stops hovering.
     *
     * @param hoverListener the listener ({@code null} permitted).
     */
    public void setHoverListener(HoverListener<T> hoverListener) {
        scrollPaneListener.setHoverListener(hoverListener);
    }

    /**
     * Actually set the {@link FlamegraphView} with typed data and configure how to use it.
     * <p>
     * It takes a list of {@link FrameBox} objects that wraps the actual data,
     * which is referred to as <em>node</em>.
     * </p>
     * <p>
     * In particular this function defines the behavior to access the typed data:
     * <ul>
     *     <li>Possible string candidates to display in frames, those are
     *     selected based on the available space</li>
     *     <li>The root node text to display, if something specific is relevant,
     *     like the type of events, their number, etc.</li>
     *     <li>The frame background color, this function can be replaced by
     *     {@link #setColorFunction(Function)}, note that the foreground color
     *     is chosen automatically</li>
     *     <li>The tooltip text from the current node</li>
     * </ul>
     *
     * @param frames              The {@code FrameBox} list to display.
     * @param frameTextsProvider  function to display label in frames.
     * @param frameColorFunction  the frame to background color function.
     * @param tooltipTextFunction the frame tooltip text function.
     */
    @Deprecated(forRemoval = true)
    public void setConfigurationAndData(
            List<FrameBox<T>> frames,
            FrameTextsProvider<T> frameTextsProvider,
            FrameColorProvider<T> frameColorFunction,
            FrameFontProvider<T> frameFontProvider,
            Function<FrameBox<T>, String> tooltipTextFunction
    ) {
        setConfigurationAndData(
                new FrameModel<>(frames),
                frameTextsProvider,
                frameColorFunction,
                frameFontProvider,
                tooltipTextFunction
        );
    }

    /**
     * Actually set the {@link FlamegraphView} with typed data and configure how to use it.
     *
     * <p>
     * It takes a {@link FrameModel} object that wraps the actual data.
     * </p>
     * <p>
     * In particular this function defines the behavior to access the typed data:
     * <ul>
     *     <li>Possible string candidates to display in frames, those are
     *     selected based on the available space</li>
     *     <li>The root node text to display, if something specific is relevant,
     *     like the type of events, their number, etc.</li>
     *     <li>The frame background color, this function can be replaced by
     *     {@link #setColorFunction(Function)}, note that the foreground color
     *     is chosen automatically</li>
     *     <li>The tooltip text from the current node</li>
     * </ul>
     *
     * @param frameModel          The {@code FrameBox} list to display.
     * @param frameTextsProvider  The function to display label in frames.
     * @param frameColorFunction  The frame to background color function.
     * @param tooltipTextFunction The frame tooltip text function.
     */
    @Deprecated(forRemoval = true)
    public void setConfigurationAndData(
            FrameModel<T> frameModel,
            FrameTextsProvider<T> frameTextsProvider,
            FrameColorProvider<T> frameColorFunction,
            FrameFontProvider<T> frameFontProvider,
            Function<FrameBox<T>, String> tooltipTextFunction
    ) {
        framesModel = Objects.requireNonNull(frameModel);
        var flamegraphRenderEngine = new FlamegraphRenderEngine<>(
                new FrameRenderer<>(
                        frameTextsProvider,
                        frameColorFunction,
                        frameFontProvider
                )
        ).init(frameModel);

        canvas.setFlamegraphRenderEngine(Objects.requireNonNull(flamegraphRenderEngine));
        Objects.requireNonNull(tooltipTextFunction);
        canvas.setToolTipTextFunction((__, frame) -> tooltipTextFunction.apply(frame));

        canvas.revalidate();
        canvas.repaint();
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
    public void setModel(FrameModel<T> frameModel) {
        framesModel = Objects.requireNonNull(frameModel);
        canvas.getFlamegraphRenderEngine().ifPresent(fre -> fre.init(frameModel));

        canvas.revalidate();
        canvas.repaint();
    }

    /**
     * Configures the rendering of {@link FlamegraphView}.
     *
     * <p>
     * When this method is invoked after a model has been set this request
     * a new repaint.
     * </p>
     *
     * <p>
     * In particular this function defines the behavior to access the typed data:
     * <ul>
     *     <li>Possible string candidates to display in frames, those are
     *     selected based on the available space</li>
     *     <li>The root node text to display, if something specific is relevant,
     *     like the type of events, their number, etc.</li>
     *     <li>The frame background color, this function can be replaced by
     *     {@link #setColorFunction(Function)}, note that the foreground color
     *     is chosen automatically</li>
     * </ul>
     *
     * @param frameTextsProvider The function to display label in frames.
     * @param frameColorFunction The frame to background color function.
     * @param frameFontProvider  The frame font provider.
     */
    public void setRenderConfiguration(
            FrameTextsProvider<T> frameTextsProvider,
            FrameColorProvider<T> frameColorFunction,
            FrameFontProvider<T> frameFontProvider
    ) {
        var flamegraphRenderEngine = new FlamegraphRenderEngine<>(
                new FrameRenderer<>(
                        frameTextsProvider,
                        frameColorFunction,
                        frameFontProvider
                )
        ).init(framesModel);

        canvas.setFlamegraphRenderEngine(Objects.requireNonNull(flamegraphRenderEngine));

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
    public void setTooltipTextFunction(BiFunction<FrameModel<T>, FrameBox<T>, String> tooltipTextFunction) {
        canvas.setToolTipTextFunction(Objects.requireNonNull(tooltipTextFunction));
    }

    /**
     * Clear all data.
     */
    public void clear() {
        framesModel = FrameModel.empty();

        canvas.getFlamegraphRenderEngine()
              .ifPresent(FlamegraphRenderEngine::reset);
        canvas.invalidate();
        canvas.repaint();
    }

    public FrameModel<T> getFrameModel() {
        return framesModel;
    }

    public List<FrameBox<T>> getFrames() {
        return framesModel.frames;
    }

    /**
     * Adds an arbitrary key/value "client property".
     *
     * @param key   the key, use {@code null} to remove.
     * @param value the value.
     * @see JComponent#putClientProperty(Object, Object)
     */
    public void putClientProperty(String key, Object value) {
        Objects.requireNonNull(key);
        // value can be null, it means removing the key (see putClientProperty)
        canvas.putClientProperty(key, value);
    }

    /**
     * Returns the value of the property with the specified key.
     *
     * @param key the key.
     * @return the value
     * @see JComponent#getClientProperty(Object)
     */
    public Object getClientProperty(String key) {
        Objects.requireNonNull(key);
        return canvas.getClientProperty(key);
    }

    /**
     * Triggers a repaint of the component.
     */
    public void requestRepaint() {
        canvas.repaint();
        canvas.triggerMinimapGeneration();
    }

    public void overrideZoomAction(ZoomAction zoomActionOverride) {
        Objects.requireNonNull(zoomActionOverride);
        this.canvas.zoomActionOverride = zoomActionOverride;
    }

    /**
     * Reset the zoom to 1:1.
     */
    public void resetZoom() {
        zoom(canvas, canvas.resetZoomTarget());
    }

    /**
     * Programmatic zoom to the specified frame.
     *
     * @param frame The frame to zoom to.
     */
    public void zoomTo(FrameBox<T> frame) {
        canvas.getFlamegraphRenderEngine().map(fgp -> fgp.calculateZoomTargetFrame(
                (Graphics2D) canvas.getGraphics(),
                canvas.getBounds(),
                canvas.getVisibleRect(),
                frame,
                2,
                0
        )).ifPresent(zoomTarget -> zoom(canvas, zoomTarget));
    }

    private static <T> void zoom(FlamegraphCanvas<T> canvas, ZoomTarget zoomTarget) {
        // adjust zoom target location for horizontal scrollbar height if canvas bigger than viewRect
        if (canvas.getMode() == Mode.FLAMEGRAPH) {
            var visibleRect = canvas.getVisibleRect();
            JViewport viewPort = (JViewport) SwingUtilities.getUnwrappedParent(canvas);
            var scrollPane = (JScrollPane) viewPort.getParent();

            var hsb = scrollPane.getHorizontalScrollBar();
            if (!hsb.isVisible() && visibleRect.getWidth() < zoomTarget.getWidth()) {
                zoomTarget.y -= hsb.getPreferredSize().height;
            }
        }

        if (canvas.zoomActionOverride == null || !canvas.zoomActionOverride.zoom(canvas, zoomTarget)) {
            canvas.zoom(zoomTarget);
        }
    }

    /**
     * Sets frames that need to be highlighted.
     * <p>
     * The passed collection must be a subset of the frames that were used
     * in {@link #setConfigurationAndData(List, FrameTextsProvider, FrameColorProvider, FrameFontProvider, Function)}.
     * this triggers a repaint event.
     * </p>
     *
     * <p>
     * To remove highlighting pass an empty collection.
     * </p>
     *
     * @param framesToHighlight Subset of frames to highlight.
     * @param searched          Text searched for.
     */
    public void highlightFrames(Set<FrameBox<T>> framesToHighlight, String searched) {
        Objects.requireNonNull(framesToHighlight);
        Objects.requireNonNull(searched);
        canvas.getFlamegraphRenderEngine()
              .ifPresent(painter -> painter.setHighlightFrames(framesToHighlight, searched));
        canvas.repaint();
    }

    /**
     * The internal mouse listener that is attached to the scrollPane.
     * <p>
     * This listener will be responsible to trigger some behaviors on the canvas itself.
     * </p>
     *
     * @param <T>
     */
    private static class FlamegraphScrollPaneMouseInputListener<T> implements MouseInputListener {
        private Point pressedPoint;
        private final FlamegraphCanvas<T> canvas;
        private Rectangle hoveredFrameRectangle;
        private HoverListener<T> hoverListener;
        private FrameBox<T> hoveredFrame;

        public FlamegraphScrollPaneMouseInputListener(FlamegraphCanvas<T> canvas) {
            this.canvas = canvas;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
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
        public void mousePressed(MouseEvent e) {
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
        public void mouseReleased(MouseEvent e) {
            pressedPoint = null;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
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
                canvas.getFlamegraphRenderEngine().flatMap(fgp -> fgp.calculateZoomTargetForFrameAt(
                        (Graphics2D) canvas.getGraphics(),
                        canvas.getBounds(),
                        canvas.getVisibleRect(),
                        latestMouseLocation
                )).ifPresent(zoomTarget -> zoom(canvas, zoomTarget));
                return;
            }

            canvas.getFlamegraphRenderEngine()
                  .ifPresent(fgp -> fgp.toggleSelectedFrameAt(
                          (Graphics2D) viewPort.getView().getGraphics(),
                          canvas.getBounds(),
                          latestMouseLocation,
                          (frame, r) -> canvas.repaint()
                  ));
        }


        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if ((e.getSource() instanceof JScrollPane)) {
                hoveredFrameRectangle = null;
                hoveredFrame = null;
                canvas.getFlamegraphRenderEngine()
                      .ifPresent(fgp -> fgp.stopHover(
                              (Graphics2D) canvas.getGraphics(),
                              canvas.getBounds(),
                              canvas::repaint
                      ));
                canvas.repaint();
                if (hoverListener != null) {
                    hoverListener.onStopHover(hoveredFrame, hoveredFrameRectangle, e);
                }
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            var latestMouseLocation = MouseInfo.getPointerInfo().getLocation();
            SwingUtilities.convertPointFromScreen(latestMouseLocation, canvas);

            if (canvas.isInsideMinimap(latestMouseLocation)) {
                if (hoverListener != null) {
                    hoverListener.onStopHover(hoveredFrame, hoveredFrameRectangle, e);
                }
                // bail out
                return;
            }

            // handle hovering
            if (hoveredFrameRectangle != null && hoveredFrameRectangle.contains(latestMouseLocation)) {
                // still hovering the same frame, avoid unnecessary work
                // and reuse what we got before
                if (hoverListener != null) {
                    hoverListener.onFrameHover(hoveredFrame, hoveredFrameRectangle, e);
                }
                return;
            }
            canvas.getFlamegraphRenderEngine()
                  .ifPresent(fgp -> {
                      var canvasGraphics = (Graphics2D) canvas.getGraphics();
                      fgp.getFrameAt(
                                 canvasGraphics,
                                 canvas.getBounds(),
                                 latestMouseLocation
                         )
                         .ifPresentOrElse(
                                 frame -> {
                                     fgp.hoverFrame(
                                             frame,
                                             canvasGraphics,
                                             canvas.getBounds(),
                                             canvas::repaint
                                     );
                                     canvas.setToolTipText(frame);
                                     hoveredFrameRectangle = fgp.getFrameRectangle(
                                             canvasGraphics,
                                             canvas.getBounds(),
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
                                             canvas.getBounds(),
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
                  });
        }

        public void setHoverListener(HoverListener<T> hoveringListener) {
            this.hoverListener = hoveringListener;
        }

        public void install(JScrollPane sp) {
            sp.addMouseListener(this);
            sp.addMouseMotionListener(this);
        }
    }

    static class FlamegraphCanvas<T> extends JPanel implements ZoomableComponent {

        public static final String GRAPH_MODE = "mode";
        private Image minimap;
        private JToolTip toolTip;
        private FlamegraphRenderEngine<T> flamegraphRenderEngine;
        private BiFunction<FrameModel<T>, FrameBox<T>, String> tooltipToTextFunction;
        private final Dimension flamegraphDimension = new Dimension();
        private int minimapWidth = 200;
        private int minimapHeight = 100;
        private int minimapInset = 10;
        private int minimapRadius = 10;
        private Point minimapLocation = new Point(50, 50);
        private Supplier<Color> minimapShadeColorSupplier = null;
        private boolean showMinimap = true;
        private Supplier<JToolTip> tooltipComponentSupplier;

        private ZoomAction zoomActionOverride;
        private BiConsumer<FrameBox<T>, MouseEvent> popupConsumer;
        private BiConsumer<FrameBox<T>, MouseEvent> selectedFrameConsumer;
        private final FlamegraphView<T> flamegraphView;
        private long lastDrawTime;

        public FlamegraphCanvas(FlamegraphView<T> flamegraphView) {
            this.flamegraphView = flamegraphView;
        }

        /**
         * Override this method to listen to LaF changes.
         */
        @Override
        public void updateUI() {
            super.updateUI();
        }

        @Override
        public void addNotify() {
            super.addNotify();

            // Adjust the width of the canvas to the width of the view rect, when
            // the scroll bar is made visible, this prevents the horizontal scrollbar
            // from appearing on first display, see #96.
            // Since a scrollbar is made visible once, this listener is called only once,
            // which is the intended behavior (otherwise it affects zooming).
            var parent = SwingUtilities.getUnwrappedParent(this);
            if (parent instanceof JViewport) {
                var viewport = (JViewport) parent;
                var scrollPane = (JScrollPane) viewport.getParent();
                var vsb = scrollPane.getVerticalScrollBar();
                vsb.addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentShown(ComponentEvent e) {
                        var canvasWidth = getWidth();
                        if (canvasWidth == 0) {
                            return;
                        }
                        setSize(viewport.getViewRect().width, getHeight());
                    }
                });
                this.addPropertyChangeListener(GRAPH_MODE, evt -> {
                    SwingUtilities.invokeLater(() -> {
                        var value = vsb.getValue();
                        var bounds = this.getBounds();
                        var visibleRect = this.getVisibleRect();

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
                    });
                });
            }
        }

        @Override
        public Dimension getPreferredSize() {
            var preferredSize = new Dimension(10, 10);
            if (flamegraphRenderEngine == null) {
                super.setPreferredSize(preferredSize);
                return preferredSize;
            }

            Insets insets = getInsets();
            var flamegraphWidth = getWidth();
            var flamegraphHeight = flamegraphRenderEngine.computeVisibleFlamegraphHeight(
                    (Graphics2D) getGraphics(),
                    flamegraphWidth,
                    getVisibleRect().width,
                    true
            );
            preferredSize.width = Math.max(preferredSize.width, flamegraphWidth + insets.left + insets.right);
            preferredSize.height = Math.max(preferredSize.height, flamegraphHeight + insets.top + insets.bottom);

            // trigger minimap generation, when the flamegraph is zoomed, more
            // frame become visible, and this may make the visible depth higher,
            // this allows to update the minimap when more details are available.
            if (flamegraphHeight != this.flamegraphDimension.height || flamegraphWidth != flamegraphDimension.width) {
                triggerMinimapGeneration();
            }
            this.flamegraphDimension.height = flamegraphHeight;
            this.flamegraphDimension.width = flamegraphWidth;

            super.setPreferredSize(preferredSize);
            return preferredSize;
        }

        @Override
        protected void paintComponent(Graphics g) {
            long start = System.currentTimeMillis();

            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            var visibleRect = getVisibleRect();
            if (flamegraphRenderEngine == null) {
                String message = "No data to display";
                Font font = g2.getFont();
                // calculate center position
                Rectangle2D bounds = g2.getFontMetrics(font).getStringBounds(message, g2);
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

        private void paintDetails(Graphics2D g2) {
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

        private void paintMinimap(Graphics g, Rectangle visibleRect) {
            if (flamegraphDimension != null && showMinimap && minimap != null) {
                var g2 = (Graphics2D) g.create(visibleRect.x + minimapLocation.x,
                                               visibleRect.y + visibleRect.height - minimapHeight - minimapLocation.y,
                                               minimapWidth + minimapInset * 2,
                                               minimapHeight + minimapInset * 2);

                g2.setColor(getBackground());
                g2.fillRoundRect(1, 1, minimapWidth + 2 * minimapInset - 1, minimapHeight + 2 * minimapInset - 1, minimapRadius, minimapRadius);
                g2.drawImage(minimap, minimapInset, minimapInset, null);

                // the image is already rendered, so the hints are only for the shapes below
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setColor(getForeground());
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(1, 1, minimapWidth + 2 * minimapInset - 2, minimapHeight + 2 * minimapInset - 2, minimapRadius, minimapRadius);

                {
                    // Zoom zone
                    double zoomZoneScaleX = (double) minimapWidth / flamegraphDimension.width;
                    double zoomZoneScaleY = (double) minimapHeight / flamegraphDimension.height;

                    int x = (int) (visibleRect.x * zoomZoneScaleX);
                    int y = (int) (visibleRect.y * zoomZoneScaleY);
                    int w = (int) (visibleRect.width * zoomZoneScaleX);
                    int h = (int) (visibleRect.height * zoomZoneScaleY);

                    var zoomZone = new Area(new Rectangle(minimapInset, minimapInset, minimapWidth, minimapHeight));
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
        public String getToolTipText(MouseEvent e) {
            if (isInsideMinimap(e.getPoint())) {
                return "";
            }

            return super.getToolTipText(e);
        }

        public boolean isInsideMinimap(Point point) {
            if (!showMinimap) {
                return false;
            }
            var visibleRect = getVisibleRect();
            var rectangle = new Rectangle(visibleRect.x + minimapLocation.y,
                                          visibleRect.y + visibleRect.height - minimapHeight - minimapLocation.y,
                                          minimapWidth + 2 * minimapInset,
                                          minimapHeight + 2 * minimapInset
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
                var height = flamegraphRenderEngine.computeVisibleFlamegraphMinimapHeight(minimapWidth);
                if (height == 0) {
                    return;
                }

                GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
                GraphicsConfiguration c = e.getDefaultScreenDevice().getDefaultConfiguration();
                BufferedImage minimapImage = c.createCompatibleImage(minimapWidth, height, Transparency.TRANSLUCENT);
                Graphics2D minimapGraphics = minimapImage.createGraphics();
                minimapGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                minimapGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                minimapGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                Rectangle bounds = new Rectangle(minimapWidth, height);
                flamegraphRenderEngine.paintMinimap(minimapGraphics, bounds);
                minimapGraphics.dispose();

                SwingUtilities.invokeLater(() -> this.setMinimapImage(minimapImage));
            }).handle((__, t) -> {
                if (t != null) {
                    t.printStackTrace(); // no thumbnail
                }
                return null;
            });
        }

        private void setMinimapImage(BufferedImage minimapImage) {
            this.minimap = minimapImage.getScaledInstance(minimapWidth, minimapHeight, Image.SCALE_SMOOTH);
            repaintMinimapArea();
        }

        private void repaintMinimapArea() {
            var visibleRect = getVisibleRect();
            repaint(visibleRect.x + minimapLocation.x,
                    visibleRect.y + visibleRect.height - minimapHeight - minimapLocation.y,
                    minimapWidth + minimapInset * 2,
                    minimapHeight + minimapInset * 2);
        }

        public void linkListenerTo(JScrollPane scrollPane) {
            var mouseAdapter = new MouseInputAdapter() {
                private Point pressedPoint;

                @Override
                public void mouseClicked(MouseEvent e) {
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
                public void mousePressed(MouseEvent e) {
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
                public void mouseReleased(MouseEvent e) {
                    handlePopup(e);
                }

                private void handlePopup(MouseEvent e) {
                    if (!e.isPopupTrigger()) {
                        return;
                    }
                    if (popupConsumer == null) {
                        return;
                    }
                    FlamegraphCanvas<T> canvas = FlamegraphCanvas.this;
                    flamegraphRenderEngine.getFrameAt((Graphics2D) canvas.getGraphics(), canvas.getBounds(), e.getPoint())
                                          .ifPresent(frame -> popupConsumer.accept(frame, e));
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (isInsideMinimap(e.getPoint()) && pressedPoint != null) {
                        processMinimapMouseEvent(e);
                    }
                }

                private void processMinimapMouseEvent(MouseEvent e) {
                    if (flamegraphDimension == null) {
                        return;
                    }

                    var pt = e.getPoint();
                    if (!(e.getComponent() instanceof FlamegraphView.FlamegraphCanvas)) {
                        return;
                    }

                    var visibleRect = ((FlamegraphCanvas<?>) e.getComponent()).getVisibleRect();

                    double zoomZoneScaleX = (double) minimapWidth / flamegraphDimension.width;
                    double zoomZoneScaleY = (double) minimapHeight / flamegraphDimension.height;

                    var h = (pt.x - (visibleRect.x + minimapLocation.x)) / zoomZoneScaleX;
                    var horizontalBarModel = scrollPane.getHorizontalScrollBar().getModel();
                    horizontalBarModel.setValue((int) h - horizontalBarModel.getExtent());


                    var v = (pt.y - (visibleRect.y + visibleRect.height - minimapHeight - minimapLocation.y)) / zoomZoneScaleY;
                    var verticalBarModel = scrollPane.getVerticalScrollBar().getModel();
                    verticalBarModel.setValue((int) v - verticalBarModel.getExtent());
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    setCursor(isInsideMinimap(e.getPoint()) ?
                              Cursor.getPredefinedCursor(System.getProperty("os.name").startsWith("Mac") ? Cursor.HAND_CURSOR : Cursor.MOVE_CURSOR) :
                              Cursor.getDefaultCursor());
                }
            };
            this.addMouseListener(mouseAdapter);
            this.addMouseMotionListener(mouseAdapter);
        }

        void setFlamegraphRenderEngine(FlamegraphRenderEngine<T> flamegraphRenderEngine) {
            this.flamegraphRenderEngine = flamegraphRenderEngine;
        }

        Optional<FlamegraphRenderEngine<T>> getFlamegraphRenderEngine() {
            return Optional.ofNullable(flamegraphRenderEngine);
        }

        public void setToolTipTextFunction(BiFunction<FrameModel<T>, FrameBox<T>, String> tooltipTextFunction) {
            this.tooltipToTextFunction = tooltipTextFunction;
        }

        public void setTooltipComponentSupplier(Supplier<JToolTip> tooltipComponentSupplier) {
            this.tooltipComponentSupplier = tooltipComponentSupplier;
        }

        public void setMinimapShadeColorSupplier(Supplier<Color> minimapShadeColorSupplier) {
            this.minimapShadeColorSupplier = minimapShadeColorSupplier;
        }

        public void showMinimap(boolean showMinimap) {
            if (this.showMinimap == showMinimap) {
                return;
            }
            this.showMinimap = showMinimap;
            triggerMinimapGeneration();
            firePropertyChange("minimap", !showMinimap, showMinimap);
        }

        public boolean isShowMinimap() {
            return showMinimap;
        }

        public void setMode(Mode mode) {
            var oldMode = getMode();
            if (oldMode == mode) {
                return;
            }

            getFlamegraphRenderEngine().ifPresent(fre -> fre.setIcicle(Mode.ICICLEGRAPH == mode));
            firePropertyChange(GRAPH_MODE, oldMode, mode);
        }

        public FlamegraphView.Mode getMode() {
            return getFlamegraphRenderEngine().map(fre -> fre.isIcicle() ? Mode.ICICLEGRAPH : Mode.FLAMEGRAPH).get();
        }

        public void setPopupConsumer(BiConsumer<FrameBox<T>, MouseEvent> consumer) {
            this.popupConsumer = consumer;
        }

        public void setSelectedFrameConsumer(BiConsumer<FrameBox<T>, MouseEvent> consumer) {
            this.selectedFrameConsumer = consumer;
        }

        public ZoomTarget resetZoomTarget() {
            var visibleRect = getVisibleRect();
            var bounds = getBounds();

            var newHeight = flamegraphRenderEngine.computeVisibleFlamegraphHeight(
                    (Graphics2D) getGraphics(),
                    visibleRect.width,
                    visibleRect.width
            );

            return new ZoomTarget(
                    0,
                    getMode() == Mode.FLAMEGRAPH ? -(bounds.height - visibleRect.height) : 0,
                    visibleRect.width,
                    newHeight
            );
        }

        @Override
        public void zoom(ZoomTarget zoomTarget) {
            // Changing the size triggers a revalidation, which triggers a layout
            setBounds(zoomTarget);
        }
    }
}
