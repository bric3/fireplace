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

import io.github.bric3.fireplace.core.ui.JScrollPaneWithButton;
import io.github.bric3.fireplace.core.ui.MouseInputListenerWorkaroundForToolTipEnabledComponent;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
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
 * FlameGraph&lt;MyNode&gt; flameGraph = new FlameGraph&lt;&gt;();
 * flameGraph.showMinimap(false);
 * flameGraph.setData(
 *     (FrameBox&lt;MyNode&gt;) listOfFrameBox(),   // list of frames
 *     List.of(n -&gt; n.stringRepresentation()),      // string representation candidates
 *     rootNode -&gt; rootNode.stringRepresentation(), // root node string representation
 *     frameToColorFunction,                        // color function
 *     frameToToolTipTextFunction                   // text tooltip function
 * );
 *
 * panel.add(flameGraph.component);
 * </code></pre>
 * <p>
 * The created and <em>final</em> {@code component} is a composite that is based
 * on a {@link JScrollPane}.
 * </p>
 *
 * @param <T> The type of the node data.
 * @see FlameGraphPainter
 */
public class FlameGraph<T> {
    /**
     * Internal key to get the FlameGraph from the component.
     */
    private static final String OWNER_KEY = "flamegraphOwner";
    /**
     * The key for a client property that controls the display of rendering statistics.
     */
    public static String SHOW_STATS = "flamegraph.show_stats";

    private final FlameGraphCanvas<T> canvas;

    /**
     * The final composite component that can display a flame graph.
     */
    public final JComponent component;

    /**
     * Mouse input listener used to move the canvas over the JScrollPane
     * as well as trigger other bhavior on the canvas.
     */
    private final FlameGraphMouseInputListener<T> listener;

    /**
     * The precomputed list of frames.
     */
    private List<FrameBox<T>> frames;

    /**
     * Represents a custom actions when zooming
     */
    public interface ZoomAction {
        <T> boolean zoom(JViewport viewPort, final FlameGraphCanvas<T> canvas, ZoomTarget zoomTarget);
    }

    /**
     * Represents a custom actions when zooming
     *
     * @param <T> The type of the node data.
     */
    public interface HoveringListener<T> {
        void onStopHover(MouseEvent e);

        void onFrameHover(FrameBox<T> frame, Rectangle hoveredFrameRectangle, MouseEvent e);
    }

    /**
     * Return the {@code FlameGraph} that created the passed component.
     * <p>
     * If this wasn't returned by a {@code FlaemGraph} then retunr empty.
     *
     * @param component the JComponent
     * @param <T>       The type of the node data.
     * @return The {@code FlameGraph} instance that crated this JComponent or empty.
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<FlameGraph<T>> from(JComponent component) {
        return Optional.ofNullable((FlameGraph<T>) component.getClientProperty(OWNER_KEY));
    }

    /**
     * Creates an empty flame graph.
     * In order to use in Swing just access the {@link #component} field.
     */
    public FlameGraph() {
        canvas = new FlameGraphCanvas<>();
        listener = new FlameGraphMouseInputListener<>(canvas);
        var scrollPane = new JScrollPane(canvas);
        var layeredScrollPane = JScrollPaneWithButton.create(
                () -> {

                    // Code to tweak the actions
                    // https://stackoverflow.com/a/71009104/48136
                    // see javax.swing.plaf.basic.BasicScrollPaneUI.Actions
                    //                    var actionMap = scrollPane.getActionMap();
                    //                    var inputMap = scrollPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
                    // var inputMap = scrollPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

                    scrollPane.getVerticalScrollBar().setUnitIncrement(16);
                    scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
                    listener.install(scrollPane);
                    new MouseInputListenerWorkaroundForToolTipEnabledComponent(scrollPane).install(canvas);
                    canvas.linkListenerTo(scrollPane);

                    return scrollPane;
                }
        );

        component = wrap(scrollPane, layeredScrollPane);
    }

    /**
     * Murky workaround to propagate the background color to the canvas
     * since JLayer is final.
     */
    private JPanel wrap(JScrollPane scrollPane, JLayer<JScrollPane> layeredScrollPane) {
        var wrapper = new JPanel(new BorderLayout()) {
            @Override
            public void updateUI() {
                super.updateUI();
                scrollPane.setBorder(null);
                scrollPane.getVerticalScrollBar().setBackground(getBackground());
                scrollPane.getHorizontalScrollBar().setBackground(getBackground());
                canvas.setBackground(getBackground());
            }

            @Override
            public void setBackground(Color bg) {
                super.setBackground(bg);
                scrollPane.setBorder(null);
                scrollPane.setBackground(bg);
                scrollPane.getVerticalScrollBar().setBackground(bg);
                scrollPane.getHorizontalScrollBar().setBackground(bg);
                canvas.setBackground(bg);
            }
        };
        wrapper.setBorder(null);
        wrapper.add(layeredScrollPane);
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
    public void setColorFunction(Function<FrameBox<T>, Color> frameColorFunction) {
        Objects.requireNonNull(frameColorFunction);
        this.canvas.getFlameGraphPainter()
                   .ifPresent(fgp -> fgp.frameColorFunction = frameColorFunction);
    }

    /**
     * Toggle the display of a gap between frames.
     *
     * @param frameGapEnabled {@code true} to show a gap between frames, {@code false} otherwise.
     */
    public void setFrameGapEnabled(boolean frameGapEnabled) {
        canvas.getFlameGraphPainter()
              .ifPresent(fgp -> fgp.frameGapEnabled = frameGapEnabled);
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
    public void showMinimap(boolean showMinimap) {
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
    public void setHoveringListener(HoveringListener<T> hoverListener) {
        listener.setHoveringListener(hoverListener);
    }

    /**
     * Actually set the {@link FlameGraph} with typed data and configure how to use it.
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
     * @param frameToString       function to display label in frames.
     * @param frameColorFunction  the frame to background color function.
     * @param tooltipTextFunction the frame tooltip text function.
     */
    @Deprecated(forRemoval = true)
    public void setData(List<FrameBox<T>> frames,
                        NodeDisplayStringProvider<T> frameToString,
                        Function<T, Color> frameColorFunction,
                        Function<FrameBox<T>, String> tooltipTextFunction) {
        setConfigurationAndData(
                frames,
                frameToString,
                frame -> frameColorFunction.apply(frame.actualNode),
                tooltipTextFunction
        );
    }

    /**
     * Actually set the {@link FlameGraph} with typed data and configure how to use it.
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
     * @param frameToString       function to display label in frames.
     * @param frameColorFunction  the frame to background color function.
     * @param tooltipTextFunction the frame tooltip text function.
     */
    public void setConfigurationAndData(List<FrameBox<T>> frames,
                                        NodeDisplayStringProvider<T> frameToString,
                                        Function<FrameBox<T>, Color> frameColorFunction,
                                        Function<FrameBox<T>, String> tooltipTextFunction) {
        var flameGraphPainter = new FlameGraphPainter<>(
                Objects.requireNonNull(frames),
                Objects.requireNonNull(frameToString),
                Objects.requireNonNull(frameColorFunction)
        );
        this.frames = Objects.requireNonNull(frames);

        canvas.setFlameGraphPainter(Objects.requireNonNull(flameGraphPainter));
        canvas.setToolTipTextFunction(Objects.requireNonNull(tooltipTextFunction));
        canvas.invalidate();
        canvas.repaint();
    }

    /**
     * Clear all data.
     */
    public void clear() {
        frames = Collections.emptyList();
        canvas.setFlameGraphPainter(null);
        canvas.invalidate();
        canvas.repaint();
    }

    public List<FrameBox<T>> getFrames() {
        return frames;
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
        zoom(
                canvas,
                (JViewport) canvas.getParent(),
                new ZoomTarget(canvas.getVisibleRect().getSize(), new Point())
        );
    }

    /**
     * Programmtic zoom to the specified frame.
     *
     * @param frame The frame to zoom to.
     */
    public void zoomTo(FrameBox<T> frame) {
        var viewport = (JViewport) canvas.getParent();
        canvas.getFlameGraphPainter().map(fgp -> fgp.calculateZoomTargetFrame(
                (Graphics2D) canvas.getGraphics(),
                canvas.getBounds(),
                viewport.getViewRect(),
                frame
        )).ifPresent(zoomTarget -> zoom(canvas, viewport, zoomTarget));
    }

    private static <T> void zoom(FlameGraphCanvas<T> canvas, JViewport viewPort, ZoomTarget zoomTarget) {
        if (canvas.zoomActionOverride == null || !canvas.zoomActionOverride.zoom(viewPort, canvas, zoomTarget)) {
            canvas.setSize(zoomTarget.bounds);
            viewPort.setViewPosition(zoomTarget.viewOffset);
        }
    }

    /**
     * Sets frames that need to be highlighted.
     * <p>
     * The passed collection must be a subset of the frames that were used
     * in {@link #setData(List, NodeDisplayStringProvider, Function, Function)}.
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
        canvas.getFlameGraphPainter().ifPresent(painter ->
                                                        painter.setHighlightFrames(framesToHighlight, searched)
        );
        canvas.repaint();
    }

    /**
     * The internal mouse listener that is attached to the scrollpane.
     * <p>
     * This listener will be responsible to trigger some behaviors on the canvas itself.
     * </p>
     *
     * @param <T>
     */
    private static class FlameGraphMouseInputListener<T> implements MouseInputListener {
        private Point pressedPoint;
        private final FlameGraphCanvas<T> canvas;
        private Rectangle hoveredFrameRectangle;
        private HoveringListener<T> hoveringListener;
        private FrameBox<T> hoveredFrame;

        public FlameGraphMouseInputListener(FlameGraphCanvas<T> canvas) {
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
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                this.pressedPoint = e.getPoint();
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

            var latestMouseLocation = MouseInfo.getPointerInfo().getLocation();
            SwingUtilities.convertPointFromScreen(latestMouseLocation, canvas);

            if (canvas.isInsideMinimap(latestMouseLocation)) {
                // bail out
                return;
            }

            if (e.getClickCount() == 2) {
                // find zoom target then do an animated transition
                canvas.getFlameGraphPainter().flatMap(fgp -> fgp.calculateZoomTargetForFrameAt(
                        (Graphics2D) viewPort.getView().getGraphics(),
                        canvas.getBounds(),
                        viewPort.getViewRect(),
                        latestMouseLocation
                )).ifPresent(zoomTarget -> zoom(canvas, viewPort, zoomTarget));
                return;
            }

            canvas.getFlameGraphPainter()
                  .ifPresent(fgp -> fgp.toggleSelectedFrameAt(
                          (Graphics2D) viewPort.getView().getGraphics(),
                          canvas.getBounds(),
                          latestMouseLocation,
                          (frame, r) -> canvas.repaint()
                  ));
        }


        @Override
        public void mouseEntered(MouseEvent e) {
            // this seems to enable key navigation
            if ((e.getComponent() instanceof JScrollPane)) {
                e.getComponent().requestFocus();
            }

        }

        @Override
        public void mouseExited(MouseEvent e) {
            if ((e.getSource() instanceof JScrollPane)) {
                hoveredFrameRectangle = null;
                hoveredFrame = null;
                canvas.getFlameGraphPainter()
                      .ifPresent(FlameGraphPainter::stopHover);
                canvas.repaint();
                if (hoveringListener != null) {
                    hoveringListener.onStopHover(e);
                }
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            var scrollPane = (JScrollPane) e.getComponent();
            var viewPort = scrollPane.getViewport();
            var canvas = (JComponent) viewPort.getView();

            var latestMouseLocation = MouseInfo.getPointerInfo().getLocation();
            SwingUtilities.convertPointFromScreen(latestMouseLocation, canvas);

            if (this.canvas.isInsideMinimap(latestMouseLocation)) {
                if (hoveringListener != null) {
                    hoveringListener.onStopHover(e);
                }
                // bail out
                return;
            }

            // handle hovering
            if (hoveredFrameRectangle != null && hoveredFrameRectangle.contains(latestMouseLocation)) {
                // still hovering the same frame, avoid unnecessary work
                // and reuse what we got before
                if (hoveringListener != null) {
                    hoveringListener.onFrameHover(hoveredFrame, hoveredFrameRectangle, e);
                }
                return;
            }
            this.canvas.getFlameGraphPainter()
                       .ifPresent(fgp -> {
                           var canvasGraphics = (Graphics2D) canvas.getGraphics();
                           fgp.getFrameAt(
                                      canvasGraphics,
                                      this.canvas.getBounds(),
                                      latestMouseLocation
                              )
                              .ifPresentOrElse(
                                      frame -> {
                                          fgp.hoverFrame(
                                                  frame,
                                                  canvasGraphics,
                                                  this.canvas.getBounds(),
                                                  this.canvas::repaint
                                          );
                                          this.canvas.setToolTipText(frame);
                                          hoveredFrameRectangle = fgp.getFrameRectangle(canvasGraphics, this.canvas.getBounds(), frame);
                                          hoveredFrame = frame;
                                          if (hoveringListener != null) {
                                              hoveringListener.onFrameHover(frame, hoveredFrameRectangle, e);
                                          }
                                      },
                                      () -> {
                                          fgp.stopHover();
                                          hoveredFrameRectangle = null;
                                          hoveredFrame = null;
                                          if (hoveringListener != null) {
                                              hoveringListener.onStopHover(e);
                                          }
                                      }
                              );
                       });
        }

        public void setHoveringListener(HoveringListener<T> hoveringListener) {
            this.hoveringListener = hoveringListener;
        }

        public void install(JScrollPane sp) {
            sp.addMouseListener(this);
            sp.addMouseMotionListener(this);
        }
    }

    static class FlameGraphCanvas<T> extends JPanel {

        private Image minimap;
        private JToolTip toolTip;
        private FlameGraphPainter<T> flameGraphPainter;
        private Function<FrameBox<T>, String> tooltipToTextFunction;
        private Dimension flameGraphDimension;
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


        public FlameGraphCanvas() {
            this(null);
        }

        public FlameGraphCanvas(FlameGraphPainter<T> flameGraphPainter) {
            this.flameGraphPainter = flameGraphPainter;
        }

        /**
         * Override this method to listen to LaF changes.
         */
        @Override
        public void updateUI() {
            super.updateUI();
            // if (flameGraphPainter != null) {
            //     flameGraphPainter.updateUI();
            // }
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension defaultDimension = super.getPreferredSize();
            defaultDimension = (defaultDimension == null) ? new Dimension(100, 50) : defaultDimension;

            if (flameGraphPainter == null) {
                return defaultDimension;
            }

            Insets insets = getInsets();
            var flameGraphDimension = flameGraphPainter.computeFlameGraphDimension((Graphics2D) getGraphics(),
                                                                                   getWidth(),
                                                                                   insets
            );
            defaultDimension.width = Math.max(defaultDimension.width, flameGraphDimension.width + insets.left + insets.right);
            defaultDimension.height = Math.max(defaultDimension.height, flameGraphDimension.height + insets.top + insets.bottom);

            // trigger minimap generation
            if (!flameGraphDimension.equals(this.flameGraphDimension)) {
                triggerMinimapGeneration();
            }
            this.flameGraphDimension = flameGraphDimension;
            return defaultDimension;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            var visibleRect = getVisibleRect();
            if (flameGraphPainter == null) {
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

            flameGraphPainter.paintDetails = getClientProperty(SHOW_STATS) == TRUE;
            flameGraphPainter.paint(g2, getBounds(), visibleRect);
            paintMinimap(g2, visibleRect);
            g2.dispose();
        }

        private void paintMinimap(Graphics g, Rectangle visibleRect) {
            if (flameGraphDimension != null && showMinimap && minimap != null) {
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
                    double zoomZoneScaleX = (double) minimapWidth / flameGraphDimension.width;
                    double zoomZoneScaleY = (double) minimapHeight / flameGraphDimension.height;

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
            setToolTipText(tooltipToTextFunction.apply(frame));
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
            if (!showMinimap || flameGraphPainter == null) {
                repaintMinimapArea();
                return;
            }

            CompletableFuture.runAsync(() -> {
                var height = flameGraphPainter.computeFlameGraphMinimapHeight(minimapWidth);
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
                flameGraphPainter.paintMinimap(minimapGraphics, bounds);
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
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() != 1 || e.getButton() != MouseEvent.BUTTON1) {
                        return;
                    }
                    if (selectedFrameConsumer == null) {
                        return;
                    }
                    FlameGraphCanvas<T> canvas = FlameGraphCanvas.this;
                    flameGraphPainter.getFrameAt((Graphics2D) canvas.getGraphics(), canvas.getBounds(), e.getPoint())
                                     .ifPresent(frame -> selectedFrameConsumer.accept(frame, e));
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (isInsideMinimap(e.getPoint())) {
                        processMinimapMouseEvent(e);
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
                    FlameGraphCanvas<T> canvas = FlameGraphCanvas.this;
                    flameGraphPainter.getFrameAt((Graphics2D) canvas.getGraphics(), canvas.getBounds(), e.getPoint())
                                     .ifPresent(frame -> popupConsumer.accept(frame, e));
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (isInsideMinimap(e.getPoint())) {
                        processMinimapMouseEvent(e);
                    }
                }

                private void processMinimapMouseEvent(MouseEvent e) {
                    if (flameGraphDimension == null) {
                        return;
                    }

                    var pt = e.getPoint();
                    if (!(e.getComponent() instanceof FlameGraph.FlameGraphCanvas)) {
                        return;
                    }

                    var visibleRect = ((FlameGraphCanvas<?>) e.getComponent()).getVisibleRect();

                    double zoomZoneScaleX = (double) minimapWidth / flameGraphDimension.width;
                    double zoomZoneScaleY = (double) minimapHeight / flameGraphDimension.height;

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

        void setFlameGraphPainter(FlameGraphPainter<T> flameGraphPainter) {
            this.flameGraphPainter = flameGraphPainter;
        }

        Optional<FlameGraphPainter<T>> getFlameGraphPainter() {
            return Optional.ofNullable(flameGraphPainter);
        }

        public void setToolTipTextFunction(Function<FrameBox<T>, String> tooltipTextFunction) {
            this.tooltipToTextFunction = tooltipTextFunction;
        }

        public void setTooltipComponentSupplier(Supplier<JToolTip> tooltipComponentSupplier) {
            this.tooltipComponentSupplier = tooltipComponentSupplier;
        }

        public void setMinimapShadeColorSupplier(Supplier<Color> minimapShadeColorSupplier) {
            this.minimapShadeColorSupplier = minimapShadeColorSupplier;
        }

        public void showMinimap(boolean showMinimap) {
            this.showMinimap = showMinimap;
            triggerMinimapGeneration();
        }

        public boolean isShowMinimap() {
            return showMinimap;
        }

        public void setPopupConsumer(BiConsumer<FrameBox<T>, MouseEvent> consumer) {
            this.popupConsumer = consumer;
        }

        public void setSelectedFrameConsumer(BiConsumer<FrameBox<T>, MouseEvent> consumer) {
            this.selectedFrameConsumer = consumer;
        }
    }
}
