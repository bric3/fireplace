/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.github.bric3.fireplace.flamegraph;

import java.util.List;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/**
 * Wrapper of the real flamegraph tree node.
 * <p>
 * This wrapper is used to pre-process, the depth, the boundaries on the X axis
 * (horizontal) as ratios of that space, likely {@code [0.0, 1.0]}.
 * </p>
 * <p>
 * In order to pre-compute this list you can use
 * {@link #flattenAndCalculateCoordinate(List, Object, Function, ToDoubleFunction, double, double, int)}
 * utility method.
 * </p>
 *
 * @param <T>
 * @see FlameGraph
 */
public class FrameBox<T> {
    public final T actualNode;
    public final double startX;
    public final double endX;
    public final int stackDepth;

    public FrameBox(T actualNode, double startX, double endX, int stackDepth) {
        this.actualNode = actualNode;
        this.startX = startX;
        this.endX = endX;
        this.stackDepth = stackDepth;
    }

    @Override
    public String toString() {
        return "FlameNode{" +
               "startX=" + startX +
               ", endX=" + endX +
               ", depth=" + stackDepth +
               '}';
    }

    /**
     * Helper method that will flatten a tree by traversing it (DFS algorithm).
     * <p>
     * Example usage:
     * <pre><code>
     * var nodes = new ArrayList&lt;FrameBox&lt;Node&gt;&gt;();
     *
     * FrameBox.flattenAndCalculateCoordinate(
     *         nodes,
     *         model.getRoot(),
     *         Node::getChildren,
     *         Node::getCumulativeWeight,
     *         0.0d,
     *         1.0d,
     *         0
     * );
     *
     * flameGraph.setData(nodes, ...);
     * </code></pre>
     * </p>
     *
     * @param accumulator The flattened list of nodes.
     * @param fromNode    Node to start from.
     * @param getChildren Get node children function.
     * @param nodeWeight  The node weight function.
     * @param startX      Start boundary on the X axis.
     * @param endX        End boundary on the X axis.
     * @param depth       The node depth.
     * @param <T>         The node type.
     */
    public static <T> void flattenAndCalculateCoordinate(
            List<FrameBox<T>> accumulator,
            T fromNode,
            Function<T, List<T>> getChildren,
            ToDoubleFunction<T> nodeWeight,
            double startX,
            double endX,
            int depth) {
        accumulator.add(new FrameBox<>(fromNode, startX, endX, depth));

        var children = getChildren.apply(fromNode);
        if (children == null || children.isEmpty()) {
            return;
        }

        depth++;
        var parentWidth = endX - startX;
        var totalWeight = children.stream().mapToDouble(nodeWeight).sum();
        for (var node : children) {
            var nodeWidth = (nodeWeight.applyAsDouble(node) / totalWeight) * parentWidth;
            endX = startX + nodeWidth;
            flattenAndCalculateCoordinate(accumulator, node, getChildren, nodeWeight, startX, endX, depth);
            startX = endX;
        }
    }
}
