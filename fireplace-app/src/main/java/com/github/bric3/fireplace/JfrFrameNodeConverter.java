/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.github.bric3.fireplace;

import com.github.bric3.fireplace.flamegraph.FrameBox;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/**
 * Creates an array of FlameNodes that live in the [0.0, 1.0] world space on the X axis and the depth of the stack representing
 * the Y axis.
 * A child node will be proportional to its parent's X space according to its proportion of time it took of its parent's time.
 * The root of the flame graph will always be full width.
 */
public class JfrFrameNodeConverter {
    public static List<FrameBox<Node>> convert(StacktraceTreeModel model) {
        var nodes = new ArrayList<FrameBox<Node>>();

        flattenAndCalculateCoordinate(
                nodes,
                model.getRoot(),
                Node::getChildren,
                Node::getCumulativeWeight,
                0.0d,
                1.0d,
                0
        );

        assert nodes.get(0).actualNode.isRoot() : "First node should be the root node";

        return nodes;
    }

    public static <T> void flattenAndCalculateCoordinate(
            List<FrameBox<T>> nodes,
            T currentNode,
            Function<T, List<T>> nodeChildren,
            ToDoubleFunction<T> nodeWeight,
            double startX,
            double endX,
            int depth) {
        nodes.add(new FrameBox<>(currentNode, startX, endX, depth));

        var children = nodeChildren.apply(currentNode);
        if (children == null || children.isEmpty()) {
            return;
        }

        depth++;
        var parentWidth = endX - startX;
        var totalWeight = children.stream().mapToDouble(nodeWeight).sum();
        for (var node : children) {
            var nodeWidth = (nodeWeight.applyAsDouble(node) / totalWeight) * parentWidth;
            endX = startX + nodeWidth;
            flattenAndCalculateCoordinate(nodes, node, nodeChildren, nodeWeight, startX, endX, depth);
            startX = endX;
        }
    }
}
