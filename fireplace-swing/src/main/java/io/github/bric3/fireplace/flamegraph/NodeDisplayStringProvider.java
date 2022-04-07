package io.github.bric3.fireplace.flamegraph;

@FunctionalInterface
public interface NodeDisplayStringProvider<T> {
    /**
     * @param node The node to calculate the displayed value for
     * @param isRoot True if this is the root node in the graph
     * @return The String to render for the frame
     */
    String frameString(T node, boolean isRoot);
}
