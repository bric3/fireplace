package io.github.bric3.fireplace.charts;

import java.util.List;

/**
 * A dataset containing zero, one or many (x, y) data items.
 * This dataset is specifically for datasets where Y is a percentage between 0 and 1.
 */
public class XYPercentageDataset extends XYDataset {
    /**
     * Creates a new dataset where Y is a percentage between 0 and 1.
     *
     * @param sourceItems the source items ({@code null} not permitted).
     * @param label The label for the dataset.
     */
    public XYPercentageDataset(List<XY<Long, Double>> sourceItems, String label) {
        super(sourceItems, label);
    }
    
    @Override
    protected Range<Double> tweakYRange(Range<Double> yRange) {
        return new Range<>(0.0, 1.0);
    }
}
