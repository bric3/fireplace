package io.github.bric3.fireplace.charts;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents a range of values between min and max.
 */
public record Range<T extends Number>(@NotNull T min, @NotNull T max) {
    /**
     * Creates a new range.
     *
     * @param min the minimum value.
     * @param max the maximum value.
     */
    public Range {
        check(min, max);
    }

    private static <T extends Number> void check(T min, T max) {
        if (min == null || max == null) {
            throw new IllegalArgumentException("Null 'min' or 'max' argument.");
        }
        if (min.getClass() != max.getClass()) {
            throw new IllegalArgumentException("min and max must be of the same type, min is " + min.getClass() + ", max is " + max.getClass());
        }
        if (min instanceof Double && min.doubleValue() > max.doubleValue()) {
            throw new IllegalArgumentException("min must be less than max, min is " + min + ", max is " + max);
        }
        if (min instanceof Long && min.longValue() > max.longValue()) {
            throw new IllegalArgumentException("min must be less than max, min is " + min + ", max is " + max);
        }
        if (min instanceof Integer && min.intValue() > max.intValue()) {
            throw new IllegalArgumentException("min must be less than max, min is " + min + ", max is " + max);
        }
    }

    public boolean isZeroLength() {
        return min.equals(max);
    }

    /**
     * Returns a range based on this range but extended (if necessary)
     * to include the specified value.
     *
     * @param v the value.
     * @return The range.
     */
    public Range<T> include(T v) {
        if (v instanceof Double && !Double.isFinite(v.doubleValue())) {
            throw new IllegalArgumentException("only finite values permitted");
        }
        if (compare(v, min) < 0) {
            return new Range<>(v, max);
        }
        if (compare(v, max) > 0) {
            return new Range<>(min, v);
        }
        return this;
    }

    /**
     * Calculates a fractional value indicating where v lies along the
     * range.  This will return 0.0 if the range has zero length.
     *
     * @param v the value.
     * @return The fraction.
     */
    public double calcFraction(T v) {
        if (compare(max, min) > 0) {
            if (min instanceof Double) {
                return (v.doubleValue() - min.doubleValue()) / (max.doubleValue() - min.doubleValue());
            }
            if (min instanceof Long) {
                return (double) (v.longValue() - min.longValue()) / (double) (max.longValue() - min.longValue());
            }
            if (min instanceof Integer) {
                return (double) (v.intValue() - min.intValue()) / (double) (max.intValue() - min.intValue());
            }
            throw new IllegalArgumentException("Unsupported type " + min.getClass());
        }
        return 0.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var range = (Range<?>) o;
        if (min.getClass() != range.min.getClass()) {
            return false;
        }
        return compare(range.min, min) == 0 && compare(range.max, max) == 0;
    }

    /**
     * compare two {@code Number} of the same type.
     *
     * @param a   the first value.
     * @param b   the second value.
     * @param <T> The type of the number.
     * @return the value {@code 0} if {@code d1} is numerically equal to {@code d2}; a value less than
     * {@code 0} if {@code d1} is numerically less than {@code d2}; and a value greater than {@code 0}
     * if {@code d1} is numerically greater than {@code d2}.
     */
    static <T extends Number> int compare(T a, T b) {
        if (a instanceof Double) {
            return Double.compare((Double) a, (Double) b);
        }
        if (a instanceof Long) {
            return Long.compare((Long) a, (Long) b);
        }
        if (a instanceof Integer) {
            return Integer.compare((Integer) a, (Integer) b);
        }
        throw new IllegalArgumentException("Unsupported type " + a.getClass());
    }

    @Override
    public int hashCode() {
        return Objects.hash(min, max);
    }

    @Override
    public String toString() {
        return "[Range: " + min + ", " + max + "]";
    }
}
