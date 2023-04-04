package io.github.bric3.fireplace.charts;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A dataset containing zero, one or many (x, y) data items.
 */
public class XYDataset {
    @NotNull
    public final List<XY<Long, Double>> items;
    @NotNull
    public final Range<Long> rangeOfX;
    @NotNull
    public final Range<Double> rangeOfY;
    @NotNull
    public final String label;

    /**
     * Creates a new dataset.
     *
     * @param sourceItems the source items ({@code null} not permitted).
     * @param label The label for the dataset.
     */
    public XYDataset(@NotNull List<@NotNull XY<@NotNull Long, @NotNull Double>> sourceItems, @NotNull String label) {
        this.label = label;
        // verify that none of the x-values is null or NaN ot INF
        // and while doing that record the mins and maxes
        Objects.requireNonNull(sourceItems, "sourceItems must not be null");
        Objects.requireNonNull(label, "label must not be null");
        if (sourceItems.isEmpty()) {
            throw new IllegalArgumentException("sourceItems must not be empty");
        }
        this.items = new ArrayList<>(sourceItems);
        long minX = Long.MAX_VALUE;
        long maxX = Long.MIN_VALUE;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (var sourceItem : sourceItems) {
            Objects.requireNonNull(sourceItem, "elements of sourceItems must not be null");
            minX = Math.min(minX, sourceItem.x);
            maxX = Math.max(maxX, sourceItem.x);

            minY = Math.min(minY, sourceItem.y);
            maxY = Math.max(maxY, sourceItem.y);
        }
        this.rangeOfX = new Range<>(minX, maxX);
        this.rangeOfY = tweakYRange(new Range<>(minY, maxY));
    }

    protected Range<Double> tweakYRange(Range<Double> yRange) {
        return yRange;
    }

    public int getItemCount() {
        return items.size();
    }

    public Long xAt(int index) {
        return this.items.get(index).x;
    }

    public Double yAt(int index) {
        return this.items.get(index).y;
    }

    /**
     * Compare with another instance.
     * Note doesn't compare the data points, only the ranges and the label.
     *
     * @param o the object to compare with this instance.
     * @return whether two data set are assumed to be equal.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XYDataset xyDataset = (XYDataset) o;
        return rangeOfX.equals(xyDataset.rangeOfX) && rangeOfY.equals(xyDataset.rangeOfY) && label.equals(xyDataset.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rangeOfX, rangeOfY, label);
    }

    /**
     * A pair of objects.
     *
     * @param <X> the type of the first object.
     * @param <Y> the type of the second object.
     */
    public record XY<X extends Number, Y extends Number>(X x, Y y) {}
}
