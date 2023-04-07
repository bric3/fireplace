package io.github.bric3.fireplace.flamegraph;

import javax.swing.*;
import java.awt.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ButterflyView<T> extends JPanel {

    private final FlamegraphView<T> predecessorView;
    private final FlamegraphView<T> successorsView;

    public ButterflyView() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add((predecessorView = new FlamegraphView<T>()).component, BorderLayout.NORTH);
        add((successorsView = new FlamegraphView<T>()).component, BorderLayout.SOUTH);
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
     *     <li>The frame background and foreground colors.</li>
     * </ul>
     *
     * @param frameTextsProvider The function to display label in frames.
     * @param frameColorProvider The frame to background color function.
     * @param frameFontProvider  The frame font provider.
     */
    public void setRenderConfiguration(
            FrameTextsProvider<T> frameTextsProvider,
            FrameColorProvider<T> frameColorProvider,
            FrameFontProvider<T> frameFontProvider
    ) {
        predecessorView.setRenderConfiguration(frameTextsProvider, frameColorProvider, frameFontProvider);
        successorsView.setRenderConfiguration(frameTextsProvider, frameColorProvider, frameFontProvider);
        predecessorView.setMode(FlamegraphView.Mode.FLAMEGRAPH);
        successorsView.setMode(FlamegraphView.Mode.ICICLEGRAPH);
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
        predecessorView.setTooltipTextFunction(tooltipTextFunction);
        successorsView.setTooltipTextFunction(tooltipTextFunction);
    }

    /**
     * Experimental configuration hook for the underlying canvas.
     *
     * @param canvasConfigurer The configurer for the canvas.
     */
    public void configureCanvas(Consumer<JComponent> canvasConfigurer) {
        predecessorView.configureCanvas(canvasConfigurer);
        successorsView.configureCanvas(canvasConfigurer);
    }

    /**
     * Replaces the default tooltip component.
     *
     * @param tooltipComponentSupplier The tooltip component supplier.
     */
    public void setTooltipComponentSupplier(Supplier<JToolTip> tooltipComponentSupplier) {
        predecessorView.setTooltipComponentSupplier(tooltipComponentSupplier);
        successorsView.setTooltipComponentSupplier(tooltipComponentSupplier);
    }

    /**
     * Replaces the frame colors provider.
     *
     * @param frameColorProvider A provider that takes a frame and returns its colors.
     * @see FrameColorProvider
     */
    public void setFrameColorProvider(FrameColorProvider<T> frameColorProvider) {
        predecessorView.setFrameColorProvider(frameColorProvider);
        successorsView.setFrameColorProvider(frameColorProvider);
    }

    public FrameColorProvider<T> getFrameColorProvider() {
        return predecessorView.getFrameColorProvider();
    }

    public void requestRepaint() {
        predecessorView.requestRepaint();
        successorsView.requestRepaint();
    }

    public void setModel(FrameModel<T> predecessorsModel, FrameModel<T> successorsModel) {
        predecessorView.setModel(predecessorsModel);
        successorsView.setModel(successorsModel);
    }
}
