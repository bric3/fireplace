package io.github.bric3.fireplace.ui;

import javax.swing.*;
import java.awt.*;
import java.util.stream.Stream;

public abstract class SwingUtils {
    public SwingUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Point getLastPointerLocation(JComponent component) {
        var location = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(location, component);
        return location;
    }

    public static Stream<Component> descendants(Container parent) {
        return Stream.of(parent.getComponents())
                     .filter(Container.class::isInstance)
                     .map(Container.class::cast)
                     .flatMap(c -> Stream.concat(Stream.of(c), descendants(c)));
    }

    public static Stream<Component> descendantOrSelf(Container parent) {
        return Stream.of(parent.getComponents())
                     .filter(Container.class::isInstance)
                     .map(c -> descendantOrSelf((Container) c))
                     .reduce(Stream.of(parent), Stream::concat);
    }
}
