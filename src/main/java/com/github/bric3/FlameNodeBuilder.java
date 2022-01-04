/*
 * Fireplace
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.github.bric3;

import java.util.ArrayList;
import java.util.List;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

/**
 * Creates an array of FlameNodes that live in the [0.0, 1.0] world space on the X axis and the depth of the stack representing
 * the Y axis.
 * A child node will be proportional to its parent's X space according to its proportion of time it took of its parent's time.
 * The root of the flame graph will always be full width.
 */
public class FlameNodeBuilder {
    static List<FlameNode<Node>> buildFlameNodes(StacktraceTreeModel model) {
        var nodes = new ArrayList<FlameNode<Node>>();

        iterate(nodes, model.getRoot(), 0.0d, 1.0d, 0);

        return nodes;
    }

    private static void iterate(List<FlameNode<Node>> nodes, Node currentNode, double startX, double endX, int depth) {
        nodes.add(new FlameNode<>(currentNode, startX, endX, depth));

        depth++;

        var parentWidth = endX - startX;
        var totalWeight = currentNode.getChildren().stream().mapToDouble(Node::getCumulativeWeight).sum();
        for (Node node : currentNode.getChildren()) {
            var nodeWidth = (node.getCumulativeWeight() / totalWeight) * parentWidth;
            endX = startX + nodeWidth;
            iterate(nodes, node, startX, endX, depth);
            startX = endX;
        }
    }
}
